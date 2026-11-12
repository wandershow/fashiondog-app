package br.com.fashiondog.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Datas são gravadas como milissegundos (epoch). Estas funções convertem para o
 * que aparece na tela, sempre no fuso do celular (o [zona] existe para os testes).
 */
object Datas {

    private val formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")

    fun paraLocalDate(millis: Long, zona: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zona).toLocalDate()

    fun paraLocalDateTime(millis: Long, zona: ZoneId = ZoneId.systemDefault()): LocalDateTime =
        Instant.ofEpochMilli(millis).atZone(zona).toLocalDateTime()

    fun paraMillis(data: LocalDate, hora: LocalTime = LocalTime.NOON, zona: ZoneId = ZoneId.systemDefault()): Long =
        data.atTime(hora).atZone(zona).toInstant().toEpochMilli()

    fun inicioDoDia(data: LocalDate, zona: ZoneId = ZoneId.systemDefault()): Long =
        data.atStartOfDay(zona).toInstant().toEpochMilli()

    fun formatarData(millis: Long, zona: ZoneId = ZoneId.systemDefault()): String =
        paraLocalDate(millis, zona).format(formatoData)

    fun formatarData(data: LocalDate): String = data.format(formatoData)

    fun formatarHora(millis: Long, zona: ZoneId = ZoneId.systemDefault()): String =
        paraLocalDateTime(millis, zona).format(formatoHora)

    fun formatarDataHora(millis: Long, zona: ZoneId = ZoneId.systemDefault()): String =
        "${formatarData(millis, zona)} às ${formatarHora(millis, zona)}"

    /** "hoje", "ontem", "há 5 dias" — usado no destaque do último banho. */
    fun haQuantoTempo(millis: Long, hoje: LocalDate = LocalDate.now(), zona: ZoneId = ZoneId.systemDefault()): String {
        val dias = ChronoUnit.DAYS.between(paraLocalDate(millis, zona), hoje)
        return when {
            dias <= 0L -> "hoje"
            dias == 1L -> "ontem"
            else -> "há $dias dias"
        }
    }

    /**
     * O seletor de data do Material 3 trabalha com meia-noite em UTC.
     * Estas duas funções fazem a ponte com o LocalDate do app.
     */
    fun deSeletor(millisUtc: Long): LocalDate =
        Instant.ofEpochMilli(millisUtc).atZone(ZoneOffset.UTC).toLocalDate()

    fun paraSeletor(data: LocalDate): Long =
        data.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
