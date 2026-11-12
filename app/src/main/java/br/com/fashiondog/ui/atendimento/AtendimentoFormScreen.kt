package br.com.fashiondog.ui.atendimento

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fashiondog.data.fotos.ArmazenamentoDeFotos
import br.com.fashiondog.data.local.entity.Atendimento
import br.com.fashiondog.data.local.entity.FormaPagamento
import br.com.fashiondog.data.local.entity.Servico
import br.com.fashiondog.data.local.entity.TipoFoto
import br.com.fashiondog.data.repository.AtendimentoRepository
import br.com.fashiondog.data.repository.CadastroRepository
import br.com.fashiondog.di.Sessao
import br.com.fashiondog.domain.Datas
import br.com.fashiondog.ui.componentes.BotaoPrincipal
import br.com.fashiondog.ui.componentes.CampoData
import br.com.fashiondog.ui.componentes.CampoDinheiro
import br.com.fashiondog.ui.componentes.CampoTexto
import br.com.fashiondog.ui.componentes.Carregando
import br.com.fashiondog.ui.componentes.EscolhaUnica
import br.com.fashiondog.ui.componentes.Formulario
import br.com.fashiondog.ui.componentes.Tela
import br.com.fashiondog.ui.componentes.TituloSecao
import br.com.fashiondog.ui.componentes.viewModelDoApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

val FormaPagamento.rotulo: String
    get() = when (this) {
        FormaPagamento.PIX -> "Pix"
        FormaPagamento.DINHEIRO -> "Dinheiro"
        FormaPagamento.CARTAO -> "Cartão"
    }

data class AtendimentoFormEstado(
    val carregando: Boolean = true,
    val ehNovo: Boolean = true,
    val petNome: String = "",
    val servicos: List<Servico> = emptyList(),
    val servicoId: Long? = null,
    val valorCentavos: Long = 0,
    val formaPagamento: FormaPagamento? = null,
    val data: LocalDate = LocalDate.now(),
    val observacoes: String = "",
    val fotos: Map<TipoFoto, String?> = emptyMap(),
    val processandoFoto: TipoFoto? = null,
    val erroServico: String? = null,
    val erroValor: String? = null,
    val erroPagamento: String? = null,
    val mensagem: String? = null,
    val salvando: Boolean = false,
    val salvoComId: Long? = null,
)

/**
 * Registrar ou editar um atendimento.
 * Argumentos da rota: `petId` (novo), `atendimentoId` (editar) e, vindos da
 * agenda (Sprint 3), `agendamentoId` e `servicoId` para já vir preenchido.
 */
