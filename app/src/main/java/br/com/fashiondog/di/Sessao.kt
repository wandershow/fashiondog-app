package br.com.fashiondog.di

import br.com.fashiondog.data.local.AppDatabase
import br.com.fashiondog.data.local.entity.Perfil
import br.com.fashiondog.data.local.entity.Usuario

/**
 * Quem está usando o app. Atendimentos, agendamentos e despesas guardam o
 * id do usuário que os registrou.
 *
 * PROVISÓRIO até a Sprint 5 (login): enquanto não há login, tudo é registrado
 * em nome de um "proprietário" padrão. Na Sprint 5 esse mesmo registro vira a
 * conta real do proprietário, então o histórico continua no nome certo.
 */
class Sessao(private val db: AppDatabase) {

    suspend fun usuarioAtualId(): Long {
        val dao = db.usuarioDao()
        return dao.buscarPorLogin(LOGIN_PROVISORIO)?.id
            ?: dao.inserir(
                Usuario(
                    nome = "Proprietário",
                    login = LOGIN_PROVISORIO,
                    senhaHash = "",
                    perfil = Perfil.PROPRIETARIO,
                ),
            )
    }

    companion object {
        const val LOGIN_PROVISORIO = "proprietario"
    }
}
