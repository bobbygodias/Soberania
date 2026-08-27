//go:build android

package main

/*
#include <stdlib.h>
*/
import "C"

import (
    "runtime/debug"
    "sync"

    "golang.org/x/sys/unix"
    "golang.zx2c4.com/wireguard/conn"
    "golang.zx2c4.com/wireguard/device"
    "golang.zx2c4.com/wireguard/tun"
)

const (
    socketInvalidHandle     = -1001
    socketUnsupportedBind   = -1002
    socketFamilyUnavailable = -1003
    socketLookupError       = -1004
)

/*
 * trackedBind não altera o transporte WireGuard.
 * Ele apenas registra quais famílias o StdNetBind conseguiu abrir para que
 * um socket ausente não seja confundido com erro de lookup no limite JNI.
 */
type trackedBind struct {
    conn.Bind

    mu    sync.RWMutex
    hasV4 bool
    hasV6 bool
}

func newTrackedBind() *trackedBind {
    return &trackedBind{
        Bind: conn.NewStdNetBind(),
    }
}

func (b *trackedBind) Open(port uint16) ([]conn.ReceiveFunc, uint16, error) {
    receive, actualPort, err := b.Bind.Open(port)

    b.mu.Lock()
    b.hasV4 = false
    b.hasV6 = false

    if err == nil {
        for _, fn := range receive {
            switch fn.PrettyName() {
            case "v4":
                b.hasV4 = true
            case "v6":
                b.hasV6 = true
            }
        }
    }
    b.mu.Unlock()

    return receive, actualPort, err
}

func (b *trackedBind) Close() error {
    err := b.Bind.Close()

    b.mu.Lock()
    b.hasV4 = false
    b.hasV6 = false
    b.mu.Unlock()

    return err
}

func (b *trackedBind) socketFD(ipv6 bool) int {
    b.mu.RLock()
    familyOpen := b.hasV4
    if ipv6 {
        familyOpen = b.hasV6
    }
    b.mu.RUnlock()

    if !familyOpen {
        return socketFamilyUnavailable
    }

    peek, ok := b.Bind.(conn.PeekLookAtSocketFd)
    if !ok {
        return socketUnsupportedBind
    }

    var (
        fd  int
        err error
    )

    if ipv6 {
        fd, err = peek.PeekLookAtSocketFd6()
    } else {
        fd, err = peek.PeekLookAtSocketFd4()
    }

    if err != nil || fd < 0 {
        return socketLookupError
    }

    return fd
}

type runningTunnel struct {
    device *device.Device
    bind   *trackedBind
}

var registry = struct {
    sync.Mutex
    next    int32
    tunnels map[int32]runningTunnel
}{
    next:    1,
    tunnels: make(map[int32]runningTunnel),
}

//export soberaniaWgTurnOn
func soberaniaWgTurnOn(
    interfaceName *C.char,
    tunFD C.int,
    settings *C.char,
) C.int {
    fd := int(tunFD)

    if fd < 0 || settings == nil {
        if fd >= 0 {
            _ = unix.Close(fd)
        }
        return -1
    }

    tunDevice, _, err := tun.CreateUnmonitoredTUNFromFD(fd)
    if err != nil {
        /*
         * Nesta falha ainda não temos um Device completo que possa assumir
         * o teardown. O FD já pertence ao adaptador, então fechamos aqui.
         */
        _ = unix.Close(fd)
        return -2
    }

    /*
     * Zero telemetria e zero histórico por padrão:
     * o logger interno do motor permanece silencioso.
     */
    logger := device.NewLogger(
        device.LogLevelSilent,
        "Soberania/WireGuard",
    )

    bind := newTrackedBind()

    wgDevice := device.NewDevice(
        tunDevice,
        bind,
        logger,
    )

    if err := wgDevice.IpcSet(C.GoString(settings)); err != nil {
        wgDevice.Close()
        return -3
    }

    wgDevice.DisableSomeRoamingForBrokenMobileSemantics()

    if err := wgDevice.Up(); err != nil {
        wgDevice.Close()
        return -4
    }

    handle := registerDevice(wgDevice, bind)
    if handle < 0 {
        wgDevice.Close()
        return -5
    }

    return C.int(handle)
}

//export soberaniaWgTurnOff
func soberaniaWgTurnOff(handle C.int) {
    h := int32(handle)

    registry.Lock()
    tunnel, ok := registry.tunnels[h]
    if ok {
        delete(registry.tunnels, h)
    }
    registry.Unlock()

    if ok {
        tunnel.device.Close()
    }
}

//export soberaniaWgSocketV4
func soberaniaWgSocketV4(handle C.int) C.int {
    tunnel, ok := lookupTunnel(int32(handle))
    if !ok {
        return C.int(socketInvalidHandle)
    }

    return C.int(tunnel.bind.socketFD(false))
}

//export soberaniaWgSocketV6
func soberaniaWgSocketV6(handle C.int) C.int {
    tunnel, ok := lookupTunnel(int32(handle))
    if !ok {
        return C.int(socketInvalidHandle)
    }

    return C.int(tunnel.bind.socketFD(true))
}

//export soberaniaWgVersion
func soberaniaWgVersion() *C.char {
    info, ok := debug.ReadBuildInfo()
    if !ok {
        return C.CString("unknown")
    }

    for _, dependency := range info.Deps {
        if dependency.Path == "golang.zx2c4.com/wireguard" {
            return C.CString(dependency.Version)
        }
    }

    return C.CString("unknown")
}

func lookupTunnel(handle int32) (runningTunnel, bool) {
    registry.Lock()
    tunnel, ok := registry.tunnels[handle]
    registry.Unlock()

    return tunnel, ok
}

/*
 * Aloca E registra o handle sob o mesmo lock.
 * Isso evita que dois starts concorrentes recebam o mesmo número.
 */
func registerDevice(
    wgDevice *device.Device,
    bind *trackedBind,
) int32 {
    registry.Lock()
    defer registry.Unlock()

    const maxHandle = int32(^uint32(0) >> 1)

    for attempts := int64(0); attempts < int64(maxHandle); attempts++ {
        candidate := registry.next
        registry.next++

        if registry.next <= 0 {
            registry.next = 1
        }

        if candidate <= 0 {
            continue
        }

        if _, exists := registry.tunnels[candidate]; exists {
            continue
        }

        registry.tunnels[candidate] = runningTunnel{
            device: wgDevice,
            bind:   bind,
        }

        return candidate
    }

    return -1
}

func main() {}
