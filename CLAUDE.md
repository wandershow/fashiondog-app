# FashionDog — app de gestão de banho e tosa

App Android para um banho e tosa (FashionDog, Rio Grande/RS). Roda em **um único celular, offline**, sem servidor. O dono do projeto é estudante de ADS e quer seguir o fluxo de uma empresa real, etapa por etapa. Responda sempre em **português**.

## Stack

- Kotlin 2.2.10 + Jetpack Compose (Material 3), AGP 9.5.1 com Kotlin embutido (não usar o plugin `kotlin-android` nem kapt)
- Room 2.8.4 com KSP 2.3.6; Navigation Compose 2.9.0
- minSdk 26, compileSdk/targetSdk 37
- Versões ficam em `gradle/libs.versions.toml`
- Injeção de dependência **manual** (`di/AppContainer.kt`), sem Hilt; ViewModels criados com `viewModelDoApp { app, args -> ... }`
- ViewModel + StateFlow (lifecycle 2.9.4); câmera do sistema + FileProvider, Coil 2.7.0 (fotos). AlarmManager (`notifications/Avisos.kt`, códigos em `CodigosDeAviso`). Previsto: DataStore (sessão)

## Comandos

- Compilar: `./gradlew assembleDebug` (no Windows: `gradlew.bat assembleDebug`)
- Testes unitários: `./gradlew testDebugUnitTest`

## Estrutura (`app/src/main/java/br/com/fashiondog/`)

- `data/local/entity` — entidades Room; `data/local/dao` — DAOs; `data/local/AppDatabase.kt`
- `domain/` — regras em Kotlin puro, com teste unitário (ex.: `GeradorDeParcelas`)
- `ui/theme` — cores da marca; `ui/navigation/FashionDogApp.kt` — barra inferior com 4 abas
- `data/repository` — repositórios; `ui/componentes` — peças de tela; `ui/pet` — pets e tutores; `ui/atendimento`; `ui/mais`; `data/fotos`
- Fotos: caminho **relativo** no banco (`fotos/x.jpg`), arquivo em `filesDir`. `di/Sessao` é provisória (usuário padrão) até o login da Sprint 5
- `ui/agenda`, `notifications/` (Sprint 3). `ui/financeiro` (Sprint 4). A criar: `ui/login`, `backup/`

## Requisitos

- **Login** com dois perfis: proprietário (tudo) e funcionário (agenda, pets, tutores, atendimentos e fotos; **sem** financeiro, sem excluir/editar registros antigos, sem backup, sem gerenciar usuários). Senha guardada como hash. Sem servidor: recuperação por código gerado na criação da conta do proprietário.
- **Cadastro** de tutor (nome, telefone) e pets (um tutor tem vários pets).
- **Agenda** com avisos; concluir um agendamento gera o atendimento com o valor sugerido.
- **Atendimento**: serviço (banho ou banho e tosa), valor cobrado, pagamento, até **2 fotos** (antes/depois). Fotos ficam na pasta interna do app, comprimidas; o banco guarda o caminho.
- **Ficha do pet**: último banho em destaque (data, serviço, valor) e histórico completo.
- **Financeiro**: despesas diárias com categoria; despesa pode ser **parcelada no boleto**, gerando parcelas mensais com aviso **um dia antes e no dia** do vencimento (cancelado se a parcela for paga). Balanço por semana (segunda a domingo), mês, trimestre, semestre e ano.
- **Backup**: exportar/restaurar um .zip com banco e fotos.

## Regras de negócio

- Dinheiro sempre em **centavos (Long)**; datas em milissegundos (epoch).
- O valor fica gravado no atendimento; mudar o preço do serviço não altera o histórico.
- Receita = soma dos atendimentos. Despesa = soma das **parcelas pelo vencimento** (despesa à vista = 1 parcela já paga). Balanço é sempre calculado por consulta, nunca armazenado.
- A última parcela absorve a diferença de centavos.
- Tutor, pet, serviço e usuário nunca são apagados: usar `ativo = false`.
- Alarmes precisam ser reagendados quando o celular reinicia (BOOT_COMPLETED).

## Identidade visual

- Cores em `ui/theme/Color.kt`: amarelo `#FFD21F` (destaque/seleção), tinta `#1A1A1A` (texto e botões principais), fundo `#FAFAF7`.
- Layout clean, navegação por barra inferior: Agenda (tela inicial), Pets, Financeiro, Mais.
- Ícone do app: logo sem telefone e redes sociais (`mipmap-xxxhdpi/ic_launcher_foreground.png`).

## Andamento

- [x] Etapas 1–3: requisitos, modelo de dados e protótipo das telas aprovados
- [x] Sprint 0: projeto, tema, ícone, banco (10 tabelas), navegação vazia, gerador de parcelas com testes
- [x] Sprint 1: cadastro de tutores e pets — `docs/sprints/sprint-1-cadastro.md`
- [x] Sprint 2: atendimentos, fotos e histórico do pet — `docs/sprints/sprint-2-atendimentos.md`
- [x] Sprint 3: agenda e notificações — `docs/sprints/sprint-3-agenda.md`
- [x] Sprint 4: financeiro (despesas, parcelas, avisos, balanço) — `docs/sprints/sprint-4-financeiro.md`
- [ ] Sprint 5: login/perfis, backup e acabamento
- [x] Sprint 0 (fim): repositório público https://github.com/wandershow/fashiondog-app, README e GitHub Actions (`.github/workflows/build.yml`) rodando build e testes em push na `main` e em PRs

## Fluxo de trabalho

- Branch `main` sempre funcionando; cada tarefa em `feature/...`; commits `feat:`, `fix:`, `test:`, `docs:`.
- Regra nova em `domain/` vem com teste unitário.
- Toda tela tem duas partes: `XxxScreen` (pega o ViewModel) e `XxxConteudo` (só desenha, recebe estado + callbacks), com **`@Preview`** do Conteudo usando `ui/preview/DadosDePreview.kt` e `PreviewNoTema`.
- Cada sprint é documentada em `docs/sprints/sprint-N-*.md` (objetivo, decisões, testes, roteiro manual).
- Emulador local não roda (pouco disco/RAM): validar com build + testes e no celular físico.
- Explicar as decisões em linguagem simples: o dono está aprendendo Android.
