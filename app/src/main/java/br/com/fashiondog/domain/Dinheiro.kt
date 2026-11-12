package br.com.fashiondog.domain

import kotlin.math.abs

/**
 * Dinheiro no app é sempre um Long em centavos (R$ 12,34 = 1234).
 * Aqui ficam as conversões entre centavos e o texto mostrado na tela.
 */
object Dinheiro {

    /** Limite de dígitos digitados: até R$ 999.999.999,99 — mais que suficiente. */
    private const val MAX_DIGITOS = 11

    /** 123456 → "R$ 1.234,56"; -500 → "-R$ 5,00". Não depende do idioma do celular. */
    fun formatar(centavos: Long): String {
        val valor = abs(centavos)
        val reais = (valor / 100).toString()
            .reversed().chunked(3).joinToString(".").reversed()
        val resto = (valor % 100).toString().padStart(2, '0')
        val sinal = if (centavos < 0) "-" else ""
        return "${sinal}R$ $reais,$resto"
    }

    /**
     * Campo de valor estilo "maquininha": os dígitos digitados entram pela direita.
     * "R$ 12,345" (usuário digitou 5) → 12345 centavos = R$ 123,45.
     */
    fun deDigitos(texto: String): Long =
        texto.filter(Char::isDigit).trimStart('0').take(MAX_DIGITOS).toLongOrNull() ?: 0L
}
