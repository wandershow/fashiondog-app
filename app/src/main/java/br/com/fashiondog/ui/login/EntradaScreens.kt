package br.com.fashiondog.ui.login

import android.content.ClipData
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fashiondog.R
import br.com.fashiondog.data.local.entity.Usuario
import br.com.fashiondog.data.repository.ContaRepository
import br.com.fashiondog.domain.ValidacaoDeConta
import br.com.fashiondog.ui.componentes.BotaoPrincipal
import br.com.fashiondog.ui.componentes.Cartao
import br.com.fashiondog.ui.componentes.Formulario
import br.com.fashiondog.ui.componentes.Tela
import br.com.fashiondog.ui.componentes.viewModelDoApp
import br.com.fashiondog.ui.preview.PreviewNoTema
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Estado e ViewModel únicos para o fluxo de entrada (primeiro acesso, login, recuperar)
// ---------------------------------------------------------------------------

data class EntradaEstado(
    val nome: String = "",
    val login: String = "",
    val senha: String = "",
    val confirmacao: String = "",
    val codigo: String = "",
    val erros: Map<String, String> = emptyMap(),
    val erroGeral: String? = null,
    val processando: Boolean = false,
    /** Código de recuperação recém-gerado, para mostrar uma única vez. */
    val codigoGerado: String? = null,
    val usuarioCriado: Usuario? = null,
)

class EntradaViewModel(private val contas: ContaRepository) : ViewModel() {

    private val _estado = MutableStateFlow(EntradaEstado())
    val estado: StateFlow<EntradaEstado> = _estado.asStateFlow()

    fun mudar(campo: String, valor: String) = _estado.update {
        when (campo) {
            NOME -> it.copy(nome = valor)
            LOGIN -> it.copy(login = valor)
            SENHA -> it.copy(senha = valor)
            CONFIRMACAO -> it.copy(confirmacao = valor)
            else -> it.copy(codigo = valor)
        }.copy(erros = it.erros - campo, erroGeral = null)
    }

    private fun validarSenhaNova(atual: EntradaEstado): Map<String, String> = buildMap {
        ValidacaoDeConta.erroSenha(atual.senha)?.let { put(SENHA, it) }
        ValidacaoDeConta.erroConfirmacao(atual.senha, atual.confirmacao)?.let { put(CONFIRMACAO, it) }
    }

    fun criarProprietario() {
        val atual = _estado.value
        val erros = buildMap {
            if (atual.nome.isBlank()) put(NOME, "Informe seu nome")
            ValidacaoDeConta.erroLogin(atual.login)?.let { put(LOGIN, it) }
            putAll(validarSenhaNova(atual))
        }
        if (erros.isNotEmpty()) return _estado.update { it.copy(erros = erros) }
        executar {
            contas.criarProprietario(atual.nome, atual.login, atual.senha)
                .onSuccess { r -> _estado.update { it.copy(codigoGerado = r.codigo, usuarioCriado = r.usuario) } }
        }
    }

    fun entrar() {
        val atual = _estado.value
        if (atual.login.isBlank() || atual.senha.isEmpty()) {
            return _estado.update { it.copy(erroGeral = "Informe login e senha") }
        }
        executar { contas.entrar(atual.login, atual.senha) }
    }

    fun recuperar() {
        val atual = _estado.value
        val erros = buildMap {
            if (atual.login.isBlank()) put(LOGIN, "Informe o login do proprietário")
            if (atual.codigo.isBlank()) put(CODIGO, "Informe o código de recuperação")
            putAll(validarSenhaNova(atual))
        }
        if (erros.isNotEmpty()) return _estado.update { it.copy(erros = erros) }
        executar {
            contas.recuperarSenha(atual.login, atual.codigo, atual.senha)
                .onSuccess { r -> _estado.update { it.copy(codigoGerado = r.codigo, usuarioCriado = r.usuario) } }
        }
    }

    /** "Anotei o código": entra no app com a conta criada/recuperada. */
    fun confirmarCodigoAnotado() {
        val usuario = _estado.value.usuarioCriado ?: return
        viewModelScope.launch { contas.entrarCom(usuario) }
    }

