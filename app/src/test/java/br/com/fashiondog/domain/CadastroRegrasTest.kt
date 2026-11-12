package br.com.fashiondog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TelefoneTest {

    @Test
    fun `formata celular com 11 digitos`() {
        assertEquals("(53) 99999-8888", Telefone.formatar("53999998888"))
    }

    @Test
    fun `formata fixo com 10 digitos`() {
        assertEquals("(53) 3222-1111", Telefone.formatar("5332221111"))
    }

    @Test
    fun `ignora pontuacao digitada`() {
        assertEquals("53999998888", Telefone.somenteDigitos("(53) 99999-8888"))
        assertTrue(Telefone.ehValido("(53) 99999-8888"))
    }

    @Test
    fun `numero sem DDD e invalido`() {
        assertFalse(Telefone.ehValido("99999-8888"))
    }

    @Test
    fun `numero do whatsapp leva codigo do Brasil`() {
        assertEquals("5553999998888", Telefone.paraWhatsApp("(53) 99999-8888"))
    }
}

class BuscaTest {

    @Test
    fun `termo vazio mostra tudo`() {
        assertTrue(Busca.corresponde("", "Rex"))
    }

    @Test
    fun `ignora maiusculas e acentos`() {
        assertTrue(Busca.corresponde("boris", "Bóris"))
        assertTrue(Busca.corresponde("JOÃO", "joao silva"))
    }

    @Test
    fun `procura em qualquer campo`() {
        assertTrue(Busca.corresponde("maria", "Rex", "Poodle", "Maria Souza"))
    }

    @Test
    fun `numero procura nos digitos do telefone`() {
        assertTrue(Busca.corresponde("9999-8", "53999998888"))
        assertFalse(Busca.corresponde("1234", "53999998888"))
    }

    @Test
    fun `campos nulos nao quebram a busca`() {
        assertFalse(Busca.corresponde("rex", null, "Thor"))
    }
}

class ValidacaoCadastroTest {

    @Test
    fun `nome em branco gera erro`() {
        assertNotNull(ValidacaoCadastro.erroNomeTutor("   "))
        assertNotNull(ValidacaoCadastro.erroNomePet(""))
    }

    @Test
    fun `dados validos nao geram erro`() {
        assertNull(ValidacaoCadastro.erroNomeTutor("Maria"))
        assertNull(ValidacaoCadastro.erroTelefone("53 99999-8888"))
        assertNull(ValidacaoCadastro.erroNomePet("Rex"))
    }

    @Test
    fun `telefone curto gera erro`() {
        assertNotNull(ValidacaoCadastro.erroTelefone("9999"))
    }
}
