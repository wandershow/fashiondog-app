package br.com.fashiondog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.com.fashiondog.data.local.entity.Categoria
import br.com.fashiondog.data.local.entity.Despesa
import br.com.fashiondog.data.local.entity.Parcela
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceiroDao {
    @Insert
    suspend fun inserirCategoria(categoria: Categoria): Long

    @Query("SELECT * FROM categoria ORDER BY nome")
    fun observarCategorias(): Flow<List<Categoria>>

    @Insert
    suspend fun inserirDespesa(despesa: Despesa): Long

    @Insert
    suspend fun inserirParcelas(parcelas: List<Parcela>)

    @Update
    suspend fun atualizarParcela(parcela: Parcela)

    @Query("SELECT * FROM parcela WHERE status = 'PENDENTE' ORDER BY vencimento")
    fun observarParcelasPendentes(): Flow<List<Parcela>>

    /** Usado para reagendar os avisos quando o celular é reiniciado. */
    @Query("SELECT * FROM parcela WHERE status = 'PENDENTE' AND vencimento >= :aPartirDe ORDER BY vencimento")
    suspend fun parcelasPendentesAPartirDe(aPartirDe: Long): List<Parcela>

    /** Despesa do período, em centavos: cada parcela conta no mês do seu vencimento. */
    @Query("SELECT COALESCE(SUM(valorCentavos), 0) FROM parcela WHERE vencimento >= :inicio AND vencimento < :fim")
    suspend fun somarDespesaEntre(inicio: Long, fim: Long): Long
}
