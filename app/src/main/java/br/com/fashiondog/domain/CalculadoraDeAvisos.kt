package br.com.fashiondog.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Quando cada aviso (notificação) deve tocar.
 * Tudo em milissegundos; [agora] vem de fora para dar para testar.
 */
object CalculadoraDeAvisos {

    /** Opções de lembrete do agendamento, em minutos antes do horário. */
    val OPCOES_LEMBRETE = listOf(0, 30, 60, 120, 1440)

    fun rotuloLembrete(minutos: Int): String = when {
        minutos == 0 -> "Na hora"
        minutos < 60 -> "$minutos min antes"
        minutos == 1440 -> "1 dia antes"
        minutos % 60 == 0 -> "${minutos / 60} h antes"
        else -> "$minutos min antes"
    }

    /**
     * Horário do lembrete de um agendamento, ou null se já passou
     * (não faz sentido agendar um aviso para o passado).
     */
    fun horarioDoLembrete(dataHora: Long, lembreteMinutos: Int, agora: Long): Long? =
        (dataHora - lembreteMinutos * MINUTO).takeIf { it > agora }

    /** Hora em que os avisos de boleto tocam (véspera e dia do vencimento). */
    val HORA_AVISO_PARCELA: LocalTime = LocalTime.of(9, 0)

    /**
     * Avisos de uma parcela: um dia antes e no dia do vencimento, às 9h.
     * Devolve só os que ainda estão no futuro (a chave diz qual é qual).
     */
    fun horariosDaParcela(
        vencimento: LocalDate,
        agora: Long,
        zona: ZoneId = ZoneId.systemDefault(),
    ): Map<AvisoDeParcela, Long> = mapOf(
        AvisoDeParcela.VESPERA to Datas.paraMillis(vencimento.minusDays(1), HORA_AVISO_PARCELA, zona),
        AvisoDeParcela.NO_DIA to Datas.paraMillis(vencimento, HORA_AVISO_PARCELA, zona),
    ).filterValues { it > agora }

    private const val MINUTO = 60_000L
}

enum class AvisoDeParcela { VESPERA, NO_DIA }

/**
 * Cada aviso precisa de um número único para o Android: é com ele que o aviso é
 * substituído (ao editar) ou cancelado (ao concluir/pagar). O último dígito diz
 * o tipo, então um agendamento e uma parcela com o mesmo id nunca se confundem.
 */
object CodigosDeAviso {
    fun agendamento(id: Long): Int = (id * 10 + 1).toInt()
    fun parcelaVespera(id: Long): Int = (id * 10 + 2).toInt()
    fun parcelaNoDia(id: Long): Int = (id * 10 + 3).toInt()
}
