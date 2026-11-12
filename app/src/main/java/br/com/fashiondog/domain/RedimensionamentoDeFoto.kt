package br.com.fashiondog.domain

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Contas para comprimir as fotos dos atendimentos. Uma foto de câmera tem uns
 * 4000×3000 px (3–5 MB); guardamos no máximo 1280 px no lado maior (~200 KB).
 */
object RedimensionamentoDeFoto {

    const val LADO_MAXIMO = 1280

    /**
     * Fator (potência de 2) que o Android usa para já abrir a imagem reduzida,
     * sem carregar a foto inteira na memória. Nunca reduz abaixo de [ladoMaximo].
     */
    fun fatorDeAmostragem(largura: Int, altura: Int, ladoMaximo: Int = LADO_MAXIMO): Int {
        val maior = max(largura, altura)
        var fator = 1
        while (maior / (fator * 2) >= ladoMaximo) fator *= 2
        return fator
    }

    /** Tamanho final mantendo a proporção, com o lado maior igual a [ladoMaximo]. */
    fun dimensoesFinais(largura: Int, altura: Int, ladoMaximo: Int = LADO_MAXIMO): Pair<Int, Int> {
        val maior = max(largura, altura)
        if (maior <= ladoMaximo) return largura to altura
        val escala = ladoMaximo.toDouble() / maior
        return (largura * escala).roundToInt() to (altura * escala).roundToInt()
    }
}
