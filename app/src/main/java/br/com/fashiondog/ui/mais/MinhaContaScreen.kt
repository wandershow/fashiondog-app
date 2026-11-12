package br.com.fashiondog.ui.mais

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fashiondog.data.repository.ContaRepository
import br.com.fashiondog.domain.ValidacaoDeConta
import br.com.fashiondog.ui.componentes.BotaoPrincipal
import br.com.fashiondog.ui.componentes.CampoTexto
import br.com.fashiondog.ui.componentes.Formulario
import br.com.fashiondog.ui.componentes.Tela
import br.com.fashiondog.ui.componentes.TituloSecao
import br.com.fashiondog.ui.componentes.viewModelDoApp
import br.com.fashiondog.ui.preview.PreviewNoTema
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MinhaContaEstado(
    // Dados
    val nome: String = "",
    val login: String = "",
    val erroDados: String? = null,
    val avisoDados: String? = null,
    val salvandoDados: Boolean = false,
    // Senha
    val senhaAtual: String = "",
    val senhaNova: String = "",
    val confirmacao: String = "",
    val erroSenha: String? = null,
    val avisoSenha: String? = null,
    val salvandoSenha: Boolean = false,
)

class MinhaContaViewModel(private val contas: ContaRepository) : ViewModel() {

    private val _estado = MutableStateFlow(
        contas.usuarioAtual().let { MinhaContaEstado(nome = it?.nome.orEmpty(), login = it?.login.orEmpty()) },
    )
    val estado: StateFlow<MinhaContaEstado> = _estado.asStateFlow()

    fun mudarNome(v: String) = _estado.update { it.copy(nome = v, erroDados = null, avisoDados = null) }
    fun mudarLogin(v: String) = _estado.update { it.copy(login = v, erroDados = null, avisoDados = null) }
    fun mudarSenhaAtual(v: String) = _estado.update { it.copy(senhaAtual = v, erroSenha = null, avisoSenha = null) }
    fun mudarSenhaNova(v: String) = _estado.update { it.copy(senhaNova = v, erroSenha = null, avisoSenha = null) }
    fun mudarConfirmacao(v: String) = _estado.update { it.copy(confirmacao = v, erroSenha = null, avisoSenha = null) }

    fun salvarDados() {
        val e = _estado.value
        _estado.update { it.copy(salvandoDados = true) }
        viewModelScope.launch {
            val resultado = contas.atualizarMinhaConta(e.nome, e.login)
            _estado.update { atual ->
                resultado.fold(
                    { usuario ->
                        atual.copy(
                            salvandoDados = false,
                            nome = usuario.nome,
                            login = usuario.login,
                            avisoDados = "Salvo. Da próxima vez, entre com o login \"${usuario.login}\".",
                        )
                    },
                    { atual.copy(salvandoDados = false, erroDados = it.message) },
                )
            }
        }
    }

    fun trocarSenha() {
        val e = _estado.value
        val erro = ValidacaoDeConta.erroSenha(e.senhaNova) ?: ValidacaoDeConta.erroConfirmacao(e.senhaNova, e.confirmacao)
        if (erro != null) return _estado.update { it.copy(erroSenha = erro) }
        _estado.update { it.copy(salvandoSenha = true) }
        viewModelScope.launch {
            val resultado = contas.trocarMinhaSenha(e.senhaAtual, e.senhaNova)
            _estado.update {
                it.copy(
                    salvandoSenha = false,
                    senhaAtual = "",
                    senhaNova = "",
                    confirmacao = "",
                    erroSenha = resultado.exceptionOrNull()?.message,
                    avisoSenha = if (resultado.isSuccess) "Senha trocada." else null,
                )
            }
        }
    }
}

/** O que a tela pode pedir ao ViewModel. Padrões vazios para o @Preview. */
data class AcoesDaMinhaConta(
    val aoVoltar: () -> Unit = {},
    val aoMudarNome: (String) -> Unit = {},
    val aoMudarLogin: (String) -> Unit = {},
    val aoSalvarDados: () -> Unit = {},
    val aoMudarSenhaAtual: (String) -> Unit = {},
    val aoMudarSenhaNova: (String) -> Unit = {},
    val aoMudarConfirmacao: (String) -> Unit = {},
    val aoTrocarSenha: () -> Unit = {},
)

@Composable
fun MinhaContaScreen(aoVoltar: () -> Unit) {
    val vm = viewModelDoApp { app, _ -> MinhaContaViewModel(app.contas) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    MinhaContaConteudo(
        estado,
        AcoesDaMinhaConta(
            aoVoltar = aoVoltar,
            aoMudarNome = vm::mudarNome,
            aoMudarLogin = vm::mudarLogin,
            aoSalvarDados = vm::salvarDados,
            aoMudarSenhaAtual = vm::mudarSenhaAtual,
            aoMudarSenhaNova = vm::mudarSenhaNova,
            aoMudarConfirmacao = vm::mudarConfirmacao,
            aoTrocarSenha = vm::trocarSenha,
        ),
    )
}

@Composable
fun MinhaContaConteudo(estado: MinhaContaEstado, acoes: AcoesDaMinhaConta) {
    Tela(titulo = "Minha conta", aoVoltar = acoes.aoVoltar) { espacamento ->
        Formulario(espacamento) {
            TituloSecao("Dados")
            CampoTexto(estado.nome, acoes.aoMudarNome, "Nome", maiusculas = KeyboardCapitalization.Words)
            OutlinedTextField(
                value = estado.login,
                onValueChange = acoes.aoMudarLogin,
                label = { Text("Login (usado para entrar)") },
                supportingText = { Text("Letras minúsculas, números, ponto ou _ (3 a 20)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
                modifier = Modifier.fillMaxWidth(),
            )
            estado.erroDados?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            estado.avisoDados?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
            BotaoPrincipal(
                if (estado.salvandoDados) "Salvando..." else "Salvar nome e login",
                acoes.aoSalvarDados,
                habilitado = !estado.salvandoDados,
            )

            TituloSecao("Senha")
            CampoSenha(estado.senhaAtual, acoes.aoMudarSenhaAtual, "Senha atual")
            CampoSenha(estado.senhaNova, acoes.aoMudarSenhaNova, "Nova senha (mín. 6 caracteres)")
            CampoSenha(estado.confirmacao, acoes.aoMudarConfirmacao, "Repita a nova senha")
            estado.erroSenha?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            estado.avisoSenha?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
            BotaoPrincipal(
                if (estado.salvandoSenha) "Salvando..." else "Trocar senha",
                acoes.aoTrocarSenha,
                habilitado = !estado.salvandoSenha,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 1000, name = "Minha conta")
@Composable
private fun PreviewMinhaConta() = PreviewNoTema {
    MinhaContaConteudo(
        MinhaContaEstado(
            nome = "Cintia",
            login = "fashiondog",
            avisoDados = "Salvo. Da próxima vez, entre com o login \"fashiondog\".",
        ),
        AcoesDaMinhaConta(),
    )
}

@Preview(showBackground = true, heightDp = 1000, name = "Login já usado")
@Composable
private fun PreviewMinhaContaErro() = PreviewNoTema {
    MinhaContaConteudo(
        MinhaContaEstado(nome = "Cintia", login = "ana", erroDados = "Este login já existe", erroSenha = "Senha atual incorreta"),
        AcoesDaMinhaConta(),
    )
}
