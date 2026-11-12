package br.com.fashiondog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.com.fashiondog.data.local.entity.Agendamento
import br.com.fashiondog.data.local.entity.Atendimento
import br.com.fashiondog.data.local.entity.Foto
import br.com.fashiondog.data.local.entity.StatusAgendamento
import br.com.fashiondog.data.local.model.AgendamentoDetalhado
import br.com.fashiondog.data.local.model.AtendimentoComServico
import br.com.fashiondog.data.local.model.AtendimentoDetalhado
import kotlinx.coroutines.flow.Flow

/** Início comum das consultas de agendamento com pet, tutor e serviço. */
private const val SELECT_DETALHADO = """
    SELECT agendamento.*, pet.nome AS petNome, tutor.nome AS tutorNome,
           tutor.telefone AS tutorTelefone, servico.nome AS servicoNome
    FROM agendamento
    INNER JOIN pet ON pet.id = agendamento.petId
    INNER JOIN tutor ON tutor.id = pet.tutorId
    INNER JOIN servico ON servico.id = agendamento.servicoId
"""

@Dao
interface AgendamentoDao {
    @Insert
    suspend fun inserir(agendamento: Agendamento): Long

    @Update
    suspend fun atualizar(agendamento: Agendamento)

    /** Agendamentos de um período (um dia ou uma semana), em ordem de horário. */
    @Query("SELECT * FROM agendamento WHERE dataHora >= :inicio AND dataHora < :fim ORDER BY dataHora")
    fun observarEntre(inicio: Long, fim: Long): Flow<List<Agendamento>>

    @Query("SELECT * FROM agendamento WHERE id = :id")
    suspend fun buscarPorId(id: Long): Agendamento?

    @Query("$SELECT_DETALHADO WHERE agendamento.dataHora >= :inicio AND agendamento.dataHora < :fim ORDER BY agendamento.dataHora")
    fun observarDetalhadosEntre(inicio: Long, fim: Long): Flow<List<AgendamentoDetalhado>>

    @Query("$SELECT_DETALHADO WHERE agendamento.id = :id")
    suspend fun buscarDetalhado(id: Long): AgendamentoDetalhado?

    /** Usado para recriar os avisos quando o celular reinicia. */
    @Query("$SELECT_DETALHADO WHERE agendamento.status = 'AGENDADO' AND agendamento.dataHora >= :aPartirDe")
    suspend fun futurosAgendados(aPartirDe: Long): List<AgendamentoDetalhado>

    @Query("UPDATE agendamento SET status = :status WHERE id = :id")
    suspend fun mudarStatus(id: Long, status: StatusAgendamento)
}

@Dao
interface AtendimentoDao {
    @Insert
    suspend fun inserir(atendimento: Atendimento): Long

    @Update
    suspend fun atualizar(atendimento: Atendimento)

    @Query("SELECT * FROM atendimento WHERE id = :id")
    suspend fun buscarPorId(id: Long): Atendimento?

    /** O "último banho" que aparece em destaque na ficha do pet. */
    @Query("SELECT * FROM atendimento WHERE petId = :petId ORDER BY data DESC LIMIT 1")
    suspend fun ultimoDoPet(petId: Long): Atendimento?

    @Query("SELECT * FROM atendimento WHERE petId = :petId ORDER BY data DESC")
    fun observarHistorico(petId: Long): Flow<List<Atendimento>>

    /** Histórico completo do pet já com o nome do serviço (o mais recente primeiro). */
    @Query(
        """
        SELECT atendimento.*, servico.nome AS servicoNome
        FROM atendimento INNER JOIN servico ON servico.id = atendimento.servicoId
        WHERE atendimento.petId = :petId
        ORDER BY atendimento.data DESC
        """,
    )
    fun observarHistoricoComServico(petId: Long): Flow<List<AtendimentoComServico>>

    @Query(
        """
        SELECT atendimento.*, servico.nome AS servicoNome, pet.nome AS petNome
        FROM atendimento
        INNER JOIN servico ON servico.id = atendimento.servicoId
        INNER JOIN pet ON pet.id = atendimento.petId
        WHERE atendimento.id = :id
        """,
    )
    fun observarDetalhado(id: Long): Flow<AtendimentoDetalhado?>

    /** Receita do período, em centavos. */
    @Query("SELECT COALESCE(SUM(valorCentavos), 0) FROM atendimento WHERE data >= :inicio AND data < :fim")
    suspend fun somarReceitaEntre(inicio: Long, fim: Long): Long

    /** Receita do período que se atualiza sozinha (para a tela do financeiro). */
    @Query("SELECT COALESCE(SUM(valorCentavos), 0) FROM atendimento WHERE data >= :inicio AND data < :fim")
    fun observarReceitaEntre(inicio: Long, fim: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM atendimento WHERE data >= :inicio AND data < :fim")
    fun observarQuantidadeEntre(inicio: Long, fim: Long): Flow<Int>

    @Insert
    suspend fun inserirFoto(foto: Foto): Long

    @Delete
    suspend fun excluirFoto(foto: Foto)

    @Query("SELECT * FROM foto WHERE atendimentoId = :atendimentoId ORDER BY tipo")
    suspend fun fotosDoAtendimento(atendimentoId: Long): List<Foto>

    @Query("SELECT * FROM foto WHERE atendimentoId = :atendimentoId ORDER BY tipo")
    fun observarFotos(atendimentoId: Long): Flow<List<Foto>>

    /** Usado para impedir a terceira foto (o limite é 2 por atendimento). */
    @Query("SELECT COUNT(*) FROM foto WHERE atendimentoId = :atendimentoId")
    suspend fun contarFotos(atendimentoId: Long): Int
}
