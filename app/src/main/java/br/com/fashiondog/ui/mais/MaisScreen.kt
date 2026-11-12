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
import br.com.fashiondog.ui.componentes.Tela

/** Um item da aba Mais. [visivel] permite esconder opções conforme o perfil (Sprint 5). */
data class OpcaoMais(
    val titulo: String,
    val descricao: String,
    val visivel: Boolean = true,
    val aoTocar: () -> Unit,
)

@Composable
fun MaisScreen(opcoes: List<OpcaoMais>) {
    Tela(titulo = "Mais") { espacamento ->
        Column(modifier = Modifier.fillMaxSize().padding(espacamento)) {
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
