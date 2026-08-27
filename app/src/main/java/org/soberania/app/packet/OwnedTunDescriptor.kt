package org.soberania.app.packet

import android.os.ParcelFileDescriptor

/**
 * Implementação Android de uma duplicata da TUN com ownership explícito.
 *
 * Este objeto pertence a exatamente um consumidor.
 */
class OwnedTunDescriptor internal constructor(
    descriptor: ParcelFileDescriptor
) : OwnedTun {

    private var descriptor: ParcelFileDescriptor? = descriptor

    /**
     * Transfere ownership do ParcelFileDescriptor ao chamador.
     *
     * Usado apenas por consumidores Android que precisam do PFD em vez do
     * número bruto do FD.
     */
    @Synchronized
    fun takeParcelFileDescriptor(): ParcelFileDescriptor {
        val owned = descriptor
            ?: error("TUN descriptor ownership has already been transferred")
        descriptor = null
        return owned
    }

    @Synchronized
    override fun detachRawFd(): Int {
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
    override fun hasOwnership(): Boolean = descriptor != null
}
