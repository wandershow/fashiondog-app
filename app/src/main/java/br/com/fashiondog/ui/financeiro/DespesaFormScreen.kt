package br.com.fashiondog.ui.financeiro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fashiondog.data.local.entity.Categoria
import br.com.fashiondog.data.local.entity.Despesa
import br.com.fashiondog.data.local.entity.FormaPagamentoDespesa
import br.com.fashiondog.data.repository.FinanceiroRepository
import br.com.fashiondog.di.Sessao
import br.com.fashiondog.domain.Datas
import br.com.fashiondog.domain.Dinheiro
import br.com.fashiondog.domain.ParcelaPlanejada
import br.com.fashiondog.domain.PlanoDeParcelas
import br.com.fashiondog.ui.componentes.BotaoPrincipal
import br.com.fashiondog.ui.componentes.CampoData
import br.com.fashiondog.ui.componentes.CampoDinheiro
import br.com.fashiondog.ui.componentes.CampoTexto
import br.com.fashiondog.ui.componentes.Cartao
import br.com.fashiondog.ui.componentes.EscolhaUnica
import br.com.fashiondog.ui.componentes.Formulario
import br.com.fashiondog.ui.componentes.Tela
import br.com.fashiondog.ui.componentes.TituloSecao
import br.com.fashiondog.ui.componentes.viewModelDoApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

val FormaPagamentoDespesa.rotulo: String
    get() = when (this) {
        FormaPagamentoDespesa.A_VISTA -> "À vista"
        FormaPagamentoDespesa.BOLETO_PARCELADO -> "Boleto parcelado"
    }

data class DespesaFormEstado(
    val descricao: String = "",
    val categoriaId: Long? = null,
    val valorCentavos: Long = 0,
    val dataCompra: LocalDate = LocalDate.now(),
    val forma: FormaPagamentoDespesa = FormaPagamentoDespesa.A_VISTA,
    val numParcelas: Int = 2,
    val primeiroVencimento: LocalDate = LocalDate.now().plusMonths(1),
    val erro: String? = null,
    val salvando: Boolean = false,
    val salvo: Boolean = false,
) {
    /** Prévia das parcelas, recalculada a cada mudança — o que vai ser gravado. */
    val plano: List<ParcelaPlanejada>
        get() = when {
            valorCentavos <= 0 -> emptyList()
            forma == FormaPagamentoDespesa.A_VISTA -> PlanoDeParcelas.aVista(valorCentavos, dataCompra)
            else -> PlanoDeParcelas.boleto(valorCentavos, numParcelas, primeiroVencimento)
        }
}

class DespesaFormViewModel(
    private val repositorio: FinanceiroRepository,
    private val sessao: Sessao,
) : ViewModel() {

    val categorias: StateFlow<List<Categoria>> = repositorio.observarCategorias()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _estado = MutableStateFlow(DespesaFormEstado())
    val estado: StateFlow<DespesaFormEstado> = _estado.asStateFlow()

    fun mudarDescricao(v: String) = _estado.update { it.copy(descricao = v, erro = null) }
    fun escolherCategoria(c: Categoria) = _estado.update { it.copy(categoriaId = c.id, erro = null) }
    fun mudarValor(v: Long) = _estado.update { it.copy(valorCentavos = v, erro = null) }
    fun mudarDataCompra(d: LocalDate) = _estado.update { it.copy(dataCompra = d) }
    fun escolherForma(f: FormaPagamentoDespesa) = _estado.update { it.copy(forma = f) }
    fun mudarPrimeiroVencimento(d: LocalDate) = _estado.update { it.copy(primeiroVencimento = d) }

    fun mudarParcelas(delta: Int) = _estado.update {
        it.copy(numParcelas = (it.numParcelas + delta).coerceIn(1, PlanoDeParcelas.MAXIMO_DE_PARCELAS))
    }

    fun criarCategoria(nome: String) {
        if (nome.isBlank()) return
        viewModelScope.launch {
            val id = repositorio.criarCategoria(nome)
            _estado.update { it.copy(categoriaId = id) }
        }
    }

    fun salvar() {
        val atual = _estado.value
        val erro = when {
            atual.descricao.isBlank() -> "Informe a descrição"
            atual.categoriaId == null -> "Escolha a categoria"
            atual.valorCentavos <= 0 -> "Informe o valor"
            else -> null
        }
        if (erro != null) {
            _estado.update { it.copy(erro = erro) }
            return
        }
        _estado.update { it.copy(salvando = true) }
        viewModelScope.launch {
            val boleto = atual.forma == FormaPagamentoDespesa.BOLETO_PARCELADO
            val plano = atual.plano
            repositorio.salvarDespesa(
                Despesa(
                    categoriaId = checkNotNull(atual.categoriaId),
                    descricao = atual.descricao.trim(),
                    valorTotalCentavos = atual.valorCentavos,
                    formaPagamento = atual.forma,
                    numParcelas = if (boleto) atual.numParcelas else 1,
                    dataCompra = Datas.paraMillis(atual.dataCompra),
                    usuarioId = sessao.usuarioAtualId(),
                ),
                plano,
            )
            _estado.update { it.copy(salvando = false, salvo = true) }
        }
    }
}

