# Modelo de Ameaça

Este documento define o que Soberania pretende proteger, contra quem e onde estão os limites.

## Regra principal

**Privacidade, anonimato e anticensura são problemas relacionados, mas diferentes.**

Nenhuma função deve ser anunciada apenas como "segura". Deve existir uma ameaça identificável que ela reduz.

## Adversários considerados

### Rede Wi-Fi não confiável

Objetivo: impedir leitura do conteúdo protegido e reduzir observação direta de destinos através da rede local.

### Provedor de internet

Objetivo: reduzir a capacidade de associar tráfego de aplicação a destinos finais quando um túnel apropriado estiver ativo.

### Resolver DNS local

Objetivo: impedir consultas DNS fora da política definida pelo usuário.

### Site remoto

Objetivo: impedir que o site receba o endereço IP original quando o tráfego estiver corretamente roteado pelo Soberania.

### Rastreadores web

Objetivo: reduzir correlação através de cookies, armazenamento, parâmetros de rastreamento e técnicas de fingerprinting quando o Browser Shield estiver disponível.

### Aplicativo tentando contornar o túnel

Objetivo: detectar ou bloquear saída fora da política de proteção, dentro dos mecanismos oferecidos pelo Android.

### Observador de rede sofisticado

Modo anônimo busca elevar substancialmente o custo de correlação de origem e destino. Soberania não promete anonimato absoluto contra um adversário global capaz de observar simultaneamente múltiplos pontos relevantes.

## Fora do escopo

Soberania não transforma em confiável um sistema operacional já comprometido.

Também não pode garantir ocultação contra:

- malware com privilégios suficientes no próprio aparelho;
- baseband/modem comprometido;
- identificação física pela rede celular;
- IMEI/SIM vistos pela operadora;
- GPS fornecido pelo próprio usuário a um aplicativo;
- contas nas quais o usuário deliberadamente se identifica;
- comprometimento físico do dispositivo;
- vulnerabilidades desconhecidas no sistema operacional ou hardware.

## TLS e HTTPS

Soberania não instalará uma autoridade certificadora própria para interceptar HTTPS.

Consequências:

- senhas permanecem fora do alcance do módulo de rede;
- conteúdo HTTPS não é inspecionado pelo VPN core;
- cookies protegidos por TLS só podem ser tratados dentro de um navegador que ofereça uma integração legítima.

Essa limitação é deliberada e reduz a superfície de confiança.

## Fingerprinting

O objetivo preferencial é reduzir distinguibilidade, não produzir valores aleatórios ilimitados.

Randomização agressiva pode criar uma configuração rara e, portanto, mais identificável.

Qualquer estratégia de fingerprinting deve ser medida empiricamente antes de ser habilitada por padrão.

## Falha segura

Quando a política ativa exigir proteção total:

```text
proteção válida    -> conexão permitida
proteção inválida  -> conexão bloqueada
```

Nunca:

```text
proteção inválida  -> fallback silencioso para rede normal
```

## Logs

Por padrão, Soberania não deve persistir:

- histórico de destinos;
- consultas DNS individuais;
- conteúdo de pacotes;
- credenciais;
- identificadores publicitários;
- identificadores de analytics.

Informações de diagnóstico devem ser mínimas, locais e explicitamente exportadas pelo usuário.

## Transparência

O modelo de ameaça deve evoluir junto do código.

Se uma implementação não corresponde mais a este documento, um dos dois está errado — e a divergência deve ser corrigida publicamente.
