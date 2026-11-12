package br.com.fashiondog.data.repository

import br.com.fashiondog.data.local.AppDatabase
import br.com.fashiondog.data.local.entity.Agendamento
import br.com.fashiondog.data.local.entity.StatusAgendamento
import br.com.fashiondog.data.local.model.AgendamentoDetalhado
import br.com.fashiondog.notifications.Avisos
import kotlinx.coroutines.flow.Flow

/** Agendamentos + os avisos de cada um. Salvar sempre reagenda o aviso. */
class AgendaRepository(
    db: AppDatabase,
    private val avisos: Avisos,
) {
    private val dao = db.agendamentoDao()

    fun observarEntre(inicio: Long, fim: Long): Flow<List<AgendamentoDetalhado>> =
        dao.observarDetalhadosEntre(inicio, fim)

    suspend fun buscar(id: Long): Agendamento? = dao.buscarPorId(id)

    suspend fun salvar(agendamento: Agendamento): Long {
        val id = if (agendamento.id == 0L) dao.inserir(agendamento)
        else agendamento.id.also { dao.atualizar(agendamento) }
        dao.buscarDetalhado(id)?.let { avisos.agendarAgendamento(it) }
        return id
    }

    suspend fun cancelar(id: Long) {
        dao.mudarStatus(id, StatusAgendamento.CANCELADO)
        avisos.cancelarAgendamento(id)
    }

    /** Recria os lembretes de todos os agendamentos futuros. */
    suspend fun reagendarAvisos(agora: Long = System.currentTimeMillis()) {
        dao.futurosAgendados(agora).forEach { avisos.agendarAgendamento(it, agora) }
    }
}
