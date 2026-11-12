package br.com.fashiondog.domain

import java.text.Normalizer

/**
 * Busca usada nas listas. Ignora maiúsculas e acentos ("bóris" acha "Boris")
 * e, quando o termo é só número, compara com os dígitos do telefone.
 */
object Busca {

    private val acentos = Regex("\\p{Mn}+")

    fun normalizar(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(acentos, "")
            .lowercase()
            .trim()

    /** true se [termo] estiver vazio ou aparecer em algum dos [campos]. */
    fun corresponde(termo: String, vararg campos: String?): Boolean {
        val termoNormalizado = normalizar(termo)
        if (termoNormalizado.isEmpty()) return true

        val termoEhNumero = termo.none(Char::isLetter)
        val digitosDoTermo = Telefone.somenteDigitos(termo)

        return campos.filterNotNull().any { campo ->
            normalizar(campo).contains(termoNormalizado) ||
                (termoEhNumero && digitosDoTermo.isNotEmpty() &&
                    Telefone.somenteDigitos(campo).contains(digitosDoTermo))
        }
    }
}
