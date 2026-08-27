# Soberania — Checkpoint de Continuidade

Atualizado em: 2026-08-27

## Identidade

**Nome:** Soberania

**Lema:** “Porque soberania e liberdade jamais serão negociáveis.”

**Repositório:** https://github.com/bobbygodias/Soberania

Soberania não deve ser apresentado como “mais uma VPN”. É uma plataforma open source de soberania digital para Android.

## Objetivo

Maximizar, dentro dos limites técnicos reais, o controle individual sobre:

- tráfego de rede;
- exposição de IP e DNS;
- vazamentos IPv4/IPv6;
- identidade web;
- cookies/storage;
- rastreadores;
- fingerprinting;
- persistência de sessão;
- escolha entre velocidade, isolamento e anonimato.

## Princípios congelados

1. Zero telemetria.
2. Nenhuma conta obrigatória.
3. Nenhum MITM de HTTPS.
4. Nenhuma CA própria para interceptar conteúdo.
5. Nenhuma criptografia caseira.
6. IPv4, IPv6 e DNS obedecem à mesma política de proteção.
7. Kill switch quando a política exigir proteção total.
8. Nunca declarar “protegido” antes de o estado real confirmar.
9. Open source, arquitetura auditável e builds reproduzíveis como meta.
10. Interface feita para pessoas leigas.
11. Limites de proteção devem ser explícitos.
12. Não prometer anonimato absoluto contra adversário global.
13. Não tratar dispositivo/SO já comprometido como recuperável por magia.
14. Browser Shield é uma camada separada da camada de rede.
15. Firefox/GeckoView permitem integração profunda; Chrome/Chromium Android recebe proteção de rede, mas não deve ser anunciado como tendo a mesma integração web.
16. Comunicação de Emergência é um recurso opcional completamente separado dos níveis de proteção.
17. Comunicação de Emergência não promete anonimato, confidencialidade fim a fim nem ocultação da identidade de rádio.
18. O rótulo "emergência" descreve o caso de uso e não deve ser tratado como isenção regulatória automática.

## Níveis de proteção — nomes de interface

- **Nível 1 — Proteção**
- **Nível 2 — Reforçada**
- **Nível 3 — Anônima**
- **Nível 4 — Máxima**

“Paranoico” foi removido da nomenclatura pública.

### Relação com transportes

- Nível 1 usa transporte rápido.
- Nível 2 usa multi-hop.
- Nível 3 usa onion routing.
- Nível 4 é uma política composta baseada no modo Onion mais controles obrigatórios.

O Nível 4 não é um quarto protocolo.

## Transportes

### Rápido
Baixa latência, uso cotidiano.

### Multi-hop
Mais de um ponto de encaminhamento; não deve ser confundido com Tor/onion.

### Onion
Direção preferencial: **Arti**, implementação moderna do Tor em Rust.

## Arquitetura Onion importante

Arti trabalha naturalmente com conexões/streams Tor, não como um backend de pacote IP bruto equivalente a WireGuard.

Portanto, o caminho pretendido é:

```text
Android VpnService
      │
      ▼
     TUN
      │
      ▼
 Packet Router
      │
      ├──────────────► backend de pacote
      │
      └──► Stream Bridge / tun2socks
                   │
                   ▼
                  Arti
                   │
                   ▼
             Tor Network
```

Essa separação deve ser preservada.

## M0 — estado atual

Já existe:

- projeto Android/Kotlin;
- Android VpnService;
- interface TUN de laboratório IPv4/IPv6;
- ownership explícito da TUN original via `TunHandle`;
- `LabPacketRouter` executável;
- `LabPacketBackend` implementando `PacketTunnelBackend`;
- duplicata da TUN com ownership explícito via `OwnedTunDescriptor`;
- classificação mínima de IPv4/IPv6 via `IpPacketInspector`;
- contadores de laboratório apenas em RAM;
- gerador determinístico de pacotes UDP de teste para IPv4 e IPv6 reservados;
- botão de teste no M0;
- notificação foreground;
- ciclo de criação/destruição do túnel;
- zero SDK de analytics;
- nenhuma interceptação HTTPS;
- rotas apenas de documentação/teste.

**O build Android Debug já foi validado com sucesso no GitHub Actions. Ainda falta instalar/testar em dispositivo físico; portanto M0 ainda não está concluído.**

