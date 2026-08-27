# Política de Checkpoints do Soberania

## Objetivo

Checkpoints existem para preservar continuidade entre sessões e reduzir o custo de reconstruir decisões arquiteturais importantes.

Eles são **fotografias do estado do projeto**, não substitutos da documentação viva.

## Quando criar um checkpoint

Criar ou atualizar um checkpoint quando ocorrer pelo menos uma destas condições:

1. conclusão de um marco técnico;
2. mudança arquitetural relevante;
3. escolha de protocolo, biblioteca ou dependência crítica;
4. alteração do threat model;
5. decisão difícil de reconstruir apenas lendo o código;
6. separação ou criação de uma funcionalidade grande;
7. antes de uma etapa de alto risco técnico;
8. ao final de uma sessão longa quando houve avanço substancial;
9. sempre que a continuidade futura puder depender de contexto conversacional.

## Formato

Preferência: Markdown UTF-8.

Nome recomendado:

```text
SOBERANIA_PROJECT_CHECKPOINT_YYYY-MM-DD.md
```

Se houver mais de um checkpoint relevante no mesmo dia, acrescentar hora ou sequência.

## Conteúdo mínimo

Um checkpoint deve registrar:

- identidade/nome/lema do projeto quando necessário;
- objetivo atual;
- princípios congelados;
- decisões arquiteturais vigentes;
- estado real do código;
- funcionalidades separadas e suas fronteiras;
- arquivos-chave;
- riscos/limitações conhecidos;
- questões abertas;
- próximos passos;
- decisões que **não** devem ser revertidas sem revisão consciente.

## Regra de honestidade

Checkpoint não pode declarar funcionalidade como concluída apenas porque ela foi planejada ou documentada.

Usar linguagem explícita:

- **implementado**
- **em laboratório**
- **planejado**
- **em avaliação**
- **bloqueado**
- **não implementado**

## Relação com as fontes do projeto

Quando um checkpoint for criado e exportado, ele pode ser anexado às fontes do projeto para recuperar contexto em sessões futuras.

O arquivo canônico dentro do repositório permanece:

`docs/PROJECT-CHECKPOINT.md`

Checkpoints datados podem ser mantidos apenas quando trouxerem valor histórico; evitar duplicação inútil.

## Princípio

**Preservar contexto suficiente para continuar — não acumular contexto por acumular.**
