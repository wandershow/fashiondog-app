package br.com.fashiondog

import android.app.Application
import br.com.fashiondog.data.DadosIniciais
import br.com.fashiondog.di.AppContainer
import kotlinx.coroutines.launch

/**
 * Classe que o Android cria antes de qualquer tela.
 * Guarda o [AppContainer], de onde as telas pegam banco e repositórios.
 */
class FashionDogApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Garante os serviços (Banho, Banho e tosa) sem travar a abertura do app.
        container.escopo.launch { DadosIniciais.garantir(container.database) }
    }
}
