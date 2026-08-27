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
- notificação foreground;
- ciclo de criação/destruição do túnel;
- zero SDK de analytics;
- nenhuma interceptação HTTPS;
- rotas apenas de documentação/teste.

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
- `docs/PACKET-ROUTER.md`

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

## Próximos passos técnicos

1. Implementar o primeiro Packet Router de laboratório.
2. Exercitar uma duplicata real da TUN sem transferir ownership do descritor original.
3. Escolher e auditar a ponte TUN → stream para o caminho Onion.
4. Testar ida/volta em laboratório.
5. Integrar primeiro backend real.
6. Só depois habilitar rotas default.
7. Posteriormente integrar Arti/Rust/JNI.
8. Depois Browser Shield.
9. Proteção Máxima somente quando os componentes que ela exige estiverem realmente implementados.

## Questões ainda abertas

- licença final do código: o repositório atualmente contém LICENSE CC0 1.0; revisar conscientemente antes de release;
- backend concreto do Nível 1;
- implementação de multi-hop;
- escolha da ponte TUN → stream;
- política de atualização;
- distribuição de relays caso haja infraestrutura própria;
- CI de Android: tentativa de adicionar workflow pelo conector foi bloqueada; ainda não considerar CI configurado.

## Regra de continuidade

Se uma sessão futura perder contexto, este arquivo deve ser tratado como o checkpoint canônico do projeto antes de qualquer alteração arquitetural.
