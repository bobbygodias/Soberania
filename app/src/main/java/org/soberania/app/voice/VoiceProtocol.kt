package org.soberania.app.voice

/**
 * Protocolos de voz planejados.
 *
 * M17 entra primeiro por alinhar-se melhor com a filosofia open source do
 * Soberania e por usar Codec2, que possui implementação livre.
 */
enum class VoiceProtocol(
    val label: String,
    val initialPriority: Int
) {
    M17(
        label = "M17",
        initialPriority = 1
    ),

    ALLSTAR_IAX2(
        label = "AllStar / IAX2",
        initialPriority = 2
    ),

    ECHOLINK(
        label = "EchoLink",
        initialPriority = 3
    )
}