M0 deliberadamente não instala `0.0.0.0/0` nem `::/0` enquanto não existir um motor de encaminhamento real.

Always-On permanece desativado durante M0 pelo mesmo motivo.

## Código arquitetural já criado

### Transporte e política

- `TransportMode.kt`
- `TransportKind.kt`
- `TransportState.kt`
- `TransportBackend.kt`
- `ProtectionProfile.kt`

### Caminho da TUN

- `TunHandle.kt`
- `PacketRouter.kt`
- `PacketRouterState.kt`
- `DataPath.kt`
- `IpVersion.kt`
- `IpPacketInspector.kt`
- `OwnedTunDescriptor.kt`
- `LabPacketRouter.kt`
- `LabPacketBackend.kt`
- `TunLabCounters.kt`
- `TunLabPacketSender.kt`
- `PacketTunnelBackend.kt`
- `StreamProxyBackend.kt`
- `TransportRuntime.kt`
- `VpnTransportRuntime.kt`
- `docs/PACKET-ROUTER.md`
- `docs/CHECKPOINT-POLICY.md`

O `SoberaniaVpnService` mantém ownership do descritor TUN original.

Para `PACKET_TUNNEL`, o router usa `TunHandle.duplicateOwned()` e transfere ownership da duplicata ao `PacketTunnelBackend`. O backend fecha essa duplicata, inclusive em caso de falha.

Para `STREAM_PROXY`, o backend não recebe a TUN diretamente; uma futura Stream Bridge consumirá a duplicata e converterá IP em streams.

`TransportKind` distingue:

- `PACKET_TUNNEL`
- `STREAM_PROXY`

Isso existe porque backends como Arti exigem uma ponte TUN → stream.

## Nível 1 — backend real em avaliação

Candidato principal: WireGuard.

Pesquisa do código oficial confirmou que o backend userspace Android:

- entrega o FD da TUN ao wireguard-go;
- usa sockets de transporte que precisam ser excluídos da própria VPN;
- protege esses sockets com `VpnService.protect()`;
- o backend oficial padrão também cria seu próprio `VpnService`/TUN.

Consequência: o Soberania não deve instanciar cegamente um segundo backend que queira possuir outro `VpnService`.

Foi criada a fronteira:

```text
TUN original -> duplicateOwned() -> PacketRouter -> PacketTunnelBackend
```

e um `TransportRuntime` restrito que expõe apenas `protectSocket(fd)`.

A direção de integração foi refinada: criar um adaptador Go/JNI mínimo do Soberania que importe `wireguard-go` upstream, em vez de usar `GoBackend` ou manter um fork grande.

`WireGuardNativeEngine` e `WireGuardPacketBackend` já existem como fronteira Kotlin, mas o motor nativo ainda não está empacotado.

Não usar reflexão, hacks de FD ou segundo VpnService concorrente.

Documentos:
- `docs/LEVEL1-TRANSPORT.md`
- `docs/WIREGUARD-NATIVE-INTEGRATION.md`
- `docs/LICENSING.md`
- `THIRD_PARTY_NOTICES.md`.

## Build / CI

GitHub Actions está ativo em `.github/workflows/android-ci.yml`.

Estado atual:

- Android API 36 instalado no runner;
- JDK 17;
- Gradle 9.5.0;
- AGP 9.3.0;
- Kotlin integrado do AGP 9;
- `:app:assembleDebug` passou com sucesso;
- artefato `soberania-m0-debug` é gerado pelo workflow.

O primeiro build falhou porque o projeto ainda aplicava `org.jetbrains.kotlin.android`. AGP 9 usa Kotlin integrado; o plugin e `kotlinOptions.jvmTarget` foram removidos. O build seguinte passou.

## WireGuard nativo isolado — estado atual

Criado em `native/wireguard/`:

- `versions.mk`;
- `go.mod`;
- `go.sum`;
- `Makefile`;
- `adapter/main.go`;
- `jni/soberania_wireguard_jni.c`.

Também existe `JniWireGuardNativeEngine.kt`, mas a biblioteca nativa ainda não está conectada ao `app/build.gradle.kts`.

Inputs congelados para o laboratório:

