package org.soberania.app.packet

/**
 * Inspeção mínima de pacote.
 *
 * Não interpreta payload, domínio, URL ou conteúdo de aplicação.
 * Lê somente o nibble de versão necessário para separar IPv4 de IPv6.
 */
object IpPacketInspector {

    fun version(
        packet: ByteArray,
        length: Int
    ): IpVersion {
        if (length <= 0 || packet.isEmpty()) {
            return IpVersion.UNKNOWN
        }

        val versionNibble = (packet[0].toInt() ushr 4) and 0x0F

        return when (versionNibble) {
            4 -> IpVersion.IPV4
            6 -> IpVersion.IPV6
            else -> IpVersion.UNKNOWN
        }
    }
}
