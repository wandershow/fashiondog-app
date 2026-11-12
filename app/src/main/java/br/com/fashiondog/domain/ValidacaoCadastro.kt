package br.com.fashiondog.domain

/**
 * Regras dos formulários de cadastro. Cada função devolve a mensagem de erro
 * para mostrar embaixo do campo, ou null quando está tudo certo.
 */
object ValidacaoCadastro {

    fun erroNomeTutor(nome: String): String? =
        if (nome.isBlank()) "Informe o nome do tutor" else null

    fun erroTelefone(telefone: String): String? =
        if (!Telefone.ehValido(telefone)) "Informe o telefone com DDD (10 ou 11 dígitos)" else null

    fun erroNomePet(nome: String): String? =
        if (nome.isBlank()) "Informe o nome do pet" else null
}
