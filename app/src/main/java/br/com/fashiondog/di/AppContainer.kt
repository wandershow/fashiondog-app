package br.com.fashiondog.di

import android.content.Context
import br.com.fashiondog.data.fotos.ArmazenamentoDeFotos
import br.com.fashiondog.data.local.AppDatabase
import br.com.fashiondog.data.repository.AgendaRepository
import br.com.fashiondog.data.repository.AtendimentoRepository
import br.com.fashiondog.data.repository.CadastroRepository
import br.com.fashiondog.data.repository.FinanceiroRepository
import br.com.fashiondog.notifications.Avisos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * "Injeção de dependência manual": um objeto que cria e guarda as peças do app
 * (banco, repositórios). Cada tela recebe daqui o que precisa.
 *
 * Escolhemos isto em vez do Hilt porque o app é pequeno: não precisa de
 * plugin extra nem anotações, e dá para ver exatamente de onde vem cada objeto.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** Tarefas que não pertencem a uma tela (ex.: preparar dados iniciais). */
    val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.obter(appContext) }

    val fotos: ArmazenamentoDeFotos by lazy { ArmazenamentoDeFotos(appContext) }

    val avisos: Avisos by lazy { Avisos(appContext) }

    val sessao: Sessao by lazy { Sessao(database) }

    val cadastro: CadastroRepository by lazy { CadastroRepository(database) }

    val atendimentos: AtendimentoRepository by lazy { AtendimentoRepository(database, fotos, avisos) }

    val agenda: AgendaRepository by lazy { AgendaRepository(database, avisos) }

    val financeiro: FinanceiroRepository by lazy { FinanceiroRepository(database, avisos) }

    /** Recria todos os avisos pendentes (reinício do celular, abertura do app). */
    suspend fun reagendarAvisos() {
        agenda.reagendarAvisos()
        financeiro.reagendarAvisos()
    }
}
