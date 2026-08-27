package org.soberania.app.voice

/**
 * Regras de fronteira para o recurso opcional de comunicação de emergência.
 *
 * Este recurso é propositalmente separado dos níveis de proteção do Soberania.
 * Ele não deve reutilizar nomes como "Proteção", "Anônima" ou "Máxima" para
 * descrever uma sessão de rádio.
 */
object EmergencyVoicePolicy {

    const val IS_SEPARATE_FEATURE = true

    /**
     * O recurso pode usar uma rede IP existente para alcançar um gateway, mas
     * não promete anonimato, confidencialidade fim a fim ou ocultação da
     * identidade exigida pela rede de rádio.
     */
    const val PROVIDES_ANONYMITY = false
    const val PROVIDES_END_TO_END_PRIVACY = false

    /**
     * O telefone, sozinho, não transmite VHF/UHF. É necessário alcançar um
     * gateway/refletor/nó por IP ou possuir hardware externo apropriado.
     */
    const val REQUIRES_IP_PATH_OR_EXTERNAL_GATEWAY = true

    /**
     * A interface deve avisar que indicativo, Radio ID, destino, horário ou
     * outros metadados podem ser expostos conforme o protocolo/rede utilizada.
     */
    const val MUST_SHOW_IDENTITY_DISCLOSURE_WARNING = true

    /**
     * "Emergência" descreve o caso de uso. Não é uma declaração de isenção
     * regulatória. Regras variam por país e serviço.
     */
    const val EMERGENCY_IS_NOT_AUTOMATIC_LEGAL_EXEMPTION = true
}
