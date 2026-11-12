package br.com.fashiondog.ui.pet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fashiondog.data.local.model.AtendimentoComServico
import br.com.fashiondog.data.local.model.PetComTutor
import br.com.fashiondog.data.repository.AtendimentoRepository
import br.com.fashiondog.data.repository.CadastroRepository
import br.com.fashiondog.domain.Datas
import br.com.fashiondog.domain.Dinheiro
import br.com.fashiondog.domain.Telefone
import br.com.fashiondog.ui.atendimento.rotulo
import br.com.fashiondog.ui.componentes.Carregando
import br.com.fashiondog.ui.componentes.Cartao
import br.com.fashiondog.ui.componentes.DialogoConfirmacao
import br.com.fashiondog.ui.componentes.Formulario
import br.com.fashiondog.ui.componentes.Tela
import br.com.fashiondog.ui.componentes.TituloSecao
import br.com.fashiondog.ui.componentes.viewModelDoApp
import br.com.fashiondog.ui.preview.DadosDePreview
import br.com.fashiondog.ui.preview.PreviewNoTema
import br.com.fashiondog.ui.theme.Amarelo
import br.com.fashiondog.ui.theme.Tinta
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FichaPetEstado(
    val carregando: Boolean = true,
    val item: PetComTutor? = null,
    /** O mais recente primeiro; o primeiro da lista é o "último banho". */
    val historico: List<AtendimentoComServico> = emptyList(),
)

class FichaPetViewModel(
    private val repositorio: CadastroRepository,
    atendimentos: AtendimentoRepository,
    argumentos: SavedStateHandle,
) : ViewModel() {

    val petId: Long = checkNotNull(argumentos.get<Long>("petId"))

    val estado: StateFlow<FichaPetEstado> = combine(
        repositorio.observarPet(petId),
        atendimentos.observarHistorico(petId),
    ) { item, historico -> FichaPetEstado(carregando = false, item = item, historico = historico) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FichaPetEstado())

    suspend fun desativar() = repositorio.desativarPet(petId)
}

