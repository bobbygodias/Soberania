package org.soberania.app.packet.lab

import android.os.ParcelFileDescriptor
import org.soberania.app.packet.IpPacketInspector
import org.soberania.app.packet.PacketRouter
import org.soberania.app.packet.PacketRouterState
import org.soberania.app.packet.TunHandle
import org.soberania.app.transport.TransportBackend
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

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

    @Volatile
    private var currentState: PacketRouterState = PacketRouterState.Detached

    @Volatile
    private var input: ParcelFileDescriptor.AutoCloseInputStream? = null

    @Volatile
    private var worker: Thread? = null

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

        TunLabCounters.reset()

        val stream = ParcelFileDescriptor.AutoCloseInputStream(duplicate)
        input = stream
        running.set(true)
        TunLabCounters.markRunning(true)
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

                    val version = IpPacketInspector.version(buffer, count)
                    TunLabCounters.record(version, count)
                }
            } catch (_: IOException) {
                // Fechar a duplicata para interromper read() durante detach()
                // é comportamento normal.
            } finally {
                running.set(false)
                TunLabCounters.markRunning(false)
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
        TunLabCounters.markRunning(false)

        runCatching { input?.close() }
        input = null

        worker?.interrupt()
        worker = null

        if (currentState !is PacketRouterState.Failed) {
            currentState = PacketRouterState.Detached
        }
    }

    override fun state(): PacketRouterState = currentState

    override fun close() = detach()

    private fun fail(reason: String): PacketRouterState {
        running.set(false)
        TunLabCounters.markRunning(false)

        val failed = PacketRouterState.Failed(reason)
        currentState = failed
        return failed
    }

    companion object {
        private const val MAX_PACKET_SIZE = 65_535
    }
}
