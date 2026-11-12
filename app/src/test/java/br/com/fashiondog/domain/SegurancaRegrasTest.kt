package br.com.fashiondog.domain

import br.com.fashiondog.data.local.entity.Perfil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HashDeSenhaTest {

    // Poucas repetições nos testes só para rodar rápido; o app usa 120.000.
    private val rapido = 1_000

    @Test
    fun `senha certa confere e errada nao`() {
        val guardado = HashDeSenha.gerar("banho123", rapido)
        assertTrue(HashDeSenha.confere("banho123", guardado))
        assertFalse(HashDeSenha.confere("Banho123", guardado))
        assertFalse(HashDeSenha.confere("", guardado))
    }

    @Test
    fun `o hash nao contem a senha`() {
        assertFalse(HashDeSenha.gerar("banho123", rapido).contains("banho123"))
    }

    @Test
    fun `mesma senha gera hashes diferentes por causa do sal`() {
        assertNotEquals(HashDeSenha.gerar("banho123", rapido), HashDeSenha.gerar("banho123", rapido))
    }

    @Test
    fun `valor guardado invalido nunca confere`() {
        assertFalse(HashDeSenha.confere("x", ""))
        assertFalse(HashDeSenha.confere("x", "texto-qualquer"))
        assertFalse(HashDeSenha.confere("x", "pbkdf2\$abc\$!!\$!!"))
    }

    @Test
    fun `formato gravado`() {
        val partes = HashDeSenha.gerar("x", rapido).split('$')
        assertEquals(4, partes.size)
        assertEquals("pbkdf2", partes[0])
        assertEquals("1000", partes[1])
    }
}

class CodigoDeRecuperacaoTest {

    @Test
    fun `codigo tem tres blocos de quatro sem letras ambiguas`() {
        repeat(50) {
            val codigo = CodigoDeRecuperacao.gerar()
            assertTrue(codigo, Regex("^[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}$").matches(codigo))
            assertTrue(codigo.none { it in "01OIL" })
        }
    }

    @Test
    fun `aceita o codigo digitado de qualquer jeito`() {
        assertEquals("K7QM-3XRP-T9WD", CodigoDeRecuperacao.normalizar("k7qm 3xrp t9wd"))
        assertEquals("K7QM-3XRP-T9WD", CodigoDeRecuperacao.normalizar("K7QM3XRPT9WD"))
    }
}

class ValidacaoDeContaTest {

    @Test
    fun `login valido e normalizado`() {
        assertEquals("maria", ValidacaoDeConta.normalizarLogin("  Maria "))
        assertNull(ValidacaoDeConta.erroLogin("joao.silva"))
        assertNotNull(ValidacaoDeConta.erroLogin("jo"))
        assertNotNull(ValidacaoDeConta.erroLogin("joão"))
        assertNotNull(ValidacaoDeConta.erroLogin("maria souza"))
    }

    @Test
    fun `mudar o login corrige erro de digitacao`() {
        // ninguém usa "fashiondog": pode trocar
        assertNull(ValidacaoDeConta.erroNovoLogin("fashiondog", meuId = 1, idDoDonoDoLogin = null))
        // manter o próprio login também pode
        assertNull(ValidacaoDeConta.erroNovoLogin("fashiodog", meuId = 1, idDoDonoDoLogin = 1))
    }

    @Test
    fun `nao pode pegar o login de outro usuario nem login invalido`() {
        assertNotNull(ValidacaoDeConta.erroNovoLogin("ana", meuId = 1, idDoDonoDoLogin = 2))
        assertNotNull(ValidacaoDeConta.erroNovoLogin("fashion dog", meuId = 1, idDoDonoDoLogin = null))
    }

    @Test
    fun `nome em branco`() {
        assertNotNull(ValidacaoDeConta.erroNome("  "))
        assertNull(ValidacaoDeConta.erroNome("Cintia"))
    }

    @Test
    fun `senha curta e confirmacao diferente`() {
        assertNotNull(ValidacaoDeConta.erroSenha("12345"))
        assertNull(ValidacaoDeConta.erroSenha("123456"))
        assertNotNull(ValidacaoDeConta.erroConfirmacao("123456", "123457"))
        assertNull(ValidacaoDeConta.erroConfirmacao("123456", "123456"))
    }
}

class PermissoesTest {

    private val hoje = LocalDate.of(2026, 11, 12)

    @Test
    fun `proprietario pode tudo`() {
        val dono = Perfil.PROPRIETARIO
        assertTrue(Permissoes.veFinanceiro(dono))
        assertTrue(Permissoes.fazBackup(dono))
        assertTrue(Permissoes.gerenciaUsuarios(dono))
        assertTrue(Permissoes.alteraCadastroExistente(dono))
        assertTrue(Permissoes.editaAtendimento(dono, hoje.minusDays(30), hoje))
    }

    @Test
    fun `funcionario nao ve financeiro backup nem usuarios`() {
        val func = Perfil.FUNCIONARIO
        assertFalse(Permissoes.veFinanceiro(func))
        assertFalse(Permissoes.fazBackup(func))
        assertFalse(Permissoes.gerenciaUsuarios(func))
        assertFalse(Permissoes.alteraPrecos(func))
        assertFalse(Permissoes.alteraCadastroExistente(func))
    }

    @Test
    fun `funcionario so corrige atendimento de hoje`() {
        assertTrue(Permissoes.editaAtendimento(Perfil.FUNCIONARIO, hoje, hoje))
        assertFalse(Permissoes.editaAtendimento(Perfil.FUNCIONARIO, hoje.minusDays(1), hoje))
    }
}

class ValidadorDeBackupTest {

    @Test
    fun `aceita o que o app exporta`() {
        assertNull(
            ValidadorDeBackup.erroNoConteudo(
                listOf("fashiondog.db", "backup-info.txt", "fotos/", "fotos/0f8e2c1a-1b2c-4d5e-8f90-123456789abc.jpg"),
            ),
        )
    }

    @Test
    fun `recusa caminhos que saem da pasta (zip slip)`() {
        assertFalse(ValidadorDeBackup.entradaPermitida("../shared_prefs/sessao.xml"))
        assertFalse(ValidadorDeBackup.entradaPermitida("fotos/../../fashiondog.db"))
        assertFalse(ValidadorDeBackup.entradaPermitida("/data/data/x.db"))
        assertNotNull(ValidadorDeBackup.erroNoConteudo(listOf("fashiondog.db", "fotos/../x.jpg")))
    }

    @Test
    fun `backup sem banco e invalido`() {
        assertNotNull(ValidadorDeBackup.erroNoConteudo(listOf("fotos/abc.jpg")))
    }

    @Test
    fun `backup de versao mais nova e recusado`() {
        assertNotNull(ValidadorDeBackup.erroDeVersao(versaoDoBackup = 3, versaoDoApp = 2))
        assertNull(ValidadorDeBackup.erroDeVersao(versaoDoBackup = 1, versaoDoApp = 2))
    }

    @Test
    fun `nome do arquivo leva a data`() {
        assertEquals("fashiondog-backup-2026-11-12.zip", ValidadorDeBackup.nomeDoArquivo(LocalDate.of(2026, 11, 12)))
    }
}
