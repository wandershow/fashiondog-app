package br.com.fashiondog.backup

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import br.com.fashiondog.data.fotos.ArmazenamentoDeFotos
import br.com.fashiondog.data.local.AppDatabase
import br.com.fashiondog.di.Sessao
import br.com.fashiondog.domain.ValidadorDeBackup
import java.io.File
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ResumoDoBackup(val fotos: Int, val bytes: Long)

/**
 * Backup = um .zip com o banco (`fashiondog.db`) e a pasta `fotos/`.
 * O arquivo é salvo onde a pessoa escolher (Downloads, Google Drive, pendrive...)
 * pelo seletor de arquivos do Android, sem precisar de permissão de armazenamento.
 */
class BackupDoApp(
    private val contexto: Context,
    private val sessao: Sessao,
) {
    private val arquivoBanco get() = contexto.getDatabasePath(AppDatabase.NOME)
    private val pastaFotos get() = File(contexto.filesDir, ArmazenamentoDeFotos.PASTA)

    suspend fun exportar(destino: Uri): Result<ResumoDoBackup> = withContext(Dispatchers.IO) {
        runCatching {
            // O Room usa "WAL": mudanças recentes ficam num arquivo à parte (-wal).
            // O checkpoint passa tudo para o arquivo principal antes de copiá-lo.
            AppDatabase.obter(contexto).openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }

            var fotos = 0
            val saida = contexto.contentResolver.openOutputStream(destino) ?: error("Não foi possível criar o arquivo")
            ZipOutputStream(saida.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(ValidadorDeBackup.NOME_INFO))
                zip.write("Backup do FashionDog\nversaoDoBanco=${AppDatabase.VERSAO}\ncriadoEm=${LocalDateTime.now()}\n".toByteArray())
                zip.closeEntry()

                zip.putNextEntry(ZipEntry(ValidadorDeBackup.NOME_BANCO))
                arquivoBanco.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()

                pastaFotos.listFiles { f -> f.isFile && f.name.endsWith(".jpg") }?.forEach { foto ->
                    zip.putNextEntry(ZipEntry(ValidadorDeBackup.PASTA_FOTOS + foto.name))
                    foto.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    fotos++
                }
            }
            val tamanho = contexto.contentResolver.openAssetFileDescriptor(destino, "r")?.use { it.length } ?: -1
            ResumoDoBackup(fotos, tamanho)
        }
    }

    /**
     * Restaura em 3 passos, para nunca deixar o app pela metade:
     * 1. extrai o zip numa pasta temporária, recusando qualquer item estranho;
     * 2. confere se o banco abre e se a versão é compatível;
     * 3. só então fecha o banco atual e troca os arquivos.
     * Depois disso o app precisa ser reiniciado ([reiniciarApp]).
     */
    suspend fun restaurar(origem: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        val temporaria = File(contexto.cacheDir, "restauracao").apply {
            deleteRecursively()
            mkdirs()
        }
        try {
            // 1) extrair conferindo cada nome
            val nomes = mutableListOf<String>()
            val entrada = contexto.contentResolver.openInputStream(origem)
                ?: return@withContext falha("Não foi possível abrir o arquivo")
            ZipInputStream(entrada.buffered()).use { zip ->
                while (true) {
                    val item = zip.nextEntry ?: break
                    if (!ValidadorDeBackup.entradaPermitida(item.name)) {
                        return@withContext falha("O arquivo tem itens que não são de um backup do FashionDog")
                    }
                    nomes += item.name
                    if (!item.isDirectory) {
                        val destino = File(temporaria, item.name)
                        destino.parentFile?.mkdirs()
                        destino.outputStream().use { zip.copyTo(it) }
                    }
                }
            }
            ValidadorDeBackup.erroNoConteudo(nomes)?.let { return@withContext falha(it) }

            // 2) o banco abre? de que versão é?
            val bancoNovo = File(temporaria, ValidadorDeBackup.NOME_BANCO)
            val versao = runCatching {
                SQLiteDatabase.openDatabase(bancoNovo.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version }
            }.getOrElse { return@withContext falha("O banco do backup está corrompido") }
            ValidadorDeBackup.erroDeVersao(versao, AppDatabase.VERSAO)?.let { return@withContext falha(it) }

            // 3) trocar os arquivos. Os ids de usuário podem ser outros: pede login de novo.
            sessao.esquecerLogin()
            AppDatabase.fechar()
            val banco = arquivoBanco
            listOf(banco, File(banco.path + "-wal"), File(banco.path + "-shm")).forEach { it.delete() }
            bancoNovo.copyTo(banco, overwrite = true)

            pastaFotos.deleteRecursively()
            File(temporaria, ArmazenamentoDeFotos.PASTA).takeIf { it.exists() }?.copyRecursively(pastaFotos)
            pastaFotos.mkdirs()
            Result.success(Unit)
        } finally {
            temporaria.deleteRecursively()
        }
    }

    private fun falha(mensagem: String): Result<Unit> = Result.failure(IllegalStateException(mensagem))

    companion object {
        /**
         * Fecha o processo e abre o app de novo, do zero, já com o banco restaurado.
         * (Ao abrir, o app também recria todos os avisos a partir do banco novo.)
         */
        fun reiniciarApp(contexto: Context) {
            val abrir = contexto.packageManager.getLaunchIntentForPackage(contexto.packageName) ?: return
            contexto.startActivity(Intent.makeRestartActivityTask(abrir.component))
            Runtime.getRuntime().exit(0)
        }
    }
}
