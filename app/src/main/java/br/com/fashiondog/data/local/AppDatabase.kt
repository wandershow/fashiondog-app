package br.com.fashiondog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.com.fashiondog.data.local.dao.AgendamentoDao
import br.com.fashiondog.data.local.dao.AtendimentoDao
import br.com.fashiondog.data.local.dao.FinanceiroDao
import br.com.fashiondog.data.local.dao.PetDao
import br.com.fashiondog.data.local.dao.ServicoDao
import br.com.fashiondog.data.local.dao.TutorDao
import br.com.fashiondog.data.local.dao.UsuarioDao
import br.com.fashiondog.data.local.entity.Agendamento
import br.com.fashiondog.data.local.entity.Atendimento
import br.com.fashiondog.data.local.entity.Categoria
import br.com.fashiondog.data.local.entity.Despesa
import br.com.fashiondog.data.local.entity.Foto
import br.com.fashiondog.data.local.entity.Parcela
import br.com.fashiondog.data.local.entity.Pet
import br.com.fashiondog.data.local.entity.Servico
import br.com.fashiondog.data.local.entity.Tutor
import br.com.fashiondog.data.local.entity.Usuario

@Database(
    entities = [
        Usuario::class,
        Tutor::class,
        Pet::class,
        Servico::class,
        Agendamento::class,
        Atendimento::class,
        Foto::class,
        Categoria::class,
        Despesa::class,
        Parcela::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun tutorDao(): TutorDao
    abstract fun petDao(): PetDao
    abstract fun servicoDao(): ServicoDao
    abstract fun agendamentoDao(): AgendamentoDao
    abstract fun atendimentoDao(): AtendimentoDao
    abstract fun financeiroDao(): FinanceiroDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        /** Uma única instância do banco para o app inteiro. */
        fun obter(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fashiondog.db",
                ).build().also { instancia = it }
            }
    }
}
