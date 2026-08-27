package org.soberania.app.packet.lab

import android.os.ParcelFileDescriptor
import org.soberania.app.packet.IpPacketInspector
import org.soberania.app.packet.IpVersion
import org.soberania.app.packet.PacketRouter
import org.soberania.app.packet.PacketRouterState
import org.soberania.app.packet.TunHandle
import org.soberania.app.transport.TransportBackend
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Primeiro PacketRouter executável do projeto.
 *
 * LAB ONLY:
 * - recebe uma duplicata da TUN;
 * - identifica IPv4 / IPv6;
 * - mantém apenas contadores efêmeros;
 * - valida a escolha do DataPath segundo o tipo de backend;
 * - NÃO encaminha tráfego;
 * - NÃO representa proteção.
 */
class LabPacketRouter : PacketRouter, Closeable {

    private val running = AtomicBoolean(false)

    private val ipv4Packets = AtomicLong(0)
    private val ipv6Packets = AtomicLong(0)
    private val unknownPackets = AtomicLong(0)
    private val totalBytes = AtomicLong(0)

    @Volatile
    private var currentState: PacketRouterState = PacketRouterState.Detached

    @Volatile
    private var input: ParcelFileDescriptor.AutoCloseInputStream? = null

    @Volatile
    private var worker: Thread? = null

    data class Snapshot(
        val running: Boolean,
        val state: PacketRouterState,
        val ipv4Packets: Long,
        val ipv6Packets: Long,
        val unknownPackets: Long,
        val totalBytes: Long
    )

    @Synchronized
    override fun attach(
        tun: TunHandle,
        backend: TransportBackend
    ): PacketRouterState {
        if (running.get()) {
            return currentState
        }

        currentState = PacketRouterState.Attaching

        val duplicate = try {
            tun.duplicate()
        } catch (exception: Exception) {
            return fail("Falha ao duplicar a TUN: " + exception.javaClass.simpleName)
        }

        val path = PacketRouter.requiredPathFor(backend)

        resetCounters()

        val stream = ParcelFileDescriptor.AutoCloseInputStream(duplicate)
        input = stream
        running.set(true)
        currentState = PacketRouterState.Attached(path)

        worker = Thread({
            val buffer = ByteArray(MAX_PACKET_SIZE)

            try {
                while (running.get()) {
                    val count = stream.read(buffer)

                    if (count < 0) {
                        break
                    }

                    if (count == 0) {
                        continue
                    }

                    totalBytes.addAndGet(count.toLong())

                    when (IpPacketInspector.version(buffer, count)) {
                        IpVersion.IPV4 -> ipv4Packets.incrementAndGet()
                        IpVersion.IPV6 -> ipv6Packets.incrementAndGet()
                        IpVersion.UNKNOWN -> unknownPackets.incrementAndGet()
                    }
                }
            } catch (_: IOException) {
                // Fechar a duplicata para interromper read() durante detach()
                // é comportamento normal.
            } finally {
                running.set(false)
                runCatching { stream.close() }

                if (currentState !is PacketRouterState.Failed) {
                    currentState = PacketRouterState.Detached
                }
            }
        }, "Soberania-M0-PacketRouter").apply {
            isDaemon = true
            start()
        }

        return currentState
    }

    @Synchronized
    override fun detach() {
        running.set(false)

        runCatching { input?.close() }
        input = null

        worker?.interrupt()
        worker = null

        if (currentState !is PacketRouterState.Failed) {
            currentState = PacketRouterState.Detached
        }
    }

    override fun state(): PacketRouterState = currentState

    fun snapshot(): Snapshot =
        Snapshot(
            running = running.get(),
            state = currentState,
            ipv4Packets = ipv4Packets.get(),
            ipv6Packets = ipv6Packets.get(),
            unknownPackets = unknownPackets.get(),
            totalBytes = totalBytes.get()
        )

    override fun close() = detach()

    private fun resetCounters() {
        ipv4Packets.set(0)
        ipv6Packets.set(0)
        unknownPackets.set(0)
        totalBytes.set(0)
    }

    private fun fail(reason: String): PacketRouterState {
        running.set(false)
        val failed = PacketRouterState.Failed(reason)
        currentState = failed
        return failed
    }

    companion object {
        private const val MAX_PACKET_SIZE = 65_535
    }
}
