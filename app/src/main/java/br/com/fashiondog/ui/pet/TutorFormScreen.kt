package br.com.fashiondog.ui.pet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fashiondog.data.local.entity.Pet
import br.com.fashiondog.data.local.entity.Tutor
import br.com.fashiondog.data.repository.CadastroRepository
import br.com.fashiondog.domain.Telefone
import br.com.fashiondog.domain.ValidacaoCadastro
import br.com.fashiondog.ui.componentes.BotaoPrincipal
import br.com.fashiondog.ui.componentes.CampoTexto
import br.com.fashiondog.ui.componentes.Carregando
import br.com.fashiondog.ui.componentes.EscolhaUnica
import br.com.fashiondog.ui.componentes.Formulario
import br.com.fashiondog.ui.componentes.Tela
import br.com.fashiondog.ui.componentes.TituloSecao
import br.com.fashiondog.ui.componentes.viewModelDoApp
import br.com.fashiondog.ui.preview.PreviewNoTema
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TutorFormEstado(
    val carregando: Boolean = false,
    val ehNovo: Boolean = true,
    val nome: String = "",
    val telefone: String = "",
    val observacoes: String = "",
    val pet: CamposPet = CamposPet(),
    val erroNome: String? = null,
    val erroTelefone: String? = null,
    val salvando: Boolean = false,
    /** Preenchido quando salvou: a tela usa para navegar para o tutor. */
    val salvoComId: Long? = null,
)

class TutorFormViewModel(
    private val repositorio: CadastroRepository,
    argumentos: SavedStateHandle,
) : ViewModel() {

    private val tutorId: Long? = argumentos.get<Long>("tutorId")?.takeIf { it > 0 }
    private var original: Tutor? = null

    private val _estado = MutableStateFlow(TutorFormEstado(ehNovo = tutorId == null, carregando = tutorId != null))
    val estado: StateFlow<TutorFormEstado> = _estado.asStateFlow()

    init {
        if (tutorId != null) {
            viewModelScope.launch {
                val tutor = repositorio.buscarTutor(tutorId)
                original = tutor
                _estado.update {
                    it.copy(
                        carregando = false,
                        nome = tutor?.nome.orEmpty(),
                        telefone = tutor?.telefone?.let(Telefone::formatar).orEmpty(),
                        observacoes = tutor?.observacoes.orEmpty(),
                    )
                }
            }
        }
    }

    fun mudarNome(v: String) = _estado.update { it.copy(nome = v, erroNome = null) }
    fun mudarTelefone(v: String) = _estado.update { it.copy(telefone = v, erroTelefone = null) }
    fun mudarObservacoes(v: String) = _estado.update { it.copy(observacoes = v) }
    fun mudarPet(campos: CamposPet) = _estado.update { it.copy(pet = campos) }

    fun salvar() {
        val atual = _estado.value
        val erroNome = ValidacaoCadastro.erroNomeTutor(atual.nome)
        val erroTelefone = ValidacaoCadastro.erroTelefone(atual.telefone)
        if (erroNome != null || erroTelefone != null) {
            _estado.update { it.copy(erroNome = erroNome, erroTelefone = erroTelefone) }
            return
        }

        _estado.update { it.copy(salvando = true) }
        viewModelScope.launch {
            val tutor = (original ?: Tutor(nome = "", telefone = "")).copy(
                nome = atual.nome.trim(),
                telefone = Telefone.somenteDigitos(atual.telefone),
                observacoes = atual.observacoes.trim().ifEmpty { null },
            )
            val id = if (tutorId == null) {
                // O pet é opcional aqui: se o nome do pet estiver vazio, cadastra só o tutor.
                val pet = atual.pet.takeIf { it.nome.isNotBlank() }?.paraPet(tutorId = 0)
                repositorio.cadastrarTutorComPet(tutor, pet)
            } else {
                repositorio.salvarTutor(tutor)
            }
            _estado.update { it.copy(salvando = false, salvoComId = id) }
        }
    }
}

@Composable
fun TutorFormScreen(aoVoltar: () -> Unit, aoSalvar: (tutorId: Long, ehNovo: Boolean) -> Unit) {
    val vm = viewModelDoApp { app, args -> TutorFormViewModel(app.cadastro, args) }
    val estado by vm.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.salvoComId) {
        estado.salvoComId?.let { aoSalvar(it, estado.ehNovo) }
    }
    TutorFormConteudo(
        estado = estado,
        aoVoltar = aoVoltar,
        aoMudarNome = vm::mudarNome,
        aoMudarTelefone = vm::mudarTelefone,
        aoMudarObservacoes = vm::mudarObservacoes,
        aoMudarPet = vm::mudarPet,
        aoSalvar = vm::salvar,
    )
}

