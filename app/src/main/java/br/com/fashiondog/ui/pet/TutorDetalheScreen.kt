package br.com.fashiondog.ui.pet

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fashiondog.data.local.entity.Pet
import br.com.fashiondog.data.local.entity.Tutor
import br.com.fashiondog.data.repository.CadastroRepository
import br.com.fashiondog.domain.Telefone
import br.com.fashiondog.ui.componentes.Carregando
import br.com.fashiondog.ui.componentes.Cartao
import br.com.fashiondog.ui.componentes.DialogoConfirmacao
import br.com.fashiondog.ui.componentes.Formulario
import br.com.fashiondog.ui.componentes.Tela
import br.com.fashiondog.ui.componentes.TituloSecao
import br.com.fashiondog.ui.componentes.viewModelDoApp
import br.com.fashiondog.ui.preview.DadosDePreview
import br.com.fashiondog.ui.preview.PreviewNoTema
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TutorDetalheEstado(
    val carregando: Boolean = true,
    val tutor: Tutor? = null,
    val pets: List<Pet> = emptyList(),
)

class TutorDetalheViewModel(
    private val repositorio: CadastroRepository,
    argumentos: SavedStateHandle,
) : ViewModel() {

    val tutorId: Long = checkNotNull(argumentos.get<Long>("tutorId"))

    val estado: StateFlow<TutorDetalheEstado> = combine(
        repositorio.observarTutor(tutorId),
        repositorio.observarPetsDoTutor(tutorId),
    ) { tutor, pets -> TutorDetalheEstado(carregando = false, tutor = tutor, pets = pets) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TutorDetalheEstado())

    suspend fun desativar() = repositorio.desativarTutor(tutorId)
}

@Composable
fun TutorDetalheScreen(
    aoVoltar: () -> Unit,
    aoEditar: (Long) -> Unit,
    aoNovoPet: (Long) -> Unit,
    aoAbrirPet: (Long) -> Unit,
    podeAlterar: Boolean = true,
) {
    val vm = viewModelDoApp { app, args -> TutorDetalheViewModel(app.cadastro, args) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val escopo = rememberCoroutineScope()
    TutorDetalheConteudo(
        estado = estado,
        aoVoltar = aoVoltar,
        aoEditar = aoEditar,
        aoNovoPet = aoNovoPet,
        aoAbrirPet = aoAbrirPet,
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
fun TutorDetalheConteudo(
    estado: TutorDetalheEstado,
    aoVoltar: () -> Unit,
    aoEditar: (Long) -> Unit,
    aoNovoPet: (Long) -> Unit,
    aoAbrirPet: (Long) -> Unit,
    aoDesativar: () -> Unit,
    podeAlterar: Boolean = true,
) {
    val contexto = LocalContext.current
    var menuAberto by remember { mutableStateOf(false) }
    var confirmarDesativar by remember { mutableStateOf(false) }

    Tela(
        titulo = estado.tutor?.nome ?: "Tutor",
        aoVoltar = aoVoltar,
        acoes = {
            if (podeAlterar && estado.tutor != null) {
                IconButton(onClick = { estado.tutor?.let { aoEditar(it.id) } }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar tutor")
                }
                IconButton(onClick = { menuAberto = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Mais opções")
                }
                DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                    DropdownMenuItem(
                        text = { Text("Desativar tutor") },
                        onClick = {
                            menuAberto = false
                            confirmarDesativar = true
                        },
                    )
                }
            }
        },
    ) { espacamento ->
        val tutor = estado.tutor
        if (estado.carregando || tutor == null) {
            Carregando()
            return@Tela
        }
        Formulario(espacamento) {
            Cartao {
                Text(Telefone.formatar(tutor.telefone), style = MaterialTheme.typography.titleLarge)
                if (!tutor.ativo) Text("Tutor desativado", color = MaterialTheme.colorScheme.error)
                tutor.observacoes?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { ligar(contexto, tutor.telefone) }) {
                        Icon(Icons.Filled.Call, contentDescription = null)
                        Text("  Ligar")
                    }
                    OutlinedButton(onClick = { abrirWhatsApp(contexto, tutor.telefone) }) {
                        Text("WhatsApp")
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TituloSecao("Pets")
                if (tutor.ativo) {
                    TextButton(onClick = { aoNovoPet(tutor.id) }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text("Adicionar pet")
                    }
                }
            }
            if (estado.pets.isEmpty()) {
                Text("Nenhum pet ativo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            estado.pets.forEach { pet ->
                ListItem(
                    headlineContent = { Text(pet.nome, fontWeight = FontWeight.Medium) },
                    supportingContent = {
                        Text(listOfNotNull(pet.raca, pet.porte).joinToString(" · ").ifEmpty { "—" })
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                    modifier = Modifier.clickable { aoAbrirPet(pet.id) },
                )
                HorizontalDivider()
            }
        }
    }

    if (confirmarDesativar) {
        DialogoConfirmacao(
            titulo = "Desativar tutor?",
            mensagem = "O tutor e os pets dele saem das listas, mas o histórico de atendimentos continua guardado.",
            textoConfirmar = "Desativar",
            aoConfirmar = {
                confirmarDesativar = false
                aoDesativar()
            },
            aoCancelar = { confirmarDesativar = false },
        )
    }
}

private fun ligar(contexto: Context, telefone: String) {
    abrir(contexto, Intent(Intent.ACTION_DIAL, "tel:${Telefone.somenteDigitos(telefone)}".toUri()))
}

private fun abrirWhatsApp(contexto: Context, telefone: String) {
    abrir(contexto, Intent(Intent.ACTION_VIEW, "https://wa.me/${Telefone.paraWhatsApp(telefone)}".toUri()))
}

private fun abrir(contexto: Context, intent: Intent) {
    try {
        contexto.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(contexto, "Nenhum app encontrado para isso.", Toast.LENGTH_SHORT).show()
    }
}

@Preview(showBackground = true, name = "Tutor com 2 pets")
@Composable
private fun PreviewTutorDetalhe() = PreviewNoTema {
    TutorDetalheConteudo(
        TutorDetalheEstado(carregando = false, tutor = DadosDePreview.maria, pets = listOf(DadosDePreview.boris, DadosDePreview.thor)),
        {}, {}, {}, {}, {},
    )
}
