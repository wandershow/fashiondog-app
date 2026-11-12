package br.com.fashiondog.data.repository

import androidx.room.withTransaction
import br.com.fashiondog.data.local.AppDatabase
import br.com.fashiondog.data.local.entity.Pet
import br.com.fashiondog.data.local.entity.Tutor
import br.com.fashiondog.data.local.model.PetComTutor
import br.com.fashiondog.data.local.model.TutorComQuantidade
import kotlinx.coroutines.flow.Flow

/**
 * Ponto único de acesso a tutores e pets.
 * As telas falam com o repositório, nunca direto com o Room: se um dia o
 * banco mudar, só esta classe precisa ser alterada.
 */
class CadastroRepository(private val db: AppDatabase) {

    private val tutorDao = db.tutorDao()
    private val petDao = db.petDao()

    fun observarPetsComTutor(): Flow<List<PetComTutor>> = petDao.observarAtivosComTutor()

    fun observarTutores(): Flow<List<TutorComQuantidade>> = tutorDao.observarAtivosComQuantidade()

    fun observarTutor(id: Long): Flow<Tutor?> = tutorDao.observarPorId(id)

    fun observarPetsDoTutor(tutorId: Long): Flow<List<Pet>> = petDao.observarPorTutor(tutorId)

    fun observarPet(id: Long): Flow<PetComTutor?> = petDao.observarComTutor(id)

    suspend fun buscarTutor(id: Long): Tutor? = tutorDao.buscarPorId(id)

    suspend fun buscarPet(id: Long): Pet? = petDao.buscarPorId(id)

    /** Insere se for novo (id = 0) ou atualiza se já existir. Devolve o id. */
    suspend fun salvarTutor(tutor: Tutor): Long =
        if (tutor.id == 0L) tutorDao.inserir(tutor) else tutor.id.also { tutorDao.atualizar(tutor) }

    suspend fun salvarPet(pet: Pet): Long =
        if (pet.id == 0L) petDao.inserir(pet) else pet.id.also { petDao.atualizar(pet) }

    /**
     * Cadastra o tutor e (opcionalmente) o primeiro pet de uma vez.
     * A transação garante que ou grava os dois, ou nenhum.
     */
    suspend fun cadastrarTutorComPet(tutor: Tutor, primeiroPet: Pet?): Long =
        db.withTransaction {
            val tutorId = tutorDao.inserir(tutor)
            if (primeiroPet != null) petDao.inserir(primeiroPet.copy(tutorId = tutorId))
            tutorId
        }

    /** Desativar o tutor também desativa os pets dele. */
    suspend fun desativarTutor(id: Long) = db.withTransaction {
        tutorDao.desativar(id)
        petDao.desativarDoTutor(id)
    }

    suspend fun desativarPet(id: Long) = petDao.desativar(id)
}
