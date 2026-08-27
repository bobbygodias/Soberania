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
- sonda `TunLabProbe` operando sobre uma duplicata do descritor;
- contadores de laboratório apenas em RAM;
- gerador determinístico de pacotes UDP de teste para IPv4 e IPv6 reservados;
- botão de teste no M0;
- notificação foreground;
- ciclo de criação/destruição do túnel;
- zero SDK de analytics;
- nenhuma interceptação HTTPS;
- rotas apenas de documentação/teste.

**Ainda não foi validado build/execução em dispositivo físico nesta etapa; portanto M0 não está concluído.**

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
- `TunLabProbe.kt`
- `TunLabCounters.kt`
- `TunLabPacketSender.kt`
- `docs/PACKET-ROUTER.md`
- `docs/CHECKPOINT-POLICY.md`

O `SoberaniaVpnService` mantém ownership do descritor TUN original. Consumidores futuros devem receber duplicatas através de `TunHandle.duplicate()`.

`TransportKind` distingue:

- `PACKET_TUNNEL`
- `STREAM_PROXY`

Isso existe porque backends como Arti exigem uma ponte TUN → stream.

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

1. Validar compilação do M0.
2. Instalar em dispositivo físico e confirmar autorização `VpnService`.
3. Confirmar que `TunLabProbe` observa pacotes IPv4/IPv6 reservados.
4. Confirmar repetidamente lifecycle e ownership dos descritores.
5. Implementar o primeiro `PacketRouter` real de laboratório.
6. Escolher e auditar a ponte TUN → stream para o caminho Onion.
7. Integrar primeiro backend real.
8. Só depois habilitar rotas default.
9. Posteriormente integrar Arti/Rust/JNI.
10. Depois Browser Shield.
11. Proteção Máxima somente quando os componentes que ela exige estiverem realmente implementados.

## Questões ainda abertas

- licença final do código: o repositório atualmente contém LICENSE CC0 1.0; revisar conscientemente antes de release;
- backend concreto do Nível 1;
- implementação de multi-hop;
- escolha da ponte TUN → stream;
- política de atualização;
- distribuição de relays caso haja infraestrutura própria;
- CI de Android: tentativa de adicionar workflow pelo conector foi bloqueada; ainda não considerar CI configurado.

## Política de checkpoint

A política formal está em `docs/CHECKPOINT-POLICY.md`.

Criar/atualizar checkpoint quando houver marco técnico, mudança arquitetural, escolha crítica, alteração de threat model, sessão longa com avanço substancial ou antes de etapa de alto risco.

Checkpoints devem distinguir claramente entre **implementado**, **em laboratório**, **planejado**, **em avaliação**, **bloqueado** e **não implementado**.

## Regra de continuidade

Se uma sessão futura perder contexto, este arquivo deve ser tratado como o checkpoint canônico do projeto antes de qualquer alteração arquitetural.
