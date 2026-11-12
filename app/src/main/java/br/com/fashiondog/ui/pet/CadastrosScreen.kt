package br.com.fashiondog.ui.pet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fashiondog.data.local.model.PetComTutor
import br.com.fashiondog.data.local.model.TutorComQuantidade
import br.com.fashiondog.data.repository.CadastroRepository
import br.com.fashiondog.domain.Busca
import br.com.fashiondog.domain.Telefone
import br.com.fashiondog.ui.componentes.Carregando
import br.com.fashiondog.ui.componentes.EstadoVazio
import br.com.fashiondog.ui.componentes.Tela
import br.com.fashiondog.ui.componentes.viewModelDoApp
import br.com.fashiondog.ui.theme.Tinta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class ModoLista(val rotulo: String) { PETS("Pets"), TUTORES("Tutores") }

data class CadastrosEstado(
    val carregando: Boolean = true,
    val busca: String = "",
    val modo: ModoLista = ModoLista.PETS,
    val pets: List<PetComTutor> = emptyList(),
    val tutores: List<TutorComQuantidade> = emptyList(),
)

class CadastrosViewModel(repositorio: CadastroRepository) : ViewModel() {

    private val busca = MutableStateFlow("")
    private val modo = MutableStateFlow(ModoLista.PETS)

    /**
     * O estado da tela é recalculado sempre que muda a busca, o modo ou o banco.
     * O Room avisa sozinho quando um tutor ou pet é salvo (Flow).
     */
    val estado: StateFlow<CadastrosEstado> = combine(
        busca,
        modo,
        repositorio.observarPetsComTutor(),
        repositorio.observarTutores(),
    ) { termo, modoAtual, pets, tutores ->
        CadastrosEstado(
            carregando = false,
            busca = termo,
            modo = modoAtual,
            pets = pets.filter {
                Busca.corresponde(termo, it.pet.nome, it.pet.raca, it.tutorNome, it.tutorTelefone)
            },
            tutores = tutores.filter {
                Busca.corresponde(termo, it.tutor.nome, it.tutor.telefone)
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CadastrosEstado())

    fun mudarBusca(texto: String) {
        busca.value = texto
    }

    fun mudarModo(novo: ModoLista) {
        modo.value = novo
    }
}

@Composable
fun CadastrosScreen(
    aoAbrirPet: (Long) -> Unit,
    aoAbrirTutor: (Long) -> Unit,
    aoNovoCadastro: () -> Unit,
) {
    val vm = viewModelDoApp { app, _ -> CadastrosViewModel(app.cadastro) }
    val estado by vm.estado.collectAsStateWithLifecycle()

    Tela(
        titulo = "Pets e tutores",
        botaoFlutuante = {
            ExtendedFloatingActionButton(
                onClick = aoNovoCadastro,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Novo cadastro") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Tinta,
            )
        },
    ) { espacamento ->
        Column(modifier = Modifier.fillMaxSize().padding(espacamento)) {
            OutlinedTextField(
                value = estado.busca,
                onValueChange = vm::mudarBusca,
                placeholder = { Text("Buscar por pet, tutor ou telefone") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
            ) {
                ModoLista.entries.forEachIndexed { indice, modo ->
                    SegmentedButton(
                        selected = estado.modo == modo,
                        onClick = { vm.mudarModo(modo) },
                        shape = SegmentedButtonDefaults.itemShape(indice, ModoLista.entries.size),
                    ) { Text(modo.rotulo) }
                }
            }

            when {
                estado.carregando -> Carregando()
                estado.modo == ModoLista.PETS -> ListaPets(estado, aoAbrirPet)
                else -> ListaTutores(estado, aoAbrirTutor)
            }
        }
    }
}

/** Espaço no fim da lista para o botão flutuante não cobrir o último item. */
private val espacoDoBotao = PaddingValues(bottom = 88.dp)

@Composable
private fun ListaPets(estado: CadastrosEstado, aoAbrirPet: (Long) -> Unit) {
    if (estado.pets.isEmpty()) {
        EstadoVazio(
            if (estado.busca.isBlank()) "Nenhum pet cadastrado ainda.\nToque em \"Novo cadastro\"."
            else "Nenhum pet encontrado para \"${estado.busca}\".",
        )
        return
    }
    LazyColumn(contentPadding = espacoDoBotao) {
        items(estado.pets, key = { it.pet.id }) { item ->
            ListItem(
                headlineContent = { Text(item.pet.nome) },
                supportingContent = {
                    Text(listOfNotNull(item.pet.raca, item.pet.porte).joinToString(" · ").ifEmpty { "—" })
                },
                trailingContent = {
                    Column {
                        Text(item.tutorNome, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            Telefone.formatar(item.tutorTelefone),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                modifier = Modifier.clickable { aoAbrirPet(item.pet.id) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun ListaTutores(estado: CadastrosEstado, aoAbrirTutor: (Long) -> Unit) {
    if (estado.tutores.isEmpty()) {
        EstadoVazio(
            if (estado.busca.isBlank()) "Nenhum tutor cadastrado ainda."
            else "Nenhum tutor encontrado para \"${estado.busca}\".",
        )
        return
    }
    LazyColumn(contentPadding = espacoDoBotao) {
        items(estado.tutores, key = { it.tutor.id }) { item ->
            ListItem(
                headlineContent = { Text(item.tutor.nome) },
                supportingContent = { Text(Telefone.formatar(item.tutor.telefone)) },
                trailingContent = {
                    Text(
                        when (item.quantidadePets) {
                            0 -> "sem pets"
                            1 -> "1 pet"
                            else -> "${item.quantidadePets} pets"
                        },
                    )
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                modifier = Modifier.clickable { aoAbrirTutor(item.tutor.id) },
            )
            HorizontalDivider()
        }
    }
}
