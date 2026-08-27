package org.soberania.app.voice

/**
 * Contrato de alto nível para redes RoIP / radioamador.
 *
 * Implementações concretas não devem controlar diretamente o VpnService.
 * O tráfego de rede do gateway deve obedecer à política de transporte definida
 * pelo Soberania.
 */
interface VoiceGateway {

    val protocol: VoiceProtocol

    fun connect(
        destination: String,
        identity: RadioIdentity
    ): VoiceSessionState

    fun disconnect()

    fun beginTransmit(): VoiceSessionState

    fun endTransmit(): VoiceSessionState

    fun state(): VoiceSessionState
}
