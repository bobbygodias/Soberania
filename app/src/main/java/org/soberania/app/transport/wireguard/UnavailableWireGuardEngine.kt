package org.soberania.app.transport.wireguard

/**
 * Implementação sentinela para builds onde o motor nativo WireGuard ainda
 * não foi empacotado.
 *
 * Evita qualquer impressão de que WireGuard está funcional antes da
 * integração nativa real.
 */
object UnavailableWireGuardEngine : WireGuardNativeEngine {

    override fun turnOn(
        interfaceName: String,
        tunFd: Int,
        userspaceConfig: String
    ): Int = UNAVAILABLE

    override fun turnOff(handle: Int) = Unit

    override fun socketV4(handle: Int): Int = UNAVAILABLE

    override fun socketV6(handle: Int): Int = UNAVAILABLE

    override fun version(): String? = null

    private const val UNAVAILABLE = -1
}
