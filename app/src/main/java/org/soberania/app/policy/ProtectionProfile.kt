package org.soberania.app.policy

import org.soberania.app.transport.TransportMode

/**
 * Perfis descrevem políticas.
 *
 * Eles não implementam criptografia nem transporte por conta própria.
 */
sealed class ProtectionProfile(
    val label: String,
    val transport: TransportMode
) {

    data object Standard : ProtectionProfile(
        label = "Padrão",
        transport = TransportMode.FAST
    )

    data object Reinforced : ProtectionProfile(
        label = "Reforçado",
        transport = TransportMode.MULTI_HOP
    )

    data object Anonymous : ProtectionProfile(
        label = "Anônimo",
        transport = TransportMode.ONION
    )

    data object Paranoid : ProtectionProfile(
        label = "Paranoico",
        transport = TransportMode.ONION
    ) {
        const val REQUIRE_KILL_SWITCH = true
        const val REQUIRE_TUNNEL_DNS = true
        const val REQUIRE_IPV4_PROTECTION = true
        const val REQUIRE_IPV6_PROTECTION = true
        const val REQUIRE_EPHEMERAL_WEB_SESSION = true
        const val REQUIRE_ZERO_PERSISTENT_LOGS = true
    }
}
