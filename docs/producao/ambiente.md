# Ambiente de desenvolvimento (onde ficam as ferramentas)

O disco C: deste computador tinha só 0,7 GB livres. Isso travava o emulador e arriscava falhar
builds. Por isso, tudo o que é do desenvolvimento Android foi movido para **`D:\AndroidDev`**
(liberou ~8 GB no C:).

| Pasta em `D:\AndroidDev` | O que é | Antes ficava em | Como os programas acham |
|---|---|---|---|
| `gradle` | Cache das bibliotecas baixadas pelo Gradle | `C:\Users\Wanderson\.gradle` | variável `GRADLE_USER_HOME` |
| `Sdk` | Android SDK (build-tools, platform-tools/adb, emulador) | `...\AppData\Local\Android\Sdk` | variáveis `ANDROID_HOME` e `ANDROID_SDK_ROOT`, `sdk.dir` no `local.properties` e configuração do Android Studio |
| `android-user` | Aparelhos virtuais (AVD), **chave de debug** (`debug.keystore`) e chave do adb (`adbkey`) | `C:\Users\Wanderson\.android` | variável `ANDROID_USER_HOME` |
| `studio` | Caches e logs do Android Studio | `...\AppData\Local\Google\AndroidStudio2026.2.2` | variável `STUDIO_PROPERTIES` → `studio\idea.properties` |
| `FashionDog-chave` | **Chave de produção** do app | `C:\Users\Wanderson\FashionDog-chave` | `storeFile` no `keystore.properties` |

O Android Studio já estava instalado em `D:\android studio`.

## Cuidados

- As variáveis de ambiente valem para **programas abertos depois** da mudança. Se algo
  procurar o SDK no lugar antigo, feche e abra de novo (terminal, Android Studio).
- **Não apague** `android-user\debug.keystore`: os APKs de teste são assinados com ela.
- **Não apague** `FashionDog-chave` sem ter as cópias de segurança (ver `assinatura-e-release.md`).
- O cache do Gradle (`gradle\caches`) pode ser apagado se ficar grande demais: ele é baixado
  de novo no próximo build (demora).

## Conferir

```powershell
[Environment]::GetEnvironmentVariable('ANDROID_HOME','User')   # D:\AndroidDev\Sdk
D:\AndroidDev\Sdk\platform-tools\adb.exe devices                  # celular conectado
```