    private fun executar(acao: suspend () -> Result<*>) {
        _estado.update { it.copy(processando = true, erroGeral = null) }
        viewModelScope.launch {
            val resultado = acao()
            _estado.update {
                it.copy(processando = false, erroGeral = resultado.exceptionOrNull()?.message, senha = "", confirmacao = "")
            }
        }
    }

    companion object {
        const val NOME = "nome"
        const val LOGIN = "login"
        const val SENHA = "senha"
        const val CONFIRMACAO = "confirmacao"
        const val CODIGO = "codigo"
    }
}

// ---------------------------------------------------------------------------
// Telas com estado (pegam o ViewModel)
// ---------------------------------------------------------------------------

@Composable
fun PrimeiroAcessoScreen() {
    val vm = viewModelDoApp { app, _ -> EntradaViewModel(app.contas) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val codigo = estado.codigoGerado
    if (codigo != null) {
        CodigoDeRecuperacaoConteudo(codigo, aoConfirmar = vm::confirmarCodigoAnotado)
    } else {
        PrimeiroAcessoConteudo(estado, vm::mudar, vm::criarProprietario)
    }
}

@Composable
fun LoginScreen(aoEsqueciSenha: () -> Unit) {
    val vm = viewModelDoApp { app, _ -> EntradaViewModel(app.contas) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    LoginConteudo(estado, vm::mudar, vm::entrar, aoEsqueciSenha)
}

@Composable
fun RecuperarSenhaScreen(aoVoltar: () -> Unit) {
    val vm = viewModelDoApp { app, _ -> EntradaViewModel(app.contas) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val codigo = estado.codigoGerado
    if (codigo != null) {
        CodigoDeRecuperacaoConteudo(codigo, aoConfirmar = vm::confirmarCodigoAnotado)
    } else {
        RecuperarSenhaConteudo(estado, vm::mudar, vm::recuperar, aoVoltar)
    }
}

// ---------------------------------------------------------------------------
// Conteúdos (só desenham) + @Preview
// ---------------------------------------------------------------------------

@Composable
fun PrimeiroAcessoConteudo(estado: EntradaEstado, aoMudar: (String, String) -> Unit, aoCriar: () -> Unit) {
    Tela(titulo = "Bem-vindo ao FashionDog") { espacamento ->
        Formulario(espacamento) {
            Logo()
            Text(
                "Crie a conta do proprietário. Ela tem acesso a tudo: financeiro, backup e cadastro de funcionários.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Campo(estado, EntradaViewModel.NOME, estado.nome, "Seu nome", aoMudar, maiusculas = KeyboardCapitalization.Words)
            Campo(estado, EntradaViewModel.LOGIN, estado.login, "Login (ex.: maria)", aoMudar)
            Campo(estado, EntradaViewModel.SENHA, estado.senha, "Senha (mín. 6 caracteres)", aoMudar, senha = true)
            Campo(estado, EntradaViewModel.CONFIRMACAO, estado.confirmacao, "Repita a senha", aoMudar, senha = true)
            estado.erroGeral?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            BotaoPrincipal(if (estado.processando) "Criando..." else "Criar conta", aoCriar, habilitado = !estado.processando)
        }
    }
}

@Composable
fun LoginConteudo(estado: EntradaEstado, aoMudar: (String, String) -> Unit, aoEntrar: () -> Unit, aoEsqueciSenha: () -> Unit) {
    Tela(titulo = "Entrar") { espacamento ->
        Formulario(espacamento) {
            Logo()
            Campo(estado, EntradaViewModel.LOGIN, estado.login, "Login", aoMudar)
            Campo(estado, EntradaViewModel.SENHA, estado.senha, "Senha", aoMudar, senha = true)
            estado.erroGeral?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            BotaoPrincipal(if (estado.processando) "Entrando..." else "Entrar", aoEntrar, habilitado = !estado.processando)
            TextButton(onClick = aoEsqueciSenha, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Esqueci a senha")
            }
            Text(
                "Funcionário: peça ao proprietário para definir uma senha nova em Mais → Usuários.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun RecuperarSenhaConteudo(
    estado: EntradaEstado,
    aoMudar: (String, String) -> Unit,
    aoRecuperar: () -> Unit,
    aoVoltar: () -> Unit,
) {
    Tela(titulo = "Recuperar senha", aoVoltar = aoVoltar) { espacamento ->
        Formulario(espacamento) {
            Text(
                "Use o código de recuperação que apareceu quando a conta do proprietário foi criada.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Campo(estado, EntradaViewModel.LOGIN, estado.login, "Login do proprietário", aoMudar)
            Campo(
                estado, EntradaViewModel.CODIGO, estado.codigo, "Código (ex.: K7QM-3XRP-T9WD)", aoMudar,
                maiusculas = KeyboardCapitalization.Characters,
            )
            Campo(estado, EntradaViewModel.SENHA, estado.senha, "Nova senha", aoMudar, senha = true)
            Campo(estado, EntradaViewModel.CONFIRMACAO, estado.confirmacao, "Repita a nova senha", aoMudar, senha = true)
            estado.erroGeral?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            BotaoPrincipal(if (estado.processando) "Conferindo..." else "Trocar senha", aoRecuperar, habilitado = !estado.processando)
        }
    }
}

/** Mostra o código uma única vez. Depois disso, só o hash fica guardado. */
@Composable
fun CodigoDeRecuperacaoConteudo(codigo: String, aoConfirmar: () -> Unit) {
    val areaDeTransferencia = LocalClipboard.current
    val escopo = rememberCoroutineScope()
    Tela(titulo = "Código de recuperação") { espacamento ->
        Formulario(espacamento) {
            Text("Anote este código num lugar seguro (fora do celular).", style = MaterialTheme.typography.titleMedium)
            Cartao(destaque = true) {
                Text(
                    codigo,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            TextButton(onClick = {
                escopo.launch { areaDeTransferencia.setClipEntry(ClipEntry(ClipData.newPlainText("Código FashionDog", codigo))) }
            }) { Text("Copiar código") }
            Text(
                "Ele é a única forma de criar uma senha nova se você esquecer a sua — o app não tem servidor " +
                    "nem e-mail de recuperação. Cada código vale uma vez: ao usar, você recebe outro.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BotaoPrincipal("Já anotei, continuar", aoConfirmar)
        }
    }
}

@Composable
private fun Logo() {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = "FashionDog",
            modifier = Modifier.size(160.dp),
        )
    }
}

@Composable
private fun Campo(
    estado: EntradaEstado,
    chave: String,
    valor: String,
    rotulo: String,
    aoMudar: (String, String) -> Unit,
    senha: Boolean = false,
    maiusculas: KeyboardCapitalization = KeyboardCapitalization.None,
) {
    val erro = estado.erros[chave]
    OutlinedTextField(
        value = valor,
        onValueChange = { aoMudar(chave, it) },
        label = { Text(rotulo) },
        isError = erro != null,
        supportingText = erro?.let { { Text(it) } },
        singleLine = true,
        visualTransformation = if (senha) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (senha) KeyboardType.Password else KeyboardType.Text,
            capitalization = maiusculas,
            autoCorrectEnabled = false,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview(showBackground = true, name = "Primeiro acesso")
@Composable
private fun PreviewPrimeiroAcesso() = PreviewNoTema {
    PrimeiroAcessoConteudo(
        EntradaEstado(nome = "Maria Souza", login = "maria", erros = mapOf(EntradaViewModel.SENHA to "A senha precisa ter pelo menos 6 caracteres")),
        { _, _ -> }, {},
    )
}

@Preview(showBackground = true, name = "Login com erro")
@Composable
private fun PreviewLogin() = PreviewNoTema {
    LoginConteudo(EntradaEstado(login = "maria", erroGeral = "Login ou senha incorretos"), { _, _ -> }, {}, {})
}

@Preview(showBackground = true, name = "Recuperar senha")
@Composable
private fun PreviewRecuperar() = PreviewNoTema {
    RecuperarSenhaConteudo(EntradaEstado(login = "maria"), { _, _ -> }, {}, {})
}

@Preview(showBackground = true, name = "Código de recuperação")
@Composable
private fun PreviewCodigo() = PreviewNoTema {
    CodigoDeRecuperacaoConteudo("K7QM-3XRP-T9WD") {}
}
