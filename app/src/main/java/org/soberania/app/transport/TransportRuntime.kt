package org.soberania.app.transport

/**
 * Capacidades mínimas que um transporte pode pedir ao ambiente Android.
 *
 * O backend não recebe acesso direto ao VpnService. Isso reduz acoplamento e
 * impede que uma implementação de transporte ganhe poderes que não precisa.
 */
fun interface TransportRuntime {

    /**
     * Retira o socket do roteamento da própria VPN para evitar loop.
     *
     * Exemplo: o socket UDP de um túnel deve sair pela rede subjacente, e não
     * voltar para a TUN que ele próprio está transportando.
     */
    fun protectSocket(socketFd: Int): Boolean
}
