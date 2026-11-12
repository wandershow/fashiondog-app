package br.com.fashiondog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.com.fashiondog.data.local.entity.Categoria
import br.com.fashiondog.data.local.entity.Despesa
import br.com.fashiondog.data.local.entity.Parcela
import br.com.fashiondog.data.local.model.ParcelaDetalhada
import br.com.fashiondog.data.local.model.TotalPorCategoria
import kotlinx.coroutines.flow.Flow

/** Início comum das consultas de parcela com a descrição e a categoria da despesa. */
private const val SELECT_PARCELA_DETALHADA = """
    SELECT parcela.*, despesa.descricao AS descricao, despesa.numParcelas AS totalParcelas,
           despesa.formaPagamento AS formaPagamento, categoria.nome AS categoriaNome
    FROM parcela
    INNER JOIN despesa ON despesa.id = parcela.despesaId
    INNER JOIN categoria ON categoria.id = despesa.categoriaId
"""

@Dao
interface FinanceiroDao {
    @Insert
    suspend fun inserirCategoria(categoria: Categoria): Long

    @Query("SELECT * FROM categoria ORDER BY nome COLLATE NOCASE")
    fun observarCategorias(): Flow<List<Categoria>>

    @Query("SELECT COUNT(*) FROM categoria")
    suspend fun contarCategorias(): Int

    @Insert
    suspend fun inserirDespesa(despesa: Despesa): Long

    @Query("DELETE FROM despesa WHERE id = :id")
    suspend fun excluirDespesa(id: Long)

    @Insert
    suspend fun inserirParcelas(parcelas: List<Parcela>)

    @Update
    suspend fun atualizarParcela(parcela: Parcela)

    @Query("SELECT * FROM parcela WHERE id = :id")
    suspend fun buscarParcela(id: Long): Parcela?

    @Query("SELECT * FROM parcela WHERE despesaId = :despesaId ORDER BY numero")
    suspend fun parcelasDaDespesa(despesaId: Long): List<Parcela>

    @Query("SELECT * FROM parcela WHERE status = 'PENDENTE' ORDER BY vencimento")
    fun observarParcelasPendentes(): Flow<List<Parcela>>

    /** Usado para reagendar os avisos quando o celular é reiniciado. */
    @Query("SELECT * FROM parcela WHERE status = 'PENDENTE' AND vencimento >= :aPartirDe ORDER BY vencimento")
    suspend fun parcelasPendentesAPartirDe(aPartirDe: Long): List<Parcela>

    @Query("$SELECT_PARCELA_DETALHADA WHERE parcela.status = 'PENDENTE' AND parcela.vencimento >= :aPartirDe")
    suspend fun pendentesDetalhadasAPartirDe(aPartirDe: Long): List<ParcelaDetalhada>

    @Query("$SELECT_PARCELA_DETALHADA WHERE parcela.id = :id")
    suspend fun buscarParcelaDetalhada(id: Long): ParcelaDetalhada?

    /** Parcelas que vencem no período (a "despesa" do período), por vencimento. */
    @Query("$SELECT_PARCELA_DETALHADA WHERE parcela.vencimento >= :inicio AND parcela.vencimento < :fim ORDER BY parcela.vencimento")
    fun observarParcelasEntre(inicio: Long, fim: Long): Flow<List<ParcelaDetalhada>>

    /** Pendentes que vencem até [ate] (inclui as atrasadas), para o quadro "a pagar". */
    @Query("$SELECT_PARCELA_DETALHADA WHERE parcela.status = 'PENDENTE' AND parcela.vencimento < :ate ORDER BY parcela.vencimento")
    fun observarPendentesAte(ate: Long): Flow<List<ParcelaDetalhada>>

    /** Despesa do período, em centavos: cada parcela conta no mês do seu vencimento. */
    @Query("SELECT COALESCE(SUM(valorCentavos), 0) FROM parcela WHERE vencimento >= :inicio AND vencimento < :fim")
    suspend fun somarDespesaEntre(inicio: Long, fim: Long): Long

    @Query("SELECT COALESCE(SUM(valorCentavos), 0) FROM parcela WHERE vencimento >= :inicio AND vencimento < :fim")
    fun observarDespesaEntre(inicio: Long, fim: Long): Flow<Long>

    @Query(
        """
        SELECT categoria.nome AS categoriaNome, SUM(parcela.valorCentavos) AS totalCentavos
        FROM parcela
        INNER JOIN despesa ON despesa.id = parcela.despesaId
        INNER JOIN categoria ON categoria.id = despesa.categoriaId
        WHERE parcela.vencimento >= :inicio AND parcela.vencimento < :fim
        GROUP BY categoria.id
        ORDER BY totalCentavos DESC
        """,
    )
    fun observarTotalPorCategoria(inicio: Long, fim: Long): Flow<List<TotalPorCategoria>>
}
