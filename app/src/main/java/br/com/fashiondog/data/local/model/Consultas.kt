package br.com.fashiondog.data.local.model

import androidx.room.Embedded
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
