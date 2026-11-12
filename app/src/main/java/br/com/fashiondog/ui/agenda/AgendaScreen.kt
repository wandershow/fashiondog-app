package br.com.fashiondog.ui.agenda

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fashiondog.data.local.entity.StatusAgendamento
import br.com.fashiondog.data.local.model.AgendamentoDetalhado
import br.com.fashiondog.data.repository.AgendaRepository
import br.com.fashiondog.domain.Datas
import br.com.fashiondog.domain.Semana
import br.com.fashiondog.ui.componentes.EstadoVazio
import br.com.fashiondog.ui.componentes.Tela
import br.com.fashiondog.ui.componentes.viewModelDoApp
import br.com.fashiondog.ui.preview.DadosDePreview
import br.com.fashiondog.ui.preview.PreviewNoTema
import br.com.fashiondog.ui.theme.Amarelo
import br.com.fashiondog.ui.theme.Tinta
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AgendaEstado(
    val dia: LocalDate = LocalDate.now(),
    val diasDaSemana: List<LocalDate> = Semana.dias(LocalDate.now()),
    /** Quantos agendamentos (não cancelados) há em cada dia — os pontinhos da semana. */
    val quantidadePorDia: Map<LocalDate, Int> = emptyMap(),
    val doDia: List<AgendamentoDetalhado> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class AgendaViewModel(private val repositorio: AgendaRepository) : ViewModel() {

    private val diaSelecionado = MutableStateFlow(LocalDate.now())

    /** Carrega a semana inteira do dia escolhido; trocar de dia na mesma semana não consulta de novo. */
    private val semana = diaSelecionado
        .map(Semana::inicio)
        .distinctUntilChanged()
        .flatMapLatest { segunda ->
            repositorio.observarEntre(Datas.inicioDoDia(segunda), Datas.inicioDoDia(segunda.plusDays(7)))
        }

    val estado: StateFlow<AgendaEstado> = combine(diaSelecionado, semana) { dia, agendamentos ->
        val porDia = agendamentos.groupBy { Datas.paraLocalDate(it.agendamento.dataHora) }
        AgendaEstado(
            dia = dia,
            diasDaSemana = Semana.dias(dia),
            quantidadePorDia = porDia.mapValues { (_, lista) ->
                lista.count { it.agendamento.status != StatusAgendamento.CANCELADO }
            },
            doDia = porDia[dia].orEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AgendaEstado())

    fun escolherDia(dia: LocalDate) {
        diaSelecionado.value = dia
    }

    fun mudarSemana(semanas: Long) {
        diaSelecionado.value = diaSelecionado.value.plusWeeks(semanas)
    }

    fun irParaHoje() {
        diaSelecionado.value = LocalDate.now()
    }

    fun cancelar(id: Long) {
        viewModelScope.launch { repositorio.cancelar(id) }
    }
}

@Composable
fun AgendaScreen(
    aoNovoAgendamento: (dia: LocalDate) -> Unit,
    aoEditar: (agendamentoId: Long) -> Unit,
    aoConcluir: (AgendamentoDetalhado) -> Unit,
) {
    val vm = viewModelDoApp { app, _ -> AgendaViewModel(app.agenda) }
    val estado by vm.estado.collectAsStateWithLifecycle()

    PedirPermissaoDeNotificacao()

    AgendaConteudo(
        estado = estado,
        aoEscolherDia = vm::escolherDia,
        aoMudarSemana = vm::mudarSemana,
        aoIrParaHoje = vm::irParaHoje,
        aoNovoAgendamento = aoNovoAgendamento,
        aoEditar = aoEditar,
        aoConcluir = aoConcluir,
        aoCancelar = vm::cancelar,
    )
}

@Composable
fun AgendaConteudo(
    estado: AgendaEstado,
    aoEscolherDia: (LocalDate) -> Unit,
    aoMudarSemana: (Long) -> Unit,
    aoIrParaHoje: () -> Unit,
    aoNovoAgendamento: (dia: LocalDate) -> Unit,
    aoEditar: (agendamentoId: Long) -> Unit,
    aoConcluir: (AgendamentoDetalhado) -> Unit,
    aoCancelar: (agendamentoId: Long) -> Unit,
) {
    var escolhido by remember { mutableStateOf<AgendamentoDetalhado?>(null) }

    Tela(
        titulo = "Agenda",
        acoes = { TextButton(onClick = aoIrParaHoje) { Text("Hoje") } },
        botaoFlutuante = {
            ExtendedFloatingActionButton(
                onClick = { aoNovoAgendamento(estado.dia) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Agendar") },
                containerColor = Amarelo,
                contentColor = Tinta,
            )
        },
    ) { espacamento ->
        Column(modifier = Modifier.fillMaxSize().padding(espacamento)) {
            FaixaDaSemana(estado, aoEscolherDia, aoMudarSemana)
            HorizontalDivider()
            if (estado.doDia.isEmpty()) {
                EstadoVazio("Nada agendado para ${Datas.formatarData(estado.dia)}.")
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(estado.doDia, key = { it.agendamento.id }) { item ->
                        LinhaDoAgendamento(item) { escolhido = item }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    escolhido?.let { item ->
        val agendado = item.agendamento.status == StatusAgendamento.AGENDADO
        AlertDialog(
            onDismissRequest = { escolhido = null },
            title = { Text("${item.petNome} · ${Datas.formatarHora(item.agendamento.dataHora)}") },
            text = {
                Text(
                    "${item.servicoNome}\nTutor: ${item.tutorNome}\n" +
                        if (agendado) "Concluir abre o atendimento já preenchido." else "Situação: ${item.agendamento.status.rotulo}",
                )
            },
            confirmButton = {
                if (agendado) {
                    TextButton(onClick = {
                        escolhido = null
                        aoConcluir(item)
                    }) { Text("Concluir") }
                }
            },
            dismissButton = {
                if (agendado) {
                    Row {
                        TextButton(onClick = {
                            escolhido = null
                            aoCancelar(item.agendamento.id)
                        }) { Text("Cancelar agendamento") }
                        TextButton(onClick = {
                            escolhido = null
                            aoEditar(item.agendamento.id)
                        }) { Text("Editar") }
                    }
                } else {
                    TextButton(onClick = { escolhido = null }) { Text("Fechar") }
                }
            },
        )
    }
}

val StatusAgendamento.rotulo: String
    get() = when (this) {
        StatusAgendamento.AGENDADO -> "Agendado"
        StatusAgendamento.CONCLUIDO -> "Concluído"
        StatusAgendamento.CANCELADO -> "Cancelado"
    }

@Composable
private fun FaixaDaSemana(estado: AgendaEstado, aoEscolherDia: (LocalDate) -> Unit, aoMudarSemana: (Long) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { aoMudarSemana(-1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Semana anterior")
            }
            Text(
                Semana.mesEAno(estado.dia).replaceFirstChar(Char::uppercase),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { aoMudarSemana(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próxima semana")
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            val hoje = LocalDate.now()
            estado.diasDaSemana.forEach { dia ->
                val selecionado = dia == estado.dia
                val quantidade = estado.quantidadePorDia[dia] ?: 0
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selecionado) Amarelo else Color.Transparent)
                        .clickable { aoEscolherDia(dia) }
                        .padding(vertical = 8.dp),
                ) {
                    Text(Semana.nomeCurto(dia), style = MaterialTheme.typography.labelSmall)
                    Text(
                        dia.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (dia == hoje) FontWeight.Bold else FontWeight.Normal,
                        textDecoration = if (dia == hoje && !selecionado) TextDecoration.Underline else null,
                    )
                    // Pontinho: tem agendamento neste dia.
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (quantidade > 0) Tinta else Color.Transparent),
                    )
                }
            }
        }
    }
}

@Composable
private fun LinhaDoAgendamento(item: AgendamentoDetalhado, aoTocar: () -> Unit) {
    val agendamento = item.agendamento
    val cancelado = agendamento.status == StatusAgendamento.CANCELADO
    ListItem(
        leadingContent = {
            Text(
                Datas.formatarHora(agendamento.dataHora),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(56.dp),
            )
        },
        headlineContent = {
            Text(item.petNome, textDecoration = if (cancelado) TextDecoration.LineThrough else null)
        },
        supportingContent = { Text("${item.servicoNome} · ${item.tutorNome}") },
        trailingContent = {
            if (agendamento.status != StatusAgendamento.AGENDADO) {
                Text(agendamento.status.rotulo, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable(onClick = aoTocar),
    )
}

/** Android 13+ exige pedir permissão para mostrar notificações. Pergunta uma vez ao abrir a agenda. */
@Composable
private fun PedirPermissaoDeNotificacao() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val contexto = LocalContext.current
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val concedida = ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!concedida) pedir.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Preview(showBackground = true, name = "Agenda do dia")
@Composable
private fun PreviewAgenda() = PreviewNoTema {
    val hoje = LocalDate.now()
    AgendaConteudo(
        estado = AgendaEstado(
            dia = hoje,
            diasDaSemana = Semana.dias(hoje),
            quantidadePorDia = mapOf(hoje to 2, hoje.plusDays(1) to 1),
            doDia = DadosDePreview.agendaDoDia,
        ),
        aoEscolherDia = {}, aoMudarSemana = {}, aoIrParaHoje = {}, aoNovoAgendamento = {},
        aoEditar = {}, aoConcluir = {}, aoCancelar = {},
    )
}

@Preview(showBackground = true, name = "Dia sem agendamentos")
@Composable
private fun PreviewAgendaVazia() = PreviewNoTema {
    AgendaConteudo(AgendaEstado(), {}, {}, {}, {}, {}, {}, {})
}
