# Sprint 5 — Login, perfis, backup e acabamento

**Branch:** `feature/sprint-5-login-backup` (criada a partir da Sprint 4) · **Status:** concluída

## Objetivo

Proteger o app com **login**, separar o que **proprietário** e **funcionário** podem fazer,
permitir **backup e restauração** de todos os dados e fotos, e dar o acabamento final.

## O que foi entregue

| Tela | O que faz |
|---|---|
| **Primeiro acesso** | Na primeira abertura, cria a conta do proprietário (nome, login, senha) e mostra o **código de recuperação** (ex.: `K7QM-3XRP-T9WD`) uma única vez, com botão de copiar. |
| **Login** | Login e senha. O app lembra quem entrou (DataStore) até a pessoa tocar em **Sair**. |
| **Recuperar senha** | Proprietário: login + código de recuperação + senha nova. Gera um **código novo** (cada código vale uma vez). Funcionário: pede ao proprietário. |
| **Mais → Usuários** (dono) | Lista de usuários; **Novo funcionário**; tocar num usuário: definir nova senha, desativar/reativar. |
| **Mais → Backup** (dono) | **Salvar backup** (.zip com banco + fotos, onde a pessoa escolher) e **Restaurar** (com confirmação), depois reinicia o app. |
| **Mais → Trocar minha senha** | Para todos: senha atual + nova. |
| **Mais → Sair** | Volta para o login. |
| Cabeçalho da aba Mais | "Maria Souza · Proprietário". |

## Perfis (requisito do CLAUDE.md)

| Pode... | Proprietário | Funcionário |
|---|:---:|:---:|
| Agenda, pets, tutores, atendimentos, fotos (criar) | ✅ | ✅ |
| Ver a aba **Financeiro** | ✅ | ❌ (a aba nem aparece) |
| Editar/desativar tutor e pet já cadastrados | ✅ | ❌ |
| Editar atendimento | ✅ qualquer um | ✅ só os **de hoje** |
| Serviços e preços, Usuários, Backup | ✅ | ❌ |

As regras ficam num lugar só, `domain/Permissoes.kt`, com testes. "Registro antigo", para o
funcionário, foi definido como: tutor/pet já cadastrado e atendimento de dia anterior. O
atendimento de hoje ele pode corrigir (erro de digitação logo depois de salvar). Para o
funcionário, as rotas do financeiro e das telas de dono **nem são registradas** na navegação:
não é só esconder o botão.

## Decisões (e por quê)

1. **Senha com PBKDF2 + sal + 120.000 repetições** (`HashDeSenha`). Já vem no Java, sem
   biblioteca. O sal aleatório faz senhas iguais terem hashes diferentes; as repetições deixam
   o "chute" de senhas muito lento. A comparação é em tempo constante. O cálculo roda fora da
   thread da tela para não travar.
2. **Recuperação sem servidor.** Sem e-mail possível, o dono recebe um código de 12
   caracteres sem letras ambíguas (0/O, 1/I/L). Ele também é guardado só como **hash**, e é
   trocado a cada uso.
3. **Mesma mensagem** para "login não existe" e "senha errada": não revela quais logins existem.
4. **Migração real do banco (versão 1 → 2).** A coluna `codigoRecuperacaoHash` foi adicionada
   com `ALTER TABLE` numa `Migration`, **sem apagar os dados** de quem já usava o app. Foi o
   primeiro uso de migração no projeto: daqui para frente, toda mudança de tabela segue esse
   caminho (aumentar `VERSAO` + escrever a migração).
5. **A conta provisória vira o dono.** Nas Sprints 2–4 os registros eram gravados em nome de
   um "proprietário" sem senha. No primeiro acesso, esse mesmo registro recebe nome, login e
   senha, então todo o histórico continua ligado à pessoa certa.
6. **Usuário nunca é apagado** (regra do projeto): só desativado. Login nunca é reaproveitado.
7. **Tela raiz decide o fluxo** (`RaizDoApp`): carregando → primeiro acesso → login → app. Ao
   sair ou trocar de usuário, a navegação é recriada do zero (sem telas do usuário anterior).

### Backup

- **Formato:** `.zip` com `fashiondog.db`, a pasta `fotos/` e um `backup-info.txt` (versão do
  banco e data). O nome sugerido é `fashiondog-backup-AAAA-MM-DD.zip`.
- **Onde salvar:** o seletor de arquivos do Android (Downloads, Google Drive, pendrive...),
  sem pedir permissão de armazenamento.
