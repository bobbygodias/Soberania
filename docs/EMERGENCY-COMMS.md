# Comunicação de Emergência — Recurso Opcional

## Status arquitetural

**Este recurso é completamente separado dos níveis de proteção do Soberania.**

Ele não é:

- Nível 1;
- Nível 2;
- Nível 3;
- Nível 4;
- uma extensão do modo Onion;
- um modo de anonimato;
- uma promessa de comunicação privada.

É uma funcionalidade extra destinada a oferecer **uma rota de voz via RoIP para redes/gateways de rádio**, quando isso for útil e legalmente permitido.

## Princípio de honestidade

A interface deve dizer claramente:

> **Este recurso não oferece anonimato.**
>
> Redes de radioamador e gateways podem exigir ou divulgar indicativo, Radio ID, destino, horário e outros metadados. A parte RF pode ser pública e observável.

Não usar linguagem como "seguro", "protegido" ou "anônimo" sem qualificação técnica específica.

## Arquitetura

```text
                         SOBERANIA
                            │
          ┌─────────────────┴─────────────────┐
          │                                   │
          ▼                                   ▼
  PROTEÇÃO DIGITAL                  RECURSO EXTRA SEPARADO
          │                                   │
  Níveis 1 / 2 / 3 / 4               Comunicação de Emergência
                                              │
                                              ▼
                                      Cliente RoIP / Voz
                                              │
                                              ▼
                                     Gateway / Refletor / Nó
                                              │
                                              ▼
                                             RF
```

Não existe herança de política entre os dois lados.

## Limitação física essencial

Um smartphone Android sem hardware de rádio apropriado **não vira um transceptor VHF/UHF por software**.

O caminho normal é:

```text
Telefone
   │
   │ Internet / rede IP
   ▼
Gateway ou refletor
   │
   │ ligação com equipamento de rádio
   ▼
RF / repetidora / estação
```

Portanto, se não houver:

- Internet;
- rede IP local que alcance um gateway;
- ou hardware externo apropriado;

o recurso, sozinho, não cria conectividade de rádio.

Essa limitação deve aparecer na interface e na documentação para que o usuário não dependa do recurso em uma situação onde ele não possa funcionar.

## Uso de emergência

"Comunicação de Emergência" é o **caso de uso do recurso**, não uma alegação de exceção automática às regras do serviço de rádio.

Regulamentos, requisitos de habilitação, identificação, criptografia e uso em emergência variam por jurisdição.

O software deve:

- permitir informar a jurisdição/perfil regulatório futuramente;
- apresentar avisos claros;
- não assumir que "emergência" elimina requisitos legais;
- não esconder indicativo quando a rede/protocolo exigir identificação;
- não criar mecanismos destinados a burlar regras de RF.

## Privacidade

A sessão de voz possui uma fronteira de confiança própria.

```text
Microfone
   │
   ▼
App
   │
   ▼
Rede IP
   │
   ▼
Gateway
   │
   ▼
RF
```

O recurso deve assumir que:

- o gateway pode conhecer a origem da conexão IP;
- a rede pode exigir identidade de rádio;
- o destino pode registrar metadados;
- a parte RF é observável;
- voz e metadados podem deixar de ter confidencialidade ao entrar no ecossistema de rádio.

Por isso, **nenhum selo de proteção do Soberania será mostrado dentro da tela de PTT**.

## Identidade de rádio

Dados possíveis:

- indicativo da estação;
- indicativo do operador, quando aplicável;
- Radio ID, quando exigido;
- destino/nó/refletor.

Esses dados devem ficar locais por padrão e ser enviados somente quando o protocolo ou a rede exigir.

Eles não pertencem a:

- analytics;
- telemetria;
- contas do Soberania;
- perfil de publicidade;
- sincronização automática em nuvem.

## Protocolos candidatos

### Prioridade 1 — M17

Preferência inicial por:

- protocolo aberto;
- Codec2;
- ecossistema open source;
- boa aderência filosófica ao projeto.

### Prioridade 2 — AllStar / IAX2

Útil para interligação com nós e repetidoras analógicas conectadas por IP.

### Prioridade 3 — EchoLink / SvxLink

Pode ampliar interoperabilidade com infraestrutura existente.

DMR, D-STAR, YSF e outros modos com dependências/licenças mais complexas ficam para avaliação posterior.

## Áudio

Pipeline planejado:

```text
AudioRecord
   │
   ▼
PCM mono
   │
   ▼
jitter buffer
   │
   ▼
codec
   │
   ▼
packetizer
   │
   ▼
gateway
```

Recepção:

```text
gateway
   │
   ▼
decoder
   │
   ▼
codec
   │
   ▼
PCM
   │
   ▼
AudioTrack
```

Objetivo: baixa latência e inteligibilidade, não áudio hi-fi.

## PTT

Estados mínimos:

```text
INATIVO
   │
   ▼
PRONTO
   │
   ├── RECEBENDO
   │
   └── TRANSMITINDO
```

Falha de rede durante transmissão deve encerrar o estado local de TX e apresentar falha explícita.

## Segurança local

Mesmo sendo uma função sem promessa de anonimato, ela continua obedecendo princípios básicos do projeto:

- zero telemetria;
- nenhuma gravação automática;
- nenhum upload de áudio pelo Soberania;
- nenhum histórico de voz por padrão;
- credenciais externas armazenadas localmente quando necessárias;
- nenhum acesso ao microfone fora de sessão ativa;
- indicador visual inequívoco de TX.

## Isolamento de código

A funcionalidade deve permanecer em namespace/pacote próprio e, quando a arquitetura do app amadurecer, pode ser promovida a módulo Gradle separado.

Ela **não deve importar `ProtectionProfile` nem decidir qual nível de proteção está ativo**.

Se no futuro o tráfego IP desse recurso atravessar alguma rota de rede protegida, isso será uma propriedade externa da conectividade do aparelho, não uma característica prometida pelo módulo de emergência.

## Próximos passos próprios

O desenvolvimento desta funcionalidade possui roadmap independente:

1. captura de microfone local;
2. reprodução local;
3. PTT local sem rede;
4. Codec2;
5. framing M17;
6. conexão a refletor/gateway de laboratório;
7. medição de jitter/perda/latência;
8. identificação correta;
9. AllStar/IAX2;
10. EchoLink/SvxLink.

Esses passos **não bloqueiam** M0/M1/M2/M3 da parte principal do Soberania.
