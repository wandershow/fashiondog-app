package br.com.fashiondog

import android.app.Application
import br.com.fashiondog.di.AppContainer

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
    }
}
