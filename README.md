# Soberania

> **“Porque soberania e liberdade jamais serão negociáveis.”**

**Soberania** é uma plataforma open source de soberania digital para Android.

Não é “mais uma VPN”. O objetivo é combinar, de forma auditável e compreensível, proteção de rede, isolamento de identidade web e modos de navegação com diferentes níveis de privacidade.

## Princípios

- **Sem confiança cega.** Cada proteção deve ser explicável e verificável.
- **Zero telemetria.** Nenhum analytics, rastreamento comercial ou coleta invisível.
- **Sem conta obrigatória.** O aplicativo deve funcionar sem nome, telefone, e-mail ou login.
- **Sem criptografia caseira.** Protocolos e primitivas consolidados, públicos e auditados.
- **Sem MITM de HTTPS.** Soberania não instala CA para interceptar o conteúdo do usuário.
- **Sem vazamentos silenciosos.** IPv4, IPv6 e DNS devem obedecer ao mesmo modelo de proteção.
- **Open source de verdade.** Código, documentação, modelo de ameaça e processo de build públicos.
- **Interface para pessoas comuns.** O usuário não deve precisar conhecer TUN, JNI, MTU, DoH ou curvas criptográficas para se proteger.
- **Limites explícitos.** Não prometemos “invisibilidade total”. Explicamos contra o quê cada camada protege e onde ela termina.

## Arquitetura pretendida

```text
Apps Android ───────────────┐
Firefox / navegador ────────┤
WebView / outros apps ──────┘
            │
            ▼
┌─────────────────────────────┐
│          SOBERANIA          │
│                             │
│  Rede                       │
│  • Android VpnService       │
│  • IPv4 + IPv6              │
│  • DNS protegido            │
│  • Kill switch              │
│  • Anti-leak                │
│                             │
│  Identidade Web (opcional)  │
│  • Browser Shield           │
│  • isolamento de cookies    │
│  • storage por contexto     │
│  • bloqueio de trackers     │
│  • proteção de fingerprint  │
│                             │
│  Transporte                 │
│  • Rápido                   │
│  • Reforçado / Multi-hop    │
│  • Onion / Arti             │
└──────────────┬──────────────┘
               │
               ▼
            Internet
```

## Modos planejados

### Nível 1 — Proteção
Proteção de rede com baixo impacto de latência para uso cotidiano.

### Nível 2 — Reforçada
Encaminhamento por mais de um ponto, aumentando isolamento sem o custo completo de uma rede onion.

### Nível 3 — Anônima
Roteamento por cebola. A direção preferencial é usar **Arti**, a implementação moderna do Tor em Rust, integrada como backend separado.

### Nível 4 — Máxima
Não é um quarto protocolo. É um perfil que combina o modo Onion com kill switch obrigatório, DNS apenas pelo túnel, proteção IPv4/IPv6, sessão web descartável, zero logs persistentes e Browser Shield quando compatível.

Veja [docs/TRANSPORTS.md](docs/TRANSPORTS.md).

## Navegação protegida

Integração opcional com navegador compatível para reduzir rastreamento por cookies, storage, trackers e fingerprinting.

## O que Soberania não promete

Soberania não pode tornar um dispositivo já comprometido magicamente seguro, apagar identificação feita pela rede celular, esconder a identidade que o próprio usuário fornece ao fazer login em um serviço, nem garantir anonimato absoluto contra um adversário capaz de observar todos os pontos relevantes de uma comunicação.

Promessas impossíveis não melhoram segurança. Transparência melhora.

## Marcos

- [ ] **M0 — Coração:** APK, VpnService, TUN, IPv4/IPv6, DNS e estado real de proteção
- [ ] **M1 — Transporte rápido:** backend seguro e auditável
- [ ] **M2 — Anti-leak:** DNS, IPv6 e políticas de queda
- [ ] **M3 — Onion / Arti:** modo de anonimato
- [ ] **M4 — Browser Shield:** integração Firefox
- [ ] **M5 — Sessões descartáveis**
- [ ] **M6 — Proteção Máxima**
- [ ] **M7 — Navegação integrada com GeckoView**
- [ ] **M8 — Builds reproduzíveis e auditoria independente**

## Documentação

- [Arquitetura](docs/ARCHITECTURE.md)
- [Modelo de ameaça](docs/THREAT-MODEL.md)
- [Transportes e integração Arti](docs/TRANSPORTS.md)
- [Checkpoint do projeto](docs/PROJECT-CHECKPOINT.md)
- [M0 — Coração](docs/M0.md)
- [Política de segurança](SECURITY.md)

## Estado atual

Projeto em início de desenvolvimento. **Ainda não existe release estável.**

Não utilize versões de desenvolvimento para situações em que falhas de privacidade possam colocar alguém em risco.

## Licença

Este projeto é software livre. Consulte [LICENSE](LICENSE).

---

**Soberania**  
*Porque soberania e liberdade jamais serão negociáveis.*
