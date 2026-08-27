package org.soberania.app.transport.wireguard

import org.soberania.app.packet.OwnedTunDescriptor
import org.soberania.app.transport.PacketTunnelBackend
import org.soberania.app.transport.TransportKind
import org.soberania.app.transport.TransportMode
import org.soberania.app.transport.TransportRuntime
import org.soberania.app.transport.TransportState

/**
 * Backend WireGuard do Nível 1.
 *
 * Estado atual: arquitetura pronta, motor nativo ainda não conectado.
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
        tun: OwnedTunDescriptor,
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
            /*
             * Ownership passa ao motor nativo.
             * O contrato do engine exige fechamento do FD em qualquer saída
             * de turnOn(): sucesso, código negativo ou exceção.
             */
            tun.detachRawFd()
        } catch (exception: Exception) {
            tun.close()
            return fail(
                "Falha ao transferir TUN para WireGuard: " +
                    exception.javaClass.simpleName
            )
        }

        val newHandle = try {
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

        val protectedV4 = protectIfPresent(
            fd = engine.socketV4(newHandle),
            runtime = runtime
        )

        val protectedV6 = protectIfPresent(
            fd = engine.socketV6(newHandle),
            runtime = runtime
        )

        if (!protectedV4 || !protectedV6) {
            engine.turnOff(newHandle)
            handle = NO_HANDLE
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

    private fun protectIfPresent(
        fd: Int,
        runtime: TransportRuntime
    ): Boolean {
        if (fd < 0) {
            return true
        }

        return runtime.protectSocket(fd)
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
