package br.com.fashiondog.ui.componentes

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.fashiondog.FashionDogApplication
import br.com.fashiondog.di.AppContainer

/**
 * Cria (ou reaproveita) o ViewModel de uma tela, entregando a ele o [AppContainer]
 * e o [SavedStateHandle] — onde chegam os parâmetros da rota, como o id do pet.
 *
 * Uso: `val vm = viewModelDoApp { app, args -> FichaPetViewModel(app.cadastro, args) }`
 */
@Composable
inline fun <reified VM : ViewModel> viewModelDoApp(
    crossinline criar: (AppContainer, SavedStateHandle) -> VM,
): VM = viewModel(
    factory = viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY] as FashionDogApplication
            criar(app.container, createSavedStateHandle())
        }
    },
)
