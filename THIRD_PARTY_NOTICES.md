# Third-Party Notices

Este arquivo registra componentes de terceiros utilizados ou planejados pelo Soberania.

> A presença nesta lista não significa necessariamente que o componente já esteja empacotado no APK.

## WireGuard — wireguard-go

**Status:** dependência fixada do módulo nativo de laboratório; ainda não empacotada no APK normal.

- Projeto: `WireGuard/wireguard-go`
- Módulo: `golang.zx2c4.com/wireguard`
- Licença: MIT
- Uso pretendido: motor userspace do transporte rápido / Nível 1
- Versão de laboratório fixada: `v0.0.0-20250521234502-f333402bd9cb`
- Política: não modificar primitivas criptográficas; fixar versão/commit no build

Copyright e licença permanecem pertencentes aos respectivos autores upstream.

## WireGuard — wgctrl / wgtypes

**Status:** candidato planejado para geração, parsing e derivação de chaves; ainda não adicionado ao build.

- Projeto: `WireGuard/wgctrl-go`
- Pacote pretendido: `golang.zx2c4.com/wireguard/wgctrl/wgtypes`
- Licença: MIT
- Commit de referência estudado: `a9ab2273dd1075ea74b88c76f8757f8b4003fcbf`
- APIs públicas relevantes: `GeneratePrivateKey()`, `ParseKey()`, `PublicKey()`

A dependência só deve ser adicionada após pinning explícito e revisão da árvore de módulos.

## WireGuard Android

**Status:** referência técnica; não copiado para o Soberania.

- Projeto: `WireGuard/wireguard-android`
- Licença dos arquivos inspecionados: Apache-2.0
- Uso: estudo da integração Android oficial, ownership da TUN e socket protection

## Arti / Tor

**Status:** planejado; ainda não empacotado.

A licença, versão e cadeia de dependências serão registradas antes da integração.

## M17 / Codec2

**Status:** planejado para a funcionalidade separada Comunicação de Emergência; ainda não empacotado.

Licenças e versões serão registradas antes da integração.
