package br.com.fashiondog.domain

import java.time.LocalDate

/** Uma parcela planejada, com a informação de já nascer paga ou não. */
data class ParcelaPlanejada(
    val numero: Int,
    val valorCentavos: Long,
    val vencimento: LocalDate,
    val jaPaga: Boolean,
)

/**
 * Transforma uma despesa nas parcelas que vão para o banco.
 * Regra do projeto: o balanço soma PARCELAS pelo vencimento, então toda despesa
 * tem ao menos uma parcela.
 */
object PlanoDeParcelas {

    const val MAXIMO_DE_PARCELAS = 48

    /** À vista: uma parcela já paga, com "vencimento" no dia da compra. */
    fun aVista(valorTotalCentavos: Long, dataCompra: LocalDate): List<ParcelaPlanejada> {
        require(valorTotalCentavos > 0) { "O valor total deve ser maior que zero" }
        return listOf(ParcelaPlanejada(1, valorTotalCentavos, dataCompra, jaPaga = true))
    }

    /** Boleto parcelado: N parcelas mensais pendentes; a última absorve os centavos. */
    fun boleto(valorTotalCentavos: Long, numParcelas: Int, primeiroVencimento: LocalDate): List<ParcelaPlanejada> {
        require(numParcelas <= MAXIMO_DE_PARCELAS) { "No máximo $MAXIMO_DE_PARCELAS parcelas" }
        return GeradorDeParcelas.gerar(valorTotalCentavos, numParcelas, primeiroVencimento).map {
            ParcelaPlanejada(it.numero, it.valorCentavos, it.vencimento, jaPaga = false)
        }
    }
}
