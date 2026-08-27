# Soberania — Adaptador WireGuard Nativo

## Estado

**Isolado. Ainda não ligado ao módulo Android `app`.**

O adaptador já compila em CI para **arm64-v8a**, incluindo o shared object Go e o JNI shim. Ele continua deliberadamente fora do APK M0.

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

## Build de laboratório validado

Workflow: `.github/workflows/wireguard-native-ci.yml`

Validado:

- Go 1.24.3;
- NDK 28.2.13676358;
- `go mod verify`;
- wireguard-go fixado;
- `-buildmode=c-shared`;
- JNI shim linkado;
- ELF arm64-v8a válido.

## Ainda não fazer

Não adicionar este módulo a `app/build.gradle.kts` antes de:

1. validar o APK M0 em dispositivo físico;
2. adicionar testes de ownership de FD;
3. revisar empacotamento das duas bibliotecas compartilhadas;
4. criar uma variante de laboratório que não possa ser confundida com release protegida.
