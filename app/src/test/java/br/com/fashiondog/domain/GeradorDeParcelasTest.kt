package br.com.fashiondog.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class GeradorDeParcelasTest {

    @Test
    fun divideIgualmenteQuandoNaoSobraCentavo() {
        val parcelas = GeradorDeParcelas.gerar(72_000, 6, LocalDate.of(2026, 10, 10))

        assertEquals(6, parcelas.size)
        assertEquals(List(6) { 12_000L }, parcelas.map { it.valorCentavos })
    }

    @Test
    fun ultimaParcelaAbsorveADiferencaDeCentavos() {
        val parcelas = GeradorDeParcelas.gerar(10_000, 3, LocalDate.of(2026, 1, 15))

        assertEquals(listOf(3_333L, 3_333L, 3_334L), parcelas.map { it.valorCentavos })
        assertEquals(10_000L, parcelas.sumOf { it.valorCentavos })
    }

    @Test
    fun vencimentosSaoMensais() {
        val parcelas = GeradorDeParcelas.gerar(30_000, 3, LocalDate.of(2026, 11, 10))

        assertEquals(
            listOf(
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 12, 10),
                LocalDate.of(2027, 1, 10),
            ),
            parcelas.map { it.vencimento },
        )
    }

    @Test
    fun despesaAVistaViraUmaParcela() {
        val parcelas = GeradorDeParcelas.gerar(90_000, 1, LocalDate.of(2026, 11, 15))

        assertEquals(1, parcelas.size)
        assertEquals(90_000L, parcelas.single().valorCentavos)
    }
}
