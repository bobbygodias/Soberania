# Transportes do Soberania

## Regra de arquitetura

Soberania separa **transporte** de **política de proteção**.

Isso evita misturar conceitos diferentes e permite substituir implementações sem reescrever o aplicativo inteiro.

```text
                 SOBERANIA
                     │
                     ▼
              ProtectionProfile
                     │
                     ▼
              TransportBackend
                     │
       ┌─────────────┼─────────────┐
       ▼             ▼             ▼
     RÁPIDO       MULTI-HOP       ONION
       │             │             │
       ▼             ▼             ▼
   backend A      backend B       Arti
```

## Rápido

Objetivo:

- baixa latência;
- proteção de rede para uso cotidiano;
- criptografia madura e auditável;
- reconexão rápida;
- suporte a roaming entre Wi-Fi e rede móvel.

A implementação concreta ainda não está congelada no M0.

## Reforçado / Multi-hop

Objetivo:

- encaminhar o tráfego por mais de um ponto;
- reduzir confiança em um único relay;
- manter desempenho melhor que onion routing completo.

Multi-hop não deve ser descrito como Tor nem como onion routing.

## Onion

Objetivo:

- anonimato de baixa latência usando roteamento por cebola;
- separar origem e destino através de múltiplos relays;
- reutilizar uma rede de anonimato madura em vez de criar uma pequena rede própria e chamá-la de "mais segura".

### Backend pretendido: Arti

A direção preferencial para o modo Onion é integrar **Arti**, a implementação moderna do Tor em Rust.

Arquitetura pretendida:

```text
Android / Kotlin
      │
      ▼
SoberaniaVpnService
      │
      ▼
TUN
      │
      ▼
Packet Router
      │
      ▼
Stream Bridge / tun2socks
      │
      ▼
TransportBackend (STREAM_PROXY)
      │
      ▼
JNI / FFI
      │
      ▼
Rust / Arti
      │
      ▼
Tor Network
```

## Por que não implementar nossa própria cebola como proteção principal?

Criar camadas criptográficas é apenas uma parte pequena do problema.

Uma rede de anonimato real precisa também lidar com:

- seleção e rotação de relays;
- guards;
- diretório e consenso;
- resistência a Sybil;
- construção de circuitos;
- negociação de chaves;
- forward secrecy;
- isolamento de streams;
- congestion control;
- bridges e anticensura;
- resistência a negação de serviço;
- diversidade operacional;
- um conjunto de anonimato suficientemente grande.

Uma implementação experimental própria pode existir futuramente em laboratório, mas não deve substituir uma rede madura apenas por ser "nossa".

## Nível 4 — Máxima

O Nível 4 **não é o quarto transporte**.

É um perfil que combina:

```text
ONION
  +
kill switch obrigatório
  +
DNS somente pelo túnel
  +
IPv4 protegido
  +
IPv6 protegido
  +
sessão web descartável
  +
storage efêmero
  +
zero logs persistentes
  +
Browser Shield quando compatível
```

Assim, melhorar o modo Onion melhora automaticamente o núcleo da Proteção Máxima sem duplicar código.

## Estado real

Todo backend deve expor seu estado ao Soberania.

Estados mínimos:

- parado;
- iniciando;
- pronto;
- falhou.

A interface não pode marcar "protegido" antes de o backend informar que está efetivamente pronto.

## Próximo marco

Antes de integrar Arti:

1. finalizar M0 e provar o ciclo do VpnService;
2. definir o caminho TUN -> packet router -> backend de pacote OU stream bridge -> backend de stream;
3. definir ownership do file descriptor TUN;
4. criar testes de ida e volta;
5. só então adicionar Rust/JNI e Arti.

Isso reduz a chance de confundir falhas de Android, JNI, TUN e Tor na mesma etapa.
