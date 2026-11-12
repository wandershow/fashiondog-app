package br.com.fashiondog.domain

import java.time.LocalDate

/** Uma parcela calculada, antes de ser gravada no banco. */
data class ParcelaCalculada(
    val numero: Int,
    val valorCentavos: Long,
    val vencimento: LocalDate,
)

object GeradorDeParcelas {

    /**
     * Divide [valorTotalCentavos] em [numParcelas] parcelas mensais a partir de
     * [primeiroVencimento]. A última parcela absorve a diferença de centavos,
     * para que a soma seja sempre igual ao total.
     */
    fun gerar(
        valorTotalCentavos: Long,
        numParcelas: Int,
        primeiroVencimento: LocalDate,
    ): List<ParcelaCalculada> {
        require(valorTotalCentavos > 0) { "O valor total deve ser maior que zero" }
        require(numParcelas >= 1) { "O número de parcelas deve ser pelo menos 1" }

        val valorBase = valorTotalCentavos / numParcelas
        val resto = valorTotalCentavos - valorBase * numParcelas

        return (1..numParcelas).map { numero ->
            ParcelaCalculada(
                numero = numero,
                valorCentavos = if (numero == numParcelas) valorBase + resto else valorBase,
                vencimento = primeiroVencimento.plusMonths((numero - 1).toLong()),
            )
        }
    }
}
