package org.soberania.app.packet

import android.os.ParcelFileDescriptor
import java.io.Closeable

/**
 * Dono explícito do ParcelFileDescriptor original da TUN.
 *
 * Regra:
 * - o SoberaniaVpnService mantém o handle original;
 * - consumidores recebem descritores duplicados;
 * - cada consumidor fecha somente a sua própria duplicata.
 */
class TunHandle(
    private val descriptor: ParcelFileDescriptor
) : Closeable {

    @Volatile
    private var closed = false

    /**
     * Duplicata simples para consumidores que mantêm o PFD em Kotlin/Java.
     */
    @Synchronized
    fun duplicate(): ParcelFileDescriptor {
        check(!closed) { "TUN handle is closed" }
        return ParcelFileDescriptor.dup(descriptor.fileDescriptor)
    }

    /**
     * Duplicata com ownership explícito para backends que podem transferir
     * o FD para código nativo.
     */
    @Synchronized
    fun duplicateOwned(): OwnedTunDescriptor {
        check(!closed) { "TUN handle is closed" }
        return OwnedTunDescriptor(
            ParcelFileDescriptor.dup(descriptor.fileDescriptor)
        )
    }

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        descriptor.close()
    }

    fun isClosed(): Boolean = closed
}
