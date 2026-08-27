package org.soberania.app.packet

import org.soberania.app.transport.TransportBackend
import org.soberania.app.transport.TransportKind

/**
 * Orquestra o caminho de dados entre a TUN e o backend escolhido.
 *
 * Ainda não encaminha pacotes no M0. Este contrato existe para impedir que
 * detalhes de WireGuard, tun2socks ou Arti vazem para o VpnService.
 */
interface PacketRouter {

    fun attach(
        tun: TunHandle,
        backend: TransportBackend
    ): PacketRouterState

    fun detach()

    fun state(): PacketRouterState

    companion object {
        fun requiredPathFor(backend: TransportBackend): DataPath =
            when (backend.kind) {
                TransportKind.PACKET_TUNNEL -> DataPath.PACKET_DIRECT
                TransportKind.STREAM_PROXY -> DataPath.TUN_TO_STREAM
            }
    }
}
