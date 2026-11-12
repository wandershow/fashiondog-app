# FashionDog 🐶

[![Build e testes](https://github.com/wandershow/fashiondog-app/actions/workflows/build.yml/badge.svg)](https://github.com/wandershow/fashiondog-app/actions/workflows/build.yml)

App Android de gestão para o **FashionDog**, um banho e tosa em Rio Grande/RS.
Funciona **offline em um único celular**, sem servidor: agenda, cadastro de tutores e pets,
atendimentos com fotos de antes/depois e controle financeiro com parcelas no boleto.

> ✅ Versão 1.0.0 — todas as sprints concluídas. Projeto acadêmico (Análise e Desenvolvimento de Sistemas) feito
> seguindo o fluxo de uma empresa real: requisitos → modelo de dados → protótipo → sprints.

## Funcionalidades

| Módulo | O que faz | Status |
|---|---|---|
| Cadastro | Tutores e pets, busca sem acento, ligar/WhatsApp, desativar sem perder histórico | ✅ Sprint 1 |
| Atendimentos | Banho ou banho e tosa, valor cobrado, pagamento e fotos antes/depois (câmera ou galeria, comprimidas) | ✅ Sprint 2 |
| Ficha do pet | Último banho em destaque e histórico completo | ✅ Sprint 2 |
| Agenda | Semana de segunda a domingo, lembretes por notificação (reagendados ao reiniciar o celular); concluir gera o atendimento | ✅ Sprint 3 |
| Financeiro | Despesas por categoria, boleto parcelado com aviso na véspera e no dia, balanço por semana/mês/trimestre/semestre/ano | ✅ Sprint 4 |
| Login e backup | Perfis proprietário e funcionário, senha com hash (PBKDF2), recuperação por código, backup/restauração em .zip | ✅ Sprint 5 |

## Tecnologias

- **Kotlin** 2.2 + **Jetpack Compose** (Material 3)
- **Room** 2.8 (banco SQLite local) com **KSP**
- **Navigation Compose** — barra inferior com Agenda, Pets, Financeiro e Mais
- **DataStore** para lembrar o usuário logado
- Android Gradle Plugin 9.5 · minSdk 26 (Android 8.0) · targetSdk 37
- **Coil** para exibir fotos · **ExifInterface** para corrigir a rotação
- **JUnit 4** para as regras de negócio · **GitHub Actions** para build e testes automáticos

## Arquitetura

```
app/src/main/java/br/com/fashiondog/
├── di/              # AppContainer (injeção manual) e Sessao (usuário logado)
├── backup/          # exportar/restaurar .zip com banco e fotos
├── data/
│   ├── fotos/       # compressão e armazenamento das fotos
│   ├── repository/  # único ponto de acesso ao banco para as telas
│   └── local/
│       ├── entity/  # tabelas do Room (tutor, pet, servico, agendamento, atendimento,
│       │            #   foto, usuario, categoria, despesa, parcela)
│       ├── model/   # resultados de consultas que juntam tabelas
│       ├── dao/     # consultas ao banco
│       └── AppDatabase.kt
├── notifications/ # avisos (AlarmManager) e reagendamento no boot
├── domain/          # regras de negócio em Kotlin puro, com testes (ex.: GeradorDeParcelas)
└── ui/
    ├── componentes/ # peças de tela reaproveitáveis
    ├── pet/         # pets e tutores (Sprint 1)
    ├── agenda/      # agenda e agendamentos (Sprint 3)
    ├── atendimento/ # atendimentos e fotos (Sprint 2)
    ├── financeiro/  # despesas, parcelas e balanço (Sprint 4)
    ├── login/       # primeiro acesso, login e recuperação (Sprint 5)
    ├── mais/        # aba Mais: serviços, configurações
    ├── preview/     # dados de exemplo dos @Preview
    ├── theme/       # cores e tipografia da marca
    └── navigation/  # rotas e barra inferior
```

### Documentação das sprints

Cada sprint tem um documento em [`docs/sprints/`](docs/sprints/) com objetivo, decisões, testes e roteiro de teste manual.

### Decisões de projeto

- **MVVM**: tela (Compose) → ViewModel (`StateFlow`) → repositório → Room.
- **Toda tela tem `@Preview`**: a parte que desenha (`XxxConteudo`) é separada da que pega os dados (`XxxScreen`).
- **Dinheiro em centavos (`Long`)** — evita erros de arredondamento de `Double`
  (ex.: R$ 100,00 em 3 parcelas = 33,33 + 33,33 + 33,34; a última absorve a diferença).
- **O valor fica gravado no atendimento** — mudar o preço do serviço não altera o histórico.
- **Balanço sempre calculado por consulta**, nunca armazenado — não há como ficar desatualizado.
- **Nada é apagado** — tutores, pets, serviços e usuários são desativados (`ativo = false`),
  preservando o histórico.
- **Fotos na pasta interna do app**, comprimidas; o banco guarda só o caminho.
- **Senhas nunca gravadas**: só o hash PBKDF2 com sal. Sem servidor, a recuperação usa um código de uso único.
- **Banco com migrações**: mudar uma tabela não apaga os dados de quem já usa o app.

## Como rodar

Pré-requisitos: [Android Studio](https://developer.android.com/studio) recente (com suporte ao AGP 9.5).

```bash
git clone https://github.com/wandershow/fashiondog-app.git
cd fashiondog-app

./gradlew assembleDebug        # gera o APK (Windows: gradlew.bat assembleDebug)
./gradlew testDebugUnitTest    # roda os testes unitários
```

O APK fica em `app/build/outputs/apk/debug/`.

## Andamento

- [x] Requisitos, modelo de dados e protótipo das telas
- [x] Sprint 0 — projeto base, tema, banco com 10 tabelas, navegação, gerador de parcelas, CI
- [x] Sprint 1 — cadastro de tutores e pets ([documentação](docs/sprints/sprint-1-cadastro.md))
- [x] Sprint 2 — atendimentos, fotos e histórico do pet ([documentação](docs/sprints/sprint-2-atendimentos.md))
- [x] Sprint 3 — agenda e notificações ([documentação](docs/sprints/sprint-3-agenda.md))
- [x] Sprint 4 — financeiro ([documentação](docs/sprints/sprint-4-financeiro.md))
- [x] Sprint 5 — login/perfis, backup e acabamento ([documentação](docs/sprints/sprint-5-login-backup.md))

## Autor

Desenvolvido por [@wandershow](https://github.com/wandershow) como projeto de estudo em ADS.
