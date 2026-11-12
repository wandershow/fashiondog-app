package br.com.fashiondog.data.repository

import br.com.fashiondog.data.local.AppDatabase
import br.com.fashiondog.data.local.entity.Perfil
import br.com.fashiondog.data.local.entity.Usuario
import br.com.fashiondog.di.Sessao
import br.com.fashiondog.domain.CodigoDeRecuperacao
import br.com.fashiondog.domain.HashDeSenha
import br.com.fashiondog.domain.ValidacaoDeConta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Resultado de criar a conta do dono ou recuperar a senha: o código novo a ser anotado. */
data class ContaComCodigo(val usuario: Usuario, val codigo: String)

/**
 * Contas, login e recuperação de senha. O cálculo do hash é propositalmente lento
 * (contra força bruta), por isso roda fora da thread da tela (`Dispatchers.Default`).
 */
class ContaRepository(
    db: AppDatabase,
    private val sessao: Sessao,
) {
    private val dao = db.usuarioDao()

    fun observarUsuarios(): Flow<List<Usuario>> = dao.observarTodos()

    /**
     * Primeiro acesso: cria o proprietário (ou transforma a conta provisória nele) e
     * devolve o código de recuperação. Ainda NÃO entra no app: a tela mostra o código
     * primeiro e chama [entrarCom] quando a pessoa confirma que anotou.
     */
    suspend fun criarProprietario(nome: String, login: String, senha: String): Result<ContaComCodigo> =
        withContext(Dispatchers.Default) {
            val loginLimpo = ValidacaoDeConta.normalizarLogin(login)
            val existente = dao.buscarProprietario()
            val ocupado = dao.buscarQualquerPorLogin(loginLimpo)
            if (ocupado != null && ocupado.id != existente?.id) {
                return@withContext Result.failure(IllegalArgumentException("Este login já existe"))
            }
            val codigo = CodigoDeRecuperacao.gerar()
            val dono = (existente ?: Usuario(nome = "", login = "", senhaHash = "", perfil = Perfil.PROPRIETARIO)).copy(
                nome = nome.trim(),
                login = loginLimpo,
                senhaHash = HashDeSenha.gerar(senha),
                perfil = Perfil.PROPRIETARIO,
                ativo = true,
                codigoRecuperacaoHash = HashDeSenha.gerar(codigo),
            )
            val salvo = if (dono.id == 0L) dono.copy(id = dao.inserir(dono)) else dono.also { dao.atualizar(it) }
            Result.success(ContaComCodigo(salvo, codigo))
        }

    suspend fun entrarCom(usuario: Usuario) = sessao.entrar(usuario)

    /** Confere login e senha. A mensagem de erro é a mesma nos dois casos, de propósito. */
    suspend fun entrar(login: String, senha: String): Result<Usuario> = withContext(Dispatchers.Default) {
        val usuario = dao.buscarPorLogin(ValidacaoDeConta.normalizarLogin(login))
        if (usuario == null || !HashDeSenha.confere(senha, usuario.senhaHash)) {
            Result.failure(IllegalArgumentException("Login ou senha incorretos"))
        } else {
            sessao.entrar(usuario)
            Result.success(usuario)
        }
    }

    /**
     * "Esqueci a senha" (só o proprietário): com o código certo, grava a senha nova e
     * gera um código novo — cada código vale uma vez só.
     */
    suspend fun recuperarSenha(login: String, codigo: String, novaSenha: String): Result<ContaComCodigo> =
        withContext(Dispatchers.Default) {
            val dono = dao.buscarProprietario()
            val codigoGuardado = dono?.codigoRecuperacaoHash
            val confere = dono != null && codigoGuardado != null &&
                dono.login == ValidacaoDeConta.normalizarLogin(login) &&
                HashDeSenha.confere(CodigoDeRecuperacao.normalizar(codigo), codigoGuardado)
            if (!confere) {
                return@withContext Result.failure(IllegalArgumentException("Login ou código de recuperação incorretos"))
            }
            val novoCodigo = CodigoDeRecuperacao.gerar()
            val atualizado = checkNotNull(dono).copy(
                senhaHash = HashDeSenha.gerar(novaSenha),
                codigoRecuperacaoHash = HashDeSenha.gerar(novoCodigo),
            )
            dao.atualizar(atualizado)
            Result.success(ContaComCodigo(atualizado, novoCodigo))
        }

    suspend fun criarFuncionario(nome: String, login: String, senha: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            val loginLimpo = ValidacaoDeConta.normalizarLogin(login)
            if (dao.buscarQualquerPorLogin(loginLimpo) != null) {
                return@withContext Result.failure(IllegalArgumentException("Este login já existe"))
            }
            dao.inserir(
                Usuario(nome = nome.trim(), login = loginLimpo, senhaHash = HashDeSenha.gerar(senha), perfil = Perfil.FUNCIONARIO),
            )
            Result.success(Unit)
        }

    /** Usuário nunca é apagado (o histórico aponta para ele): só desativado. */
    suspend fun definirAtivo(usuario: Usuario, ativo: Boolean) = dao.atualizar(usuario.copy(ativo = ativo))

    /** O proprietário define uma senha nova para o funcionário que esqueceu a dele. */
    suspend fun redefinirSenha(usuario: Usuario, novaSenha: String) = withContext(Dispatchers.Default) {
        dao.atualizar(usuario.copy(senhaHash = HashDeSenha.gerar(novaSenha)))
    }

    fun usuarioAtual(): Usuario? = sessao.usuarioAtual

    /** Muda o nome e o login de quem está logado (ex.: corrigir um erro de digitação). */
    suspend fun atualizarMinhaConta(nome: String, login: String): Result<Usuario> = withContext(Dispatchers.Default) {
        val eu = sessao.usuarioAtual?.let { dao.buscarPorId(it.id) }
            ?: return@withContext Result.failure(IllegalStateException("Nenhum usuário logado"))
        val loginLimpo = ValidacaoDeConta.normalizarLogin(login)
        val erro = ValidacaoDeConta.erroNome(nome)
            ?: ValidacaoDeConta.erroNovoLogin(loginLimpo, eu.id, dao.buscarQualquerPorLogin(loginLimpo)?.id)
        if (erro != null) return@withContext Result.failure(IllegalArgumentException(erro))

        val atualizado = eu.copy(nome = nome.trim(), login = loginLimpo)
        dao.atualizar(atualizado)
        sessao.atualizar(atualizado)
        Result.success(atualizado)
    }

    suspend fun trocarMinhaSenha(senhaAtual: String, novaSenha: String): Result<Unit> = withContext(Dispatchers.Default) {
        val eu = sessao.usuarioAtual?.let { dao.buscarPorId(it.id) }
        if (eu == null || !HashDeSenha.confere(senhaAtual, eu.senhaHash)) {
            return@withContext Result.failure(IllegalArgumentException("Senha atual incorreta"))
        }
        val atualizado = eu.copy(senhaHash = HashDeSenha.gerar(novaSenha))
        dao.atualizar(atualizado)
        sessao.atualizar(atualizado)
        Result.success(Unit)
    }

    suspend fun sair() = sessao.sair()
}