@Composable
fun DespesaFormScreen(aoVoltar: () -> Unit) {
    val vm = viewModelDoApp { app, _ -> DespesaFormViewModel(app.financeiro, app.sessao) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val categorias by vm.categorias.collectAsStateWithLifecycle()
    var novaCategoria by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(estado.salvo) { if (estado.salvo) aoVoltar() }

    Tela(titulo = "Nova despesa", aoVoltar = aoVoltar) { espacamento ->
        Formulario(espacamento) {
            CampoTexto(estado.descricao, vm::mudarDescricao, "Descrição (ex.: shampoo 5 L)")
            EscolhaUnica(
                titulo = "Categoria",
                opcoes = categorias,
                selecionada = categorias.firstOrNull { it.id == estado.categoriaId },
                aoEscolher = vm::escolherCategoria,
                rotuloDe = { it.nome },
            )
            TextButton(onClick = { novaCategoria = "" }) { Text("+ Nova categoria") }

            CampoDinheiro(estado.valorCentavos, vm::mudarValor, "Valor total")
            CampoData(estado.dataCompra, vm::mudarDataCompra, "Data da compra")

            EscolhaUnica(
                titulo = "Pagamento",
                opcoes = FormaPagamentoDespesa.entries,
                selecionada = estado.forma,
                aoEscolher = vm::escolherForma,
                rotuloDe = { it.rotulo },
            )

            if (estado.forma == FormaPagamentoDespesa.BOLETO_PARCELADO) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Parcelas", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { vm.mudarParcelas(-1) }) { Text("−") }
                    Text("${estado.numParcelas}x", style = MaterialTheme.typography.titleLarge)
                    OutlinedButton(onClick = { vm.mudarParcelas(1) }) { Text("+") }
                }
                CampoData(estado.primeiroVencimento, vm::mudarPrimeiroVencimento, "1º vencimento")
            }

            PreviaDasParcelas(estado)

            estado.erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            BotaoPrincipal(
                texto = if (estado.salvando) "Salvando..." else "Salvar despesa",
                aoClicar = vm::salvar,
                habilitado = !estado.salvando,
            )
        }
    }

    novaCategoria?.let { nome ->
        AlertDialog(
            onDismissRequest = { novaCategoria = null },
            title = { Text("Nova categoria") },
            text = {
                CampoTexto(nome, { novaCategoria = it }, "Nome", maiusculas = KeyboardCapitalization.Sentences)
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.criarCategoria(nome)
                    novaCategoria = null
                }, enabled = nome.isNotBlank()) { Text("Criar") }
            },
            dismissButton = { TextButton(onClick = { novaCategoria = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun PreviaDasParcelas(estado: DespesaFormEstado) {
    val plano = estado.plano
    if (plano.isEmpty()) return
    TituloSecao("Como vai entrar no balanço")
    Cartao {
        plano.forEach { parcela ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (plano.size > 1) "${parcela.numero}/${plano.size} · ${Datas.formatarData(parcela.vencimento)}"
                    else "${Datas.formatarData(parcela.vencimento)} · ${if (parcela.jaPaga) "paga" else "pendente"}",
                )
                Text(Dinheiro.formatar(parcela.valorCentavos), fontWeight = FontWeight.Medium)
            }
        }
        if (estado.forma == FormaPagamentoDespesa.BOLETO_PARCELADO) {
            Text(
                "Cada parcela conta no mês do vencimento. Aviso na véspera e no dia, às 9h.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
