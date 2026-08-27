package org.soberania.app.packet.lab

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Gera tráfego apenas para endereços reservados usados pelo M0.
 *
 * O objetivo é validar que a rota de laboratório entrega pacotes à TUN.
 * Nenhum destino público real é contatado.
 */
object TunLabPacketSender {

    data class Result(
        val ipv4Attempted: Boolean,
        val ipv6Attempted: Boolean
    )

    fun send(): Result {
        sendUdp(
            host = "192.0.2.1",
            port = TEST_PORT,
            payload = "SOBERANIA-M0-IPV4".encodeToByteArray()
        )

        sendUdp(
            host = "2001:db8::1",
            port = TEST_PORT,
            payload = "SOBERANIA-M0-IPV6".encodeToByteArray()
        )

        return Result(
            ipv4Attempted = true,
            ipv6Attempted = true
        )
    }

    private fun sendUdp(
        host: String,
        port: Int,
        payload: ByteArray
    ) {
        runCatching {
            DatagramSocket().use { socket ->
                val address = InetAddress.getByName(host)
                val packet = DatagramPacket(
                    payload,
                    payload.size,
                    address,
                    port
                )
                socket.send(packet)
            }
        }
    }

    private const val TEST_PORT = 1701
}
