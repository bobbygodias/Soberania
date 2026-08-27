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

`WireGuardPacketBackend` recebe a interface `OwnedTun`. A implementação Android é `OwnedTunDescriptor`.

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

## Estado de integração em APK

O APK M0 normal continua sem bibliotecas WireGuard empacotadas.

Existe uma variante separada `wireguardLab` que:

- usa `applicationIdSuffix = ".wireguardlab"`;
- empacota somente `arm64-v8a` nesta etapa;
- contém `libsoberania-wg.so` e `libsoberania-wireguard-go.so`;
- expõe apenas um diagnóstico passivo de carga/versão;
- não ativa peer, rotas, DNS ou transporte WireGuard.

O CI `.github/workflows/android-wireguard-lab-ci.yml` confirmou build, testes JVM e presença das duas bibliotecas dentro do APK.

## Testes de contrato

`OwnedTun` torna o ownership testável no JVM.

Há cobertura automática para:

- engine indisponível;
- transferência de ownership ao engine;
- falha de socket protection;
- lifecycle Ready → Stopped sem double turnOff.

Teste JVM não substitui teste de FD real no Android.

## Próxima implementação

1. validar no aparelho o carregamento JNI + Go e a versão;
2. criar teste Android real de ownership do FD;
3. separar semanticamente “socket não aberto” de erro de lookup nativo;
4. testar `protectSocket()` fail-closed;
5. só então testar um peer WireGuard de laboratório;
6. continuar sem rota default até ida/volta e fail-closed estarem validados.


## Android x86_64 smoke

O WireGuard LAB possui agora smoke test automático em emulador Android x86_64.

O teste usa uma Activity exclusiva do source set `wireguardLab` e não estabelece VPN real.

Ele:

1. carrega JNI + Go;
2. confirma a revisão upstream pinada;
3. cria um `ParcelFileDescriptor` Android real;
4. duplica o descritor;
5. transfere ownership da duplicata ao motor nativo;
6. envia deliberadamente um FD que não é TUN;
7. espera `turnOn() == -2`;
8. confirma por `fcntl(F_GETFD)` que a duplicata foi fechada;
9. confirma que o descritor original continua vivo.

Resultado canônico:

```text
before=true
result=-2
after=false
originalAlive=true
```

Isso cobre a fronteira de ownership em Android real, complementando os testes JVM.

Ainda não cobre:

- TUN real entregue ao WireGuard;
- socket protection real;
- peer;
- rota;
- DNS;
- tráfego protegido.