class AtendimentoFormViewModel(
    private val atendimentos: AtendimentoRepository,
    private val cadastro: CadastroRepository,
    private val armazenamento: ArmazenamentoDeFotos,
    private val sessao: Sessao,
    argumentos: SavedStateHandle,
) : ViewModel() {

    private val atendimentoId = argumentos.get<Long>("atendimentoId")?.takeIf { it > 0 }
    private var petId = argumentos.get<Long>("petId") ?: -1
    private val agendamentoId = argumentos.get<Long>("agendamentoId")?.takeIf { it > 0 }
    private val servicoInicial = argumentos.get<Long>("servicoId")?.takeIf { it > 0 }

    private var original: Atendimento? = null
    /** Fotos comprimidas nesta tela; se o usuário desistir, são apagadas. */
    private val fotosNovas = mutableSetOf<String>()
    private var salvou = false

    private val _estado = MutableStateFlow(AtendimentoFormEstado(ehNovo = atendimentoId == null))
    val estado: StateFlow<AtendimentoFormEstado> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val servicos = atendimentos.observarServicos().first()
            val existente = atendimentoId?.let { atendimentos.buscar(it) }
            original = existente
            if (existente != null) petId = existente.petId
            val pet = cadastro.buscarPet(petId)

            _estado.update { atual ->
                if (existente != null) {
                    atual.copy(
                        servicoId = existente.servicoId,
                        valorCentavos = existente.valorCentavos,
                        formaPagamento = existente.formaPagamento,
                        data = Datas.paraLocalDate(existente.data),
                        observacoes = existente.observacoes.orEmpty(),
                        fotos = atendimentos.fotosDe(existente.id).associate { it.tipo to it.caminho },
                    )
                } else {
                    val servico = servicos.firstOrNull { it.id == servicoInicial }
                    atual.copy(servicoId = servico?.id, valorCentavos = servico?.precoSugeridoCentavos ?: 0)
                }.copy(carregando = false, petNome = pet?.nome.orEmpty(), servicos = servicos)
            }
        }
    }

    /** Escolher o serviço preenche o valor com o preço sugerido (pode ser alterado). */
    fun escolherServico(servico: Servico) = _estado.update {
        it.copy(servicoId = servico.id, valorCentavos = servico.precoSugeridoCentavos, erroServico = null, erroValor = null)
    }

    fun mudarValor(centavos: Long) = _estado.update { it.copy(valorCentavos = centavos, erroValor = null) }
    fun escolherPagamento(forma: FormaPagamento) = _estado.update { it.copy(formaPagamento = forma, erroPagamento = null) }
    fun mudarData(data: LocalDate) = _estado.update { it.copy(data = data) }
    fun mudarObservacoes(texto: String) = _estado.update { it.copy(observacoes = texto) }
    fun mensagemMostrada() = _estado.update { it.copy(mensagem = null) }

    fun destinoParaCamera(): Uri = armazenamento.novoDestinoParaCamera()

    fun receberFoto(tipo: TipoFoto, origem: Uri) {
        _estado.update { it.copy(processandoFoto = tipo) }
        viewModelScope.launch {
            try {
                val caminho = armazenamento.importar(origem)
                fotosNovas += caminho
                _estado.update { it.copy(fotos = it.fotos + (tipo to caminho)) }
            } catch (e: Exception) {
                _estado.update { it.copy(mensagem = "Não foi possível usar a foto: ${e.message}") }
            } finally {
                _estado.update { it.copy(processandoFoto = null) }
            }
        }
    }

    fun removerFoto(tipo: TipoFoto) = _estado.update { it.copy(fotos = it.fotos + (tipo to null)) }

    fun salvar() {
        val atual = _estado.value
        val erroServico = if (atual.servicoId == null) "Escolha o serviço" else null
        val erroValor = if (atual.valorCentavos <= 0) "Informe o valor cobrado" else null
        val erroPagamento = if (atual.formaPagamento == null) "Escolha a forma de pagamento" else null
        if (erroServico != null || erroValor != null || erroPagamento != null) {
            _estado.update { it.copy(erroServico = erroServico, erroValor = erroValor, erroPagamento = erroPagamento) }
            return
        }

        _estado.update { it.copy(salvando = true) }
        viewModelScope.launch {
            val atendimento = (original ?: Atendimento(
                petId = petId,
                servicoId = 0,
                agendamentoId = agendamentoId,
                data = 0,
                valorCentavos = 0,
                formaPagamento = FormaPagamento.PIX,
                usuarioId = sessao.usuarioAtualId(),
            )).copy(
                servicoId = checkNotNull(atual.servicoId),
                valorCentavos = atual.valorCentavos,
                formaPagamento = checkNotNull(atual.formaPagamento),
                data = dataParaGravar(atual.data),
                observacoes = atual.observacoes.trim().ifEmpty { null },
            )
            val id = atendimentos.salvar(atendimento, atual.fotos)
            salvou = true
            // Fotos tiradas aqui mas trocadas antes de salvar não são usadas: apaga.
            fotosNovas.filterNot { it in atual.fotos.values }.forEach(armazenamento::excluir)
            _estado.update { it.copy(salvando = false, salvoComId = id) }
        }
    }

    /**
     * Mantém a hora original ao editar sem mudar o dia. Atendimento novo de hoje
     * grava a hora atual; de outro dia, meio-dia (só a data importa no histórico).
     */
    private fun dataParaGravar(data: LocalDate): Long {
        val anterior = original?.data
        return when {
            anterior != null && Datas.paraLocalDate(anterior) == data -> anterior
            data == LocalDate.now() -> System.currentTimeMillis()
            else -> Datas.paraMillis(data)
        }
    }

    override fun onCleared() {
        if (!salvou) fotosNovas.forEach(armazenamento::excluir)
    }
}

