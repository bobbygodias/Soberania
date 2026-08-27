# WireGuard — Integração Nativa no Soberania

## Status

**Arquitetura definida. Motor nativo ainda não integrado.**

## Resultado da pesquisa upstream

Foi inspecionado o código oficial atual do projeto `WireGuard/wireguard-android`.

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

Portanto, usar `GoBackend` diretamente faria o Soberania perder a propriedade de uma única TUN e criaria um segundo lifecycle de VPN.

## ABI nativa upstream

A camada `libwg-go` oficial expõe conceitualmente:

```text
wgTurnOn(interfaceName, tunFd, settings)
wgTurnOff(handle)
wgGetSocketV4(handle)
wgGetSocketV6(handle)
wgGetConfig(handle)
wgVersion()
```

Isso encaixa diretamente na arquitetura do Soberania porque já possuímos a TUN.

## Direção escolhida

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
wireguard-go upstream
```

O `GoBackend` oficial não será usado como controlador do túnel.

## Fronteira Kotlin/native

Foi criado o contrato `WireGuardNativeEngine`.

Ele expõe somente:

- consultar se o motor nativo está disponível;
- iniciar motor com TUN externa;
- desligar motor;
- obter sockets IPv4/IPv6;
- obter versão.

Nenhuma API de criptografia proprietária é criada no Soberania.

## Socket protection

Após iniciar o motor:

```text
wireguard-go
   │
   ├── socket IPv4 ──► VpnService.protect(fd)
   └── socket IPv6 ──► VpnService.protect(fd)
```

Se um socket existente não puder ser protegido, o backend deve falhar e derrubar o motor em vez de manter um túnel com risco de loop.

## Ownership do FD

`WireGuardPacketBackend` recebe um `OwnedTunDescriptor`.

Antes de `detachRawFd()`, o backend exige `engine.isAvailable() == true`.

Ao chamar `detachRawFd()`:

- ownership passa ao motor nativo;
- Kotlin deixa de poder fechar aquele FD;
- o motor nativo deve assumir a responsabilidade de fechar o FD em **qualquer saída de `turnOn()`**: sucesso, código negativo ou exceção.

A implementação sentinela indisponível também fecha qualquer FD que receba por engano. Essa regra precisa ser testada especificamente na implementação JNI real.

## O que NÃO fazer

- não instanciar `GoBackend` como segundo VpnService;
- não usar reflection para chamar os métodos privados de `GoBackend`;
- não depender de nomes privados JNI da classe upstream;
- não copiar código sem preservar licença e avisos;
- não modificar primitivas criptográficas;
- não criar protocolo derivado próprio;
- não marcar Nível 1 como protegido enquanto o backend nativo não estiver funcional e testado.

## Licenciamento

Os arquivos atuais inspecionados do `wireguard-android`/`libwg-go` carregam licença Apache-2.0.

O repositório Soberania atualmente possui LICENSE CC0 1.0.

Antes de copiar/vendorizar qualquer código upstream, precisamos definir uma política de terceiros:

```text
código Soberania        -> licença principal do projeto
código WireGuard        -> Apache-2.0 + avisos upstream
outros componentes      -> licença original preservada
```

Uma dependência binária/Maven reduz cópia de código, mas a API pública atual não resolve a TUN externa. Portanto, a decisão de build/vendor ainda está aberta.

## Próximo passo

Comparar duas opções:

### A — build próprio de libwg-go upstream

Prós:
- usa diretamente a ABI apropriada;
- uma única TUN;
- integração pequena;
- sem reflection.

Contras:
- exige Go + NDK/CMake no build;
- aumenta complexidade de build reproduzível;
- exige manutenção e política clara de código de terceiros.

### B — pequeno fork auditável do módulo tunnel upstream

Prós:
- segue estrutura oficial Android;
- reutiliza pipeline de build já testado upstream.

Contras:
- precisamos carregar alterações;
- aumenta custo de atualização;
- risco de divergência do upstream.

A decisão deve priorizar manutenção, reprodutibilidade e capacidade de acompanhar atualizações de segurança do WireGuard.