@Composable
fun TutorFormConteudo(
    estado: TutorFormEstado,
    aoVoltar: () -> Unit,
    aoMudarNome: (String) -> Unit,
    aoMudarTelefone: (String) -> Unit,
    aoMudarObservacoes: (String) -> Unit,
    aoMudarPet: (CamposPet) -> Unit,
    aoSalvar: () -> Unit,
) {
    Tela(titulo = if (estado.ehNovo) "Novo cadastro" else "Editar tutor", aoVoltar = aoVoltar) { espacamento ->
        if (estado.carregando) {
            Carregando()
            return@Tela
        }
        Formulario(espacamento) {
            TituloSecao("Tutor")
            CampoTexto(
                valor = estado.nome,
                aoMudar = aoMudarNome,
                rotulo = "Nome *",
                erro = estado.erroNome,
                maiusculas = KeyboardCapitalization.Words,
            )
            CampoTexto(
                valor = estado.telefone,
                aoMudar = aoMudarTelefone,
                rotulo = "Telefone com DDD *",
                erro = estado.erroTelefone,
                teclado = KeyboardType.Phone,
            )
            CampoTexto(
                valor = estado.observacoes,
                aoMudar = aoMudarObservacoes,
                rotulo = "Observações",
                linhas = 2,
            )

            if (estado.ehNovo) {
                TituloSecao("Primeiro pet (opcional)")
                CamposDoPet(estado.pet, aoMudarPet)
            }

            BotaoPrincipal(
                texto = if (estado.salvando) "Salvando..." else "Salvar",
                aoClicar = aoSalvar,
                habilitado = !estado.salvando,
            )
        }
    }
}

/** Campos do pet, usados tanto no novo cadastro quanto no formulário do pet. */
data class CamposPet(
    val nome: String = "",
    val raca: String = "",
    val porte: String? = null,
    val observacoes: String = "",
    val erroNome: String? = null,
) {
    fun paraPet(tutorId: Long, base: Pet? = null): Pet =
        (base ?: Pet(tutorId = tutorId, nome = "")).copy(
            nome = nome.trim(),
            raca = raca.trim().ifEmpty { null },
            porte = porte,
            observacoes = observacoes.trim().ifEmpty { null },
        )

    companion object {
        val PORTES = listOf("Pequeno", "Médio", "Grande")

        fun de(pet: Pet) = CamposPet(
            nome = pet.nome,
            raca = pet.raca.orEmpty(),
            porte = pet.porte,
            observacoes = pet.observacoes.orEmpty(),
        )
    }
}

@Composable
fun CamposDoPet(campos: CamposPet, aoMudar: (CamposPet) -> Unit) {
    CampoTexto(
        valor = campos.nome,
        aoMudar = { aoMudar(campos.copy(nome = it, erroNome = null)) },
        rotulo = "Nome do pet",
        erro = campos.erroNome,
        maiusculas = KeyboardCapitalization.Words,
    )
    CampoTexto(
        valor = campos.raca,
        aoMudar = { aoMudar(campos.copy(raca = it)) },
        rotulo = "Raça",
        maiusculas = KeyboardCapitalization.Words,
    )
    EscolhaUnica(
        titulo = "Porte",
        opcoes = CamposPet.PORTES,
        selecionada = campos.porte,
        // tocar de novo no porte marcado desmarca
        aoEscolher = { aoMudar(campos.copy(porte = if (campos.porte == it) null else it)) },
        rotuloDe = { it },
    )
    CampoTexto(
        valor = campos.observacoes,
        aoMudar = { aoMudar(campos.copy(observacoes = it)) },
        rotulo = "Observações (alergias, comportamento...)",
        linhas = 2,
    )
}

@Preview(showBackground = true, name = "Novo cadastro (tutor + pet)")
@Composable
private fun PreviewNovoCadastro() = PreviewNoTema {
    TutorFormConteudo(
        TutorFormEstado(nome = "Maria Souza", telefone = "53", erroTelefone = "Informe o telefone com DDD (10 ou 11 dígitos)",
            pet = CamposPet(nome = "Bóris", porte = "Pequeno")),
        {}, {}, {}, {}, {}, {},
    )
}

@Preview(showBackground = true, name = "Editar tutor")
@Composable
private fun PreviewEditarTutor() = PreviewNoTema {
    TutorFormConteudo(TutorFormEstado(ehNovo = false, nome = "Maria Souza", telefone = "(53) 99999-8888"), {}, {}, {}, {}, {}, {})
}