- Go 1.24.3;
- SHA-256 do tarball Linux amd64 da toolchain registrado em `versions.mk`;
- wireguard-go `v0.0.0-20250521234502-f333402bd9cb`;
- NDK `28.2.13676358`;
- Android API nativa mínima 26.

O workflow `.github/workflows/wireguard-native-ci.yml` compilou com sucesso para `arm64-v8a`.

Validações aprovadas:

- `go mod verify`;
- build Go `c-shared`;
- build/link do JNI shim;
- ELF aarch64 válido;
- dependência dinâmica do JNI shim para `libsoberania-wireguard-go.so` confirmada.

Artefato de laboratório do workflow: `soberania-wireguard-native-arm64-lab`.

O módulo permanece **isolado do APK** até o M0 ser validado em dispositivo físico.

## Browser Shield planejado

Objetivos:

- isolamento de cookies;
- storage por contexto;
- bloqueio de trackers;
- sessões persistentes/descartáveis;
- redução de fingerprinting por normalização, não randomização caótica.

Integrações prioritárias:

1. Firefox Android via extensão;
2. GeckoView opcional integrado ao Soberania.

Não fazer MITM de TLS para tentar controlar cookies de Chrome.

## Comunicação de Emergência — recurso separado

Foi decidido que a funcionalidade RoIP/rádio não pertence à matriz Nível 1/2/3/4.

Arquitetura:

```text
Soberania
├── Proteção digital
│   └── Níveis 1 / 2 / 3 / 4
└── Comunicação de Emergência
    └── RoIP -> gateway/refletor -> RF
```

Regras congeladas:

- sem selo de "protegido" ou "anônimo" na tela de PTT;
- pode expor indicativo, Radio ID, destino, horário e outros metadados;
- smartphone sem hardware RF não transmite VHF/UHF sozinho;
- o recurso depende de caminho IP até gateway/refletor ou de hardware externo;
- M17/Codec2 é a prioridade inicial;
- AllStar/IAX2 e EchoLink/SvxLink ficam como fases posteriores;
- zero telemetria e nenhuma gravação automática continuam válidos;
- `VoiceProtectionCompatibility.kt` foi removido para eliminar acoplamento com `ProtectionProfile`;
- documentação canônica: `docs/EMERGENCY-COMMS.md`.

Este roadmap é independente do desenvolvimento do núcleo de privacidade.

## Próximos passos técnicos

1. Instalar o APK M0 em dispositivo físico e confirmar autorização `VpnService`.
2. Confirmar que `LabPacketBackend` observa pacotes IPv4/IPv6 reservados.
3. Confirmar repetidamente lifecycle e ownership original -> duplicata -> backend.
4. Validar no aparelho que o M0 pode iniciar/parar repetidamente sem afetar a rede comum.
5. Criar testes específicos de ownership do FD para a ponte nativa.
6. Empacotar temporariamente o adaptador WireGuard apenas em uma variante de laboratório.
7. Testar `protectSocket()` antes de qualquer rota default.
8. Testar peer WireGuard de laboratório sem rota default global.
9. Escolher e auditar a ponte TUN → stream para o caminho Onion.
10. Só depois habilitar rotas default.
11. Posteriormente integrar Arti/Rust/JNI.
12. Depois Browser Shield.
13. Proteção Máxima somente quando os componentes que ela exige estiverem realmente implementados.

## Questões ainda abertas

- licença final do código: o repositório atualmente contém LICENSE CC0 1.0; revisar conscientemente antes de release;
- detalhes finais do build Go/NDK do adaptador WireGuard mínimo;
- geração/armazenamento seguro de chaves WireGuard;
- implementação de multi-hop;
- escolha da ponte TUN → stream;
- política de atualização;
- distribuição de relays caso haja infraestrutura própria;
- política de assinatura/release do APK ainda não definida.

## Política de checkpoint

A política formal está em `docs/CHECKPOINT-POLICY.md`.

Criar/atualizar checkpoint quando houver marco técnico, mudança arquitetural, escolha crítica, alteração de threat model, sessão longa com avanço substancial ou antes de etapa de alto risco.

Checkpoints devem distinguir claramente entre **implementado**, **em laboratório**, **planejado**, **em avaliação**, **bloqueado** e **não implementado**.

## Regra de continuidade

Se uma sessão futura perder contexto, este arquivo deve ser tratado como o checkpoint canônico do projeto antes de qualquer alteração arquitetural.
