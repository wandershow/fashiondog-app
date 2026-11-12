package br.com.fashiondog.data.local.model

import androidx.room.Embedded
import br.com.fashiondog.data.local.entity.Agendamento
import br.com.fashiondog.data.local.entity.Atendimento
import br.com.fashiondog.data.local.entity.FormaPagamentoDespesa
import br.com.fashiondog.data.local.entity.Parcela
import br.com.fashiondog.data.local.entity.Pet
import br.com.fashiondog.data.local.entity.Tutor

/**
 * Resultados de consultas que juntam mais de uma tabela.
 * Não são tabelas: o Room só preenche estes objetos a partir de um SELECT.
 */

/** Um pet com o nome e o telefone do tutor (para a lista e a busca). */
data class PetComTutor(
    @Embedded val pet: Pet,
    val tutorNome: String,
    val tutorTelefone: String,
)

/** Um tutor com a quantidade de pets ativos (para a lista de tutores). */
data class TutorComQuantidade(
    @Embedded val tutor: Tutor,
    val quantidadePets: Int,
)

/** Um atendimento com o nome do serviço (para o histórico do pet). */
data class AtendimentoComServico(
    @Embedded val atendimento: Atendimento,
    val servicoNome: String,
)

/** Um atendimento com serviço e pet (para a tela de detalhe). */
data class AtendimentoDetalhado(
    @Embedded val atendimento: Atendimento,
    val servicoNome: String,
    val petNome: String,
)

/** Um agendamento com pet, tutor e serviço (para a agenda e os avisos). */
data class AgendamentoDetalhado(
    @Embedded val agendamento: Agendamento,
    val petNome: String,
    val tutorNome: String,
    val tutorTelefone: String,
    val servicoNome: String,
)

/** Uma parcela com a descrição e a categoria da despesa (para o financeiro e os avisos). */
data class ParcelaDetalhada(
    @Embedded val parcela: Parcela,
    val descricao: String,
    val totalParcelas: Int,
    val formaPagamento: FormaPagamentoDespesa,
    val categoriaNome: String,
)

/** Total de despesa de uma categoria no período. */
data class TotalPorCategoria(
    val categoriaNome: String,
    val totalCentavos: Long,
)
