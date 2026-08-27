package org.soberania.app.packet

import java.io.Closeable

/**
 * Ownership abstrato de uma duplicata da TUN.
 *
 * Esta interface é deliberadamente livre de tipos Android para que o contrato
 * de lifecycle/fail-closed dos backends possa ser testado no JVM.
 *
 * O descritor original do VpnService nunca deve implementar/ser entregue por
 * esta interface. Apenas duplicatas pertencentes a um consumidor.
 */
interface OwnedTun : Closeable {

    /**
     * Transfere ownership do FD bruto ao receptor.
     *
     * Depois do retorno, o receptor é responsável por fechar o FD e close()
     * deste objeto não deve mais fechá-lo.
     */
    fun detachRawFd(): Int

    fun hasOwnership(): Boolean
}
