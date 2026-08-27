# Nível 1 — Transporte Rápido

## Status

**Em avaliação. Ainda não integrado.**

O candidato principal para o primeiro transporte real continua sendo WireGuard por sua maturidade, simplicidade de protocolo, desempenho e ecossistema open source.

Nenhuma dependência WireGuard foi adicionada ao APK nesta etapa.

## Descoberta arquitetural importante

A implementação userspace oficial do WireGuard para Android trabalha diretamente sobre um descritor TUN.

O fluxo usado pelo backend oficial é conceitualmente:

```text
VpnService
   │
   ▼
  TUN
   │
   ▼
wireguard-go
   │
   ▼
sockets UDP protegidos
   │
   ▼
rede subjacente
```

Os sockets do próprio transporte precisam sair do roteamento da VPN. No Android isso é feito com `VpnService.protect()`; caso contrário, o transporte pode voltar para a própria TUN e criar um loop.

## Consequência para o Soberania

Soberania possui seu próprio `VpnService` e precisa manter um único ponto de controle da TUN.

Portanto, não devemos simplesmente instanciar um backend que queira:

1. iniciar outro `VpnService`;
2. criar outra TUN;
3. assumir controle do lifecycle da VPN inteira.

A fronteira adotada pelo Soberania é:

```text
SoberaniaVpnService
       │
       ▼
 TUN ORIGINAL
       │
       ▼
   TunHandle
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
duplicata da TUN
```

O descritor original nunca é transferido ao backend.

## Ownership

`PacketTunnelBackend.start()` recebe um `OwnedTunDescriptor`.

Regra:

- ao entrar em `start()`, o backend passa a ser dono da duplicata;
- o backend deve fechá-la mesmo em caso de erro;
- se a duplicata for convertida em FD bruto com `detachRawFd()`, o código nativo passa a ser responsável por fechá-lo;
- o `SoberaniaVpnService` continua dono apenas da TUN original.

Esse desenho permite integração com motores nativos sem entregar a eles o descritor original.

## Runtime restrito

Backends não recebem o `VpnService` inteiro.

Recebem somente `TransportRuntime`, que atualmente expõe:

```text
protectSocket(fd)
```

Isso é suficiente para permitir que o transporte retire seus sockets do roteamento da própria VPN sem ganhar acesso desnecessário ao restante do serviço.

## WireGuard oficial — cuidado de integração

A biblioteca Android oficial é publicamente descrita como uma biblioteca embutível. Porém, o backend userspace padrão mantém sua própria implementação de `VpnService` e cria sua própria TUN.

Para o Soberania isso exige avaliação antes da integração.

Não usar:

- reflexão para acessar métodos privados;
- cópia cega de código interno;
- segundo `VpnService` concorrente;
- hacks de file descriptor;
- dependência não versionada.

Precisamos escolher uma integração suportável que preserve:

- TUN única;
- socket protection;
- ownership claro;
- atualizações futuras;
- auditabilidade;
- licença compatível.

## Próximos passos

1. validar o M0 em build/aparelho;
2. confirmar ownership TUN -> backend em laboratório;
3. avaliar a API publicada da biblioteca `com.wireguard.android:tunnel`;
4. verificar se existe ponto de integração estável para TUN externa;
5. se não existir, avaliar integração explícita e auditável com a camada nativa `wireguard-go`;
6. só adicionar a dependência depois de a fronteira estar definida;
7. testar socket protection antes de ativar rota default.

## Regra

**Nível 1 só será chamado de protegido quando existir caminho completo TUN -> transporte criptográfico -> rede -> retorno e os testes anti-leak estiverem aprovados.**
