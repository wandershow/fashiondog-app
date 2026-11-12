package br.com.fashiondog.data.repository

import androidx.room.withTransaction
import br.com.fashiondog.data.fotos.ArmazenamentoDeFotos
import br.com.fashiondog.data.local.AppDatabase
import br.com.fashiondog.data.local.entity.Atendimento
import br.com.fashiondog.data.local.entity.Foto
import br.com.fashiondog.data.local.entity.Servico
import br.com.fashiondog.data.local.entity.StatusAgendamento
import br.com.fashiondog.data.local.entity.TipoFoto
import br.com.fashiondog.data.local.model.AtendimentoComServico
import br.com.fashiondog.data.local.model.AtendimentoDetalhado
import br.com.fashiondog.notifications.Avisos
import kotlinx.coroutines.flow.Flow

class AtendimentoRepository(
    private val db: AppDatabase,
    private val fotos: ArmazenamentoDeFotos,
    private val avisos: Avisos,
) {
    private val dao = db.atendimentoDao()
    private val servicoDao = db.servicoDao()

    fun observarHistorico(petId: Long): Flow<List<AtendimentoComServico>> =
        dao.observarHistoricoComServico(petId)

    fun observarDetalhado(id: Long): Flow<AtendimentoDetalhado?> = dao.observarDetalhado(id)

    fun observarFotos(atendimentoId: Long): Flow<List<Foto>> = dao.observarFotos(atendimentoId)

    suspend fun buscar(id: Long): Atendimento? = dao.buscarPorId(id)

    suspend fun fotosDe(atendimentoId: Long): List<Foto> = dao.fotosDoAtendimento(atendimentoId)

    fun observarServicos(): Flow<List<Servico>> = servicoDao.observarAtivos()

    suspend fun salvarServico(servico: Servico) = servicoDao.atualizar(servico)

    /**
     * Grava o atendimento e as fotos (no máximo uma "antes" e uma "depois",
     * por isso o limite de 2). Fotos trocadas ou removidas têm o arquivo apagado.
     *
     * [fotosEscolhidas] = caminho da foto de cada tipo, ou null para "sem foto".
     */
    suspend fun salvar(atendimento: Atendimento, fotosEscolhidas: Map<TipoFoto, String?>): Long {
        val arquivosParaApagar = mutableListOf<String>()

        val id = db.withTransaction {
            val novo = atendimento.id == 0L
            val id = if (novo) dao.inserir(atendimento) else atendimento.id.also { dao.atualizar(atendimento) }

            // Veio da agenda: o agendamento vira "concluído" junto com o atendimento.
            if (novo && atendimento.agendamentoId != null) {
                db.agendamentoDao().mudarStatus(atendimento.agendamentoId, StatusAgendamento.CONCLUIDO)
            }

            val atuais = dao.fotosDoAtendimento(id)
            TipoFoto.entries.forEach { tipo ->
                val antiga = atuais.firstOrNull { it.tipo == tipo }
                val nova = fotosEscolhidas[tipo]
                if (antiga?.caminho != nova) {
                    antiga?.let {
                        dao.excluirFoto(it)
                        arquivosParaApagar += it.caminho
                    }
                    nova?.let { dao.inserirFoto(Foto(atendimentoId = id, caminho = it, tipo = tipo)) }
                }
            }
            id
        }

        // Só apaga os arquivos depois que o banco confirmou a gravação.
        arquivosParaApagar.forEach(fotos::excluir)
        atendimento.agendamentoId?.let(avisos::cancelarAgendamento)
        return id
    }
}
