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
 *
 * Isso evita transferência implícita de ownership entre Kotlin, bridges e JNI.
 */
class TunHandle(
    private val descriptor: ParcelFileDescriptor
) : Closeable {

    @Volatile
    private var closed = false

    /**
     * Cria uma nova referência para o mesmo descritor de arquivo do kernel.
     * O chamador passa a ser responsável por fechar a duplicata.
     */
    @Synchronized
    fun duplicate(): ParcelFileDescriptor {
        check(!closed) { "TUN handle is closed" }
        return ParcelFileDescriptor.dup(descriptor.fileDescriptor)
    }

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        descriptor.close()
    }

    fun isClosed(): Boolean = closed
}