- **Antes de copiar o banco**, um `PRAGMA wal_checkpoint(FULL)` leva para o arquivo principal
  as mudanças que o Room guarda no arquivo `-wal`.
- **Restauração segura, em 3 passos:** (1) extrai numa pasta temporária, **recusando qualquer
  item estranho** — protege contra o ataque "zip slip" (`../../arquivo`); (2) confere se o banco
  abre e se a versão não é mais nova que a do app; (3) só então fecha o banco e troca os arquivos.
  Se algo falha nos passos 1–2, nada no celular é alterado.
- **Depois de restaurar**, o app reinicia: abre o banco novo (migrando, se o backup for de uma
  versão antiga), recria todos os avisos e pede login de novo (os usuários podem ser outros).

## Acabamento

- **Barra de status legível:** ícones escuros fixos (o app é sempre claro), corrigindo a hora e
  a bateria que sumiam no fundo claro — visto no teste no celular real.
- **Botão principal invisível (bug desde a Sprint 1, achado no celular real):** o texto do
  `BotaoPrincipal` usava a cor padrão `onPrimary`, que era tinta — preto no preto. Agora o texto é
  branco explícito.
- **Contraste do amarelo:** o Material usa a cor `primary` em botões de texto ("Hoje",
  "Cancelar"), foco dos campos e calendário. Com `primary` = amarelo, ficavam ilegíveis no fundo
  claro. Agora `primary` = tinta, e o amarelo é aplicado de propósito nos destaques (botões
  flutuantes, seleções, aba ativa) — como o CLAUDE.md define.
- **CI:** `actions/upload-artifact` v4 → v5 (fim do aviso de Node 20 no GitHub Actions).
- **Sem avisos de compilação** (troca de `LocalClipboardManager`, obsoleto, por `LocalClipboard`).
- `versionName` 1.0.0.
- `@Preview` também nas telas novas (32 prévias no total).

## Arquivos principais

- `domain/Seguranca.kt` (`HashDeSenha`, `CodigoDeRecuperacao`, `ValidacaoDeConta`),
  `domain/Permissoes.kt`, `domain/ValidadorDeBackup.kt`
- `data/local/AppDatabase.kt` (versão 2 + `MIGRACAO_1_2` + `fechar()`), `entity/Cadastro.kt`
- `data/repository/ContaRepository.kt`, `di/Sessao.kt` (agora real, com DataStore)
- `backup/BackupDoApp.kt`
- `ui/login/EntradaScreens.kt`, `ui/navigation/RaizDoApp.kt`
- `ui/mais/UsuariosScreen.kt`, `BackupScreen.kt`, `TrocarSenhaScreen.kt`
- `MainActivity.kt` (raiz + barras do sistema)

## Testes automatizados

`SegurancaRegrasTest.kt` — 17 testes novos (**67 no total**):
- **Hash de senha:** certa confere e errada não; hash não contém a senha; sal muda o hash;
  valor guardado inválido nunca confere; formato gravado.
- **Código de recuperação:** formato e ausência de letras ambíguas; aceita digitação "bagunçada".
- **Conta:** login válido/normalizado; senha curta; confirmação diferente.
- **Permissões:** dono pode tudo; funcionário sem financeiro/backup/usuários/preços/cadastros;
  funcionário só edita atendimento de hoje.
- **Backup:** aceita o que o app exporta; recusa zip slip; exige o banco; recusa versão mais nova;
  nome do arquivo.

## Como testar à mão (no celular)

1. Abra o app: aparece **Bem-vindo ao FashionDog**. Crie a conta (ex.: `maria` / `banho123`).
2. **Anote o código** que aparece → "Já anotei, continuar" → abre a Agenda.
3. **Mais → Usuários → Novo funcionário**: `ana` / `ana123`.
4. **Mais → Sair** → entre como `ana`: a aba Financeiro some; em Mais só há Avisos, Trocar
   senha e Sair; na ficha de um pet não aparecem editar/desativar.
5. Saia e entre como `maria`. **Mais → Backup → Salvar backup** → escolha Downloads/Drive.
6. Cadastre um pet de teste → **Restaurar** o backup do passo 5 → o app reinicia, pede login e o
   pet de teste sumiu (voltou ao estado do backup).
7. Na tela de login, **Esqueci a senha** → `maria` + o código anotado + senha nova → aparece
   um código novo (o antigo deixou de valer).

## Pendências / próximos passos sugeridos

- Ofuscação/minificação na versão de release e assinatura para distribuir o APK.
- Testes instrumentados (Room e telas) quando houver um emulador/dispositivo no CI.
- Lembrete periódico para fazer backup (ex.: semanal).
