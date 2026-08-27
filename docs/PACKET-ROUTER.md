# Packet Router

## Função

O Packet Router é a fronteira entre a interface TUN criada pelo Android e os motores de transporte.

Ele existe para impedir que `SoberaniaVpnService` conheça detalhes internos de WireGuard, multi-hop, tun2socks, JNI ou Arti.

## Ownership da TUN

Regra canônica:

```text
SoberaniaVpnService
        │
        ▼
   TUN original
        │
        ▼
    TunHandle
        │
        ├── duplicate() -> consumidor A
        ├── duplicate() -> consumidor B
        └── close()     -> fecha somente o original
```

Cada consumidor fecha a sua própria duplicata.

Nenhum backend deve assumir silenciosamente ownership do descritor original.

## Dois caminhos de dados

### Packet Direct

```text
TUN
 │
 ▼
Packet Router
 │
 ▼
backend compatível com pacote/túnel
```

### TUN to Stream

```text
TUN
 │
 ▼
Packet Router
 │
 ▼
userspace TCP/IP / stream bridge
 │
 ▼
SOCKS ou API de streams
 │
 ▼
Arti
 │
 ▼
Tor
```

O modo Onion precisa do segundo caminho.

## Por que essa distinção existe?

Uma TUN entrega pacotes IP.

Arti oferece conectividade Tor orientada a streams/conexões. Portanto, ligar o file descriptor da TUN diretamente ao Arti não resolve o problema de transporte de todos os aplicativos Android.

É necessário converter o tráfego IP em fluxos que o backend Onion consiga transportar.

## M0

No M0, o Packet Router é apenas um contrato. Nenhuma rota default será instalada até existir:

1. caminho de dados funcional;
2. backend funcional;
3. tratamento de falhas;
4. teste de ida e volta;
5. comportamento de fail-closed validado.

## Próxima decisão

Comparar opções para a ponte TUN -> stream:

- implementação baseada em tun2socks madura e auditável;
- stack TCP/IP userspace reutilizável;
- implementação própria apenas se houver justificativa técnica forte.

A escolha deve considerar licença, manutenção, IPv6, UDP, DNS, performance, JNI/NDK e superfície de ataque.
