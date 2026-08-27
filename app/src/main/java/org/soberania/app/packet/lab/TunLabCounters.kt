package org.soberania.app.packet.lab

import java.util.concurrent.atomic.AtomicLong

/**
 * Contadores exclusivamente locais e efêmeros do laboratório M0.
 *
 * Não são analytics, não são persistidos e nunca são enviados para fora
 * do processo. Servem somente para provar que a duplicata da TUN está
 * recebendo pacotes de teste.
 */
object TunLabCounters {

    private val packets = AtomicLong(0)
    private val bytes = AtomicLong(0)

    @Volatile
    private var running = false

    data class Snapshot(
        val running: Boolean,
        val packets: Long,
        val bytes: Long
    )

    fun reset() {
        packets.set(0)
        bytes.set(0)
    }

    fun markRunning(value: Boolean) {
        running = value
    }

    fun record(packetBytes: Int) {
        packets.incrementAndGet()
        bytes.addAndGet(packetBytes.toLong())
    }

    fun snapshot(): Snapshot =
        Snapshot(
            running = running,
            packets = packets.get(),
            bytes = bytes.get()
        )
}
