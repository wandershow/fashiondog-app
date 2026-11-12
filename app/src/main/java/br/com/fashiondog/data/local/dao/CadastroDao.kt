package br.com.fashiondog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.com.fashiondog.data.local.entity.Pet
import br.com.fashiondog.data.local.entity.Servico
import br.com.fashiondog.data.local.entity.Tutor
import br.com.fashiondog.data.local.entity.Usuario
import br.com.fashiondog.data.local.model.PetComTutor
import br.com.fashiondog.data.local.model.TutorComQuantidade
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

    @Query("SELECT * FROM usuario WHERE id = :id")
    suspend fun buscarPorId(id: Long): Usuario?

    /** Inclui inativos: um login nunca pode ser reaproveitado (o histórico aponta para ele). */
    @Query("SELECT * FROM usuario WHERE login = :login LIMIT 1")
    suspend fun buscarQualquerPorLogin(login: String): Usuario?

    @Query("SELECT * FROM usuario WHERE perfil = 'PROPRIETARIO' ORDER BY id LIMIT 1")
    suspend fun buscarProprietario(): Usuario?

    @Query("SELECT * FROM usuario ORDER BY ativo DESC, nome COLLATE NOCASE")
    fun observarTodos(): Flow<List<Usuario>>
}

@Dao
interface TutorDao {
    @Insert
    suspend fun inserir(tutor: Tutor): Long

    @Update
    suspend fun atualizar(tutor: Tutor)

    @Query("SELECT * FROM tutor WHERE id = :id")
    suspend fun buscarPorId(id: Long): Tutor?

    @Query("SELECT * FROM tutor WHERE id = :id")
    fun observarPorId(id: Long): Flow<Tutor?>

    @Query("SELECT * FROM tutor WHERE ativo = 1 ORDER BY nome COLLATE NOCASE")
    fun observarAtivos(): Flow<List<Tutor>>

    @Query(
        """
        SELECT tutor.*,
               (SELECT COUNT(*) FROM pet WHERE pet.tutorId = tutor.id AND pet.ativo = 1) AS quantidadePets
        FROM tutor
        WHERE tutor.ativo = 1
        ORDER BY tutor.nome COLLATE NOCASE
        """,
    )
    fun observarAtivosComQuantidade(): Flow<List<TutorComQuantidade>>

    /** Tutor nunca é apagado: só fica inativo (some das listas, mas o histórico continua). */
    @Query("UPDATE tutor SET ativo = 0 WHERE id = :id")
    suspend fun desativar(id: Long)
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

    @Query("SELECT * FROM pet WHERE tutorId = :tutorId AND ativo = 1 ORDER BY nome COLLATE NOCASE")
    fun observarPorTutor(tutorId: Long): Flow<List<Pet>>

    @Query(
        """
        SELECT pet.*, tutor.nome AS tutorNome, tutor.telefone AS tutorTelefone
        FROM pet INNER JOIN tutor ON tutor.id = pet.tutorId
        WHERE pet.ativo = 1 AND tutor.ativo = 1
        ORDER BY pet.nome COLLATE NOCASE
        """,
    )
    fun observarAtivosComTutor(): Flow<List<PetComTutor>>

    @Query(
        """
        SELECT pet.*, tutor.nome AS tutorNome, tutor.telefone AS tutorTelefone
        FROM pet INNER JOIN tutor ON tutor.id = pet.tutorId
        WHERE pet.id = :id
        """,
    )
    fun observarComTutor(id: Long): Flow<PetComTutor?>

    @Query("UPDATE pet SET ativo = 0 WHERE id = :id")
    suspend fun desativar(id: Long)

    @Query("UPDATE pet SET ativo = 0 WHERE tutorId = :tutorId")
    suspend fun desativarDoTutor(tutorId: Long)
}

@Dao
interface ServicoDao {
    @Insert
    suspend fun inserir(servico: Servico): Long

    @Update
    suspend fun atualizar(servico: Servico)

    @Query("SELECT * FROM servico WHERE ativo = 1 ORDER BY nome")
    fun observarAtivos(): Flow<List<Servico>>

    @Query("SELECT * FROM servico WHERE id = :id")
    suspend fun buscarPorId(id: Long): Servico?

    @Query("SELECT COUNT(*) FROM servico")
    suspend fun contar(): Int
}
