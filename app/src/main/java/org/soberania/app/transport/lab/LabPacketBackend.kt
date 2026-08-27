package org.soberania.app.transport.lab

import android.os.ParcelFileDescriptor
import org.soberania.app.packet.IpPacketInspector
import org.soberania.app.packet.OwnedTun
import org.soberania.app.packet.OwnedTunDescriptor
import org.soberania.app.packet.lab.TunLabCounters
import org.soberania.app.transport.PacketTunnelBackend
import org.soberania.app.transport.TransportKind
import org.soberania.app.transport.TransportMode
import org.soberania.app.transport.TransportRuntime
import org.soberania.app.transport.TransportState
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Backend de pacote exclusivamente para M0.
 *
 * O laboratório precisa do ParcelFileDescriptor Android para usar
 * AutoCloseInputStream. O contrato público do PacketTunnelBackend, porém,
 * permanece baseado em OwnedTun para ser testável fora do Android.
 */
class LabPacketBackend : PacketTunnelBackend {

    override val mode: TransportMode = TransportMode.FAST

    override val kind: TransportKind = TransportKind.PACKET_TUNNEL

    private val running = AtomicBoolean(false)

    @Volatile
    private var currentState: TransportState = TransportState.Stopped

    @Volatile
    private var input: ParcelFileDescriptor.AutoCloseInputStream? = null

    @Volatile
    private var worker: Thread? = null

    @Synchronized
    override fun start(
        tun: OwnedTun,
        runtime: TransportRuntime
    ): TransportState {
        if (running.get()) {
            tun.close()
            return currentState
        }

        currentState = TransportState.Starting
        TunLabCounters.reset()

        val androidTun = tun as? OwnedTunDescriptor
            ?: run {
                tun.close()
                return fail("Backend LAB exige descritor Android")
            }

        val stream = try {
            val pfd = androidTun.takeParcelFileDescriptor()
            ParcelFileDescriptor.AutoCloseInputStream(pfd)
        } catch (exception: Exception) {
            tun.close()
            return fail(
                "Falha ao assumir a TUN de laboratório: " +
                    exception.javaClass.simpleName
            )
        }

        input = stream
        running.set(true)
        TunLabCounters.markRunning(true)

        currentState = TransportState.Ready(
            mode = mode,
            detail = "LAB ONLY — TUN consumida, sem transporte de rede"
        )

        worker = Thread({
            val buffer = ByteArray(MAX_PACKET_SIZE)

            try {
                while (running.get()) {
                    val count = stream.read(buffer)

                    if (count < 0) break
                    if (count == 0) continue

                    val version = IpPacketInspector.version(buffer, count)
                    TunLabCounters.record(version, count)
                }
            } catch (_: IOException) {
                // stop() fecha a duplicata para interromper read().
            } finally {
                running.set(false)
                TunLabCounters.markRunning(false)
                runCatching { stream.close() }

                if (currentState !is TransportState.Failed) {
                    currentState = TransportState.Stopped
                }
            }
        }, "Soberania-M0-LabPacketBackend").apply {
            isDaemon = true
            start()
        }

        return currentState
    }

    @Synchronized
    override fun stop() {
        running.set(false)
        TunLabCounters.markRunning(false)

        runCatching { input?.close() }
        input = null

        worker?.interrupt()
        worker = null

        if (currentState !is TransportState.Failed) {
            currentState = TransportState.Stopped
        }
    }

    override fun state(): TransportState = currentState

    private fun fail(reason: String): TransportState {
        running.set(false)
        TunLabCounters.markRunning(false)

        val failed = TransportState.Failed(reason)
        currentState = failed
        return failed
    }

    companion object {
        private const val MAX_PACKET_SIZE = 65_535
    }
}
