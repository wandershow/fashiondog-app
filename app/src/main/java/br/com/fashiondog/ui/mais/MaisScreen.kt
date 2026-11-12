package br.com.fashiondog.ui.mais

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.fashiondog.ui.componentes.Tela
import br.com.fashiondog.ui.preview.PreviewNoTema

/** Um item da aba Mais. [visivel] permite esconder opções conforme o perfil (Sprint 5). */
data class OpcaoMais(
    val titulo: String,
    val descricao: String,
    val visivel: Boolean = true,
    val aoTocar: () -> Unit,
)

@Composable
fun MaisScreen(opcoes: List<OpcaoMais>, cabecalho: String? = null) {
    Tela(titulo = "Mais") { espacamento ->
        Column(modifier = Modifier.fillMaxSize().padding(espacamento)) {
            cabecalho?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            opcoes.filter { it.visivel }.forEach { opcao ->
                ListItem(
                    headlineContent = { Text(opcao.titulo) },
                    supportingContent = { Text(opcao.descricao) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                    modifier = Modifier.clickable(onClick = opcao.aoTocar),
                )
                HorizontalDivider()
            }
        }
    }
}

@Preview(showBackground = true, name = "Aba Mais")
@Composable
private fun PreviewMais() = PreviewNoTema {
    MaisScreen(
        cabecalho = "Maria Souza · Proprietário",
        opcoes = listOf(
            OpcaoMais("Serviços e preços", "Preço sugerido de cada serviço") {},
            OpcaoMais("Avisos", "Notificações e horário exato dos lembretes") {},
        ),
    )
}
