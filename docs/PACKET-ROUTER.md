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
        ├── duplicate()      -> consumidor Kotlin/Java
        ├── duplicateOwned() -> backend com ownership explícito
        └── close()          -> fecha somente o original
```

O descritor original nunca é entregue a um backend.

## Dois caminhos de dados

### Packet Direct

Para um backend `PACKET_TUNNEL`, o Packet Router **não deve ficar lendo pacotes em paralelo**.

```text
TUN original
    │
    ▼
duplicateOwned()
    │
    ▼
PacketRouter
    │
    ▼
PacketTunnelBackend
    │
    ▼
motor de túnel
```

Ao chamar `PacketTunnelBackend.start()`, o backend recebe ownership da duplicata e deve liberá-la mesmo se a inicialização falhar.

Esse é o caminho pretendido para transportes que trabalham diretamente com uma interface TUN.

### TUN to Stream

```text
TUN original
 │
 ▼
duplicata
 │
 ▼
Stream Bridge / userspace TCP-IP
 │
 ▼
streams / SOCKS / API equivalente
 │
 ▼
StreamProxyBackend
 │
 ▼
Arti
 │
 ▼
Tor
```

O modo Onion precisa do segundo caminho.

## TransportRuntime

Backends não recebem acesso direto ao `VpnService`.

Recebem um `TransportRuntime` restrito. Atualmente ele expõe somente:

```text
protectSocket(fd)
```

Isso permite retirar os sockets do próprio transporte do roteamento da VPN e evita loop de túnel, sem entregar poderes desnecessários ao backend.

## OwnedTunDescriptor

`OwnedTunDescriptor` representa uma duplicata que pertence a exatamente um consumidor.

Pode:

- ser fechada normalmente; ou
- transferir o `ParcelFileDescriptor`; ou
- transferir o FD bruto com `detachRawFd()`.

Depois de `detachRawFd()`, o código receptor/nativo passa a ser responsável pelo fechamento.

## M0

M0 já possui um `LabPacketRouter` executável.

No caminho `PACKET_DIRECT`:

1. o router cria `duplicateOwned()`;
2. entrega ao `LabPacketBackend`;
3. o backend lê a duplicata;
4. classifica IPv4/IPv6;
5. mantém apenas contadores em RAM;
6. não encaminha tráfego externo.

O antigo `TunLabProbe` foi removido para evitar dois consumidores lendo a mesma TUN.

## Descoberta para o Nível 1

A implementação userspace oficial do WireGuard para Android entrega o FD da TUN ao wireguard-go e protege os sockets do próprio túnel para evitar recursão.

Isso confirma a necessidade de:

- ownership explícito da duplicata;
- backend de pacote fora do caminho de leitura do router;
- capacidade restrita de `protectSocket()`.

Detalhes de avaliação estão em `docs/LEVEL1-TRANSPORT.md`.

## Próxima decisão

Para o caminho Onion, comparar opções para a ponte TUN -> stream:

- implementação baseada em tun2socks madura e auditável;
- stack TCP/IP userspace reutilizável;
- implementação própria apenas se houver justificativa técnica forte.

A escolha deve considerar licença, manutenção, IPv6, UDP, DNS, performance, JNI/NDK e superfície de ataque.
