package br.com.fashiondog.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class DinheiroTest {

    @Test
    fun `formata com separador de milhar e centavos`() {
        assertEquals("R$ 1.234,56", Dinheiro.formatar(123456))
        assertEquals("R$ 0,05", Dinheiro.formatar(5))
        assertEquals("R$ 1.000.000,00", Dinheiro.formatar(100_000_000))
    }

    @Test
    fun `formata valor negativo`() {
        assertEquals("-R$ 5,00", Dinheiro.formatar(-500))
    }

    @Test
    fun `digitos entram pela direita como na maquininha`() {
        assertEquals(12345L, Dinheiro.deDigitos("R$ 123,45"))
        assertEquals(123456L, Dinheiro.deDigitos("R$ 1.234,56"))
    }

    @Test
    fun `apagar o ultimo digito divide por 10`() {
        // "R$ 12,34" com backspace vira "R$ 12,3"
        assertEquals(123L, Dinheiro.deDigitos("R$ 12,3"))
    }

    @Test
    fun `texto sem digitos vale zero`() {
        assertEquals(0L, Dinheiro.deDigitos("R$ ,"))
        assertEquals(0L, Dinheiro.deDigitos(""))
    }
}

class DatasTest {

    private val zona = ZoneId.of("America/Sao_Paulo")

    @Test
    fun `ida e volta entre LocalDate e millis`() {
        val data = LocalDate.of(2026, 11, 12)
        val millis = Datas.paraMillis(data, LocalTime.of(14, 30), zona)
        assertEquals(data, Datas.paraLocalDate(millis, zona))
        assertEquals("12/11/2026 às 14:30", Datas.formatarDataHora(millis, zona))
    }

    @Test
    fun `ha quanto tempo`() {
        val hoje = LocalDate.of(2026, 11, 12)
        assertEquals("hoje", Datas.haQuantoTempo(Datas.paraMillis(hoje, zona = zona), hoje, zona))
        assertEquals("ontem", Datas.haQuantoTempo(Datas.paraMillis(hoje.minusDays(1), zona = zona), hoje, zona))
        assertEquals("há 7 dias", Datas.haQuantoTempo(Datas.paraMillis(hoje.minusDays(7), zona = zona), hoje, zona))
    }

    @Test
    fun `seletor de data usa UTC`() {
        val data = LocalDate.of(2026, 1, 31)
        assertEquals(data, Datas.deSeletor(Datas.paraSeletor(data)))
    }
}

class RedimensionamentoDeFotoTest {

    @Test
    fun `foto de camera abre reduzida pela metade`() {
        assertEquals(2, RedimensionamentoDeFoto.fatorDeAmostragem(4000, 3000))
    }

    @Test
    fun `foto pequena nao e reduzida`() {
        assertEquals(1, RedimensionamentoDeFoto.fatorDeAmostragem(800, 600))
        assertEquals(800 to 600, RedimensionamentoDeFoto.dimensoesFinais(800, 600))
    }

    @Test
    fun `mantem a proporcao com lado maior de 1280`() {
        assertEquals(1280 to 960, RedimensionamentoDeFoto.dimensoesFinais(2000, 1500))
        assertEquals(960 to 1280, RedimensionamentoDeFoto.dimensoesFinais(1500, 2000))
    }
}
