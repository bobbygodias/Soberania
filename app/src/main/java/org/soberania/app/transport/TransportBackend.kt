package org.soberania.app.transport

/**
 * Contrato comum para qualquer motor de transporte do Soberania.
 *
 * O VpnService não deve conhecer detalhes de WireGuard, multi-hop ou Arti.
 * Ele conhece apenas este contrato.
 *
 * A passagem efetiva do descritor TUN será definida quando o primeiro backend
 * real for integrado; este contrato evita acoplamento prematuro.
 */
interface TransportBackend {

    val mode: TransportMode

    fun start(): TransportState

    fun stop()

    fun state(): TransportState
}
