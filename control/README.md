# Soberania Controle

**Soberania Controle** é um APK Android separado, mantido dentro do repositório Soberania, para devolver ao usuário controles que normalmente ficam presos ao ADB de um computador.

O aplicativo usa **Shizuku**. Em modo Wireless Debugging/ADB, o processo remoto roda como **UID 2000 (shell)**. Isso não é root e não finge ser root.

## Objetivo do v0.1

- listar aplicativos instalados que possuem activity de lançamento;
- solicitar autorização do Shizuku;
- mostrar o UID real do serviço remoto;
- aplicar/remover `FORCE_RESIZE_APP` por pacote;
- tentar abrir dois apps em split screen usando windowing modes do Activity Manager;
- abrir um app explicitamente em tela cheia;
- ativar/restaurar `force_resizable_activities` global;
- liberar ou restringir execução em segundo plano com AppOps/standby bucket;
- diagnosticar suporte do dispositivo a split screen;
- mostrar informações de DPM/device policy;
- oferecer um shell avançado, limitado aos privilégios reais do Shizuku.

## O que ele NÃO faz

- não obtém root;
- não transforma magicamente o app em Device Owner;
- não contorna SELinux;
- não acessa os dados privados de outros apps;
- não promete que todo OEM obedecerá aos mesmos windowing modes;
- não mantém o Shizuku vivo após reboot quando ele foi iniciado por Wireless Debugging.

### Sobre Device Owner

O comando Android é:

```sh
adb shell dpm set-device-owner pacote/.DeviceAdminReceiver
```

Ele possui requisitos de provisionamento do próprio Android (por exemplo, ausência de contas e outros estados incompatíveis). O Soberania Controle, nesta primeira versão, **diagnostica DPM**, mas não tenta se autoprovisionar silenciosamente como Device Owner.

## Build

Na raiz do repositório:

```sh
gradle --no-daemon :control:assembleDebug
```

APK:

```text
control/build/outputs/apk/debug/control-debug.apk
```

## Filosofia

O aplicativo não coleta telemetria, não exige conta e não depende da Play Store. A interface é deliberadamente simples: ação, comando, resultado.

O aparelho é do usuário. A ferramenta deve deixar claro o que o sistema aceitou, recusou ou ignorou.

## Licença

Mesma licença do repositório Soberania.
