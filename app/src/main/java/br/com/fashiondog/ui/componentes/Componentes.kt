package br.com.fashiondog.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.fashiondog.ui.theme.Amarelo
import br.com.fashiondog.ui.theme.Branco
import br.com.fashiondog.ui.theme.Tinta

/** Estrutura padrão das telas: barra de título (com voltar, se houver) + conteúdo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Tela(
    titulo: String,
    aoVoltar: (() -> Unit)? = null,
    acoes: @Composable RowScope.() -> Unit = {},
    botaoFlutuante: @Composable () -> Unit = {},
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    conteudo: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (aoVoltar != null) {
                        IconButton(onClick = aoVoltar) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                },
                actions = acoes,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = botaoFlutuante,
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        content = conteudo,
    )
}

/** Coluna com rolagem para formulários. */
@Composable
fun Formulario(
    espacamento: PaddingValues,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(espacamento)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = conteudo,
    )
}

@Composable
fun CampoTexto(
    valor: String,
    aoMudar: (String) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    erro: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    maiusculas: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    linhas: Int = 1,
    habilitado: Boolean = true,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        isError = erro != null,
        supportingText = erro?.let { { Text(it) } },
        singleLine = linhas == 1,
        minLines = linhas,
        enabled = habilitado,
        keyboardOptions = KeyboardOptions(keyboardType = teclado, capitalization = maiusculas),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Escolha de uma opção entre poucas (ex.: porte, serviço, forma de pagamento). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> EscolhaUnica(
    opcoes: List<T>,
    selecionada: T?,
    aoEscolher: (T) -> Unit,
    rotuloDe: (T) -> String,
    titulo: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (titulo != null) {
            Text(titulo, style = MaterialTheme.typography.labelLarge)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            opcoes.forEach { opcao ->
                FilterChip(
                    selected = opcao == selecionada,
                    onClick = { aoEscolher(opcao) },
                    label = { Text(rotuloDe(opcao)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Amarelo,
                        selectedLabelColor = Tinta,
                    ),
                )
            }
        }
    }
}

/** Botão principal: tinta escura, como no protótipo. */
@Composable
fun BotaoPrincipal(
    texto: String,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
) {
    Button(
        onClick = aoClicar,
        enabled = habilitado,
        // Texto branco explícito: o padrão do tema (onPrimary) é tinta, e ficaria preto no preto.
        colors = ButtonDefaults.buttonColors(containerColor = Tinta, contentColor = Branco),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(texto, modifier = Modifier.padding(vertical = 4.dp))
    }
}

@Composable
fun Cartao(
    modifier: Modifier = Modifier,
    destaque: Boolean = false,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (destaque) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        ),
        border = if (destaque) null else CardDefaults.outlinedCardBorder(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = conteudo,
        )
    }
}

@Composable
fun TituloSecao(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 8.dp),
    )
}

@Composable
fun EstadoVazio(mensagem: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            mensagem,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun Carregando(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Tinta)
    }
}

/** Pergunta antes de ações que não dá para desfazer pela tela (ex.: desativar). */
@Composable
fun DialogoConfirmacao(
    titulo: String,
    mensagem: String,
    textoConfirmar: String,
    aoConfirmar: () -> Unit,
    aoCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text(titulo) },
        text = { Text(mensagem) },
        confirmButton = { TextButton(onClick = aoConfirmar) { Text(textoConfirmar) } },
        dismissButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } },
    )
}
