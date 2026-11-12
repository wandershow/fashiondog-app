# Sprint 3 — Agenda e notificações

**Branch:** `feature/sprint-3-agenda` (criada a partir da Sprint 2) · **Status:** concluída

## Objetivo

Agendar banhos e tosas, receber um **aviso** antes do horário e, ao **concluir** um
agendamento, abrir o atendimento já preenchido com o pet, o serviço e o valor sugerido.

## O que foi entregue

| Tela | O que faz |
|---|---|
| **Agenda** (tela inicial) | Mês e ano, faixa da semana de **segunda a domingo** com pontinho nos dias com agendamento, setas para mudar de semana, botão **Hoje**, lista do dia escolhido em ordem de horário. |
| **Ações do agendamento** | Tocar abre: **Concluir** (vai para o atendimento preenchido), **Editar**, **Cancelar agendamento**. Concluídos e cancelados aparecem marcados. |
| **Novo / editar agendamento** | Pet (diálogo com busca), serviço, data, hora e quando avisar (na hora, 30 min, 1 h, 2 h ou 1 dia antes). Já vem com o dia selecionado na agenda. |
| **Notificação** | "Banho e tosa às 14:00 — Rex / Tutor: Maria · 12/11/2026". Tocar abre o app. |
| **Mais → Avisos** | Mostra se as notificações e o horário exato estão liberados, com atalhos para as configurações do Android. |

## Decisões (e por quê)

1. **AlarmManager, não WorkManager.** O WorkManager serve para tarefas que podem atrasar
   (sincronizar, por exemplo). Lembrete de horário precisa tocar na hora, então usamos o
   `AlarmManager` com `setExactAndAllowWhileIdle`, que funciona mesmo com o celular em repouso.

2. **Horário exato e permissões.** Do Android 12 em diante, alarme exato exige permissão:
   - Android 12 (API 31–32): `SCHEDULE_EXACT_ALARM`, que o usuário libera em Mais → Avisos;
   - Android 13+: `USE_EXACT_ALARM`, liberada na instalação para apps de agenda.
   Se não houver permissão, o app usa uma **janela de 10 minutos** (`setWindow`): o aviso chega,
   só que pode atrasar um pouco. Nunca deixa de avisar.
   > Se um dia o app for para a Play Store, o Google pede uma justificativa para o
   > `USE_EXACT_ALARM` (agenda com horário marcado se encaixa).

3. **Permissão de notificação (Android 13+)** é pedida ao abrir a Agenda, só se ainda não foi dada.

4. **Reagendar no reinício (regra do CLAUDE.md).** O Android apaga todos os alarmes quando o
   celular reinicia. O `ReceptorDeInicializacao` escuta `BOOT_COMPLETED` (e também atualização do
   app e mudança de relógio/fuso) e recria os avisos de todos os agendamentos futuros. O app
   também recria tudo ao abrir, cobrindo o caso de ter sido "forçado a parar".

5. **Cada aviso tem um código único** (`CodigosDeAviso`): `id × 10 + tipo`. Com ele o aviso é
   **substituído** ao editar o agendamento e **cancelado** ao concluir ou cancelar. O último dígito
   separa agendamento (1) das parcelas da Sprint 4 (2 = véspera, 3 = no dia).

6. **Concluir = atendimento + status numa transação.** O atendimento é gravado com o
   `agendamentoId`, e na **mesma transação** o agendamento vira `CONCLUIDO`. Se a pessoa desistir
   do formulário, o agendamento continua aberto. O valor vem do preço sugerido e pode ser mudado.

7. **A agenda carrega a semana inteira** de uma vez (uma consulta) e só consulta de novo ao
   trocar de semana. Assim os pontinhos dos 7 dias e a lista do dia saem do mesmo resultado.

8. **Não dá para agendar no passado** (só editar um agendamento antigo). O lembrete que já
   passou (ex.: agendado para daqui a 20 min com aviso de 1 h) não é criado.

## Arquivos principais

- `domain/Semana.kt`, `domain/CalculadoraDeAvisos.kt` (inclui `CodigosDeAviso`)
- `notifications/Avisos.kt` — canais, agendar/cancelar alarmes, texto do aviso
- `notifications/Receptores.kt` — `ReceptorDeAvisos` (mostra) e `ReceptorDeInicializacao` (reagenda)
- `data/repository/AgendaRepository.kt`; `AtendimentoRepository` agora conclui o agendamento
- `data/local/dao/AtendimentoDao.kt` — `AgendamentoDao` com consultas detalhadas
- `ui/agenda/` — Agenda e formulário; `ui/mais/AvisosScreen.kt`
- `AndroidManifest.xml` — permissões e os dois receptores

## Testes automatizados

`AgendaRegrasTest.kt` — 9 testes novos (38 no total):
- **Semana:** começa na segunda; domingo pertence à semana anterior; segunda é o próprio início;
  7 dias atravessando o ano; nomes em português.
- **Avisos:** lembrete 1 h antes; lembrete no passado não é agendado; rótulos; códigos não se repetem.

## Como testar à mão (no celular)

1. Abra o app: a Agenda pede permissão de notificação → **Permitir**.
2. **Agendar** → escolha o pet, "Banho", hoje, daqui a **3 minutos**, aviso **Na hora** → salvar.
3. Feche o app. Em ~3 minutos chega a notificação.
4. Crie outro para amanhã com aviso "1 dia antes"… ou para daqui a 5 min, e **reinicie o
   celular** antes do horário: o aviso chega mesmo assim (reagendado no boot).
5. Toque no agendamento → **Concluir** → o atendimento abre com pet e serviço preenchidos →
   salvar. Na agenda ele aparece "Concluído"; na ficha do pet, vira o último banho.
6. Toque em outro → **Cancelar agendamento**: aparece riscado e o aviso não toca mais.
7. **Mais → Avisos** mostra o estado das permissões.

## Pendências

- Funcionário pode agendar e concluir (é permitido); as restrições de editar registros antigos
  entram na Sprint 5.
- Os avisos das parcelas usam a mesma infraestrutura e entram na Sprint 4.
