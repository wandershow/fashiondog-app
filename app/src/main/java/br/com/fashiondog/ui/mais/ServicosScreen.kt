package br.com.fashiondog.ui.mais

import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fashiondog.data.local.entity.Servico
import br.com.fashiondog.data.repository.AtendimentoRepository
import br.com.fashiondog.domain.Dinheiro
import br.com.fashiondog.ui.componentes.CampoDinheiro
import br.com.fashiondog.ui.componentes.Formulario
import br.com.fashiondog.ui.componentes.Tela
import br.com.fashiondog.ui.componentes.viewModelDoApp
import br.com.fashiondog.ui.preview.DadosDePreview
import br.com.fashiondog.ui.preview.PreviewNoTema
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ServicosViewModel(private val repositorio: AtendimentoRepository) : ViewModel() {

    val servicos: StateFlow<List<Servico>> = repositorio.observarServicos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Muda só o preço SUGERIDO. Os atendimentos já registrados guardam o valor
     * cobrado no dia, então o histórico e o balanço não mudam.
     */
    fun mudarPreco(servico: Servico, centavos: Long) {
        viewModelScope.launch { repositorio.salvarServico(servico.copy(precoSugeridoCentavos = centavos)) }
    }
}

@Composable
fun ServicosScreen(aoVoltar: () -> Unit) {
    val vm = viewModelDoApp { app, _ -> ServicosViewModel(app.atendimentos) }
    val servicos by vm.servicos.collectAsStateWithLifecycle()
    ServicosConteudo(servicos, aoVoltar, vm::mudarPreco)
}

@Composable
fun ServicosConteudo(
    servicos: List<Servico>,
    aoVoltar: () -> Unit,
    aoMudarPreco: (Servico, Long) -> Unit,
) {
    var editando by remember { mutableStateOf<Servico?>(null) }

    Tela(titulo = "Serviços e preços", aoVoltar = aoVoltar) { espacamento ->
        Formulario(espacamento) {
            Text(
                "Toque num serviço para mudar o preço sugerido. Atendimentos já registrados não mudam.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            servicos.forEach { servico ->
                ListItem(
                    headlineContent = { Text(servico.nome) },
                    trailingContent = { Text(Dinheiro.formatar(servico.precoSugeridoCentavos)) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                    modifier = Modifier.clickable { editando = servico },
                )
                HorizontalDivider()
            }
        }
    }

    editando?.let { servico ->
        var centavos by remember(servico.id) { mutableLongStateOf(servico.precoSugeridoCentavos) }
        AlertDialog(
            onDismissRequest = { editando = null },
            title = { Text(servico.nome) },
            text = { CampoDinheiro(centavos = centavos, aoMudar = { centavos = it }, rotulo = "Preço sugerido") },
            confirmButton = {
                TextButton(
                    onClick = {
                        aoMudarPreco(servico, centavos)
                        editando = null
                    },
                    enabled = centavos > 0,
                ) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { editando = null }) { Text("Cancelar") } },
        )
    }
}

@Preview(showBackground = true, name = "Serviços e preços")
@Composable
private fun PreviewServicos() = PreviewNoTema {
    ServicosConteudo(DadosDePreview.servicos, aoVoltar = {}, aoMudarPreco = { _, _ -> })
}
