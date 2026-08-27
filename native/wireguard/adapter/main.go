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

type runningTunnel struct {
    device *device.Device
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
        // CreateUnmonitoredTUNFromFD recebe o FD, mas em caminhos de erro
        // anteriores à criação completa não há Device que possamos fechar.
        _ = unix.Close(fd)
        return -2
    }

    // Privacidade por padrão: nada de log verboso do motor.
    logger := device.NewLogger(device.LogLevelSilent, "Soberania/WireGuard")

    wgDevice := device.NewDevice(
        tunDevice,
        conn.NewStdNetBind(),
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

    handle := allocateHandle()
    if handle < 0 {
        wgDevice.Close()
        return -5
    }

    registry.Lock()
    registry.tunnels[handle] = runningTunnel{device: wgDevice}
    registry.Unlock()

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
    dev := lookupDevice(int32(handle))
    if dev == nil {
        return -1
    }

    bind, ok := dev.Bind().(conn.PeekLookAtSocketFd)
    if !ok {
        return -1
    }

    fd, err := bind.PeekLookAtSocketFd4()
    if err != nil {
        return -1
    }

    return C.int(fd)
}

//export soberaniaWgSocketV6
func soberaniaWgSocketV6(handle C.int) C.int {
    dev := lookupDevice(int32(handle))
    if dev == nil {
        return -1
    }

    bind, ok := dev.Bind().(conn.PeekLookAtSocketFd)
    if !ok {
        return -1
    }

    fd, err := bind.PeekLookAtSocketFd6()
    if err != nil {
        return -1
    }

    return C.int(fd)
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

func lookupDevice(handle int32) *device.Device {
    registry.Lock()
    tunnel, ok := registry.tunnels[handle]
    registry.Unlock()

    if !ok {
        return nil
    }

    return tunnel.device
}

func allocateHandle() int32 {
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

        if _, exists := registry.tunnels[candidate]; !exists {
            return candidate
        }
    }

    return -1
}

func main() {}
