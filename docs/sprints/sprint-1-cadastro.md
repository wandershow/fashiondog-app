# Sprint 1 — Cadastro de tutores e pets

**Branch:** `feature/sprint-1-cadastro` · **Status:** concluída

## Objetivo

Permitir cadastrar, encontrar, editar e desativar **tutores** e seus **pets**. É a base de
tudo: agenda, atendimentos e histórico dependem de um pet cadastrado.

## O que foi entregue

| Tela | O que faz |
|---|---|
| **Pets e tutores** (aba Pets) | Lista de pets ou de tutores (botão de alternância), busca por nome do pet, raça, tutor ou telefone, e botão "Novo cadastro". |
| **Novo cadastro** | Cadastra o tutor (nome, telefone, observações) e, opcionalmente, o primeiro pet na mesma tela. |
| **Tutor** | Telefone formatado, botões **Ligar** e **WhatsApp**, lista dos pets, "Adicionar pet", editar e desativar. |
| **Ficha do pet** | Raça, porte, observações e o tutor (toque para abrir). Editar e desativar. O histórico de banhos entra na Sprint 2. |
| **Formulário do pet** | Nome, raça, porte (Pequeno / Médio / Grande) e observações. |

## Decisões (e por quê)

1. **Arquitetura MVVM: Tela → ViewModel → Repositório → DAO (Room).**
   - A *tela* (Compose) só desenha e repassa toques.
   - O *ViewModel* guarda o estado da tela num `StateFlow` e sobrevive à rotação do celular.
   - O *repositório* (`CadastroRepository`) é o único que fala com o banco.
   Assim, cada camada tem uma responsabilidade e fica fácil testar e trocar peças.

2. **Injeção de dependência manual em vez de Hilt.** O `AppContainer` cria o banco e os
   repositórios uma vez; o `FashionDogApplication` guarda o container. Para um app deste
   tamanho isso evita um plugin a mais (e problemas de compatibilidade com o AGP 9) e deixa
   explícito de onde vem cada objeto. A função `viewModelDoApp { app, args -> ... }` liga o
   container aos ViewModels.

3. **As listas se atualizam sozinhas.** Os DAOs devolvem `Flow`: quando um pet é salvo, o
   Room avisa e a lista é recalculada, sem precisar "recarregar".

4. **Busca feita em Kotlin, não no SQL.** O `LIKE` do SQLite não ignora acentos ("Bóris" ≠
   "boris"). Como um banho e tosa tem algumas centenas de pets, filtrar na memória é instantâneo
   e permite a regra `Busca.corresponde`, que ignora acentos e compara números com o telefone.

5. **Telefone gravado só com dígitos** (`53999998888`) e formatado na tela
   (`(53) 99999-8888`). Isso facilita a busca e o link do WhatsApp (`wa.me/55...`).

6. **Nada é apagado.** "Desativar" marca `ativo = false`: o tutor/pet some das listas, mas os
   atendimentos antigos continuam ligados a ele. Desativar o tutor desativa os pets dele, numa
   **transação** (ou grava tudo, ou nada).

7. **Cadastro do tutor e do primeiro pet juntos**, também numa transação: é o caso mais comum
   no balcão (cliente novo chegando com o cachorro).

8. **Barra inferior só nas 4 abas.** Formulários e fichas ocupam a tela toda, com seta de voltar.

## Arquivos principais

- `data/local/dao/CadastroDao.kt` — consultas novas (pet + tutor, tutor + quantidade de pets, desativar)
- `data/local/model/Consultas.kt` — `PetComTutor`, `TutorComQuantidade`
- `data/repository/CadastroRepository.kt`
- `di/AppContainer.kt`, `FashionDogApplication.kt`
- `domain/Telefone.kt`, `domain/Busca.kt`, `domain/ValidacaoCadastro.kt`
- `ui/componentes/` — peças reaproveitáveis (Tela, CampoTexto, EscolhaUnica, Cartao...)
- `ui/pet/` — as 5 telas e seus ViewModels
- `ui/navigation/Rotas.kt` e `FashionDogApp.kt`

## Testes automatizados

`CadastroRegrasTest.kt` — 13 testes novos:
- **Telefone:** formatar celular e fixo, ignorar pontuação, rejeitar número sem DDD, número do WhatsApp.
- **Busca:** termo vazio, maiúsculas/acentos, qualquer campo, busca por dígitos do telefone, campos nulos.
- **Validação:** nome em branco, telefone curto, dados válidos.

## Como testar à mão

1. Aba **Pets** → **Novo cadastro** → preencha "Maria Souza", "53999998888" e o pet "Bóris".
2. Salvar abre a página da tutora. Toque em **Adicionar pet** e cadastre "Thor".
3. Volte para a lista: busque `boris` (sem acento), `maria` e `9999` — todos acham o pet.
4. Alterne para **Tutores**: Maria aparece com "2 pets".
5. Abra o Thor → menu ⋮ → **Desativar pet**: ele some da lista; a Maria passa a ter "1 pet".

## Dependências adicionadas

- `lifecycle-viewmodel-compose` e `lifecycle-runtime-compose` 2.9.4 (ViewModel e `collectAsStateWithLifecycle`)
- `material-icons-core` (ícones de adicionar, buscar, editar, voltar)

## Pendências para próximas sprints

- O funcionário ainda consegue editar/desativar: as permissões por perfil entram na Sprint 5.
