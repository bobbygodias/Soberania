package org.soberania.app.packet

import org.soberania.app.transport.TransportBackend
import org.soberania.app.transport.TransportKind
import org.soberania.app.transport.TransportRuntime

/**
 * Orquestra o caminho de dados entre a TUN e o backend escolhido.
 *
 * PACKET_TUNNEL:
 * o router transfere uma duplicata com ownership explícito ao backend e sai
 * do caminho de dados.
 *
 * STREAM_PROXY:
 * uma StreamBridge separada deverá consumir a TUN e alimentar o backend.
 */
interface PacketRouter {

    fun attach(
        tun: TunHandle,
        backend: TransportBackend,
        runtime: TransportRuntime
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
