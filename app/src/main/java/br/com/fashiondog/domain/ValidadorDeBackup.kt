package br.com.fashiondog.domain

/**
 * Regras do arquivo de backup (.zip). Restaurar um zip é perigoso se ele vier
 * adulterado: um nome como `../../shared_prefs/x.xml` faria o app gravar fora da
 * pasta certa (ataque conhecido como "zip slip"). Só aceitamos o que o próprio app exporta.
 */
object ValidadorDeBackup {

    const val NOME_BANCO = "fashiondog.db"
    const val NOME_INFO = "backup-info.txt"
    const val PASTA_FOTOS = "fotos/"

    private val fotoValida = Regex("^fotos/[A-Za-z0-9-]{1,64}\\.jpg$")

    /** Nome sugerido do arquivo: "fashiondog-backup-2026-11-12.zip". */
    fun nomeDoArquivo(data: java.time.LocalDate) = "fashiondog-backup-$data.zip"

    fun entradaPermitida(nome: String): Boolean =
        nome == NOME_BANCO || nome == NOME_INFO || nome == PASTA_FOTOS || fotoValida.matches(nome)

    /** Um backup só é válido se tiver o banco e nenhuma entrada estranha. */
    fun erroNoConteudo(nomes: List<String>): String? = when {
        nomes.any { !entradaPermitida(it) } -> "O arquivo tem itens que não são de um backup do FashionDog"
        NOME_BANCO !in nomes -> "O arquivo não tem o banco de dados do FashionDog"
        else -> null
    }

    /** Backup feito por uma versão mais nova do app não pode ser aberto por esta. */
    fun erroDeVersao(versaoDoBackup: Int, versaoDoApp: Int): String? =
        if (versaoDoBackup > versaoDoApp) "Este backup é de uma versão mais nova do app. Atualize o app antes de restaurar." else null
}
