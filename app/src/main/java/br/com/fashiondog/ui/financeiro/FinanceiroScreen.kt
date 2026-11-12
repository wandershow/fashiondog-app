package br.com.fashiondog.ui.financeiro

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fashiondog.data.local.entity.FormaPagamentoDespesa
import br.com.fashiondog.data.local.entity.StatusParcela
import br.com.fashiondog.data.local.model.ParcelaDetalhada
import br.com.fashiondog.data.local.model.TotalPorCategoria
import br.com.fashiondog.data.repository.Balanco
import br.com.fashiondog.data.repository.FinanceiroRepository
import br.com.fashiondog.domain.Datas
import br.com.fashiondog.domain.Dinheiro
import br.com.fashiondog.domain.Periodo
import br.com.fashiondog.domain.TipoPeriodo
import br.com.fashiondog.ui.componentes.Cartao
import br.com.fashiondog.ui.componentes.DialogoConfirmacao
import br.com.fashiondog.ui.componentes.EscolhaUnica
import br.com.fashiondog.ui.componentes.Formulario
import br.com.fashiondog.ui.componentes.Tela
import br.com.fashiondog.ui.componentes.TituloSecao
import br.com.fashiondog.ui.componentes.viewModelDoApp
import br.com.fashiondog.ui.preview.DadosDePreview
import br.com.fashiondog.ui.preview.PreviewNoTema
import br.com.fashiondog.ui.theme.Erro
import br.com.fashiondog.ui.theme.Tinta
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FinanceiroEstado(
    val periodo: Periodo = Periodo.de(TipoPeriodo.MES, LocalDate.now()),
    val balanco: Balanco = Balanco(),
    val porCategoria: List<TotalPorCategoria> = emptyList(),
    val parcelasDoPeriodo: List<ParcelaDetalhada> = emptyList(),
    val aPagar: List<ParcelaDetalhada> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceiroViewModel(private val repositorio: FinanceiroRepository) : ViewModel() {

    private val periodo = MutableStateFlow(Periodo.de(TipoPeriodo.MES, LocalDate.now()))

    /** Toda vez que o período muda, as consultas do período são refeitas. */
    val estado: StateFlow<FinanceiroEstado> = combine(
        periodo,
        periodo.flatMapLatest(repositorio::observarBalanco),
        periodo.flatMapLatest(repositorio::observarTotalPorCategoria),
        periodo.flatMapLatest(repositorio::observarParcelasDoPeriodo),
        repositorio.observarAPagar(),
    ) { p, balanco, categorias, parcelas, aPagar ->
        FinanceiroEstado(p, balanco, categorias, parcelas, aPagar)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinanceiroEstado())

    fun escolherTipo(tipo: TipoPeriodo) {
        // Se o período atual inclui hoje, o novo também inclui hoje; senão, mantém o
        // ponto em que a pessoa estava (ex.: de "março" para "Ano" mostra aquele ano).
        val atual = periodo.value
        val referencia = if (atual.contem(LocalDate.now())) LocalDate.now() else atual.inicio
        periodo.value = Periodo.de(tipo, referencia)
    }

    fun anterior() {
        periodo.value = periodo.value.anterior()
    }

    fun proximo() {
        periodo.value = periodo.value.proximo()
    }

    fun marcarPaga(parcelaId: Long, paga: Boolean) {
        viewModelScope.launch { repositorio.marcarPaga(parcelaId, paga) }
    }

    fun excluirDespesa(despesaId: Long) {
        viewModelScope.launch { repositorio.excluirDespesa(despesaId) }
    }
}

@Composable
fun FinanceiroScreen(aoNovaDespesa: () -> Unit) {
    val vm = viewModelDoApp { app, _ -> FinanceiroViewModel(app.financeiro) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    FinanceiroConteudo(
        estado = estado,
        aoEscolherTipo = vm::escolherTipo,
        aoAnterior = vm::anterior,
        aoProximo = vm::proximo,
        aoMarcarPaga = vm::marcarPaga,
        aoExcluirDespesa = vm::excluirDespesa,
        aoNovaDespesa = aoNovaDespesa,
    )
}

@Composable
fun FinanceiroConteudo(
    estado: FinanceiroEstado,
    aoEscolherTipo: (TipoPeriodo) -> Unit,
    aoAnterior: () -> Unit,
    aoProximo: () -> Unit,
    aoMarcarPaga: (parcelaId: Long, paga: Boolean) -> Unit,
    aoExcluirDespesa: (despesaId: Long) -> Unit,
    aoNovaDespesa: () -> Unit,
) {
    var escolhida by remember { mutableStateOf<ParcelaDetalhada?>(null) }
    var confirmarExclusao by remember { mutableStateOf<ParcelaDetalhada?>(null) }

    Tela(
        titulo = "Financeiro",
        botaoFlutuante = {
            ExtendedFloatingActionButton(
                onClick = aoNovaDespesa,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nova despesa") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Tinta,
            )
        },
    ) { espacamento ->
        Formulario(espacamento) {
            EscolhaUnica(
                opcoes = TipoPeriodo.entries,
                selecionada = estado.periodo.tipo,
                aoEscolher = aoEscolherTipo,
                rotuloDe = { it.rotulo },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = aoAnterior) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Período anterior")
                }
                Text(
                    estado.periodo.rotulo.replaceFirstChar(Char::uppercase),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = aoProximo) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próximo período")
                }
            }

            CartaoBalanco(estado.balanco)

            if (estado.aPagar.isNotEmpty()) {
                TituloSecao("A pagar (próximos 30 dias)")
                estado.aPagar.forEach { LinhaParcela(it) { escolhida = it } }
            }

            if (estado.porCategoria.isNotEmpty()) {
                TituloSecao("Despesa por categoria")
                estado.porCategoria.forEach { total ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(total.categoriaNome)
                        Text(Dinheiro.formatar(total.totalCentavos), fontWeight = FontWeight.Medium)
                    }
                }
            }

            TituloSecao("Despesas do período")
            if (estado.parcelasDoPeriodo.isEmpty()) {
                Text("Nenhuma despesa vence neste período.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            estado.parcelasDoPeriodo.forEach { LinhaParcela(it) { escolhida = it } }
            Spacer(Modifier.height(72.dp))
        }
    }

    escolhida?.let { item ->
        val parcela = item.parcela
        val paga = parcela.status == StatusParcela.PAGA
        AlertDialog(
            onDismissRequest = { escolhida = null },
            title = { Text(item.descricao) },
            text = {
                Text(
                    buildString {
                        append("${item.categoriaNome}\n")
                        if (item.totalParcelas > 1) append("Parcela ${parcela.numero} de ${item.totalParcelas}\n")
                        append("${Dinheiro.formatar(parcela.valorCentavos)} · vence ${Datas.formatarData(parcela.vencimento)}\n")
                        append(
                            if (paga) "Paga em ${parcela.dataPagamento?.let(Datas::formatarData) ?: "-"}"
                            else "Pendente — avisos na véspera e no dia, às 9h",
                        )
                    },
                )
            },
            confirmButton = {
                // Compra à vista já nasce paga e não tem o que desfazer.
                if (item.formaPagamento == FormaPagamentoDespesa.BOLETO_PARCELADO) {
                    TextButton(onClick = {
                        aoMarcarPaga(parcela.id, !paga)
                        escolhida = null
                    }) { Text(if (paga) "Marcar como não paga" else "Marcar como paga") }
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        confirmarExclusao = item
                        escolhida = null
                    }) { Text("Excluir despesa", color = Erro) }
                    TextButton(onClick = { escolhida = null }) { Text("Fechar") }
                }
            },
        )
    }

    confirmarExclusao?.let { item ->
        DialogoConfirmacao(
            titulo = "Excluir \"${item.descricao}\"?",
            mensagem = if (item.totalParcelas > 1) "As ${item.totalParcelas} parcelas e os avisos serão apagados."
            else "A despesa será apagada do balanço.",
            textoConfirmar = "Excluir",
            aoConfirmar = {
                aoExcluirDespesa(item.parcela.despesaId)
                confirmarExclusao = null
            },
            aoCancelar = { confirmarExclusao = null },
        )
    }
}

