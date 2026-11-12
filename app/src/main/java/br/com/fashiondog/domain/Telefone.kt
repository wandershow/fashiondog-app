package br.com.fashiondog.domain

/**
 * Telefone é gravado só com os dígitos (ex.: "53999998888") e formatado na tela.
 * Assim a busca e o link do WhatsApp funcionam independente de como foi digitado.
 */
object Telefone {

    fun somenteDigitos(texto: String): String = texto.filter(Char::isDigit)

    /** Válido = DDD + número: 10 dígitos (fixo) ou 11 (celular). */
    fun ehValido(texto: String): Boolean = somenteDigitos(texto).length in 10..11

    /** "53999998888" → "(53) 99999-8888"; "5332221111" → "(53) 3222-1111". */
    fun formatar(texto: String): String {
        val d = somenteDigitos(texto)
        return when (d.length) {
            11 -> "(${d.substring(0, 2)}) ${d.substring(2, 7)}-${d.substring(7)}"
            10 -> "(${d.substring(0, 2)}) ${d.substring(2, 6)}-${d.substring(6)}"
            else -> texto
        }
    }

    /** Número no formato internacional que o WhatsApp espera: 55 + DDD + número. */
    fun paraWhatsApp(texto: String): String = "55" + somenteDigitos(texto)
}
