package br.com.fashiondog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class PeriodoTest {

    private val quinta = LocalDate.of(2026, 11, 12)

    @Test
    fun `semana vai de segunda a domingo`() {
        val p = Periodo.de(TipoPeriodo.SEMANA, quinta)
        assertEquals(LocalDate.of(2026, 11, 9), p.inicio)
        assertEquals(LocalDate.of(2026, 11, 16), p.fim) // segunda seguinte, não incluída
        assertTrue(p.contem(LocalDate.of(2026, 11, 15))) // domingo
        assertFalse(p.contem(LocalDate.of(2026, 11, 16)))
        assertEquals("09/11 a 15/11/2026", p.rotulo)
    }

    @Test
    fun `mes inteiro inclusive em fevereiro bissexto`() {
        val p = Periodo.de(TipoPeriodo.MES, LocalDate.of(2028, 2, 15))
        assertEquals(LocalDate.of(2028, 2, 1), p.inicio)
        assertEquals(LocalDate.of(2028, 3, 1), p.fim)
        assertTrue(p.contem(LocalDate.of(2028, 2, 29)))
        assertEquals("fevereiro de 2028", p.rotulo)
    }

    @Test
    fun `trimestres comecam em janeiro abril julho e outubro`() {
        assertEquals(LocalDate.of(2026, 10, 1), Periodo.de(TipoPeriodo.TRIMESTRE, quinta).inicio)
        assertEquals(LocalDate.of(2026, 4, 1), Periodo.de(TipoPeriodo.TRIMESTRE, LocalDate.of(2026, 6, 30)).inicio)
        assertEquals("4º trimestre de 2026", Periodo.de(TipoPeriodo.TRIMESTRE, quinta).rotulo)
    }

    @Test
    fun `semestres comecam em janeiro e julho`() {
        val p = Periodo.de(TipoPeriodo.SEMESTRE, quinta)
        assertEquals(LocalDate.of(2026, 7, 1), p.inicio)
        assertEquals(LocalDate.of(2027, 1, 1), p.fim)
        assertEquals("2º semestre de 2026", p.rotulo)
    }

    @Test
    fun `ano inteiro`() {
        val p = Periodo.de(TipoPeriodo.ANO, quinta)
        assertEquals(LocalDate.of(2026, 1, 1), p.inicio)
        assertEquals(LocalDate.of(2027, 1, 1), p.fim)
    }

    @Test
    fun `anterior e proximo andam um periodo inteiro`() {
        val mes = Periodo.de(TipoPeriodo.MES, LocalDate.of(2026, 1, 20))
        assertEquals(LocalDate.of(2025, 12, 1), mes.anterior().inicio)
        assertEquals(LocalDate.of(2026, 2, 1), mes.proximo().inicio)
        val semana = Periodo.de(TipoPeriodo.SEMANA, quinta)
        assertEquals(LocalDate.of(2026, 11, 2), semana.anterior().inicio)
        assertEquals(LocalDate.of(2026, 11, 16), semana.proximo().inicio)
    }
}

class PlanoDeParcelasTest {

    @Test
    fun `a vista vira uma parcela paga no dia da compra`() {
        val compra = LocalDate.of(2026, 11, 12)
        val parcelas = PlanoDeParcelas.aVista(15_990, compra)
        assertEquals(1, parcelas.size)
        assertEquals(ParcelaPlanejada(1, 15_990, compra, jaPaga = true), parcelas.single())
    }

    @Test
    fun `boleto gera parcelas pendentes que somam o total`() {
        val parcelas = PlanoDeParcelas.boleto(100_000, 3, LocalDate.of(2026, 12, 10))
        assertEquals(listOf(33_333L, 33_333L, 33_334L), parcelas.map { it.valorCentavos })
        assertTrue(parcelas.none { it.jaPaga })
        assertEquals(LocalDate.of(2027, 2, 10), parcelas.last().vencimento)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `boleto com parcelas demais e recusado`() {
        PlanoDeParcelas.boleto(100_000, 49, LocalDate.of(2026, 12, 10))
    }
}

class AvisosDeParcelaTest {

    private val zona = ZoneId.of("America/Sao_Paulo")
    private val vencimento = LocalDate.of(2026, 11, 20)

    @Test
    fun `avisa na vespera e no dia as nove`() {
        val agora = Datas.paraMillis(LocalDate.of(2026, 11, 1), LocalTime.NOON, zona)
        val avisos = CalculadoraDeAvisos.horariosDaParcela(vencimento, agora, zona)
        assertEquals(Datas.paraMillis(LocalDate.of(2026, 11, 19), LocalTime.of(9, 0), zona), avisos[AvisoDeParcela.VESPERA])
        assertEquals(Datas.paraMillis(vencimento, LocalTime.of(9, 0), zona), avisos[AvisoDeParcela.NO_DIA])
    }

    @Test
    fun `na vespera a tarde so sobra o aviso do dia`() {
        val agora = Datas.paraMillis(LocalDate.of(2026, 11, 19), LocalTime.of(15, 0), zona)
        val avisos = CalculadoraDeAvisos.horariosDaParcela(vencimento, agora, zona)
        assertEquals(setOf(AvisoDeParcela.NO_DIA), avisos.keys)
    }

    @Test
    fun `parcela vencida nao gera aviso`() {
        val agora = Datas.paraMillis(LocalDate.of(2026, 11, 21), LocalTime.NOON, zona)
        assertTrue(CalculadoraDeAvisos.horariosDaParcela(vencimento, agora, zona).isEmpty())
    }
}
