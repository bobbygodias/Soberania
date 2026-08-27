package org.soberania.app.transport.wireguard.lab

/**
 * Probe LAB para consultar se um número de FD ainda está aberto no processo.
 *
 * Existe somente no source set wireguardLab.
 */
object LabNativeFdProbe {

    private val nativeLibraryLoaded: Boolean =
        runCatching {
            System.loadLibrary("soberania-wg")
            true
        }.getOrDefault(false)

    fun isOpen(fd: Int): Boolean {
        check(nativeLibraryLoaded) {
            "Soberania WireGuard JNI unavailable"
        }

        return nativeIsFdOpen(fd)
    }

    private external fun nativeIsFdOpen(fd: Int): Boolean
}
