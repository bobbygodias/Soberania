package org.soberania.app.transport

/**
 * Backend orientado a conexões/streams.
 *
 * Ele não recebe a TUN diretamente. Uma StreamBridge separada converte o
 * tráfego IP da TUN em streams adequados ao backend.
 *
 * Este é o modelo esperado para Onion/Arti.
 */
interface StreamProxyBackend : TransportBackend {

    fun start(
        runtime: TransportRuntime
    ): TransportState
}
