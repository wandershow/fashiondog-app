package br.com.fashiondog.ui.mais

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fashiondog.data.repository.ContaRepository
import br.com.fashiondog.domain.ValidacaoDeConta
import br.com.fashiondog.ui.componentes.BotaoPrincipal
import br.com.fashiondog.ui.componentes.Formulario
import br.com.fashiondog.ui.componentes.Tela
import br.com.fashiondog.ui.componentes.viewModelDoApp
import br.com.fashiondog.ui.preview.PreviewNoTema
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrocarSenhaEstado(
    val atual: String = "",
    val nova: String = "",
    val confirmacao: String = "",
    val erro: String? = null,
    val salvando: Boolean = false,
    val concluido: Boolean = false,
)

class TrocarSenhaViewModel(private val contas: ContaRepository) : ViewModel() {
    private val _estado = MutableStateFlow(TrocarSenhaEstado())
    val estado: StateFlow<TrocarSenhaEstado> = _estado.asStateFlow()

    fun mudarAtual(v: String) = _estado.update { it.copy(atual = v, erro = null) }
    fun mudarNova(v: String) = _estado.update { it.copy(nova = v, erro = null) }
    fun mudarConfirmacao(v: String) = _estado.update { it.copy(confirmacao = v, erro = null) }

    fun salvar() {
        val e = _estado.value
        val erro = ValidacaoDeConta.erroSenha(e.nova) ?: ValidacaoDeConta.erroConfirmacao(e.nova, e.confirmacao)
        if (erro != null) return _estado.update { it.copy(erro = erro) }
        _estado.update { it.copy(salvando = true) }
        viewModelScope.launch {
            val resultado = contas.trocarMinhaSenha(e.atual, e.nova)
            _estado.update {
                TrocarSenhaEstado(erro = resultado.exceptionOrNull()?.message, concluido = resultado.isSuccess)
            }
        }
    }
}

@Composable
fun TrocarSenhaScreen(aoVoltar: () -> Unit) {
    val vm = viewModelDoApp { app, _ -> TrocarSenhaViewModel(app.contas) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    TrocarSenhaConteudo(estado, aoVoltar, vm::mudarAtual, vm::mudarNova, vm::mudarConfirmacao, vm::salvar)
}

@Composable
fun TrocarSenhaConteudo(
    estado: TrocarSenhaEstado,
    aoVoltar: () -> Unit,
    aoMudarAtual: (String) -> Unit,
    aoMudarNova: (String) -> Unit,
    aoMudarConfirmacao: (String) -> Unit,
    aoSalvar: () -> Unit,
) {
    Tela(titulo = "Trocar minha senha", aoVoltar = aoVoltar) { espacamento ->
        Formulario(espacamento) {
            CampoSenha(estado.atual, aoMudarAtual, "Senha atual")
            CampoSenha(estado.nova, aoMudarNova, "Nova senha (mín. 6 caracteres)")
            CampoSenha(estado.confirmacao, aoMudarConfirmacao, "Repita a nova senha")
            estado.erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (estado.concluido) Text("Senha trocada.", style = MaterialTheme.typography.titleSmall)
            BotaoPrincipal(if (estado.salvando) "Salvando..." else "Salvar", aoSalvar, habilitado = !estado.salvando)
        }
    }
}

@Preview(showBackground = true, name = "Trocar senha")
@Composable
private fun PreviewTrocarSenha() = PreviewNoTema {
    TrocarSenhaConteudo(TrocarSenhaEstado(erro = "Senha atual incorreta"), {}, {}, {}, {}, {})
}
