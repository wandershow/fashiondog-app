package br.com.fashiondog.ui.agenda

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fashiondog.data.local.entity.Agendamento
import br.com.fashiondog.data.local.entity.Servico
import br.com.fashiondog.data.local.model.PetComTutor
import br.com.fashiondog.data.repository.AgendaRepository
import br.com.fashiondog.data.repository.AtendimentoRepository
import br.com.fashiondog.data.repository.CadastroRepository
import br.com.fashiondog.di.Sessao
import br.com.fashiondog.domain.Busca
import br.com.fashiondog.domain.CalculadoraDeAvisos
import br.com.fashiondog.domain.Datas
import br.com.fashiondog.ui.componentes.BotaoPrincipal
import br.com.fashiondog.ui.componentes.CampoData
import br.com.fashiondog.ui.componentes.CampoHora
import br.com.fashiondog.ui.componentes.Carregando
import br.com.fashiondog.ui.componentes.EscolhaUnica
import br.com.fashiondog.ui.componentes.Formulario
import br.com.fashiondog.ui.componentes.Tela
import br.com.fashiondog.ui.componentes.viewModelDoApp
import br.com.fashiondog.ui.preview.DadosDePreview
import br.com.fashiondog.ui.preview.PreviewNoTema
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgendamentoFormEstado(
    val carregando: Boolean = true,
    val ehNovo: Boolean = true,
    val pets: List<PetComTutor> = emptyList(),
    val servicos: List<Servico> = emptyList(),
    val pet: PetComTutor? = null,
    val servicoId: Long? = null,
    val data: LocalDate = LocalDate.now(),
    val hora: LocalTime = LocalTime.of(9, 0),
    val lembreteMinutos: Int = 60,
    val erro: String? = null,
    val salvando: Boolean = false,
    val salvo: Boolean = false,
)

class AgendamentoFormViewModel(
    private val agenda: AgendaRepository,
    private val cadastro: CadastroRepository,
    private val atendimentos: AtendimentoRepository,
    private val sessao: Sessao,
    argumentos: SavedStateHandle,
) : ViewModel() {

    private val agendamentoId = argumentos.get<Long>("agendamentoId")?.takeIf { it > 0 }
    /** Dia que estava selecionado na agenda (epoch day), para já vir preenchido. */
    private val diaInicial = argumentos.get<Long>("dia")?.takeIf { it > 0 }?.let(LocalDate::ofEpochDay)
    private var original: Agendamento? = null

    private val _estado = MutableStateFlow(AgendamentoFormEstado(ehNovo = agendamentoId == null))
    val estado: StateFlow<AgendamentoFormEstado> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val pets = cadastro.observarPetsComTutor().first()
            val servicos = atendimentos.observarServicos().first()
            val existente = agendamentoId?.let { agenda.buscar(it) }
            original = existente
            _estado.update { atual ->
                val base = atual.copy(carregando = false, pets = pets, servicos = servicos)
                if (existente != null) {
                    val quando = Datas.paraLocalDateTime(existente.dataHora)
                    base.copy(
                        pet = pets.firstOrNull { it.pet.id == existente.petId },
                        servicoId = existente.servicoId,
                        data = quando.toLocalDate(),
                        hora = quando.toLocalTime(),
                        lembreteMinutos = existente.lembreteMinutos,
                    )
                } else {
                    base.copy(data = diaInicial?.takeIf { !it.isBefore(LocalDate.now()) } ?: LocalDate.now())
                }
            }
        }
    }

    fun escolherPet(pet: PetComTutor) = _estado.update { it.copy(pet = pet, erro = null) }
    fun escolherServico(servico: Servico) = _estado.update { it.copy(servicoId = servico.id, erro = null) }
    fun mudarData(data: LocalDate) = _estado.update { it.copy(data = data, erro = null) }
    fun mudarHora(hora: LocalTime) = _estado.update { it.copy(hora = hora, erro = null) }
    fun escolherLembrete(minutos: Int) = _estado.update { it.copy(lembreteMinutos = minutos) }

    fun salvar() {
        val atual = _estado.value
        val dataHora = Datas.paraMillis(atual.data, atual.hora)
        val erro = when {
            atual.pet == null -> "Escolha o pet"
            atual.servicoId == null -> "Escolha o serviço"
            original == null && dataHora < System.currentTimeMillis() -> "Esse horário já passou"
            else -> null
        }
        if (erro != null) {
            _estado.update { it.copy(erro = erro) }
            return
        }
        _estado.update { it.copy(salvando = true) }
        viewModelScope.launch {
            val agendamento = (original ?: Agendamento(petId = 0, servicoId = 0, dataHora = 0, usuarioId = sessao.usuarioAtualId()))
                .copy(
                    petId = checkNotNull(atual.pet).pet.id,
                    servicoId = checkNotNull(atual.servicoId),
                    dataHora = dataHora,
                    lembreteMinutos = atual.lembreteMinutos,
                )
            agenda.salvar(agendamento)
            _estado.update { it.copy(salvando = false, salvo = true) }
        }
    }
}

