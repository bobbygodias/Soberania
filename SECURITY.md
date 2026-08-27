# Política de Segurança

Soberania trata vulnerabilidades de privacidade como falhas de segurança.

## Ainda não há release estável

O projeto está em desenvolvimento inicial. Não utilize builds experimentais em situações em que uma falha de proteção possa causar dano real.

## Princípios para relatos

Ao relatar uma vulnerabilidade, tente incluir:

- versão/commit afetado;
- versão do Android;
- fabricante/modelo quando relevante;
- passos mínimos para reproduzir;
- comportamento esperado;
- comportamento observado;
- evidências de vazamento, se houver.

Nunca inclua credenciais reais, chaves privadas ou informações pessoais de terceiros em issues públicas.

## Classes prioritárias

Tratamos como críticas, entre outras:

- tráfego escapando do túnel sem aviso;
- vazamento DNS;
- vazamento IPv6;
- bypass de kill switch;
- armazenamento inadvertido de histórico;
- exposição de chaves;
- possibilidade de interceptação indevida de HTTPS;
- atualização não autenticada;
- telemetria ou conexão externa não documentada.

## Dependências

Toda dependência nova deve responder:

1. Por que ela é necessária?
2. É open source?
3. Qual licença utiliza?
4. Possui telemetria?
5. Qual superfície de ataque adiciona?
6. Há alternativa menor ou já presente no projeto?

Dependências devem ter versões fixadas quando isso melhorar reprodutibilidade e segurança.

## Filosofia

Segurança não será tratada como marketing.

Quando encontrarmos uma limitação, documentaremos a limitação.
