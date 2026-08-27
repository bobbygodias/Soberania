package org.soberania.app.policy

import org.soberania.app.transport.TransportMode

/**
 * Perfis descrevem políticas de proteção.
 *
 * "Nível" é linguagem de interface. O transporte continua sendo uma
 * implementação técnica separada.
 */
sealed class ProtectionProfile(
    val level: Int,
    val label: String,
    val transport: TransportMode
) {

    data object Standard : ProtectionProfile(
        level = 1,
        label = "Proteção",
        transport = TransportMode.FAST
    )

    data object Reinforced : ProtectionProfile(
        level = 2,
        label = "Reforçada",
        transport = TransportMode.MULTI_HOP
    )

    data object Anonymous : ProtectionProfile(
        level = 3,
        label = "Anônima",
        transport = TransportMode.ONION
    )

    data object Maximum : ProtectionProfile(
        level = 4,
        label = "Máxima",
        transport = TransportMode.ONION
    ) {
        const val REQUIRE_KILL_SWITCH = true
        const val REQUIRE_TUNNEL_DNS = true
        const val REQUIRE_IPV4_PROTECTION = true
        const val REQUIRE_IPV6_PROTECTION = true
        const val REQUIRE_EPHEMERAL_WEB_SESSION = true
        const val REQUIRE_ZERO_PERSISTENT_LOGS = true
    }

    fun displayName(): String = "Nível $level — $label"
}
