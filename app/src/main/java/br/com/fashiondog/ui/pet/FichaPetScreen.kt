package br.com.fashiondog.ui.pet

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fashiondog.data.local.model.PetComTutor
import br.com.fashiondog.data.repository.CadastroRepository
import br.com.fashiondog.domain.Telefone
import br.com.fashiondog.ui.componentes.Cartao
import br.com.fashiondog.ui.componentes.Carregando
import br.com.fashiondog.ui.componentes.DialogoConfirmacao
import br.com.fashiondog.ui.componentes.Formulario
import br.com.fashiondog.ui.componentes.Tela
import br.com.fashiondog.ui.componentes.viewModelDoApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FichaPetEstado(
    val carregando: Boolean = true,
    val item: PetComTutor? = null,
)

class FichaPetViewModel(
    private val repositorio: CadastroRepository,
    argumentos: SavedStateHandle,
) : ViewModel() {

    val petId: Long = checkNotNull(argumentos.get<Long>("petId"))

    val estado: StateFlow<FichaPetEstado> = repositorio.observarPet(petId)
        .map { FichaPetEstado(carregando = false, item = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FichaPetEstado())

    suspend fun desativar() = repositorio.desativarPet(petId)
}

@Composable
fun FichaPetScreen(
    aoVoltar: () -> Unit,
    aoEditar: (Long) -> Unit,
    aoAbrirTutor: (Long) -> Unit,
    podeAlterar: Boolean = true,
) {
    val vm = viewModelDoApp { app, args -> FichaPetViewModel(app.cadastro, args) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val escopo = rememberCoroutineScope()
    var menuAberto by remember { mutableStateOf(false) }
    var confirmarDesativar by remember { mutableStateOf(false) }

    Tela(
        titulo = estado.item?.pet?.nome ?: "Pet",
        aoVoltar = aoVoltar,
        acoes = {
            if (podeAlterar && estado.item != null) {
                IconButton(onClick = { aoEditar(vm.petId) }) {
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
        }
    }

    if (confirmarDesativar) {
        DialogoConfirmacao(
            titulo = "Desativar pet?",
            mensagem = "O pet sai das listas, mas o histórico de atendimentos continua guardado.",
            textoConfirmar = "Desativar",
            aoConfirmar = {
                confirmarDesativar = false
                escopo.launch {
                    vm.desativar()
                    aoVoltar()
                }
            },
            aoCancelar = { confirmarDesativar = false },
        )
    }
}
