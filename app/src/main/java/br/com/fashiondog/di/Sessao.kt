package br.com.fashiondog.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import br.com.fashiondog.data.local.AppDatabase
import br.com.fashiondog.data.local.entity.Perfil
import br.com.fashiondog.data.local.entity.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/** Em que ponto o app está em relação ao login. A tela raiz decide o que mostrar por isto. */
sealed interface EstadoDaSessao {
    data object Carregando : EstadoDaSessao
    /** Ainda não existe proprietário com senha: mostrar a criação da conta. */
    data object PrimeiroAcesso : EstadoDaSessao
    data object Deslogado : EstadoDaSessao
    data class Logado(val usuario: Usuario) : EstadoDaSessao
}

/**
 * Quem está usando o app. (Nas Sprints 2–4 havia uma conta provisória "proprietario" sem
 * senha; no primeiro acesso ela vira a conta real do dono — ver ContaRepository.)
 * O id do usuário logado fica no DataStore (um arquivo pequeno
 * de preferências), então a pessoa continua logada ao fechar e abrir o app.
 */
class Sessao(
    private val db: AppDatabase,
    private val preferencias: DataStore<Preferences>,
) {
    private val _estado = MutableStateFlow<EstadoDaSessao>(EstadoDaSessao.Carregando)
    val estado: StateFlow<EstadoDaSessao> = _estado.asStateFlow()

    val usuarioAtual: Usuario? get() = (_estado.value as? EstadoDaSessao.Logado)?.usuario

    val perfilAtual: Perfil get() = usuarioAtual?.perfil ?: Perfil.FUNCIONARIO

    /** Lê o banco e o DataStore para descobrir o estado inicial. Chamado ao abrir o app. */
    suspend fun carregar() {
        val dao = db.usuarioDao()
        val dono = dao.buscarProprietario()
        if (dono == null || dono.senhaHash.isEmpty()) {
            _estado.value = EstadoDaSessao.PrimeiroAcesso
            return
        }
        val id = preferencias.data.first()[CHAVE_USUARIO]
        val usuario = id?.let { dao.buscarPorId(it) }?.takeIf { it.ativo }
        _estado.value = usuario?.let { EstadoDaSessao.Logado(it) } ?: EstadoDaSessao.Deslogado
    }

    /** Id de quem está registrando (atendimento, agendamento, despesa). */
    fun usuarioAtualId(): Long = checkNotNull(usuarioAtual) { "Nenhum usuário logado" }.id

    suspend fun entrar(usuario: Usuario) {
        preferencias.edit { it[CHAVE_USUARIO] = usuario.id }
        _estado.value = EstadoDaSessao.Logado(usuario)
    }

    suspend fun sair() {
        preferencias.edit { it.remove(CHAVE_USUARIO) }
        _estado.value = EstadoDaSessao.Deslogado
    }

    /** Só apaga o "lembrar login" sem mudar a tela (usado na restauração do backup). */
    suspend fun esquecerLogin() {
        preferencias.edit { it.remove(CHAVE_USUARIO) }
    }

    /** Atualiza os dados do usuário logado (ex.: depois de trocar o nome). */
    fun atualizar(usuario: Usuario) {
        if (usuarioAtual?.id == usuario.id) _estado.value = EstadoDaSessao.Logado(usuario)
    }

    companion object {
        private val CHAVE_USUARIO = longPreferencesKey("usuario_logado")
    }
}
