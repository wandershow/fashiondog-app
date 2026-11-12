package br.com.fashiondog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.com.fashiondog.data.local.entity.Pet
import br.com.fashiondog.data.local.entity.Servico
import br.com.fashiondog.data.local.entity.Tutor
import br.com.fashiondog.data.local.entity.Usuario
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Insert
    suspend fun inserir(usuario: Usuario): Long

    @Update
    suspend fun atualizar(usuario: Usuario)

    @Query("SELECT * FROM usuario WHERE login = :login AND ativo = 1 LIMIT 1")
    suspend fun buscarPorLogin(login: String): Usuario?

    @Query("SELECT COUNT(*) FROM usuario")
    suspend fun contar(): Int
}

@Dao
interface TutorDao {
    @Insert
    suspend fun inserir(tutor: Tutor): Long

    @Update
    suspend fun atualizar(tutor: Tutor)

    @Query("SELECT * FROM tutor WHERE id = :id")
    suspend fun buscarPorId(id: Long): Tutor?

    @Query("SELECT * FROM tutor WHERE ativo = 1 ORDER BY nome")
    fun observarAtivos(): Flow<List<Tutor>>
}

@Dao
interface PetDao {
    @Insert
    suspend fun inserir(pet: Pet): Long

    @Update
    suspend fun atualizar(pet: Pet)

    @Query("SELECT * FROM pet WHERE id = :id")
    suspend fun buscarPorId(id: Long): Pet?

    @Query("SELECT * FROM pet WHERE ativo = 1 ORDER BY nome")
    fun observarAtivos(): Flow<List<Pet>>

    @Query("SELECT * FROM pet WHERE tutorId = :tutorId AND ativo = 1 ORDER BY nome")
    fun observarPorTutor(tutorId: Long): Flow<List<Pet>>
}

@Dao
interface ServicoDao {
    @Insert
    suspend fun inserir(servico: Servico): Long

    @Update
    suspend fun atualizar(servico: Servico)

    @Query("SELECT * FROM servico WHERE ativo = 1 ORDER BY nome")
    fun observarAtivos(): Flow<List<Servico>>
}
