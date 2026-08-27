package org.soberania.app.transport.wireguard

/**
 * Implementação JNI do contrato WireGuardNativeEngine.
 *
 * O módulo nativo ainda NÃO está ligado ao build Android. Esta classe é
 * deliberadamente dormente: se a biblioteca não estiver empacotada,
 * isAvailable() retorna false e nenhum FD deve ser transferido a ela.
 */
class JniWireGuardNativeEngine : WireGuardNativeEngine {

    override fun isAvailable(): Boolean = nativeLibraryLoaded

    override fun turnOn(
        interfaceName: String,
        tunFd: Int,
        userspaceConfig: String
    ): Int {
        check(nativeLibraryLoaded) {
            "Soberania WireGuard native library is unavailable"
        }

        return nativeTurnOn(
            interfaceName,
            tunFd,
            userspaceConfig
        )
    }

    override fun turnOff(handle: Int) {
        if (nativeLibraryLoaded) {
            nativeTurnOff(handle)
        }
    }

    override fun socketV4(handle: Int): Int =
        if (nativeLibraryLoaded) nativeSocketV4(handle) else -1

    override fun socketV6(handle: Int): Int =
        if (nativeLibraryLoaded) nativeSocketV6(handle) else -1

    override fun version(): String? =
        if (nativeLibraryLoaded) nativeVersion() else null

    private external fun nativeTurnOn(
        interfaceName: String,
        tunFd: Int,
        userspaceConfig: String
    ): Int

    private external fun nativeTurnOff(handle: Int)

    private external fun nativeSocketV4(handle: Int): Int

    private external fun nativeSocketV6(handle: Int): Int

    private external fun nativeVersion(): String?

    companion object {
        private val nativeLibraryLoaded: Boolean =
            runCatching {
                System.loadLibrary("soberania-wg")
                true
            }.getOrDefault(false)
    }
}
