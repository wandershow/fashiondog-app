package br.com.fashiondog.data.fotos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import br.com.fashiondog.domain.RedimensionamentoDeFoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Guarda as fotos dos atendimentos na pasta interna do app (`files/fotos`),
 * que outros apps não acessam. O banco grava só o caminho **relativo**
 * (ex.: `fotos/abc.jpg`), assim o backup restaurado em outro celular continua valendo.
 */
class ArmazenamentoDeFotos(private val contexto: Context) {

    private val pastaFotos get() = File(contexto.filesDir, PASTA).apply { mkdirs() }
    private val pastaCapturas get() = File(contexto.cacheDir, "camera").apply { mkdirs() }

    /** Arquivo de um caminho guardado no banco. */
    fun arquivo(caminho: String): File = File(contexto.filesDir, caminho)

    /**
     * Endereço temporário onde o app de câmera do sistema grava a foto original.
     * O [FileProvider] dá permissão só para esse arquivo.
     */
    fun novoDestinoParaCamera(): Uri {
        val arquivo = File(pastaCapturas, "captura_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(contexto, "${contexto.packageName}.arquivos", arquivo)
    }

    /**
     * Lê a foto (da câmera ou da galeria), reduz para no máximo 1280 px, corrige a
     * rotação e salva em JPEG com qualidade 80. Devolve o caminho relativo.
     */
    suspend fun importar(origem: Uri): String = withContext(Dispatchers.IO) {
        val resolver = contexto.contentResolver

        // 1) Só lê o tamanho, sem carregar a imagem (economiza memória).
        val tamanho = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(origem)?.use { BitmapFactory.decodeStream(it, null, tamanho) }
            ?: error("Não foi possível abrir a foto")

        // 2) Abre já reduzida por um fator potência de 2.
        val opcoes = BitmapFactory.Options().apply {
            inSampleSize = RedimensionamentoDeFoto.fatorDeAmostragem(tamanho.outWidth, tamanho.outHeight)
        }
        val original = resolver.openInputStream(origem)?.use { BitmapFactory.decodeStream(it, null, opcoes) }
            ?: error("Formato de foto não suportado")

        // 3) Ajuste fino do tamanho e da rotação (a câmera guarda a rotação no EXIF).
        val (largura, altura) = RedimensionamentoDeFoto.dimensoesFinais(original.width, original.height)
        val rotacao = resolver.openInputStream(origem)?.use { ExifInterface(it).rotationDegrees } ?: 0
        val matriz = Matrix().apply {
            postScale(largura.toFloat() / original.width, altura.toFloat() / original.height)
            if (rotacao != 0) postRotate(rotacao.toFloat())
        }
        val final = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matriz, true)

        // 4) Grava comprimida.
        val nome = "${UUID.randomUUID()}.jpg"
        FileOutputStream(File(pastaFotos, nome)).use { final.compress(Bitmap.CompressFormat.JPEG, QUALIDADE, it) }
        if (final !== original) original.recycle()
        final.recycle()
        limparCapturas()
        "$PASTA/$nome"
    }

    fun excluir(caminho: String) {
        arquivo(caminho).delete()
    }

    /** Apaga as fotos originais da câmera (só servem até serem comprimidas). */
    private fun limparCapturas() {
        pastaCapturas.listFiles()?.forEach { it.delete() }
    }

    companion object {
        const val PASTA = "fotos"
        private const val QUALIDADE = 80
    }
}
