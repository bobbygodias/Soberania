# Licenciamento do Soberania

## Princípio

Soberania é um projeto open source.

O fato de o código principal possuir uma licença não apaga nem substitui licenças de componentes de terceiros.

## Código próprio

O repositório atualmente possui `LICENSE` com **CC0 1.0 Universal**.

Até decisão posterior, tratar arquivos autorais do Soberania como abrangidos por essa licença, salvo indicação explícita em contrário.

## Código e dependências de terceiros

Regra obrigatória:

> **Toda dependência mantém sua licença, copyright e avisos originais.**

Não remover cabeçalhos, NOTICE, LICENSE ou atribuições exigidas.

## WireGuard

### wireguard-go

O repositório oficial `WireGuard/wireguard-go` utiliza licença **MIT**.

Direção pretendida:

```text
código de integração Soberania
        │
        ▼
módulo Go próprio e mínimo
        │
        ▼
import golang.zx2c4.com/wireguard
        │
        ▼
wireguard-go upstream (MIT)
```

Não modificar primitivas criptográficas.

### wireguard-android

O código de integração Android estudado no `WireGuard/wireguard-android` utiliza **Apache-2.0**.

Ele foi usado como referência arquitetural para compreender a integração oficial Android.

A direção atual **não exige copiar `GoBackend` nem seu JNI**.

Se algum arquivo Apache-2.0 for incorporado futuramente, ele deverá:

- manter o cabeçalho SPDX/copyright upstream;
- permanecer identificado como Apache-2.0;
- acompanhar os avisos exigidos.

## Arti / Tor / outras dependências

Antes de adicionar qualquer dependência:

1. identificar licença;
2. registrar versão/commit;
3. verificar compatibilidade de distribuição;
4. preservar notices;
5. registrar no inventário de terceiros;
6. preferir dependência upstream a cópia modificada quando viável.

## Inventário

O projeto deverá manter `THIRD_PARTY_NOTICES.md` antes da primeira release pública.

## Regra

**Open source não significa licença inexistente. Liberdade de uso exige que a liberdade e os avisos dos componentes que recebemos também sejam respeitados.**
