package br.com.fashiondog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.com.fashiondog.data.local.entity.Agendamento
import br.com.fashiondog.data.local.entity.Atendimento
import br.com.fashiondog.data.local.entity.Foto
import kotlinx.coroutines.flow.Flow

@Dao
interface AgendamentoDao {
    @Insert
    suspend fun inserir(agendamento: Agendamento): Long

    @Update
    suspend fun atualizar(agendamento: Agendamento)

    /** Agendamentos de um período (um dia ou uma semana), em ordem de horário. */
    @Query("SELECT * FROM agendamento WHERE dataHora >= :inicio AND dataHora < :fim ORDER BY dataHora")
    fun observarEntre(inicio: Long, fim: Long): Flow<List<Agendamento>>
}

@Dao
interface AtendimentoDao {
    @Insert
    suspend fun inserir(atendimento: Atendimento): Long

    /** O "último banho" que aparece em destaque na ficha do pet. */
    @Query("SELECT * FROM atendimento WHERE petId = :petId ORDER BY data DESC LIMIT 1")
    suspend fun ultimoDoPet(petId: Long): Atendimento?

    @Query("SELECT * FROM atendimento WHERE petId = :petId ORDER BY data DESC")
    fun observarHistorico(petId: Long): Flow<List<Atendimento>>

    /** Receita do período, em centavos. */
    @Query("SELECT COALESCE(SUM(valorCentavos), 0) FROM atendimento WHERE data >= :inicio AND data < :fim")
    suspend fun somarReceitaEntre(inicio: Long, fim: Long): Long

    @Insert
    suspend fun inserirFoto(foto: Foto): Long

    @Query("SELECT * FROM foto WHERE atendimentoId = :atendimentoId ORDER BY tipo")
    suspend fun fotosDoAtendimento(atendimentoId: Long): List<Foto>

    /** Usado para impedir a terceira foto (o limite é 2 por atendimento). */
    @Query("SELECT COUNT(*) FROM foto WHERE atendimentoId = :atendimentoId")
    suspend fun contarFotos(atendimentoId: Long): Int
}
