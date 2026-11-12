package br.com.fashiondog.ui.atendimento

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.fashiondog.data.local.entity.TipoFoto
import br.com.fashiondog.ui.theme.Tinta
import coil.compose.AsyncImage
import java.io.File

val TipoFoto.rotulo: String
    get() = when (this) {
        TipoFoto.ANTES -> "Antes"
        TipoFoto.DEPOIS -> "Depois"
    }

private val formato = RoundedCornerShape(12.dp)

/** Mostra uma foto guardada na pasta interna (o banco guarda o caminho relativo). */
@Composable
fun FotoMiniatura(caminho: String, descricao: String, modifier: Modifier = Modifier, aoTocar: (() -> Unit)? = null) {
    val arquivo = File(LocalContext.current.filesDir, caminho)
    AsyncImage(
        model = arquivo,
        contentDescription = descricao,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(formato)
            .then(if (aoTocar != null) Modifier.clickable(onClick = aoTocar) else Modifier),
    )
}

/** Espaço da foto no formulário: vazio (com botões) ou com a foto e um "x". */
@Composable
fun FotoComAcoes(
    titulo: String,
    caminho: String?,
    processando: Boolean,
    aoCamera: () -> Unit,
    aoGaleria: () -> Unit,
    aoRemover: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(titulo, style = MaterialTheme.typography.labelLarge)
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f)) {
            when {
                processando -> Box(
                    modifier = Modifier.fillMaxSize().clip(formato).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = Tinta) }

                caminho != null -> {
                    FotoMiniatura(caminho, "Foto $titulo")
                    IconButton(
                        onClick = aoRemover,
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Remover foto",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(formato)
                        .border(1.dp, MaterialTheme.colorScheme.outline, formato)
                        .background(MaterialTheme.colorScheme.surface),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Sem foto", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row {
                        TextButton(onClick = aoCamera) { Text("Câmera") }
                    }
                    TextButton(onClick = aoGaleria) { Text("Galeria") }
                }
            }
        }
    }
}
