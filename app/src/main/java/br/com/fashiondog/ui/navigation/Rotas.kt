package br.com.fashiondog.ui.navigation

/**
 * Endereços (rotas) das telas. Cada tela tem um "molde" com parâmetros entre
 * chaves, usado no NavHost, e uma função que monta a rota com os valores.
 */
object Rotas {
    const val TUTOR_FORM = "tutor/form?tutorId={tutorId}"
    fun tutorForm(tutorId: Long? = null) = "tutor/form?tutorId=${tutorId ?: -1}"

    const val TUTOR = "tutor/{tutorId}"
    fun tutor(tutorId: Long) = "tutor/$tutorId"

    const val PET_FORM = "pet/form?petId={petId}&tutorId={tutorId}"
    fun novoPet(tutorId: Long) = "pet/form?petId=-1&tutorId=$tutorId"
    fun editarPet(petId: Long) = "pet/form?petId=$petId&tutorId=-1"

    const val PET = "pet/{petId}"
    fun pet(petId: Long) = "pet/$petId"

    const val ATENDIMENTO_FORM =
        "atendimento/form?petId={petId}&atendimentoId={atendimentoId}&agendamentoId={agendamentoId}&servicoId={servicoId}"
    fun novoAtendimento(petId: Long, agendamentoId: Long? = null, servicoId: Long? = null) =
        "atendimento/form?petId=$petId&atendimentoId=-1&agendamentoId=${agendamentoId ?: -1}&servicoId=${servicoId ?: -1}"
    fun editarAtendimento(atendimentoId: Long) =
        "atendimento/form?petId=-1&atendimentoId=$atendimentoId&agendamentoId=-1&servicoId=-1"

    const val ATENDIMENTO = "atendimento/{atendimentoId}"
    fun atendimento(atendimentoId: Long) = "atendimento/$atendimentoId"

    const val SERVICOS = "mais/servicos"
    const val AVISOS = "mais/avisos"

    const val AGENDAMENTO_FORM = "agendamento/form?agendamentoId={agendamentoId}&dia={dia}"
    /** [dia] vai como "epoch day" (número de dias desde 1970), um Long simples de passar na rota. */
    fun novoAgendamento(dia: java.time.LocalDate) = "agendamento/form?agendamentoId=-1&dia=${dia.toEpochDay()}"
    fun editarAgendamento(id: Long) = "agendamento/form?agendamentoId=$id&dia=-1"
}
