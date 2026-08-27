package org.soberania.app.voice

import org.soberania.app.policy.ProtectionProfile

/**
 * Voz em tempo real tem requisitos de latência e, em alguns protocolos,
 * depende de UDP. Onion/Arti não deve ser tratado como caminho de voz padrão.
 */
object VoiceProtectionCompatibility {

    enum class Compatibility {
        SUPPORTED,
        NOT_RECOMMENDED
    }

    fun forProfile(profile: ProtectionProfile): Compatibility =
        when (profile) {
            ProtectionProfile.Standard,
            ProtectionProfile.Reinforced -> Compatibility.SUPPORTED

            ProtectionProfile.Anonymous,
            ProtectionProfile.Maximum -> Compatibility.NOT_RECOMMENDED
        }
}
