package org.soberania.app.transport

/**
 * Transportes disponíveis no Soberania.
 *
 * Importante: PARANOICO não aparece aqui porque não é um transporte.
 * É uma política composta que escolhe e endurece várias camadas ao mesmo tempo.
 */
enum class TransportMode(
    val label: String,
    val description: String
) {
    FAST(
        label = "Rápido",
        description = "Baixa latência para uso diário."
    ),

    MULTI_HOP(
        label = "Reforçado",
        description = "Encaminhamento por mais de um ponto para aumentar isolamento."
    ),

    ONION(
        label = "Anônimo",
        description = "Roteamento por cebola através de uma rede de anonimato."
    )
}
