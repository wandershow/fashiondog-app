package br.com.fashiondog.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.fashiondog.R
import br.com.fashiondog.ui.atendimento.AtendimentoDetalheScreen
import br.com.fashiondog.ui.atendimento.AtendimentoFormScreen
import br.com.fashiondog.ui.mais.MaisScreen
import br.com.fashiondog.ui.mais.OpcaoMais
import br.com.fashiondog.ui.mais.ServicosScreen
import br.com.fashiondog.ui.pet.CadastrosScreen
import br.com.fashiondog.ui.pet.FichaPetScreen
import br.com.fashiondog.ui.pet.PetFormScreen
import br.com.fashiondog.ui.pet.TutorDetalheScreen
import br.com.fashiondog.ui.pet.TutorFormScreen

/** As quatro abas da barra inferior. */
enum class Aba(val rota: String, val rotulo: String, @param:DrawableRes val icone: Int) {
    AGENDA("agenda", "Agenda", R.drawable.ic_aba_agenda),
    PETS("pets", "Pets", R.drawable.ic_aba_pets),
    FINANCEIRO("financeiro", "Financeiro", R.drawable.ic_aba_financeiro),
    MAIS("mais", "Mais", R.drawable.ic_aba_mais),
}

private fun idArg(nome: String) = navArgument(nome) {
    type = NavType.LongType
    defaultValue = -1L
}

@Composable
fun FashionDogApp() {
    val navController = rememberNavController()
    val entradaAtual by navController.currentBackStackEntryAsState()
    val rotaAtual = entradaAtual?.destination?.route
    val abas = Aba.entries
    // A barra inferior só aparece nas telas principais; formulários e fichas ocupam a tela toda.
    val mostrarBarra = abas.any { it.rota == rotaAtual }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // Cada tela cuida das próprias margens (barra de status), por isso zeramos aqui.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (mostrarBarra) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    abas.forEach { aba ->
                        NavigationBarItem(
                            selected = rotaAtual == aba.rota,
                            onClick = { navController.irParaAba(aba) },
                            icon = { Icon(painterResource(aba.icone), contentDescription = null) },
                            label = { Text(aba.rotulo) },
                        )
                    }
                }
            }
        },
    ) { espacamento ->
        NavHost(
            navController = navController,
            startDestination = Aba.AGENDA.rota,
            modifier = Modifier.padding(espacamento).consumeWindowInsets(espacamento),
        ) {
            composable(Aba.AGENDA.rota) { TelaEmConstrucao("Agenda") }
            composable(Aba.FINANCEIRO.rota) { TelaEmConstrucao("Financeiro") }
            composable(Aba.MAIS.rota) {
                MaisScreen(
                    opcoes = listOf(
                        OpcaoMais("Serviços e preços", "Preço sugerido de cada serviço") {
                            navController.navigate(Rotas.SERVICOS)
                        },
                    ),
                )
            }
            composable(Rotas.SERVICOS) { ServicosScreen(aoVoltar = { navController.popBackStack() }) }

            cadastros(navController)
            atendimentos(navController)
        }
    }
}

/** Telas da Sprint 1: pets e tutores. */
private fun androidx.navigation.NavGraphBuilder.cadastros(nav: NavHostController) {
    composable(Aba.PETS.rota) {
        CadastrosScreen(
            aoAbrirPet = { nav.navigate(Rotas.pet(it)) },
            aoAbrirTutor = { nav.navigate(Rotas.tutor(it)) },
            aoNovoCadastro = { nav.navigate(Rotas.tutorForm()) },
        )
    }
    composable(Rotas.TUTOR_FORM, arguments = listOf(idArg("tutorId"))) {
        TutorFormScreen(
            aoVoltar = { nav.popBackStack() },
            aoSalvar = { tutorId, ehNovo ->
                if (ehNovo) {
                    // Depois de cadastrar, abre a página do tutor no lugar do formulário.
                    nav.navigate(Rotas.tutor(tutorId)) { popUpTo(Rotas.TUTOR_FORM) { inclusive = true } }
                } else {
                    nav.popBackStack()
                }
            },
        )
    }
    composable(Rotas.TUTOR, arguments = listOf(idArg("tutorId"))) {
        TutorDetalheScreen(
            aoVoltar = { nav.popBackStack() },
            aoEditar = { nav.navigate(Rotas.tutorForm(it)) },
            aoNovoPet = { nav.navigate(Rotas.novoPet(it)) },
            aoAbrirPet = { nav.navigate(Rotas.pet(it)) },
        )
    }
    composable(Rotas.PET_FORM, arguments = listOf(idArg("petId"), idArg("tutorId"))) {
        PetFormScreen(
            aoVoltar = { nav.popBackStack() },
            aoSalvar = { nav.popBackStack() },
        )
    }
    composable(Rotas.PET, arguments = listOf(idArg("petId"))) {
        FichaPetScreen(
            aoVoltar = { nav.popBackStack() },
            aoEditar = { nav.navigate(Rotas.editarPet(it)) },
            aoAbrirTutor = { nav.navigate(Rotas.tutor(it)) },
            aoNovoAtendimento = { nav.navigate(Rotas.novoAtendimento(it)) },
            aoAbrirAtendimento = { nav.navigate(Rotas.atendimento(it)) },
        )
    }
}

/** Telas da Sprint 2: atendimentos. */
private fun androidx.navigation.NavGraphBuilder.atendimentos(nav: NavHostController) {
    composable(
        Rotas.ATENDIMENTO_FORM,
        arguments = listOf(idArg("petId"), idArg("atendimentoId"), idArg("agendamentoId"), idArg("servicoId")),
    ) {
        AtendimentoFormScreen(
            aoVoltar = { nav.popBackStack() },
            aoSalvar = { nav.popBackStack() },
        )
    }
    composable(Rotas.ATENDIMENTO, arguments = listOf(idArg("atendimentoId"))) {
        AtendimentoDetalheScreen(
            aoVoltar = { nav.popBackStack() },
            aoEditar = { nav.navigate(Rotas.editarAtendimento(it)) },
        )
    }
}

fun NavHostController.irParaAba(aba: Aba) {
    navigate(aba.rota) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Tela provisória; cada aba ganha sua tela real nas próximas sprints. */
@Composable
private fun TelaEmConstrucao(titulo: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = titulo, style = MaterialTheme.typography.headlineMedium)
    }
}
