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
}
