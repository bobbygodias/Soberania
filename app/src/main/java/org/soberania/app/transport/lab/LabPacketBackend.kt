package org.soberania.app.transport.lab

import org.soberania.app.transport.TransportBackend
import org.soberania.app.transport.TransportKind
import org.soberania.app.transport.TransportMode
import org.soberania.app.transport.TransportState

/**
 * Backend marcador exclusivamente para M0.
 *
 * Não cria socket, não criptografa e não alcança a Internet.
 * Existe somente para exercitar o contrato PacketRouter -> TransportBackend
 * antes de um backend real entrar no projeto.
 */
class LabPacketBackend : TransportBackend {

    override val mode: TransportMode = TransportMode.FAST

    override val kind: TransportKind = TransportKind.PACKET_TUNNEL

    @Volatile
    private var currentState: TransportState = TransportState.Stopped

    override fun start(): TransportState {
        currentState = TransportState.Ready(
            mode = mode,
            detail = "LAB ONLY — sem transporte de rede"
        )
        return currentState
    }

    override fun stop() {
        currentState = TransportState.Stopped
    }

    override fun state(): TransportState = currentState
}
