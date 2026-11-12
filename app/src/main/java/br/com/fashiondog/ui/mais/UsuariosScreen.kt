package br.com.fashiondog.ui.mais

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fashiondog.data.local.entity.Perfil
import br.com.fashiondog.data.local.entity.Usuario
import br.com.fashiondog.data.repository.ContaRepository
import br.com.fashiondog.domain.ValidacaoDeConta
import br.com.fashiondog.ui.componentes.CampoTexto
import br.com.fashiondog.ui.componentes.Formulario
import br.com.fashiondog.ui.componentes.Tela
import br.com.fashiondog.ui.componentes.viewModelDoApp
import br.com.fashiondog.ui.preview.PreviewNoTema
import br.com.fashiondog.ui.theme.Tinta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UsuariosViewModel(private val contas: ContaRepository) : ViewModel() {

    val usuarios: StateFlow<List<Usuario>> = contas.observarUsuarios()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _mensagem = MutableStateFlow<String?>(null)
    val mensagem: StateFlow<String?> = _mensagem.asStateFlow()

    fun criarFuncionario(nome: String, login: String, senha: String) {
        viewModelScope.launch {
            _mensagem.value = contas.criarFuncionario(nome, login, senha)
                .fold({ "Funcionário criado" }, { it.message })
        }
    }

    fun definirAtivo(usuario: Usuario, ativo: Boolean) {
        viewModelScope.launch { contas.definirAtivo(usuario, ativo) }
    }

    fun redefinirSenha(usuario: Usuario, senha: String) {
        viewModelScope.launch {
            contas.redefinirSenha(usuario, senha)
            _mensagem.value = "Senha de ${usuario.nome} redefinida"
        }
    }

    fun mensagemVista() {
        _mensagem.value = null
    }
}

@Composable
fun UsuariosScreen(meuId: Long, aoVoltar: () -> Unit) {
    val vm = viewModelDoApp { app, _ -> UsuariosViewModel(app.contas) }
    val usuarios by vm.usuarios.collectAsStateWithLifecycle()
    val mensagem by vm.mensagem.collectAsStateWithLifecycle()
    UsuariosConteudo(
        usuarios = usuarios,
        meuId = meuId,
        mensagem = mensagem,
        aoVoltar = aoVoltar,
        aoCriar = vm::criarFuncionario,
        aoDefinirAtivo = vm::definirAtivo,
        aoRedefinirSenha = vm::redefinirSenha,
        aoMensagemVista = vm::mensagemVista,
    )
}

/** Qual diálogo está aberto: criar funcionário, ações de um usuário ou nova senha. */
private sealed interface Dialogo {
    data object Novo : Dialogo
    data class Acoes(val usuario: Usuario) : Dialogo
    data class NovaSenha(val usuario: Usuario) : Dialogo
}