@Composable
fun AgendamentoFormScreen(aoVoltar: () -> Unit) {
    val vm = viewModelDoApp { app, args ->
        AgendamentoFormViewModel(app.agenda, app.cadastro, app.atendimentos, app.sessao, args)
    }
    val estado by vm.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.salvo) { if (estado.salvo) aoVoltar() }

    AgendamentoFormConteudo(
        estado,
        AcoesDoAgendamento(
            aoVoltar = aoVoltar,
            aoEscolherPet = vm::escolherPet,
            aoEscolherServico = vm::escolherServico,
            aoMudarData = vm::mudarData,
            aoMudarHora = vm::mudarHora,
            aoEscolherLembrete = vm::escolherLembrete,
            aoSalvar = vm::salvar,
        ),
    )
}

/** O que a tela pode pedir ao ViewModel. Padrões vazios para o @Preview. */
data class AcoesDoAgendamento(
    val aoVoltar: () -> Unit = {},
    val aoEscolherPet: (PetComTutor) -> Unit = {},
    val aoEscolherServico: (Servico) -> Unit = {},
    val aoMudarData: (LocalDate) -> Unit = {},
    val aoMudarHora: (LocalTime) -> Unit = {},
    val aoEscolherLembrete: (Int) -> Unit = {},
    val aoSalvar: () -> Unit = {},
)

@Composable
fun AgendamentoFormConteudo(estado: AgendamentoFormEstado, acoes: AcoesDoAgendamento) {
    var escolhendoPet by remember { mutableStateOf(false) }
    val aoVoltar = acoes.aoVoltar

    Tela(titulo = if (estado.ehNovo) "Novo agendamento" else "Editar agendamento", aoVoltar = aoVoltar) { espacamento ->
        if (estado.carregando) {
            Carregando()
            return@Tela
        }
        Formulario(espacamento) {
            Text("Pet", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = { escolhendoPet = true }, modifier = Modifier.fillMaxWidth()) {
                Text(estado.pet?.let { "${it.pet.nome} (${it.tutorNome})" } ?: "Escolher pet")
            }
            if (estado.pets.isEmpty()) {
                Text("Cadastre um pet na aba Pets antes de agendar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            EscolhaUnica(
                titulo = "Serviço",
                opcoes = estado.servicos,
                selecionada = estado.servicos.firstOrNull { it.id == estado.servicoId },
                aoEscolher = acoes.aoEscolherServico,
                rotuloDe = { it.nome },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoData(estado.data, acoes.aoMudarData, "Data", modifier = Modifier.weight(1f))
                CampoHora(estado.hora, acoes.aoMudarHora, "Hora", modifier = Modifier.weight(1f))
            }

            EscolhaUnica(
                titulo = "Aviso",
                opcoes = CalculadoraDeAvisos.OPCOES_LEMBRETE,
                selecionada = estado.lembreteMinutos,
                aoEscolher = acoes.aoEscolherLembrete,
                rotuloDe = CalculadoraDeAvisos::rotuloLembrete,
            )

            estado.erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            BotaoPrincipal(
                texto = if (estado.salvando) "Salvando..." else "Salvar agendamento",
                aoClicar = acoes.aoSalvar,
                habilitado = !estado.salvando,
            )
        }
    }

    if (escolhendoPet) {
        SeletorDePet(
            pets = estado.pets,
            aoEscolher = {
                acoes.aoEscolherPet(it)
                escolhendoPet = false
            },
            aoFechar = { escolhendoPet = false },
        )
    }
}

/** Diálogo com busca para escolher o pet (mesma busca da aba Pets). */
@Composable
private fun SeletorDePet(pets: List<PetComTutor>, aoEscolher: (PetComTutor) -> Unit, aoFechar: () -> Unit) {
    var termo by remember { mutableStateOf("") }
    val filtrados = pets.filter { Busca.corresponde(termo, it.pet.nome, it.tutorNome, it.tutorTelefone) }
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Escolher pet") },
        text = {
            Column {
                OutlinedTextField(
                    value = termo,
                    onValueChange = { termo = it },
                    placeholder = { Text("Buscar pet ou tutor") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(filtrados, key = { it.pet.id }) { item ->
                        ListItem(
                            headlineContent = { Text(item.pet.nome) },
                            supportingContent = { Text(item.tutorNome) },
                            modifier = Modifier.clickable { aoEscolher(item) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = aoFechar) { Text("Fechar") } },
    )
}

@Preview(showBackground = true, name = "Novo agendamento")
@Composable
private fun PreviewAgendamentoForm() = PreviewNoTema {
    AgendamentoFormConteudo(
        AgendamentoFormEstado(
            carregando = false,
            pets = DadosDePreview.petsComTutor,
            servicos = DadosDePreview.servicos,
            pet = DadosDePreview.petsComTutor.first(),
            servicoId = DadosDePreview.banho.id,
            hora = LocalTime.of(14, 30),
        ),
        AcoesDoAgendamento(),
    )
}

@Preview(showBackground = true, name = "Escolher pet")
@Composable
private fun PreviewSeletorDePet() = PreviewNoTema {
    SeletorDePet(DadosDePreview.petsComTutor, aoEscolher = {}, aoFechar = {})
}
