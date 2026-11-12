package br.com.fashiondog.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Semana do app: de segunda a domingo (como o balanço semanal da Sprint 4). */
object Semana {

    private val portugues = Locale.forLanguageTag("pt-BR")

    fun inicio(data: LocalDate): LocalDate = data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun dias(data: LocalDate): List<LocalDate> = inicio(data).let { segunda -> (0L..6L).map(segunda::plusDays) }

    /** "seg", "ter", ... */
    fun nomeCurto(data: LocalDate): String =
        data.dayOfWeek.getDisplayName(TextStyle.SHORT, portugues).removeSuffix(".").lowercase()

    /** "novembro de 2026" */
    fun mesEAno(data: LocalDate): String =
        "${data.month.getDisplayName(TextStyle.FULL_STANDALONE, portugues).lowercase()} de ${data.year}"
}