@Composable
fun UsuariosConteudo(
    usuarios: List<Usuario>,
    meuId: Long,
    mensagem: String?,
    aoVoltar: () -> Unit,
    aoCriar: (nome: String, login: String, senha: String) -> Unit,
    aoDefinirAtivo: (Usuario, Boolean) -> Unit,
    aoRedefinirSenha: (Usuario, String) -> Unit,
    aoMensagemVista: () -> Unit,
) {
    var dialogo by remember { mutableStateOf<Dialogo?>(null) }

    Tela(
        titulo = "Usuários",
        aoVoltar = aoVoltar,
        botaoFlutuante = {
            ExtendedFloatingActionButton(
                onClick = { dialogo = Dialogo.Novo },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Novo funcionário") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Tinta,
            )
        },
    ) { espacamento ->
        Formulario(espacamento) {
            Text(
                "Funcionários usam agenda, pets e atendimentos. Não veem o financeiro nem o backup " +
                    "e não alteram registros antigos. Usuário não é apagado: fica desativado.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            mensagem?.let {
                Text(it, style = MaterialTheme.typography.titleSmall)
            }
            usuarios.forEach { usuario ->
                ListItem(
                    headlineContent = { Text(usuario.nome + if (usuario.id == meuId) " (você)" else "") },
                    supportingContent = {
                        Text(
                            "${usuario.login} · " +
                                (if (usuario.perfil == Perfil.PROPRIETARIO) "Proprietário" else "Funcionário") +
                                if (!usuario.ativo) " · desativado" else "",
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                    // O proprietário não mexe na própria conta por aqui (usa "Trocar minha senha").
                    modifier = if (usuario.id != meuId) Modifier.clickable {
                        aoMensagemVista()
                        dialogo = Dialogo.Acoes(usuario)
                    } else Modifier,
                )
                HorizontalDivider()
            }
        }
    }

    when (val atual = dialogo) {
        Dialogo.Novo -> DialogoNovoFuncionario(
            aoCriar = { nome, login, senha ->
                aoCriar(nome, login, senha)
                dialogo = null
            },
            aoFechar = { dialogo = null },
        )
        is Dialogo.Acoes -> AlertDialog(
            onDismissRequest = { dialogo = null },
            title = { Text(atual.usuario.nome) },
            text = { Text("Login: ${atual.usuario.login}") },
            confirmButton = {
                TextButton(onClick = { dialogo = Dialogo.NovaSenha(atual.usuario) }) { Text("Definir nova senha") }
            },
            dismissButton = {
                TextButton(onClick = {
                    aoDefinirAtivo(atual.usuario, !atual.usuario.ativo)
                    dialogo = null
                }) { Text(if (atual.usuario.ativo) "Desativar" else "Reativar") }
            },
        )
        is Dialogo.NovaSenha -> DialogoSenha(
            titulo = "Nova senha de ${atual.usuario.nome}",
            aoConfirmar = {
                aoRedefinirSenha(atual.usuario, it)
                dialogo = null
            },
            aoFechar = { dialogo = null },
        )
        null -> Unit
    }
}

@Composable
private fun DialogoNovoFuncionario(aoCriar: (String, String, String) -> Unit, aoFechar: () -> Unit) {
    var nome by remember { mutableStateOf("") }
    var login by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    val erroLogin = login.takeIf { it.isNotEmpty() }?.let(ValidacaoDeConta::erroLogin)
    val erroSenha = senha.takeIf { it.isNotEmpty() }?.let(ValidacaoDeConta::erroSenha)
    val valido = nome.isNotBlank() && login.isNotEmpty() && senha.isNotEmpty() && erroLogin == null && erroSenha == null

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Novo funcionário") },
        text = {
            Column {
                CampoTexto(nome, { nome = it }, "Nome", maiusculas = KeyboardCapitalization.Words)
                CampoTexto(login, { login = it }, "Login", erro = erroLogin, maiusculas = KeyboardCapitalization.None)
                CampoSenha(senha, { senha = it }, "Senha inicial", erroSenha)
            }
        },
        confirmButton = { TextButton(onClick = { aoCriar(nome, login, senha) }, enabled = valido) { Text("Criar") } },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
    )
}

@Composable
fun DialogoSenha(titulo: String, aoConfirmar: (String) -> Unit, aoFechar: () -> Unit) {
    var senha by remember { mutableStateOf("") }
    val erro = senha.takeIf { it.isNotEmpty() }?.let(ValidacaoDeConta::erroSenha)
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(titulo) },
        text = { CampoSenha(senha, { senha = it }, "Nova senha", erro) },
        confirmButton = {
            TextButton(onClick = { aoConfirmar(senha) }, enabled = senha.isNotEmpty() && erro == null) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
    )
}

@Composable
fun CampoSenha(valor: String, aoMudar: (String) -> Unit, rotulo: String, erro: String? = null) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        isError = erro != null,
        supportingText = erro?.let { { Text(it) } },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview(showBackground = true, name = "Usuários")
@Composable
private fun PreviewUsuarios() = PreviewNoTema {
    UsuariosConteudo(
        usuarios = listOf(
            Usuario(1, "Maria Souza", "maria", "", Perfil.PROPRIETARIO),
            Usuario(2, "Ana Lima", "ana", "", Perfil.FUNCIONARIO),
            Usuario(3, "Pedro Rocha", "pedro", "", Perfil.FUNCIONARIO, ativo = false),
        ),
        meuId = 1,
        mensagem = "Funcionário criado",
        aoVoltar = {}, aoCriar = { _, _, _ -> }, aoDefinirAtivo = { _, _ -> }, aoRedefinirSenha = { _, _ -> }, aoMensagemVista = {},
    )
}
