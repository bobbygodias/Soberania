package org.soberania.app.transport

/**
 * Contrato comum para motores de transporte do Soberania.
 *
 * O VpnService não deve conhecer detalhes de WireGuard, multi-hop ou Arti.
 *
 * Atenção: nem todo backend consome pacotes IP diretamente. Backends do tipo
 * STREAM_PROXY, como o caminho Onion/Arti planejado, precisam de uma ponte
 * entre a TUN e conexões/streams.
 */
interface TransportBackend {

    val mode: TransportMode

    val kind: TransportKind

    fun start(): TransportState

    fun stop()

    fun state(): TransportState
}
