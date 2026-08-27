package org.soberania.app.transport.wireguard

/**
 * Fronteira mínima entre o Soberania e um motor WireGuard userspace.
 *
 * Esta interface NÃO implementa criptografia.
 * Uma implementação futura deverá chamar uma biblioteca WireGuard upstream
 * auditável (wireguard-go ou equivalente), preservando licenças e avisos.
 */
interface WireGuardNativeEngine {

    /**
     * Indica se a implementação nativa necessária está carregada e pronta
     * para receber ownership de um FD.
     *
     * O backend consulta isso ANTES de detachRawFd(), evitando perder ownership
     * de uma duplicata quando a biblioteca nem sequer está disponível.
     */
    fun isAvailable(): Boolean

    /**
     * Inicia um dispositivo WireGuard sobre uma TUN já criada pelo Soberania.
     *
     * Ownership:
     * ao entrar neste método, a implementação passa a ser responsável por
     * fechar tunFd em QUALQUER resultado — sucesso, código negativo ou exceção.
     *
     * @param interfaceName nome lógico da interface
     * @param tunFd FD bruto cuja ownership foi transferida ao motor nativo
     * @param userspaceConfig configuração no formato userspace do WireGuard
     * @return handle >= 0 em sucesso; valor negativo em falha
     */
    fun turnOn(
        interfaceName: String,
        tunFd: Int,
        userspaceConfig: String
    ): Int

    fun turnOff(handle: Int)

    fun socketV4(handle: Int): Int

    fun socketV6(handle: Int): Int

    fun version(): String?
}
