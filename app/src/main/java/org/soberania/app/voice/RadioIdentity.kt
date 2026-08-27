package org.soberania.app.voice

/**
 * Identidade necessária para redes de radioamador.
 *
 * Estes dados devem permanecer locais por padrão. Não pertencem a analytics,
 * telemetria ou contas do Soberania.
 *
 * stationCallsign representa o indicativo da estação efetivamente operada.
 * operatorCallsign é opcional porque regulamentações podem distinguir operador
 * e estação.
 */
data class RadioIdentity(
    val stationCallsign: String,
    val operatorCallsign: String? = null,
    val radioId: String? = null
)
