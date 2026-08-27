package org.soberania.app.packet.lab

import android.os.ParcelFileDescriptor
import org.soberania.app.packet.TunHandle
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Sonda M0 que lê somente a duplicata da TUN de laboratório.
 *
 * As rotas do M0 apontam apenas para redes reservadas de documentação,
 * portanto esta sonda não consome o tráfego normal do usuário.
 *
 * Ela NÃO encaminha pacotes e NÃO representa um backend de proteção.
 */
class TunLabProbe : Closeable {

    private val started = AtomicBoolean(false)

    @Volatile
    private var input: ParcelFileDescriptor.AutoCloseInputStream? = null

    @Volatile
    private var worker: Thread? = null

    @Synchronized
    fun start(tun: TunHandle): Boolean {
        if (started.get()) return true

        val duplicate = try {
            tun.duplicate()
        } catch (_: Exception) {
            return false
        }

        val stream = ParcelFileDescriptor.AutoCloseInputStream(duplicate)
        input = stream

        TunLabCounters.reset()
        TunLabCounters.markRunning(true)
        started.set(true)

        worker = Thread({
            val buffer = ByteArray(MAX_PACKET_SIZE)

            try {
                while (started.get()) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    if (count > 0) {
                        TunLabCounters.record(count)
                    }
                }
            } catch (_: IOException) {
                // Fechar a duplicata para encerrar uma leitura bloqueada é
                // comportamento esperado durante stop().
            } finally {
                started.set(false)
                TunLabCounters.markRunning(false)
                runCatching { stream.close() }
            }
        }, "Soberania-M0-TunProbe").apply {
            isDaemon = true
            start()
        }

        return true
    }

    @Synchronized
    fun stop() {
        if (!started.getAndSet(false)) {
            TunLabCounters.markRunning(false)
            return
        }

        TunLabCounters.markRunning(false)

        // AutoCloseInputStream é dono da duplicata do PFD. Fechá-lo deve
        // liberar a leitura bloqueada sem tocar no descritor TUN original.
        runCatching { input?.close() }
        input = null

        worker?.interrupt()
        worker = null
    }

    override fun close() = stop()

    companion object {
        private const val MAX_PACKET_SIZE = 65_535
    }
}
