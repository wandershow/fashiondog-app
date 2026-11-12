# Como instalar o FashionDog em um celular (pelo arquivo APK)

O FashionDog não está na Play Store. Ele é instalado direto pelo arquivo **APK** — o
"instalador" do app, como um `.exe` do Windows.

- Arquivo de produção: **`FashionDog-1.0.0.apk`** (3 MB), na pasta `D:\Fashion Dog\dist\`.
- Funciona em **Android 8.0 ou mais novo**.
- É o mesmo APK para todos os celulares da loja. Cada celular tem os **próprios dados**
  (o app é offline): instalar em outro celular **não** copia pets, agenda nem financeiro.
  Para levar os dados, use o **backup** (seção 5).

---

## 1. Levar o APK até o celular (escolha um jeito)

| Jeito | Como |
|---|---|
| **Cabo USB** (mais seguro) | Ligue o celular no PC → no celular, toque na notificação "USB" e escolha **Transferência de arquivos** → no PC, abra o celular no Explorador de Arquivos e copie o APK para a pasta **Download**. |
| **Google Drive** | No PC, envie o APK para o seu Drive → no celular, abra o app Drive, toque no arquivo → **Baixar**. |
| **WhatsApp** (para você mesmo) | Envie o APK como **Documento** (não como foto/arquivo de mídia) para um contato seu ou "Você" → no celular, toque no arquivo. |

> ⚠️ Só instale APK que **você mesmo gerou** ou recebeu de quem gerou. APK de origem
> desconhecida pode ser vírus.

## 2. Liberar a instalação de "apps desconhecidos" (só na primeira vez)

O Android bloqueia instalar apps fora da Play Store até você autorizar **o app que vai abrir o
APK** (Meus Arquivos, Drive, WhatsApp ou Chrome).

1. Toque no arquivo `FashionDog-1.0.0.apk`.
2. Aparece: *"Por motivos de segurança, seu telefone não tem permissão para instalar apps
   desconhecidos desta fonte"* → toque em **Configurações**.
3. Ative **Permitir desta fonte** → volte (seta ◀).

Caminho manual, se precisar (Samsung): **Configurações → Aplicativos → ⋮ (menu) → Acesso
especial → Instalar apps desconhecidos →** escolha *Meus Arquivos* (ou Drive/WhatsApp) **→
Permitir**. Em outras marcas o caminho é parecido: procure "apps desconhecidos" nas Configurações.

## 3. Instalar

1. Toque em **Instalar**.
2. O **Google Play Protect** pode avisar *"App não reconhecido"* ou *"Bloqueado pelo Play
   Protect"*. É normal: ele não conhece o app porque não veio da Play Store. Toque em
   **Mais detalhes → Instalar mesmo assim**. (Se pedir para "enviar o app para verificação",
   pode escolher **Não enviar**.)
3. Toque em **Abrir**.
4. Depois de instalar, você pode **desativar** de novo a permissão do passo 2 (mais seguro).

## 4. Primeiro uso no celular novo

- **Celular da proprietária (primeiro de todos):** o app abre em *Bem-vindo ao FashionDog* →
  crie a conta de proprietária → **anote o código de recuperação no papel**.
- **Permitir notificações** quando a Agenda pedir.
- **Bateria (Samsung):** para os avisos não atrasarem: Configurações → Aplicativos →
  FashionDog → **Bateria → Sem restrições**.

## 5. Levar os dados de um celular para outro (troca de aparelho)

1. No celular **antigo**: Mais → **Backup → Salvar backup** → salve no **Google Drive**.
2. No celular **novo**: instale o APK (passos 1–3) e crie uma conta de proprietária qualquer
   (é temporária).
3. Mais → **Backup → Restaurar** → escolha o arquivo `fashiondog-backup-....zip` no Drive.
4. O app reinicia. Entre com o login e a senha **do celular antigo**.

## 6. Atualizar para uma versão nova

1. **Faça um backup** antes (Mais → Backup), por segurança.
2. Instale o APK novo **por cima** (mesmos passos 1 e 3). Aparece **"Atualizar"** em vez de
   "Instalar" — os dados continuam lá.
3. **Nunca desinstale** para atualizar: desinstalar **apaga todos os dados** do celular.

Se aparecer *"App não instalado"* ou *"pacote em conflito"*:
- o APK novo foi assinado com **outra chave** (ver `assinatura-e-release.md`), ou
- o `versionCode` do APK novo é **menor ou igual** ao instalado.

## 7. Caso especial: o celular que hoje tem o app de teste

O celular onde o app foi testado durante o desenvolvimento tem a versão **debug**, assinada com
outra chave. Por isso a versão de produção **não instala por cima** dela. Para trocar sem perder
nada:

1. No app atual: Mais → **Backup → Salvar backup** (no Drive ou em Downloads).
2. Desinstale o app atual (Configurações → Aplicativos → FashionDog → **Desinstalar**).
3. Instale `FashionDog-1.0.0.apk` (passos 1–3).
4. Crie uma conta temporária → Mais → **Backup → Restaurar** → escolha o backup do passo 1.
5. Entre com o seu login e senha de antes.

> Se o celular só tinha dados de teste, pode pular os passos 1 e 4 e começar do zero.
