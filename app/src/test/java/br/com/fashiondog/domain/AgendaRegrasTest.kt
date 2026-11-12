package br.com.fashiondog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class SemanaTest {

    @Test
    fun `semana comeca na segunda`() {
        // 12/11/2026 é uma quinta-feira
        assertEquals(LocalDate.of(2026, 11, 9), Semana.inicio(LocalDate.of(2026, 11, 12)))
    }

    @Test
    fun `domingo pertence a semana que comecou na segunda anterior`() {
        assertEquals(LocalDate.of(2026, 11, 9), Semana.inicio(LocalDate.of(2026, 11, 15)))
    }

    @Test
    fun `segunda e o proprio inicio`() {
        assertEquals(LocalDate.of(2026, 11, 16), Semana.inicio(LocalDate.of(2026, 11, 16)))
    }

    @Test
    fun `semana tem sete dias de segunda a domingo`() {
        val dias = Semana.dias(LocalDate.of(2026, 12, 31))
        assertEquals(7, dias.size)
        assertEquals(DayOfWeek.MONDAY, dias.first().dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, dias.last().dayOfWeek)
        assertEquals(LocalDate.of(2027, 1, 3), dias.last()) // atravessa o ano
    }

    @Test
    fun `nomes em portugues`() {
        assertEquals("qui", Semana.nomeCurto(LocalDate.of(2026, 11, 12)))
        assertEquals("novembro de 2026", Semana.mesEAno(LocalDate.of(2026, 11, 12)))
    }
}

class CalculadoraDeAvisosTest {

    private val hora = 60 * 60_000L
    private val agora = 1_000_000_000_000L

    @Test
    fun `lembrete uma hora antes`() {
        val banho = agora + 3 * hora
        assertEquals(agora + 2 * hora, CalculadoraDeAvisos.horarioDoLembrete(banho, 60, agora))
    }

    @Test
    fun `lembrete que ja passou nao e agendado`() {
        val banhoDaquiAMeiaHora = agora + hora / 2
        assertNull(CalculadoraDeAvisos.horarioDoLembrete(banhoDaquiAMeiaHora, 60, agora))
    }

    @Test
    fun `rotulos dos lembretes`() {
        assertEquals("Na hora", CalculadoraDeAvisos.rotuloLembrete(0))
        assertEquals("30 min antes", CalculadoraDeAvisos.rotuloLembrete(30))
        assertEquals("2 h antes", CalculadoraDeAvisos.rotuloLembrete(120))
        assertEquals("1 dia antes", CalculadoraDeAvisos.rotuloLembrete(1440))
    }

    @Test
    fun `codigos de aviso nunca se repetem entre tipos`() {
        val codigos = listOf(
            CodigosDeAviso.agendamento(7),
            CodigosDeAviso.parcelaVespera(7),
            CodigosDeAviso.parcelaNoDia(7),
        )
        assertEquals(3, codigos.toSet().size)
        assertNotEquals(CodigosDeAviso.agendamento(1), CodigosDeAviso.agendamento(2))
    }
}
