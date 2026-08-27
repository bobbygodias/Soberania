package org.soberania.app.packet.lab

import org.soberania.app.packet.IpVersion
import java.util.concurrent.atomic.AtomicLong

/**
 * Contadores exclusivamente locais e efêmeros do laboratório M0.
 *
 * Não são analytics, não são persistidos e nunca são enviados para fora
 * do processo. Servem somente para provar que o PacketRouter está recebendo
 * e classificando pacotes de teste da TUN.
 */
object TunLabCounters {

    private val ipv4Packets = AtomicLong(0)
    private val ipv6Packets = AtomicLong(0)
    private val unknownPackets = AtomicLong(0)
    private val bytes = AtomicLong(0)

    @Volatile
    private var running = false

    data class Snapshot(
        val running: Boolean,
        val ipv4Packets: Long,
        val ipv6Packets: Long,
        val unknownPackets: Long,
        val bytes: Long
    ) {
        val packets: Long
            get() = ipv4Packets + ipv6Packets + unknownPackets
    }

    fun reset() {
        ipv4Packets.set(0)
        ipv6Packets.set(0)
        unknownPackets.set(0)
        bytes.set(0)
    }

    fun markRunning(value: Boolean) {
        running = value
    }

    fun record(
        version: IpVersion,
        packetBytes: Int
    ) {
        when (version) {
            IpVersion.IPV4 -> ipv4Packets.incrementAndGet()
            IpVersion.IPV6 -> ipv6Packets.incrementAndGet()
            IpVersion.UNKNOWN -> unknownPackets.incrementAndGet()
        }

        bytes.addAndGet(packetBytes.toLong())
    }

    fun snapshot(): Snapshot =
        Snapshot(
            running = running,
            ipv4Packets = ipv4Packets.get(),
            ipv6Packets = ipv6Packets.get(),
            unknownPackets = unknownPackets.get(),
            bytes = bytes.get()
        )
}
