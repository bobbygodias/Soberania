package org.soberania.app.packet

import android.os.ParcelFileDescriptor
import java.io.Closeable

/**
 * Uma duplicata da TUN com ownership explícito.
 *
 * Este objeto pertence a exatamente um consumidor. O consumidor pode:
 *
 * - fechar a duplicata normalmente; ou
 * - transferir o FD bruto para código nativo através de detachRawFd().
 *
 * Em nenhum caso este objeto representa o descritor TUN original mantido
 * pelo SoberaniaVpnService.
 */
class OwnedTunDescriptor internal constructor(
    descriptor: ParcelFileDescriptor
) : Closeable {

    private var descriptor: ParcelFileDescriptor? = descriptor

    /**
     * Transfere ownership do ParcelFileDescriptor ao chamador.
     *
     * Após esta chamada, close() deste wrapper não fecha mais o PFD retornado.
     */
    @Synchronized
    fun takeParcelFileDescriptor(): ParcelFileDescriptor {
        val owned = descriptor
            ?: error("TUN descriptor ownership has already been transferred")
        descriptor = null
        return owned
    }

    /**
     * Transfere ownership do FD bruto ao chamador/código nativo.
     *
     * O receptor passa a ser responsável por fechar o FD.
     */
    @Synchronized
    fun detachRawFd(): Int {
        val owned = takeParcelFileDescriptor()
        return owned.detachFd()
    }

    @Synchronized
    override fun close() {
        val owned = descriptor ?: return
        descriptor = null
        owned.close()
    }

    @Synchronized
    fun hasOwnership(): Boolean = descriptor != null
}