@Composable
private fun CartaoBalanco(balanco: Balanco) {
    Cartao(destaque = true) {
        LinhaValor("Receita (${balanco.quantidadeAtendimentos} atendimentos)", balanco.receitaCentavos)
        LinhaValor("Despesa", balanco.despesaCentavos)
        HorizontalDivider(color = Tinta.copy(alpha = 0.2f))
        LinhaValor("Saldo", balanco.saldoCentavos, destaque = true, cor = if (balanco.saldoCentavos < 0) Erro else Tinta)
    }
}

@Composable
private fun LinhaValor(rotulo: String, centavos: Long, destaque: Boolean = false, cor: Color = Tinta) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(rotulo, style = if (destaque) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge)
        Text(
            Dinheiro.formatar(centavos),
            style = if (destaque) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = if (destaque) FontWeight.Bold else FontWeight.Medium,
            color = cor,
        )
    }
}

@Composable
private fun LinhaParcela(item: ParcelaDetalhada, aoTocar: () -> Unit) {
    val parcela = item.parcela
    val vencimento = Datas.paraLocalDate(parcela.vencimento)
    val pendente = parcela.status == StatusParcela.PENDENTE
    val atrasada = pendente && vencimento.isBefore(LocalDate.now())
    ListItem(
        headlineContent = {
            Text(item.descricao + if (item.totalParcelas > 1) " (${parcela.numero}/${item.totalParcelas})" else "")
        },
        supportingContent = {
            Text(
                "${Datas.formatarData(vencimento)} · ${item.categoriaNome} · " +
                    when {
                        atrasada -> "ATRASADA"
                        pendente -> "pendente"
                        else -> "paga"
                    },
                color = if (atrasada) Erro else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = { Text(Dinheiro.formatar(parcela.valorCentavos), fontWeight = FontWeight.Medium) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable(onClick = aoTocar),
    )
    HorizontalDivider()
}

@Preview(showBackground = true, heightDp = 1000, name = "Balanço do mês")
@Composable
private fun PreviewFinanceiro() = PreviewNoTema {
    FinanceiroConteudo(
        estado = FinanceiroEstado(
            balanco = Balanco(receitaCentavos = 412_500, despesaCentavos = 165_990, quantidadeAtendimentos = 58),
            porCategoria = listOf(
                TotalPorCategoria("Aluguel", 120_000),
                TotalPorCategoria("Equipamentos", 30_000),
                TotalPorCategoria("Produtos de banho", 15_990),
            ),
            parcelasDoPeriodo = DadosDePreview.parcelas,
            aPagar = DadosDePreview.parcelas.filter { it.parcela.status == StatusParcela.PENDENTE },
        ),
        aoEscolherTipo = {}, aoAnterior = {}, aoProximo = {}, aoMarcarPaga = { _, _ -> },
        aoExcluirDespesa = {}, aoNovaDespesa = {},
    )
}

@Preview(showBackground = true, name = "Saldo negativo")
@Composable
private fun PreviewSaldoNegativo() = PreviewNoTema {
    FinanceiroConteudo(
        FinanceiroEstado(balanco = Balanco(receitaCentavos = 50_000, despesaCentavos = 120_000, quantidadeAtendimentos = 7)),
        {}, {}, {}, { _, _ -> }, {}, {},
    )
}
