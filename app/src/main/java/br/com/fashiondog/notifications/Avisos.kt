package br.com.fashiondog.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import br.com.fashiondog.data.local.entity.StatusAgendamento
import br.com.fashiondog.data.local.model.AgendamentoDetalhado
import br.com.fashiondog.domain.CalculadoraDeAvisos
import br.com.fashiondog.domain.CodigosDeAviso
import br.com.fashiondog.domain.Datas

/** Um aviso pronto para ser agendado: quando toca e o que mostra. */
data class Aviso(
    val codigo: Int,
    val quando: Long,
    val canal: String,
    val titulo: String,
    val texto: String,
)

/**
 * Agenda e cancela avisos com o AlarmManager do Android. O alarme acorda o
 * [ReceptorDeAvisos] na hora certa, mesmo com o app fechado, e ele mostra a notificação.
 */
class Avisos(private val contexto: Context) {

    private val alarmes = contexto.getSystemService(AlarmManager::class.java)

    /** Cria os canais (categorias que o usuário pode ligar/desligar nas configurações). */
    fun criarCanais() {
        val gerenciador = contexto.getSystemService(NotificationManager::class.java)
        gerenciador.createNotificationChannel(
            NotificationChannel(CANAL_AGENDA, "Agenda", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Lembretes de banhos e tosas agendados"
            },
        )
        gerenciador.createNotificationChannel(
            NotificationChannel(CANAL_PARCELAS, "Parcelas", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Vencimento de boletos parcelados"
            },
        )
    }

    /**
     * Alarme exato precisa de permissão a partir do Android 12. Sem ela, usamos uma
     * janela de 10 minutos — o aviso chega, só que com um pequeno atraso possível.
     */
    fun podeAgendarExato(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmes.canScheduleExactAlarms()

    fun agendar(aviso: Aviso) {
        val intencao = PendingIntent.getBroadcast(
            contexto,
            aviso.codigo,
            Intent(contexto, ReceptorDeAvisos::class.java)
                .putExtra(ReceptorDeAvisos.EXTRA_CODIGO, aviso.codigo)
                .putExtra(ReceptorDeAvisos.EXTRA_CANAL, aviso.canal)
                .putExtra(ReceptorDeAvisos.EXTRA_TITULO, aviso.titulo)
                .putExtra(ReceptorDeAvisos.EXTRA_TEXTO, aviso.texto),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (podeAgendarExato()) {
            alarmes.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, aviso.quando, intencao)
        } else {
            alarmes.setWindow(AlarmManager.RTC_WAKEUP, aviso.quando, JANELA_SEM_PERMISSAO, intencao)
        }
    }

    fun cancelar(codigo: Int) {
        val intencao = PendingIntent.getBroadcast(
            contexto,
            codigo,
            Intent(contexto, ReceptorDeAvisos::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmes.cancel(intencao)
        intencao.cancel()
    }

    // ----- Agenda -----

    /** Agenda (ou substitui) o lembrete do agendamento; se não couber mais, cancela. */
    fun agendarAgendamento(item: AgendamentoDetalhado, agora: Long = System.currentTimeMillis()) {
        val agendamento = item.agendamento
        val codigo = CodigosDeAviso.agendamento(agendamento.id)
        val quando = CalculadoraDeAvisos.horarioDoLembrete(agendamento.dataHora, agendamento.lembreteMinutos, agora)
        if (agendamento.status != StatusAgendamento.AGENDADO || quando == null) {
            cancelar(codigo)
            return
        }
        agendar(
            Aviso(
                codigo = codigo,
                quando = quando,
                canal = CANAL_AGENDA,
                titulo = "${item.servicoNome} às ${Datas.formatarHora(agendamento.dataHora)} — ${item.petNome}",
                texto = "Tutor: ${item.tutorNome} · ${Datas.formatarData(agendamento.dataHora)}",
            ),
        )
    }

    fun cancelarAgendamento(agendamentoId: Long) = cancelar(CodigosDeAviso.agendamento(agendamentoId))

    companion object {
        const val CANAL_AGENDA = "agenda"
        const val CANAL_PARCELAS = "parcelas"
        private const val JANELA_SEM_PERMISSAO = 10 * 60_000L
    }
}
