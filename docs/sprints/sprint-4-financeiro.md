# Sprint 4 — Financeiro (despesas, parcelas, avisos e balanço)

**Branch:** `feature/sprint-4-financeiro` (criada a partir da Sprint 3) · **Status:** concluída

## Objetivo

Registrar as despesas do dia a dia, inclusive compras **parceladas no boleto**, avisar o
vencimento de cada parcela e mostrar o **balanço** (receita − despesa) por semana, mês,
trimestre, semestre ou ano.

## O que foi entregue

| Tela | O que faz |
|---|---|
| **Financeiro** (aba) | Escolha do período (Semana / Mês / Trimestre / Semestre / Ano) com setas para voltar e avançar; cartão do **balanço** (receita com nº de atendimentos, despesa, saldo — vermelho se negativo); quadro **A pagar** (próximos 30 dias e atrasadas); **despesa por categoria**; **despesas do período**. |
| **Ações da parcela** | Tocar abre: marcar como paga / não paga (boleto) e excluir a despesa inteira. |
| **Nova despesa** | Descrição, categoria (com "+ Nova categoria"), valor total, data da compra, **À vista** ou **Boleto parcelado** (nº de parcelas e 1º vencimento) e a **prévia** de como cada parcela entra no balanço. |
| **Notificações** | "Boleto vence amanhã: R$ 300,00" e "Boleto vence hoje: …", às 9h. |

Bônus pedido no meio da sprint: **@Preview em todas as telas** (ver seção abaixo).

## Regras de negócio aplicadas (do CLAUDE.md)

- **Despesa = soma das parcelas pelo vencimento.** Um boleto de R$ 900 em 3x de novembro a
  janeiro conta R$ 300 em cada mês, não R$ 900 em novembro.
- **À vista = 1 parcela já paga** com vencimento no dia da compra. Assim o balanço só precisa
  somar parcelas, sem caso especial.
- **A última parcela absorve os centavos** (R$ 1.000 em 3x = 333,33 + 333,33 + 333,34) —
  `PlanoDeParcelas.boleto` usa o `GeradorDeParcelas` da Sprint 0.
- **Aviso um dia antes e no dia** do vencimento; **cancelado ao pagar** (e recriado se a
  pessoa desmarcar o pagamento).
- **Balanço calculado por consulta, nunca gravado.** As somas são consultas do Room que
  devolvem `Flow`: registrar um atendimento ou pagar uma parcela atualiza a tela na hora.
- **Semana de segunda a domingo** (`Periodo` reaproveita `Semana.inicio` da Sprint 3).

## Decisões (e por quê)

1. **Período com fim "aberto"** (`inicio` incluído, `fim` não). Novembro = de 01/11 até
   01/12 (exclusive). Evita esquecer o último dia ou contar duas vezes algo à meia-noite.
   Trimestres começam em jan/abr/jul/out; semestres em jan/jul.
2. **Vencimento gravado ao meio-dia** do dia escolhido. Longe da virada do dia, uma mudança de
   fuso/horário de verão não joga a parcela para o dia (ou mês) errado.
3. **Avisos às 9h**, horário comercial. Códigos `id×10+2` (véspera) e `id×10+3` (no dia), da
   mesma família dos avisos da agenda, então nunca colidem.
4. **Reagendamento no boot** agora recria também os avisos das parcelas pendentes desde ontem
   (a parcela de hoje ainda pode ter o aviso "no dia" pela frente).
5. **Excluir em vez de editar despesa.** Editar valor ou nº de parcelas exigiria refazer as
   parcelas e decidir o que fazer com as já pagas; excluir e lançar de novo é mais simples e
   sem ambiguidade. As parcelas vão junto (`ON DELETE CASCADE`) e os avisos são cancelados.
6. **Categorias iniciais** criadas na primeira abertura (Produtos de banho, Aluguel, Água e luz,
   Equipamentos, Manutenção, Outros); novas podem ser criadas no formulário.
7. **Máximo de 48 parcelas** — limite de segurança contra erro de digitação.

## @Preview nas telas (pedido durante a sprint)

Toda tela agora tem duas partes:

- **`XxxScreen`** — pega o ViewModel e o estado (precisa do banco, então não roda no Preview);
- **`XxxConteudo`** — só desenha, recebendo o estado e funções ("callbacks") para os toques.

Os `@Preview` desenham o `Conteudo` com dados de exemplo de `ui/preview/DadosDePreview.kt`,
dentro do tema do app (`PreviewNoTema`). Formulários grandes agrupam os callbacks numa classe
(`AcoesDoAtendimento`, `AcoesDoAgendamento`, `AcoesDaDespesa`) com padrões vazios, para a
prévia ficar curta. Câmera e galeria não existem no Preview: o espaço de foto detecta o modo
de inspeção (`LocalInspectionMode`) e só desenha.

Prévias criadas (24): lista de pets/tutores/busca vazia, novo cadastro, editar tutor, novo pet,
detalhe do tutor, ficha com e sem histórico, atendimento novo e com erros, detalhe do
atendimento, Mais, Serviços, Avisos liberados/bloqueados, agenda do dia e vazia, novo
agendamento, seletor de pet, financeiro e saldo negativo, despesa em boleto e à vista.

Essa separação também é uma boa prática por si: a tela fica fácil de testar e de entender.

## Arquivos principais

- `domain/Periodo.kt`, `domain/PlanoDeParcelas.kt`, `domain/CalculadoraDeAvisos.kt` (`horariosDaParcela`)
- `data/local/dao/FinanceiroDao.kt` — parcelas detalhadas, somas em `Flow`, total por categoria
- `data/repository/FinanceiroRepository.kt` — `Balanco`, salvar/pagar/excluir, reagendar avisos
- `notifications/Avisos.kt` — `agendarParcela` / `cancelarParcela`
- `ui/financeiro/` — Financeiro e Nova despesa
- `ui/preview/DadosDePreview.kt` — dados das prévias

## Testes automatizados

`FinanceiroRegrasTest.kt` — 12 testes novos (50 no total):
- **Período:** semana seg–dom; mês com fevereiro bissexto; trimestres; semestres; ano; anterior/próximo.
- **Plano de parcelas:** à vista = 1 parcela paga; boleto soma o total com centavos na última; limite de 48.
- **Avisos de parcela:** véspera e dia às 9h; na véspera à tarde só sobra o do dia; vencida não avisa.

## Como testar à mão

1. **Financeiro → Nova despesa**: "Shampoo 5 L", Produtos de banho, R$ 159,90, À vista → salvar.
   Aparece no mês como "paga" e a despesa do mês sobe R$ 159,90.
2. **Nova despesa**: "Secador", Equipamentos, R$ 900,00, Boleto, 3x, 1º vencimento amanhã.
   A prévia mostra 3 × R$ 300,00. Salve: em "A pagar" aparece a 1/3.
3. Mude para **Trimestre** e **Ano**: os valores somam as parcelas que vencem em cada período.
4. Às 9h de hoje… ou amanhã às 9h chega "Boleto vence hoje". Toque na parcela →
   **Marcar como paga**: os avisos dela são cancelados.
5. Registre um atendimento (Sprint 2): a **receita** do período sobe na hora.

## Pendências

- Na Sprint 5 a aba Financeiro fica **só para o proprietário**.
