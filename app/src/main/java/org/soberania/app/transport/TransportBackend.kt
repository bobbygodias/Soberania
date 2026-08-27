package org.soberania.app.transport

/**
 * Contrato comum para motores de transporte do Soberania.
 *
 * O método de inicialização é deliberadamente definido nos subtipos:
 *
 * - PacketTunnelBackend recebe uma duplicata da TUN;
 * - StreamProxyBackend recebe somente o runtime, enquanto uma StreamBridge
 *   separada trata a TUN.
 *
 * Isso impede que um backend de pacote e um backend de stream finjam ter a
 * mesma relação com o caminho de dados.
 */
interface TransportBackend {

    val mode: TransportMode

    val kind: TransportKind

    fun stop()

    fun state(): TransportState
}
