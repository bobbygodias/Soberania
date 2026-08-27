package org.soberania.app.transport

import org.soberania.app.packet.OwnedTunDescriptor

/**
 * Backend que consome diretamente uma TUN.
 *
 * Ownership:
 * ao entrar em start(), o backend recebe ownership de 'tun' e deve liberar a
 * duplicata mesmo quando a inicialização falhar.
 */
interface PacketTunnelBackend : TransportBackend {

    fun start(
        tun: OwnedTunDescriptor,
        runtime: TransportRuntime
    ): TransportState
}
