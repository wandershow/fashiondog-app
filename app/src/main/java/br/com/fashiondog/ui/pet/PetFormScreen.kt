package br.com.fashiondog.ui.pet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fashiondog.data.local.entity.Pet
import br.com.fashiondog.data.repository.CadastroRepository
import br.com.fashiondog.domain.ValidacaoCadastro
import br.com.fashiondog.ui.componentes.BotaoPrincipal
import br.com.fashiondog.ui.componentes.Carregando
import br.com.fashiondog.ui.componentes.Formulario
import br.com.fashiondog.ui.componentes.Tela
import br.com.fashiondog.ui.componentes.viewModelDoApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PetFormEstado(
    val carregando: Boolean = false,
    val ehNovo: Boolean = true,
    val campos: CamposPet = CamposPet(),
    val salvando: Boolean = false,
    val salvoComId: Long? = null,
)

/**
 * Formulário do pet. Rota com `petId` = editar; com `tutorId` = novo pet daquele tutor.
 */
class PetFormViewModel(
    private val repositorio: CadastroRepository,
    argumentos: SavedStateHandle,
) : ViewModel() {

    private val petId: Long? = argumentos.get<Long>("petId")?.takeIf { it > 0 }
    private val tutorId: Long = argumentos.get<Long>("tutorId") ?: 0
    private var original: Pet? = null

    private val _estado = MutableStateFlow(PetFormEstado(ehNovo = petId == null, carregando = petId != null))
    val estado: StateFlow<PetFormEstado> = _estado.asStateFlow()

    init {
        if (petId != null) {
            viewModelScope.launch {
                val pet = repositorio.buscarPet(petId)
                original = pet
                _estado.update {
                    it.copy(carregando = false, campos = pet?.let(CamposPet::de) ?: CamposPet())
                }
            }
        }
    }

    fun mudarCampos(campos: CamposPet) = _estado.update { it.copy(campos = campos) }

    fun salvar() {
        val campos = _estado.value.campos
        val erro = ValidacaoCadastro.erroNomePet(campos.nome)
        if (erro != null) {
            _estado.update { it.copy(campos = campos.copy(erroNome = erro)) }
            return
        }
        _estado.update { it.copy(salvando = true) }
        viewModelScope.launch {
            val pet = campos.paraPet(tutorId = original?.tutorId ?: tutorId, base = original)
            val id = repositorio.salvarPet(pet)
            _estado.update { it.copy(salvando = false, salvoComId = id) }
        }
    }
}

@Composable
fun PetFormScreen(aoVoltar: () -> Unit, aoSalvar: (petId: Long) -> Unit) {
    val vm = viewModelDoApp { app, args -> PetFormViewModel(app.cadastro, args) }
    val estado by vm.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.salvoComId) {
        estado.salvoComId?.let(aoSalvar)
    }

    Tela(titulo = if (estado.ehNovo) "Novo pet" else "Editar pet", aoVoltar = aoVoltar) { espacamento ->
        if (estado.carregando) {
            Carregando()
            return@Tela
        }
        Formulario(espacamento) {
            CamposDoPet(estado.campos, vm::mudarCampos)
            BotaoPrincipal(
                texto = if (estado.salvando) "Salvando..." else "Salvar",
                aoClicar = vm::salvar,
                habilitado = !estado.salvando,
            )
        }
    }
}
