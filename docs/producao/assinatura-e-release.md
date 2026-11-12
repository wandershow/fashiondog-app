# Versão de produção e chave de assinatura

## O que é a chave de assinatura (e por que ela importa tanto)

Todo app Android é **assinado** com uma chave digital. O Android usa essa assinatura para
garantir que uma atualização veio da mesma pessoa que fez o app instalado:

- **Atualizar** (instalar a versão nova por cima, mantendo os dados) **só funciona se o APK novo
  tiver a mesma assinatura** do instalado.
- Com outra chave, o Android recusa ("pacote em conflito"). A única saída é desinstalar —
  e desinstalar **apaga todos os dados** do app no celular.

Por isso: **perder a chave = não conseguir mais atualizar o app sem desinstalar.**

> Como o FashionDog tem **backup**, perder a chave não é o fim do mundo: dá para fazer backup,
> desinstalar, instalar o app novo e restaurar. Mas dá trabalho e é arriscado — guarde a chave.

## Onde está a chave hoje

| O quê | Onde |
|---|---|
| Arquivo da chave | `D:\AndroidDev\FashionDog-chave\fashiondog-release.jks` |
| Senha e nome (alias) | `D:\AndroidDev\FashionDog-chave\LEIA-ME-senha-da-chave.txt` |
| Configuração para o Gradle | `D:\Fashion Dog\keystore.properties` (aponta para o arquivo acima) |

- Tipo: PKCS12, RSA 4096 bits, válida por ~27 anos. Alias: `fashiondog`.
- A senha foi gerada aleatoriamente (28 caracteres) e **não aparece em nenhum outro lugar**.
- `keystore.properties`, `*.jks` e `*.keystore` estão no `.gitignore`: **nunca vão para o GitHub**.
  O repositório é público — se a chave fosse para lá, qualquer pessoa poderia publicar um
  "FashionDog" falso capaz de substituir o verdadeiro.

## Onde guardar (faça hoje): a regra dos 3 lugares

1. **Senha no gerenciador de senhas** — Google Senhas (passwords.google.com), Bitwarden ou
   similar. Crie um item "Chave do app FashionDog" e cole a senha e o alias.
2. **Arquivo `.jks` + o LEIA-ME numa pasta privada do Google Drive** (não compartilhada).
3. **Arquivo `.jks` + o LEIA-ME num pendrive** guardado fora da loja (em casa, por exemplo).

E **não**: mandar por WhatsApp/e-mail, colocar no GitHub, deixar só no PC (se o HD queimar, perdeu).

Depois de copiar para os 3 lugares, você pode até apagar o `LEIA-ME` do PC — o Gradle usa o
`keystore.properties`. (Se trocar de computador, recrie o `keystore.properties` com os dados
do gerenciador de senhas; o modelo está no fim deste documento.)

## Tipos de build

| Tipo | ID no celular | Nome no ícone | Para quê |
|---|---|---|---|
| `debug` | `br.com.fashiondog.debug` | FashionDog (teste) | Desenvolvimento. Assinado com a chave de debug do Android Studio. |
| `homologacao` | `br.com.fashiondog.homolog` | FashionDog (homolog) | Igual à produção (otimizado e assinado), mas instala **ao lado** dela: serve para conferir uma versão antes de entregar. |
| `release` | `br.com.fashiondog` | FashionDog | **Produção.** É o que vai para os celulares da loja. |

Como os IDs são diferentes, os três podem ficar instalados no mesmo celular sem misturar dados.

## Como gerar o APK de produção

```bash
gradlew.bat assembleRelease
```

O APK sai em `app/build/outputs/apk/release/app-release.apk`. Para distribuir, copie com um nome
claro, por exemplo `FashionDog-1.0.0.apk` (a pasta `dist/` do projeto está no `.gitignore` para isso).

### Antes de cada nova versão

1. Aumente **`versionCode`** (1 → 2 → 3...) em `app/build.gradle.kts`. O Android só aceita
   atualizar para um `versionCode` **maior** que o instalado.
2. Ajuste **`versionName`** ("1.0.0" → "1.1.0"), que é o número que as pessoas veem.
3. Gere o APK de homologação, instale ao lado e teste; depois gere o de produção.
4. Faça **backup** no app antes de atualizar o celular da loja (por segurança).

### Conferir a assinatura de um APK

```bash
D:\AndroidDev\Sdk\build-tools\36.0.0\apksigner.bat verify --print-certs FashionDog-1.0.0.apk
```

Deve mostrar `CN=FashionDog, O=FashionDog Banho e Tosa, L=Rio Grande, ST=RS, C=BR`.

## O que muda na versão de produção

- **Otimizada (R8):** remove código não usado das bibliotecas; o APK fica menor e mais rápido.
- **Não "depurável":** ninguém consegue copiar o banco do app pelo cabo USB (no app de debug
  isso é possível — foi assim que o problema do login foi investigado).

## Modelo do `keystore.properties`

```properties
storeFile=D:/AndroidDev/FashionDog-chave/fashiondog-release.jks
storePassword=<senha do gerenciador de senhas>
keyAlias=fashiondog
keyPassword=<a mesma senha>
```
