package br.com.fashiondog.ui.mais

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fashiondog.backup.BackupDoApp
import br.com.fashiondog.domain.ValidadorDeBackup
import br.com.fashiondog.ui.componentes.BotaoPrincipal
import br.com.fashiondog.ui.componentes.Cartao
import br.com.fashiondog.ui.componentes.DialogoConfirmacao
import br.com.fashiondog.ui.componentes.Formulario
import br.com.fashiondog.ui.componentes.Tela
import br.com.fashiondog.ui.componentes.viewModelDoApp
import br.com.fashiondog.ui.preview.PreviewNoTema
import br.com.fashiondog.ui.theme.Tinta
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BackupEstado(
    val processando: String? = null,
    val mensagem: String? = null,
    val erro: Boolean = false,
    /** Restauração terminou: a tela pede para reiniciar o app. */
    val restaurado: Boolean = false,
)

class BackupViewModel(private val backup: BackupDoApp) : ViewModel() {

    private val _estado = MutableStateFlow(BackupEstado())
    val estado: StateFlow<BackupEstado> = _estado.asStateFlow()

    fun exportar(destino: Uri) {
        _estado.value = BackupEstado(processando = "Gerando backup...")
        viewModelScope.launch {
            _estado.value = backup.exportar(destino).fold(
                { BackupEstado(mensagem = "Backup salvo: ${it.fotos} fotos, ${tamanho(it.bytes)}.") },
                { BackupEstado(mensagem = "Não foi possível salvar: ${it.message}", erro = true) },
            )
        }
    }

    fun restaurar(origem: Uri) {
        _estado.value = BackupEstado(processando = "Restaurando...")
        viewModelScope.launch {
            _estado.value = backup.restaurar(origem).fold(
                { BackupEstado(restaurado = true, mensagem = "Backup restaurado. O app vai reiniciar.") },
                { BackupEstado(mensagem = it.message, erro = true) },
            )
        }
    }

    private fun tamanho(bytes: Long): String = when {
        bytes < 0 -> "tamanho desconhecido"
        bytes < 1_048_576 -> "${bytes / 1024} KB"
        else -> String.format(Locale.forLanguageTag("pt-BR"), "%.1f MB", bytes / 1_048_576.0)
    }
}

@Composable
fun BackupScreen(aoVoltar: () -> Unit) {
    val vm = viewModelDoApp { app, _ -> BackupViewModel(app.backup) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current

    // Seletores de arquivo do Android: a pessoa escolhe onde salvar / qual arquivo abrir.
    val criarArquivo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(vm::exportar)
    }
    val abrirArquivo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::restaurar)
    }

    BackupConteudo(
        estado = estado,
        aoVoltar = aoVoltar,
        aoExportar = { criarArquivo.launch(ValidadorDeBackup.nomeDoArquivo(LocalDate.now())) },
        aoRestaurar = { abrirArquivo.launch(arrayOf("application/zip", "application/octet-stream")) },
        aoReiniciar = { BackupDoApp.reiniciarApp(contexto) },
    )
}

@Composable
fun BackupConteudo(
    estado: BackupEstado,
    aoVoltar: () -> Unit,
    aoExportar: () -> Unit,
    aoRestaurar: () -> Unit,
    aoReiniciar: () -> Unit,
) {
    var confirmarRestauracao by remember { mutableStateOf(false) }
    val ocupado = estado.processando != null

    Tela(titulo = "Backup", aoVoltar = aoVoltar) { espacamento ->
        Formulario(espacamento) {
            Cartao {
                Text("Fazer backup", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Gera um arquivo .zip com todos os dados e fotos. Salve no Google Drive ou envie " +
                        "para outro lugar: se o celular quebrar ou for perdido, é com ele que você recupera tudo.",
                )
                BotaoPrincipal("Salvar backup", aoExportar, habilitado = !ocupado)
            }
            Cartao {
                Text("Restaurar backup", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Substitui TODOS os dados deste celular pelos do arquivo escolhido. " +
                        "Depois é preciso entrar de novo com login e senha.",
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(onClick = { confirmarRestauracao = true }, enabled = !ocupado) { Text("Escolher arquivo...") }
            }
            if (estado.processando != null) {
                CircularProgressIndicator(color = Tinta)
                Text(estado.processando)
            }
            estado.mensagem?.let {
                Text(it, color = if (estado.erro) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            }
            if (estado.restaurado) {
                BotaoPrincipal("Reiniciar o app agora", aoReiniciar)
            }
        }
    }

    if (confirmarRestauracao) {
        DialogoConfirmacao(
            titulo = "Restaurar backup?",
            mensagem = "Tudo o que está neste celular será trocado pelo conteúdo do arquivo. Essa ação não pode ser desfeita. " +
                "Se tiver dúvida, faça um backup antes.",
            textoConfirmar = "Restaurar",
            aoConfirmar = {
                confirmarRestauracao = false
                aoRestaurar()
            },
            aoCancelar = { confirmarRestauracao = false },
        )
    }
}

@Preview(showBackground = true, name = "Backup")
@Composable
private fun PreviewBackup() = PreviewNoTema {
    BackupConteudo(BackupEstado(mensagem = "Backup salvo: 128 fotos, 24,3 MB."), {}, {}, {}, {})
}

@Preview(showBackground = true, name = "Backup restaurado")
@Composable
private fun PreviewBackupRestaurado() = PreviewNoTema {
    BackupConteudo(BackupEstado(restaurado = true, mensagem = "Backup restaurado. O app vai reiniciar."), {}, {}, {}, {})
}
