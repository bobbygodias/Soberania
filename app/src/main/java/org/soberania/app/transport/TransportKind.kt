package org.soberania.app.transport

/**
 * Nem todo transporte recebe a mesma forma de tráfego.
 *
 * PACKET_TUNNEL: trabalha naturalmente com tráfego IP/túnel.
 * STREAM_PROXY: trabalha com conexões/streams e exige uma ponte TUN -> stream.
 */
enum class TransportKind {
    PACKET_TUNNEL,
    STREAM_PROXY
}
