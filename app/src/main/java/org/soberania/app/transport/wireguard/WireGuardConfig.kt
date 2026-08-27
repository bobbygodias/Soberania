package org.soberania.app.transport.wireguard

/**
 * Configuração já validada e serializada para o formato userspace WireGuard.
 *
 * Esta classe deliberadamente não aceita campos de configuração soltos.
 * Parsing, validação de chaves e construção da configuração serão uma camada
 * separada para evitar strings montadas ad-hoc espalhadas pelo código.
 */
data class WireGuardConfig(
    val interfaceName: String,
    val userspaceConfig: String
)
