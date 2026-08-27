# WireGuard — Integração Nativa no Soberania

## Status

**Direção arquitetural escolhida. Motor nativo ainda não integrado.**

## Resultado da pesquisa upstream

Foi inspecionado o código oficial atual dos projetos:

- `WireGuard/wireguard-android`;
- `WireGuard/wireguard-go`.

O módulo Android oficial publica uma biblioteca embutível, mas o `GoBackend`:

- cria seu próprio `VpnService`;
- cria sua própria TUN;
- chama métodos JNI privados;
- entrega o FD da TUN para `wireguard-go`;
- protege os sockets IPv4/IPv6 do transporte com `VpnService.protect()`.

Os métodos centrais são privados no `GoBackend`:

```text
wgTurnOn(...)
wgTurnOff(...)
wgGetSocketV4(...)
wgGetSocketV6(...)
wgGetConfig(...)
wgVersion()
```

Portanto, usar `GoBackend` diretamente criaria um segundo lifecycle de VPN e violaria a decisão de manter uma única TUN sob controle do Soberania.

## Decisão

A direção escolhida é um **adaptador nativo mínimo do próprio Soberania**, sem copiar `GoBackend` e sem criar um fork grande do módulo Android.

```text
SoberaniaVpnService
       │
       ▼
 TUN original
       │
       ▼
duplicateOwned()
       │
       ▼
PacketRouter
       │
       ▼
WireGuardPacketBackend
       │
       ▼
WireGuardNativeEngine
       │
       ▼
adaptador Go/JNI mínimo do Soberania
       │
       ▼
golang.zx2c4.com/wireguard
       │
       ▼
wireguard-go upstream
```

## Por que essa direção

Ela preserva:

- uma única TUN;
- um único `VpnService`;
- ownership explícito;
- `VpnService.protect()` para sockets do transporte;
- criptografia/protocolo no código WireGuard upstream;
- atualização do motor por versão/commit fixado;
- ausência de reflection;
- ausência de dependência de métodos JNI privados da classe `GoBackend`.

## wireguard-go

O projeto `WireGuard/wireguard-go` utiliza licença MIT.

O build Android oficial de referência:

- usa Go 1.24.3;
- verifica o tarball da toolchain com SHA-256;
- compila com `-buildmode c-shared`;
- fixa a revisão do módulo `golang.zx2c4.com/wireguard` no `go.mod`.

O Soberania deve adotar a mesma filosofia de pinning e verificação, sem necessariamente copiar o build upstream.

## Fronteira Kotlin/native

O contrato `WireGuardNativeEngine` expõe somente:

- `isAvailable()`;
- `turnOn(interfaceName, tunFd, userspaceConfig)`;
- `turnOff(handle)`;
- `socketV4(handle)`;
- `socketV6(handle)`;
- `version()`.

Nenhuma API criptográfica proprietária é criada no Soberania.

## Socket protection

Após iniciar o motor:

```text
wireguard-go
   │
   ├── socket IPv4 ──► VpnService.protect(fd)
   └── socket IPv6 ──► VpnService.protect(fd)
```

Se um socket existente não puder ser protegido, o backend deve derrubar o motor e entrar em estado de falha.

## Ownership do FD

`WireGuardPacketBackend` recebe um `OwnedTunDescriptor`.

Antes de transferir ownership:

```text
engine.isAvailable() == true
```

Depois de `detachRawFd()`:

- Kotlin deixa de possuir o FD;
- `turnOn()` passa a ser responsável pelo FD;
- o adaptador nativo deve fechar o FD em sucesso, código negativo ou exceção;
- o descritor TUN original permanece com `SoberaniaVpnService`.

Essa regra é obrigatória e terá teste específico.

## Chaves

Não implementar geração/derivação de chaves WireGuard manualmente em Kotlin.

Direção:

- usar API criptográfica já existente no ecossistema WireGuard;
- ou expor geração/derivação pelo adaptador Go usando biblioteca upstream apropriada;
- nunca copiar pequenas rotinas criptográficas só por parecerem simples.

## O que NÃO fazer

- não instanciar `GoBackend` como segundo VpnService;
- não usar reflection para chamar métodos privados;
- não depender dos nomes JNI privados de `GoBackend`;
- não modificar primitivas criptográficas;
- não criar protocolo derivado;
- não usar dependência dinâmica sem versão;
- não marcar Nível 1 como protegido antes do caminho completo e dos testes anti-leak.

## Licenciamento

- código autoral do Soberania: atualmente CC0 1.0, salvo indicação em contrário;
- `wireguard-go`: MIT;
- arquivos do adaptador Android upstream estudados: Apache-2.0.

A política está em `docs/LICENSING.md` e `THIRD_PARTY_NOTICES.md`.

## Próxima implementação

1. criar módulo nativo isolado;
2. fixar versões de Go e `wireguard-go`;
3. criar adaptador Go mínimo;
4. criar JNI mínimo para `WireGuardNativeEngine`;
5. adicionar build NDK/Go reproduzível;
6. compilar no CI para ABIs Android suportadas;
7. testar ownership de FD;
8. testar `protectSocket()`;
9. só então testar um peer WireGuard de laboratório;
10. ainda sem rota default até ida/volta e fail-closed estarem validados.
