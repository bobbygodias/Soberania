# Arquitetura do Soberania

## Objetivo

Soberania é uma plataforma Android de proteção de rede e identidade digital. A arquitetura é modular para que cada camada possa ser auditada, substituída e testada separadamente.

## Visão geral

```text
┌─────────────────────────────────────────────────────────────┐
│                         ANDROID                             │
│                                                             │
│  Apps ──────────────┐                                       │
│  Navegador ─────────┼──────────────┐                        │
│  WebView ───────────┘              │                        │
│                                    ▼                        │
│                    ┌──────────────────────────┐              │
│                    │      Soberania App       │              │
│                    │                          │              │
│                    │  Network Controller      │              │
│                    │  Identity Controller     │              │
│                    │  Browser Integration     │              │
│                    │  Protection State        │              │
│                    └────────────┬─────────────┘              │
│                                 │                            │
│                         Android VpnService                   │
│                                 │                            │
│                              TUN                             │
└─────────────────────────────────┼────────────────────────────┘
                                  │
                ┌─────────────────┴──────────────────┐
                │                                    │
                ▼                                    ▼
       Transporte rápido                      Transporte anônimo
       (backend auditável)                           Tor
                │                                    │
                └─────────────────┬──────────────────┘
                                  ▼
                               Internet
```

## Camada 1 — Rede

Responsabilidades:

- criar e manter a interface TUN através de `VpnService`;
- encaminhar IPv4 e IPv6;
- impedir vazamento de DNS;
- detectar perda do túnel;
- representar corretamente o estado de proteção;
- respeitar Always-On/Lockdown quando configurado pelo sistema;
- não registrar destinos ou conteúdo de tráfego.

A camada de rede **não deve descriptografar HTTPS**.

## Camada 2 — Transporte

Transportes são módulos substituíveis.

### Rápido

Prioriza baixa latência mantendo o tráfego entre o dispositivo e o ponto de saída protegido por protocolo criptográfico maduro e auditável.

Nenhuma criptografia proprietária será inventada pelo projeto.

### Anônimo

Usa rede de múltiplos saltos apropriada para anonimato, inicialmente Tor.

Maior anonimato implica custo de desempenho. A interface não deve esconder esse fato do usuário.

## Camada 3 — DNS

Regras:

1. DNS não sai diretamente pela interface física quando a proteção está ativa.
2. IPv4 e IPv6 seguem a mesma política.
3. Falha de resolução não autoriza fallback silencioso para DNS externo.
4. O resolvedor deve ser configurável e auditável.

## Camada 4 — Identidade Web

A rede não consegue manipular conteúdo protegido por TLS sem interceptação, e Soberania deliberadamente não fará MITM.

Proteções de cookies, storage, trackers e fingerprinting pertencem a uma integração de navegador compatível.

Estratégias planejadas:

- extensão Firefox;
- GeckoView integrado opcionalmente;
- isolamento de contextos;
- sessões persistentes ou descartáveis.

Chrome/Chromium Android pode receber proteção de rede, mas não deve ser anunciado como tendo integração profunda quando a plataforma não oferecer mecanismo equivalente.

## Camada 5 — Sessões

Uma sessão descreve o estado de identidade associado à navegação.

### Persistente

Mantém os dados explicitamente escolhidos pelo usuário.

### Descartável

Ao ser encerrada, deve eliminar o estado controlado pelo Soberania, incluindo cookies/storage do contexto compatível e solicitar novo circuito/estado de transporte quando aplicável.

## Modo Paranoico

Modo Paranoico não é um protocolo. É uma política composta.

Pretendido:

- transporte anônimo obrigatório;
- DNS exclusivamente dentro do túnel;
- proteção IPv4 e IPv6;
- kill switch;
- sessão web descartável;
- storage efêmero;
- nenhum log persistente;
- bloqueio de rastreadores compatível;
- aviso visível quando a proteção de navegador não estiver disponível.

## Estado de proteção

Nunca mostrar apenas um ícone verde genérico.

O estado deve ser derivado de fatos observáveis:

```text
Rede             PROTEGIDA / PARCIAL / INATIVA
DNS              PROTEGIDO / VAZANDO / INDISPONÍVEL
IPv4             PROTEGIDO / INATIVO
IPv6             PROTEGIDO / INATIVO / NÃO DISPONÍVEL
Navegador        COMPLETO / REDE APENAS / NÃO SUPORTADO
Transporte       RÁPIDO / ANÔNIMO / DESCONECTADO
Kill switch      ATIVO / INATIVO
```

## Princípio arquitetônico

Se uma função exige esconder do usuário como ela funciona para parecer mais segura, ela não pertence ao Soberania.
