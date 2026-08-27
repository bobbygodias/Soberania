package org.soberania.app.transport.wireguard

import org.soberania.app.packet.OwnedTun
import org.soberania.app.transport.PacketTunnelBackend
import org.soberania.app.transport.TransportKind
import org.soberania.app.transport.TransportMode
import org.soberania.app.transport.TransportRuntime
import org.soberania.app.transport.TransportState

/**
 * Backend WireGuard do Nível 1.
 *
 * O backend recebe ownership de uma DUPLICATA da TUN. O descritor original
 * continua pertencendo ao SoberaniaVpnService.
 */
class WireGuardPacketBackend(
    private val config: WireGuardConfig,
    private val engine: WireGuardNativeEngine
) : PacketTunnelBackend {

    override val mode: TransportMode = TransportMode.FAST

    override val kind: TransportKind = TransportKind.PACKET_TUNNEL

    @Volatile
    private var currentState: TransportState = TransportState.Stopped

    @Volatile
    private var handle: Int = NO_HANDLE

    @Synchronized
    override fun start(
        tun: OwnedTun,
        runtime: TransportRuntime
    ): TransportState {
        if (handle != NO_HANDLE) {
            tun.close()
            return currentState
        }

        currentState = TransportState.Starting

        if (!engine.isAvailable()) {
            tun.close()
            return fail("Motor WireGuard nativo indisponível")
        }

        val rawFd = try {
            tun.detachRawFd()
        } catch (exception: Exception) {
            tun.close()
            return fail(
                "Falha ao transferir TUN para WireGuard: " +
                    exception.javaClass.simpleName
            )
        }

        val newHandle = try {
            /*
             * A partir desta chamada o engine possui rawFd e deve fechá-lo
             * em qualquer resultado: sucesso, código negativo ou exceção.
             */
            engine.turnOn(
                interfaceName = config.interfaceName,
                tunFd = rawFd,
                userspaceConfig = config.userspaceConfig
            )
        } catch (exception: Exception) {
            return fail(
                "Falha no motor WireGuard: " +
                    exception.javaClass.simpleName
            )
        }

        if (newHandle < 0) {
            return fail("Motor WireGuard recusou a inicialização")
        }

        handle = newHandle

        val socketV4 = engine.socketV4(newHandle)
        val socketV6 = engine.socketV6(newHandle)

        if (socketV4 < 0 || socketV6 < 0) {
            stopNativeHandle(newHandle)
            return fail(
                "Socket WireGuard indisponível: " +
                    "IPv4=${describeSocketResult(socketV4)}, " +
                    "IPv6=${describeSocketResult(socketV6)}"
            )
        }

        val protectedV4 = runtime.protectSocket(socketV4)
        val protectedV6 = runtime.protectSocket(socketV6)

        if (!protectedV4 || !protectedV6) {
            stopNativeHandle(newHandle)
            return fail("Falha ao proteger sockets do transporte WireGuard")
        }

        currentState = TransportState.Ready(
            mode = mode,
            detail = "WireGuard userspace ativo"
        )

        return currentState
    }

    @Synchronized
    override fun stop() {
        val currentHandle = handle
        handle = NO_HANDLE

        if (currentHandle != NO_HANDLE) {
            runCatching {
                engine.turnOff(currentHandle)
            }
        }

        currentState = TransportState.Stopped
    }

    override fun state(): TransportState = currentState

    private fun stopNativeHandle(nativeHandle: Int) {
        runCatching {
            engine.turnOff(nativeHandle)
        }
        handle = NO_HANDLE
    }

    private fun describeSocketResult(value: Int): String = when (value) {
        WireGuardNativeEngine.SOCKET_INVALID_HANDLE -> "handle-inválido"
        WireGuardNativeEngine.SOCKET_UNSUPPORTED_BIND -> "bind-sem-inspeção"
        WireGuardNativeEngine.SOCKET_FAMILY_UNAVAILABLE -> "família-indisponível"
        WireGuardNativeEngine.SOCKET_LOOKUP_ERROR -> "erro-de-lookup"
        else -> if (value >= 0) "fd-$value" else "erro-$value"
    }

    private fun fail(reason: String): TransportState {
        val failed = TransportState.Failed(reason)
        currentState = failed
        return failed
    }

    companion object {
        private const val NO_HANDLE = -1
    }
}