@Composable
fun FichaPetScreen(
    aoVoltar: () -> Unit,
    aoEditar: (Long) -> Unit,
    aoAbrirTutor: (Long) -> Unit,
    aoNovoAtendimento: (petId: Long) -> Unit,
    aoAbrirAtendimento: (Long) -> Unit,
    podeAlterar: Boolean = true,
) {
    val vm = viewModelDoApp { app, args -> FichaPetViewModel(app.cadastro, app.atendimentos, args) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val escopo = rememberCoroutineScope()
    FichaPetConteudo(
        estado = estado,
        aoVoltar = aoVoltar,
        aoEditar = aoEditar,
        aoAbrirTutor = aoAbrirTutor,
        aoNovoAtendimento = aoNovoAtendimento,
        aoAbrirAtendimento = aoAbrirAtendimento,
        aoDesativar = {
            escopo.launch {
                vm.desativar()
                aoVoltar()
            }
        },
        podeAlterar = podeAlterar,
    )
}

@Composable
fun FichaPetConteudo(
    estado: FichaPetEstado,
    aoVoltar: () -> Unit,
    aoEditar: (Long) -> Unit,
    aoAbrirTutor: (Long) -> Unit,
    aoNovoAtendimento: (petId: Long) -> Unit,
    aoAbrirAtendimento: (Long) -> Unit,
    aoDesativar: () -> Unit,
    podeAlterar: Boolean = true,
) {
    var menuAberto by remember { mutableStateOf(false) }
    var confirmarDesativar by remember { mutableStateOf(false) }

    Tela(
        titulo = estado.item?.pet?.nome ?: "Pet",
        aoVoltar = aoVoltar,
        acoes = {
            if (podeAlterar && estado.item != null) {
                IconButton(onClick = { estado.item?.let { aoEditar(it.pet.id) } }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar pet")
                }
                IconButton(onClick = { menuAberto = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Mais opções")
                }
                DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                    DropdownMenuItem(
                        text = { Text("Desativar pet") },
                        onClick = {
                            menuAberto = false
                            confirmarDesativar = true
                        },
                    )
                }
            }
        },
        botaoFlutuante = {
            if (estado.item?.pet?.ativo == true) {
                ExtendedFloatingActionButton(
                    onClick = { estado.item?.let { aoNovoAtendimento(it.pet.id) } },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Registrar atendimento") },
                    containerColor = Amarelo,
                    contentColor = Tinta,
                )
            }
        },
    ) { espacamento ->
        val item = estado.item
        if (estado.carregando || item == null) {
            Carregando()
            return@Tela
        }
        val pet = item.pet
        Formulario(espacamento) {
            Cartao {
                Text(
                    listOfNotNull(pet.raca, pet.porte).joinToString(" · ").ifEmpty { "Raça não informada" },
                    style = MaterialTheme.typography.titleMedium,
                )
                if (!pet.ativo) Text("Pet desativado", color = MaterialTheme.colorScheme.error)
                pet.observacoes?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Cartao(modifier = Modifier.clickable { aoAbrirTutor(pet.tutorId) }) {
                Text("Tutor", style = MaterialTheme.typography.labelLarge)
                Text(item.tutorNome, style = MaterialTheme.typography.titleMedium)
                Text(Telefone.formatar(item.tutorTelefone), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            UltimoBanho(estado.historico.firstOrNull(), aoAbrirAtendimento)

            if (estado.historico.isNotEmpty()) {
                TituloSecao("Histórico (${estado.historico.size})")
                estado.historico.forEach { linha ->
                    val atendimento = linha.atendimento
                    ListItem(
                        headlineContent = { Text(linha.servicoNome) },
                        supportingContent = {
                            Text("${Datas.formatarData(atendimento.data)} · ${atendimento.formaPagamento.rotulo}")
                        },
                        trailingContent = { Text(Dinheiro.formatar(atendimento.valorCentavos)) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                        modifier = Modifier.clickable { aoAbrirAtendimento(atendimento.id) },
                    )
                    HorizontalDivider()
                }
            }
            // espaço para o botão flutuante não cobrir o último item
            Spacer(Modifier.height(72.dp))
        }
    }

    if (confirmarDesativar) {
        DialogoConfirmacao(
            titulo = "Desativar pet?",
            mensagem = "O pet sai das listas, mas o histórico de atendimentos continua guardado.",
            textoConfirmar = "Desativar",
            aoConfirmar = {
                confirmarDesativar = false
                aoDesativar()
            },
            aoCancelar = { confirmarDesativar = false },
        )
    }
}

/** O destaque da ficha: quando foi o último banho, qual serviço e quanto custou. */
@Composable
private fun UltimoBanho(ultimo: AtendimentoComServico?, aoAbrir: (Long) -> Unit) {
    if (ultimo == null) {
        Cartao(destaque = true) {
            Text("Último banho", style = MaterialTheme.typography.labelLarge)
            Text("Nenhum atendimento registrado ainda.")
        }
        return
    }
    val atendimento = ultimo.atendimento
    Cartao(destaque = true, modifier = Modifier.clickable { aoAbrir(atendimento.id) }) {
        Text("Último banho · ${Datas.haQuantoTempo(atendimento.data)}", style = MaterialTheme.typography.labelLarge)
        Text(Datas.formatarData(atendimento.data), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text("${ultimo.servicoNome} · ${Dinheiro.formatar(atendimento.valorCentavos)}", style = MaterialTheme.typography.titleMedium)
    }
}

@Preview(showBackground = true, name = "Ficha com histórico")
@Composable
private fun PreviewFichaPet() = PreviewNoTema {
    FichaPetConteudo(
        FichaPetEstado(carregando = false, item = DadosDePreview.petsComTutor.first(), historico = DadosDePreview.historico),
        {}, {}, {}, {}, {}, {},
    )
}

@Preview(showBackground = true, name = "Ficha sem atendimentos")
@Composable
private fun PreviewFichaPetVazia() = PreviewNoTema {
    FichaPetConteudo(FichaPetEstado(carregando = false, item = DadosDePreview.petsComTutor[1]), {}, {}, {}, {}, {}, {})
}
