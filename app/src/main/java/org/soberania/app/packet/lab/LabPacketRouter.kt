package org.soberania.app.packet.lab

import org.soberania.app.packet.DataPath
import org.soberania.app.packet.PacketRouter
import org.soberania.app.packet.PacketRouterState
import org.soberania.app.packet.TunHandle
import org.soberania.app.transport.PacketTunnelBackend
import org.soberania.app.transport.StreamProxyBackend
import org.soberania.app.transport.TransportBackend
import org.soberania.app.transport.TransportKind
import org.soberania.app.transport.TransportRuntime
import org.soberania.app.transport.TransportState
import java.io.Closeable

/**
 * Primeiro PacketRouter executável do projeto.
 *
 * No caminho PACKET_DIRECT ele não lê pacotes: entrega uma duplicata da TUN
 * ao PacketTunnelBackend, reproduzindo o modelo necessário a backends como
 * wireguard-go.
 *
 * O caminho TUN_TO_STREAM permanece não implementado no M0.
 */
class LabPacketRouter : PacketRouter, Closeable {

    @Volatile
    private var currentState: PacketRouterState = PacketRouterState.Detached

    @Volatile
    private var activeBackend: TransportBackend? = null

    @Synchronized
    override fun attach(
        tun: TunHandle,
        backend: TransportBackend,
        runtime: TransportRuntime
    ): PacketRouterState {
        if (currentState is PacketRouterState.Attached) {
            return currentState
        }

        currentState = PacketRouterState.Attaching

        return when (backend.kind) {
            TransportKind.PACKET_TUNNEL ->
                attachPacketBackend(tun, backend, runtime)

            TransportKind.STREAM_PROXY ->
                attachStreamBackend(backend)
        }
    }

    @Synchronized
    override fun detach() {
        runCatching { activeBackend?.stop() }
        activeBackend = null
        currentState = PacketRouterState.Detached
    }

    override fun state(): PacketRouterState = currentState

    override fun close() = detach()

    private fun attachPacketBackend(
        tun: TunHandle,
        backend: TransportBackend,
        runtime: TransportRuntime
    ): PacketRouterState {
        val packetBackend = backend as? PacketTunnelBackend
            ?: return fail("Backend PACKET_TUNNEL não implementa PacketTunnelBackend")

        val ownedTun = try {
            tun.duplicateOwned()
        } catch (exception: Exception) {
            return fail(
                "Falha ao duplicar a TUN: " +
                    exception.javaClass.simpleName
            )
        }

        val transportState = try {
            // A partir desta chamada, ownership de ownedTun pertence ao backend.
            packetBackend.start(
                tun = ownedTun,
                runtime = runtime
            )
        } catch (exception: Exception) {
            // O contrato exige que o backend libere ownedTun mesmo ao falhar.
            return fail(
                "Falha ao iniciar backend de pacote: " +
                    exception.javaClass.simpleName
            )
        }

        if (transportState !is TransportState.Ready) {
            return fail("Backend de pacote não ficou pronto")
        }

        activeBackend = backend
        currentState = PacketRouterState.Attached(DataPath.PACKET_DIRECT)
        return currentState
    }

    private fun attachStreamBackend(
        backend: TransportBackend
    ): PacketRouterState {
        if (backend !is StreamProxyBackend) {
            return fail("Backend STREAM_PROXY não implementa StreamProxyBackend")
        }

        // O stream bridge ainda não existe no M0.
        return fail("TUN_TO_STREAM ainda não implementado")
    }

    private fun fail(reason: String): PacketRouterState {
        runCatching { activeBackend?.stop() }
        activeBackend = null

        val failed = PacketRouterState.Failed(reason)
        currentState = failed
        return failed
    }
}
