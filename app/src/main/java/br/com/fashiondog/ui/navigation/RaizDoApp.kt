package br.com.fashiondog.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.fashiondog.FashionDogApplication
import br.com.fashiondog.di.EstadoDaSessao
import br.com.fashiondog.ui.componentes.Carregando
import br.com.fashiondog.ui.login.LoginScreen
import br.com.fashiondog.ui.login.PrimeiroAcessoScreen
import br.com.fashiondog.ui.login.RecuperarSenhaScreen

/**
 * Primeira tela do app: olha a sessão e decide o que mostrar.
 * Quando o login acontece (ou a pessoa sai), a sessão muda e esta tela troca sozinha.
 */
@Composable
fun RaizDoApp() {
    val sessao = (LocalContext.current.applicationContext as FashionDogApplication).container.sessao
    val estado by sessao.estado.collectAsStateWithLifecycle()

    when (val atual = estado) {
        EstadoDaSessao.Carregando -> Carregando()
        EstadoDaSessao.PrimeiroAcesso -> PrimeiroAcessoScreen()
        EstadoDaSessao.Deslogado -> FluxoDeLogin()
        // key: trocar de usuário recria a navegação do zero (sem telas do usuário anterior).
        is EstadoDaSessao.Logado -> androidx.compose.runtime.key(atual.usuario.id) {
            FashionDogApp(usuario = atual.usuario)
        }
    }
}

@Composable
private fun FluxoDeLogin() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "login") {
        composable("login") { LoginScreen(aoEsqueciSenha = { nav.navigate("recuperar") }) }
        composable("recuperar") { RecuperarSenhaScreen(aoVoltar = { nav.popBackStack() }) }
    }
}
