package br.com.fashiondog.data.repository

import androidx.room.withTransaction
import br.com.fashiondog.data.local.AppDatabase
import br.com.fashiondog.data.local.entity.Categoria
import br.com.fashiondog.data.local.entity.Despesa
import br.com.fashiondog.data.local.entity.Parcela
import br.com.fashiondog.data.local.entity.StatusParcela
import br.com.fashiondog.data.local.model.ParcelaDetalhada
import br.com.fashiondog.data.local.model.TotalPorCategoria
import br.com.fashiondog.domain.Datas
import br.com.fashiondog.domain.ParcelaPlanejada
import br.com.fashiondog.domain.Periodo
import br.com.fashiondog.notifications.Avisos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

/** Os números do balanço de um período. Sempre calculados por consulta, nunca gravados. */
data class Balanco(
    val receitaCentavos: Long = 0,
    val despesaCentavos: Long = 0,
    val quantidadeAtendimentos: Int = 0,
) {
    val saldoCentavos: Long get() = receitaCentavos - despesaCentavos
}

class FinanceiroRepository(
    private val db: AppDatabase,
    private val avisos: Avisos,
) {
    private val dao = db.financeiroDao()
    private val atendimentoDao = db.atendimentoDao()

    fun observarCategorias(): Flow<List<Categoria>> = dao.observarCategorias()

    suspend fun criarCategoria(nome: String): Long = dao.inserirCategoria(Categoria(nome = nome.trim()))

    /**
     * Receita = soma dos atendimentos; despesa = soma das parcelas pelo vencimento.
     * O Room avisa quando qualquer um muda, e o balanço é recalculado na hora.
     */
    fun observarBalanco(periodo: Periodo): Flow<Balanco> {
        val (inicio, fim) = limites(periodo)
        return combine(
            atendimentoDao.observarReceitaEntre(inicio, fim),
            dao.observarDespesaEntre(inicio, fim),
            atendimentoDao.observarQuantidadeEntre(inicio, fim),
        ) { receita, despesa, quantidade -> Balanco(receita, despesa, quantidade) }
    }

    fun observarParcelasDoPeriodo(periodo: Periodo): Flow<List<ParcelaDetalhada>> {
        val (inicio, fim) = limites(periodo)
        return dao.observarParcelasEntre(inicio, fim)
    }

    fun observarTotalPorCategoria(periodo: Periodo): Flow<List<TotalPorCategoria>> {
        val (inicio, fim) = limites(periodo)
        return dao.observarTotalPorCategoria(inicio, fim)
    }

    /** Pendentes que vencem nos próximos [dias] dias, incluindo as atrasadas. */
    fun observarAPagar(hoje: LocalDate = LocalDate.now(), dias: Long = 30): Flow<List<ParcelaDetalhada>> =
        dao.observarPendentesAte(Datas.inicioDoDia(hoje.plusDays(dias + 1)))

    /**
     * Grava a despesa e as parcelas numa transação e agenda os avisos das pendentes.
     * O vencimento é gravado ao meio-dia, longe da virada do dia, para não cair no
     * dia errado por causa de fuso ou horário de verão.
     */
    suspend fun salvarDespesa(despesa: Despesa, plano: List<ParcelaPlanejada>): Long {
        val despesaId = db.withTransaction {
            val id = dao.inserirDespesa(despesa)
            dao.inserirParcelas(
                plano.map {
                    Parcela(
                        despesaId = id,
                        numero = it.numero,
                        valorCentavos = it.valorCentavos,
                        vencimento = Datas.paraMillis(it.vencimento),
                        status = if (it.jaPaga) StatusParcela.PAGA else StatusParcela.PENDENTE,
                        dataPagamento = if (it.jaPaga) despesa.dataCompra else null,
                    )
                },
            )
            id
        }
        dao.parcelasDaDespesa(despesaId).forEach { parcela ->
            dao.buscarParcelaDetalhada(parcela.id)?.let { avisos.agendarParcela(it) }
        }
        return despesaId
    }

    /** Pagar cancela os dois avisos; desfazer o pagamento os recria. */
    suspend fun marcarPaga(parcelaId: Long, paga: Boolean) {
        val parcela = dao.buscarParcela(parcelaId) ?: return
        dao.atualizarParcela(
            parcela.copy(
                status = if (paga) StatusParcela.PAGA else StatusParcela.PENDENTE,
                dataPagamento = if (paga) System.currentTimeMillis() else null,
            ),
        )
        if (paga) avisos.cancelarParcela(parcelaId)
        else dao.buscarParcelaDetalhada(parcelaId)?.let { avisos.agendarParcela(it) }
    }

    /** Exclui a despesa inteira (as parcelas vão junto, por CASCADE) e os avisos. */
    suspend fun excluirDespesa(despesaId: Long) {
        val parcelas = dao.parcelasDaDespesa(despesaId)
        dao.excluirDespesa(despesaId)
        parcelas.forEach { avisos.cancelarParcela(it.id) }
    }

    suspend fun reagendarAvisos(agora: Long = System.currentTimeMillis()) {
        // Desde ontem: a parcela de hoje ainda pode ter o aviso "no dia" pela frente.
        val desde = Datas.inicioDoDia(Datas.paraLocalDate(agora).minusDays(1))
        dao.pendentesDetalhadasAPartirDe(desde).forEach { avisos.agendarParcela(it, agora) }
    }

    private fun limites(periodo: Periodo): Pair<Long, Long> =
        Datas.inicioDoDia(periodo.inicio) to Datas.inicioDoDia(periodo.fim)
}
