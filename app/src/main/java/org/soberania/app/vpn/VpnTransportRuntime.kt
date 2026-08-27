package org.soberania.app.vpn

import android.net.VpnService
import org.soberania.app.transport.TransportRuntime

/**
 * Adaptador estreito entre um backend de transporte e o Android VpnService.
 *
 * Transportes recebem somente a capacidade de proteger sockets. Eles não
 * recebem o VpnService inteiro.
 */
class VpnTransportRuntime(
    private val service: VpnService
) : TransportRuntime {

    override fun protectSocket(socketFd: Int): Boolean =
        service.protect(socketFd)
}
