# Soberania — Adaptador WireGuard Nativo

## Estado

**Isolado. Ainda não ligado ao módulo Android `app`.**

Este diretório prepara o backend userspace do Nível 1 sem alterar o APK M0 já validado pelo CI.

## Objetivo

Criar a menor ponte possível entre:

```text
WireGuardNativeEngine (Kotlin)
        │
        ▼
JNI mínimo
        │
        ▼
adaptador Go do Soberania
        │
        ▼
wireguard-go upstream
```

O adaptador **não implementa o protocolo WireGuard nem suas primitivas criptográficas**.

## Princípios

- uma única TUN, criada pelo `SoberaniaVpnService`;
- o motor recebe somente uma duplicata com ownership explícito;
- nenhum segundo `VpnService`;
- nenhum `GoBackend`;
- nenhuma reflection;
- nenhum log persistente;
- logger do wireguard-go em modo silencioso por padrão;
- versões/toolchain fixadas;
- dependências de terceiros mantêm suas licenças.

## Arquivos

- `versions.mk` — versões e hashes congelados;
- `go.mod` — versão fixada do wireguard-go;
- `adapter/main.go` — API C mínima exportada pelo Go;
- `jni/soberania_wireguard_jni.c` — ponte JNI para Kotlin.

## Ownership

Ao entrar em `soberaniaWgTurnOn`, o FD já pertence ao adaptador nativo.

Se `tun.CreateUnmonitoredTUNFromFD()` falhar, o adaptador fecha explicitamente o FD.

Depois que a TUN userspace é criada com sucesso, o objeto `tun.Device` passa a possuir o descritor e seu fechamento ocorre via `device.Close()`.

## Ainda não fazer

Não adicionar este módulo a `app/build.gradle.kts` antes de:

1. validar o APK M0 em dispositivo físico;
2. gerar `go.sum` reproduzivelmente;
3. compilar este módulo isoladamente no CI;
4. revisar símbolos JNI/ABIs;
5. adicionar testes de ownership de FD.
