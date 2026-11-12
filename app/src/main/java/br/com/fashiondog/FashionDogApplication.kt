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
        container.avisos.criarCanais()
        // Sem travar a abertura do app: garante os serviços (Banho, Banho e tosa) e
        // recria os avisos (caso o app tenha sido fechado à força, o que apaga os alarmes).
        container.escopo.launch {
            DadosIniciais.garantir(container.database)
            container.sessao.carregar() // decide: primeiro acesso, login ou direto para a agenda
            container.reagendarAvisos()
        }
    }
}
