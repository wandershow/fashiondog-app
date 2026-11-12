# Ajuste 01 — Minha conta (mudar nome e login)

**Branch:** `feature/minha-conta` (criada a partir da Sprint 5) · **Origem:** pedido da dona após o primeiro uso

## Problema

Ao criar a conta de proprietária, o login foi digitado com erro (`fashiodog`, sem o "n").
O app permitia trocar a senha, mas **não o login**: a única saída seria apagar os dados e
recomeçar, perdendo tudo o que já tinha sido cadastrado.

## O que mudou

- **Mais → Minha conta** substitui "Trocar minha senha" e reúne:
  - **Dados:** nome e login, com o botão "Salvar nome e login";
  - **Senha:** senha atual, nova e confirmação (como antes).
- Na aba Mais, a opção mostra o login atual: "Nome, login (fashiodog) e senha".
- Depois de salvar, aparece: *Salvo. Da próxima vez, entre com o login "fashiondog".*
  O cabeçalho da aba Mais é atualizado na hora.

## Regras

- O login novo segue as mesmas regras da criação: 3 a 20 caracteres, letras minúsculas,
  números, ponto ou `_` (é convertido para minúsculas automaticamente).
- **Não pode usar o login de outro usuário**, nem de um usuário desativado (o histórico
  aponta para ele). Manter o próprio login é permitido.
- Nome não pode ficar em branco.
- Os registros não mudam: atendimentos, agendamentos e despesas guardam o **id** do usuário,
  não o login. Por isso mudar o login não afeta o histórico.

A regra fica em `ValidacaoDeConta.erroNovoLogin` (domínio, com testes) e é aplicada em
`ContaRepository.atualizarMinhaConta`.

## Testes

3 testes novos em `SegurancaRegrasTest.kt` (**70 no total**):
- corrigir erro de digitação é permitido (login livre) e manter o próprio login também;
- não pode pegar o login de outro usuário nem usar login inválido;
- nome em branco é recusado.

Prévias novas: "Minha conta" e "Login já usado".

## Como testar

1. Entre com `fashiodog`.
2. **Mais → Minha conta** → mude o login para `fashiondog` → **Salvar nome e login**.
3. **Mais → Sair** e entre com `fashiondog` e a mesma senha.
