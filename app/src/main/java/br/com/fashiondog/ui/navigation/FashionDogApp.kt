package br.com.fashiondog.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.fashiondog.R

/** As quatro abas da barra inferior. */
enum class Aba(val rota: String, val rotulo: String, @DrawableRes val icone: Int) {
    AGENDA("agenda", "Agenda", R.drawable.ic_aba_agenda),
    PETS("pets", "Pets", R.drawable.ic_aba_pets),
    FINANCEIRO("financeiro", "Financeiro", R.drawable.ic_aba_financeiro),
    MAIS("mais", "Mais", R.drawable.ic_aba_mais),
}

@Composable
fun FashionDogApp() {
    val navController = rememberNavController()
    val entradaAtual by navController.currentBackStackEntryAsState()
    val rotaAtual = entradaAtual?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Aba.entries.forEach { aba ->
                    NavigationBarItem(
                        selected = rotaAtual == aba.rota,
                        onClick = {
                            navController.navigate(aba.rota) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(aba.icone),
                                contentDescription = null,
                            )
                        },
                        label = { Text(aba.rotulo) },
                    )
                }
            }
        },
    ) { espacamento ->
        NavHost(
            navController = navController,
            startDestination = Aba.AGENDA.rota,
            modifier = Modifier.padding(espacamento),
        ) {
            Aba.entries.forEach { aba ->
                composable(aba.rota) { TelaEmConstrucao(aba.rotulo) }
            }
        }
    }
}

/** Tela provisória; cada aba ganha sua tela real nas próximas sprints. */
@Composable
private fun TelaEmConstrucao(titulo: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = titulo, style = MaterialTheme.typography.headlineMedium)
    }
}
