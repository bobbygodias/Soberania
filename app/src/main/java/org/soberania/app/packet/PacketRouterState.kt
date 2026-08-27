package org.soberania.app.packet

/**
 * Estado observável do caminho de dados entre a TUN e o transporte.
 */
sealed interface PacketRouterState {

    data object Detached : PacketRouterState

    data object Attaching : PacketRouterState

    data class Attached(
        val path: DataPath
    ) : PacketRouterState

    data class Failed(
        val reason: String
    ) : PacketRouterState
}
