package br.com.fashiondog.ui.atendimento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fashiondog.data.local.entity.Foto
import br.com.fashiondog.data.local.model.AtendimentoDetalhado
import br.com.fashiondog.data.repository.AtendimentoRepository
import br.com.fashiondog.domain.Datas
import br.com.fashiondog.domain.Dinheiro
import br.com.fashiondog.ui.componentes.Carregando
import br.com.fashiondog.ui.componentes.Cartao
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

data class AtendimentoDetalheEstado(
    val carregando: Boolean = true,
    val detalhe: AtendimentoDetalhado? = null,
    val fotos: List<Foto> = emptyList(),
)

class AtendimentoDetalheViewModel(repositorio: AtendimentoRepository, argumentos: SavedStateHandle) : ViewModel() {

    val atendimentoId: Long = checkNotNull(argumentos.get<Long>("atendimentoId"))

    val estado: StateFlow<AtendimentoDetalheEstado> = combine(
        repositorio.observarDetalhado(atendimentoId),
        repositorio.observarFotos(atendimentoId),
    ) { detalhe, fotos -> AtendimentoDetalheEstado(false, detalhe, fotos) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AtendimentoDetalheEstado())
}

@Composable
fun AtendimentoDetalheScreen(
    aoVoltar: () -> Unit,
    aoEditar: (Long) -> Unit,
    /** Recebe a data do atendimento: funcionário só edita os de hoje. */
    podeEditar: (dataAtendimento: Long) -> Boolean = { true },
) {
    val vm = viewModelDoApp { app, args -> AtendimentoDetalheViewModel(app.atendimentos, args) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    AtendimentoDetalheConteudo(estado, aoVoltar, aoEditar, podeEditar)
}

@Composable
fun AtendimentoDetalheConteudo(
    estado: AtendimentoDetalheEstado,
    aoVoltar: () -> Unit,
    aoEditar: (Long) -> Unit,
    /** Recebe a data do atendimento: funcionário só edita os de hoje. */
    podeEditar: (dataAtendimento: Long) -> Boolean = { true },
) {
    var fotoAmpliada by remember { mutableStateOf<Foto?>(null) }

    Tela(
        titulo = estado.detalhe?.petNome ?: "Atendimento",
        aoVoltar = aoVoltar,
        acoes = {
            val detalhe = estado.detalhe
            if (detalhe != null && podeEditar(detalhe.atendimento.data)) {
                IconButton(onClick = { aoEditar(detalhe.atendimento.id) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar atendimento")
                }
            }
        },
    ) { espacamento ->
        val detalhe = estado.detalhe
        if (estado.carregando || detalhe == null) {
            Carregando()
            return@Tela
        }
        val atendimento = detalhe.atendimento
        Formulario(espacamento) {
            Cartao(destaque = true) {
                Text(detalhe.servicoNome, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(Datas.formatarDataHora(atendimento.data))
                Text(
                    "${Dinheiro.formatar(atendimento.valorCentavos)} · ${atendimento.formaPagamento.rotulo}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            atendimento.observacoes?.let {
                TituloSecao("Observações")
                Text(it)
            }
            if (estado.fotos.isNotEmpty()) {
                TituloSecao("Fotos")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    estado.fotos.forEach { foto ->
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(foto.tipo.rotulo, style = MaterialTheme.typography.labelLarge)
                            FotoMiniatura(foto.caminho, "Foto ${foto.tipo.rotulo}") { fotoAmpliada = foto }
                        }
                    }
                }
            }
        }
    }

    fotoAmpliada?.let { foto ->
        // Toque na miniatura amplia; toque fora fecha.
        Dialog(onDismissRequest = { fotoAmpliada = null }) {
            FotoMiniatura(foto.caminho, "Foto ${foto.tipo.rotulo} ampliada") { fotoAmpliada = null }
        }
    }
}

@Preview(showBackground = true, name = "Detalhe do atendimento")
@Composable
private fun PreviewAtendimentoDetalhe() = PreviewNoTema {
    val linha = DadosDePreview.historico.first()
    AtendimentoDetalheConteudo(
        AtendimentoDetalheEstado(
            carregando = false,
            detalhe = AtendimentoDetalhado(linha.atendimento.copy(observacoes = "Unhas cortadas"), linha.servicoNome, "Bóris"),
        ),
        aoVoltar = {},
        aoEditar = {},
    )
}
