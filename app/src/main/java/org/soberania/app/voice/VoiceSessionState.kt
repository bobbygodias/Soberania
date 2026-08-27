package org.soberania.app.voice

sealed interface VoiceSessionState {

    data object Idle : VoiceSessionState

    data object Connecting : VoiceSessionState

    data class Ready(
        val protocol: VoiceProtocol,
        val remoteLabel: String
    ) : VoiceSessionState

    data class Receiving(
        val protocol: VoiceProtocol,
        val remoteLabel: String
    ) : VoiceSessionState

    data class Transmitting(
        val protocol: VoiceProtocol,
        val remoteLabel: String
    ) : VoiceSessionState

    data class Failed(
        val reason: String
    ) : VoiceSessionState
}
