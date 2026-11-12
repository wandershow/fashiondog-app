package br.com.fashiondog.domain

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

enum class TipoPeriodo(val rotulo: String) {
    SEMANA("Semana"),
    MES("Mês"),
    TRIMESTRE("Trimestre"),
    SEMESTRE("Semestre"),
    ANO("Ano"),
}

/**
 * Intervalo de datas do balanço: de [inicio] (incluído) até [fim] (NÃO incluído).
 * Usar o fim "aberto" evita o erro clássico de esquecer o último dia ou contar
 * duas vezes um registro feito à meia-noite.
 */
data class Periodo(val tipo: TipoPeriodo, val inicio: LocalDate, val fim: LocalDate) {

    fun anterior(): Periodo = de(tipo, inicio.minusDays(1))

    fun proximo(): Periodo = de(tipo, fim)

    fun contem(data: LocalDate): Boolean = !data.isBefore(inicio) && data.isBefore(fim)

    /** "09/11 a 15/11/2026", "novembro de 2026", "4º trimestre de 2026", "2º semestre de 2026", "2026". */
    val rotulo: String
        get() = when (tipo) {
            TipoPeriodo.SEMANA -> {
                val domingo = fim.minusDays(1)
                "%02d/%02d a %s".format(inicio.dayOfMonth, inicio.monthValue, Datas.formatarData(domingo))
            }
            TipoPeriodo.MES -> "${inicio.month.getDisplayName(TextStyle.FULL_STANDALONE, portugues).lowercase()} de ${inicio.year}"
            TipoPeriodo.TRIMESTRE -> "${(inicio.monthValue - 1) / 3 + 1}º trimestre de ${inicio.year}"
            TipoPeriodo.SEMESTRE -> "${(inicio.monthValue - 1) / 6 + 1}º semestre de ${inicio.year}"
            TipoPeriodo.ANO -> "${inicio.year}"
        }

    companion object {
        private val portugues = Locale.forLanguageTag("pt-BR")

        /** O período do [tipo] que contém a data [referencia]. */
        fun de(tipo: TipoPeriodo, referencia: LocalDate): Periodo = when (tipo) {
            TipoPeriodo.SEMANA -> Semana.inicio(referencia).let { Periodo(tipo, it, it.plusWeeks(1)) }
            TipoPeriodo.MES -> referencia.withDayOfMonth(1).let { Periodo(tipo, it, it.plusMonths(1)) }
            TipoPeriodo.TRIMESTRE -> inicioDoBloco(referencia, 3).let { Periodo(tipo, it, it.plusMonths(3)) }
            TipoPeriodo.SEMESTRE -> inicioDoBloco(referencia, 6).let { Periodo(tipo, it, it.plusMonths(6)) }
            TipoPeriodo.ANO -> referencia.withDayOfYear(1).let { Periodo(tipo, it, it.plusYears(1)) }
        }

        /** Primeiro dia do bloco de [meses] meses (jan, abr, jul, out para trimestres). */
        private fun inicioDoBloco(data: LocalDate, meses: Int): LocalDate {
            val primeiroMes = (data.monthValue - 1) / meses * meses + 1
            return LocalDate.of(data.year, primeiroMes, 1)
        }
    }
}