@Composable
fun AtendimentoFormScreen(aoVoltar: () -> Unit, aoSalvar: (atendimentoId: Long) -> Unit) {
    val vm = viewModelDoApp { app, args ->
        AtendimentoFormViewModel(app.atendimentos, app.cadastro, app.fotos, app.sessao, args)
    }
    val estado by vm.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.salvoComId) { estado.salvoComId?.let(aoSalvar) }

    Tela(
        titulo = if (estado.ehNovo) "Novo atendimento" else "Editar atendimento",
        aoVoltar = aoVoltar,
    ) { espacamento ->
        if (estado.carregando) {
            Carregando()
            return@Tela
        }
        Formulario(espacamento) {
            Text(estado.petNome, style = MaterialTheme.typography.headlineSmall)

            EscolhaUnica(
                titulo = "Serviço",
                opcoes = estado.servicos,
                selecionada = estado.servicos.firstOrNull { it.id == estado.servicoId },
                aoEscolher = vm::escolherServico,
                rotuloDe = { it.nome },
            )
            estado.erroServico?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            CampoDinheiro(
                centavos = estado.valorCentavos,
                aoMudar = vm::mudarValor,
                rotulo = "Valor cobrado",
                erro = estado.erroValor,
            )

            EscolhaUnica(
                titulo = "Pagamento",
                opcoes = FormaPagamento.entries,
                selecionada = estado.formaPagamento,
                aoEscolher = vm::escolherPagamento,
                rotuloDe = { it.rotulo },
            )
            estado.erroPagamento?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            CampoData(data = estado.data, aoMudar = vm::mudarData, rotulo = "Data")
            CampoTexto(valor = estado.observacoes, aoMudar = vm::mudarObservacoes, rotulo = "Observações", linhas = 2)

            TituloSecao("Fotos")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TipoFoto.entries.forEach { tipo ->
                    EspacoDeFoto(
                        tipo = tipo,
                        caminho = estado.fotos[tipo],
                        processando = estado.processandoFoto == tipo,
                        vm = vm,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            estado.mensagem?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                LaunchedEffect(it) {
                    delay(4_000)
                    vm.mensagemMostrada()
                }
            }

            BotaoPrincipal(
                texto = if (estado.salvando) "Salvando..." else "Salvar atendimento",
                aoClicar = vm::salvar,
                habilitado = !estado.salvando && estado.processandoFoto == null,
            )
        }
    }
}

/** Um espaço de foto (antes ou depois) com os botões Câmera / Galeria / Remover. */
@Composable
private fun EspacoDeFoto(
    tipo: TipoFoto,
    caminho: String?,
    processando: Boolean,
    vm: AtendimentoFormViewModel,
    modifier: Modifier,
) {
    // rememberSaveable: o Android pode recriar a tela enquanto a câmera está aberta.
    var destinoCamera by rememberSaveable { mutableStateOf<Uri?>(null) }

    // Abre o app de câmera do sistema; ele grava a foto em destinoCamera.
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { tirou ->
        val destino = destinoCamera
        if (tirou && destino != null) vm.receberFoto(tipo, destino)
    }
    // Seletor de fotos do Android (não precisa pedir permissão de armazenamento).
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.receberFoto(tipo, uri)
    }

    FotoComAcoes(
        titulo = tipo.rotulo,
        caminho = caminho,
        processando = processando,
        aoCamera = {
            val destino = vm.destinoParaCamera()
            destinoCamera = destino
            camera.launch(destino)
        },
        aoGaleria = { galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        aoRemover = { vm.removerFoto(tipo) },
        modifier = modifier,
    )
}
