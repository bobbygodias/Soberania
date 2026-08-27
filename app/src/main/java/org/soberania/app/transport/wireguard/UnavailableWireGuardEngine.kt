package org.soberania.app.transport.wireguard

import android.os.ParcelFileDescriptor

/**
 * Implementação sentinela para builds onde o motor nativo WireGuard ainda
 * não foi empacotado.
 *
 * Evita qualquer impressão de que WireGuard está funcional antes da
 * integração nativa real.
 */
object UnavailableWireGuardEngine : WireGuardNativeEngine {

    override fun isAvailable(): Boolean = false

    override fun turnOn(
        interfaceName: String,
        tunFd: Int,
        userspaceConfig: String
    ): Int {
        /*
         * O backend não deve chegar aqui porque isAvailable() é false.
         * Ainda assim cumprimos o contrato de ownership: se alguém transferiu
         * um FD para esta implementação, ele é fechado aqui.
         */
        runCatching {
            ParcelFileDescriptor.adoptFd(tunFd).close()
        }

        return UNAVAILABLE
    }

    override fun turnOff(handle: Int) = Unit

    override fun socketV4(handle: Int): Int = UNAVAILABLE

    override fun socketV6(handle: Int): Int = UNAVAILABLE

    override fun version(): String? = null

    private const val UNAVAILABLE = -1
}
