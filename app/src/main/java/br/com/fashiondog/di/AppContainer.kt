package br.com.fashiondog.di

import android.content.Context
import br.com.fashiondog.data.local.AppDatabase
import br.com.fashiondog.data.repository.CadastroRepository

/**
 * "Injeção de dependência manual": um objeto que cria e guarda as peças do app
 * (banco, repositórios). Cada tela recebe daqui o que precisa.
 *
 * Escolhemos isto em vez do Hilt porque o app é pequeno: não precisa de
 * plugin extra nem anotações, e dá para ver exatamente de onde vem cada objeto.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.obter(appContext) }

    val cadastro: CadastroRepository by lazy { CadastroRepository(database) }
}
