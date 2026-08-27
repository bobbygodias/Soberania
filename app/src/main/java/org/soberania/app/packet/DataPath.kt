package org.soberania.app.packet

/**
 * Caminho necessário para levar tráfego da TUN até um backend.
 */
enum class DataPath {
    /**
     * Backend capaz de integrar-se a um caminho de pacote/túnel.
     */
    PACKET_DIRECT,

    /**
     * Exige conversão de IP/TCP/UDP da TUN para conexões/streams.
     * Este é o caminho esperado para Onion/Arti.
     */
    TUN_TO_STREAM
}
