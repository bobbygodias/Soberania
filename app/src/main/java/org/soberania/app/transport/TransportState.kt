package org.soberania.app.transport

/**
 * Estado real de um backend de transporte.
 *
 * A UI deve derivar seus indicadores deste estado, e nunca assumir
 * "protegido" apenas porque o usuário apertou um botão.
 */
sealed interface TransportState {

    data object Stopped : TransportState

    data object Starting : TransportState

    data class Ready(
        val mode: TransportMode,
        val detail: String? = null
    ) : TransportState

    data class Failed(
        val reason: String
    ) : TransportState
}
