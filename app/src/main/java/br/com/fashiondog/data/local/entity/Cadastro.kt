package br.com.fashiondog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usuario",
    indices = [Index(value = ["login"], unique = true)],
)
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val login: String,
    val senhaHash: String,
    val perfil: Perfil,
    val ativo: Boolean = true,
)

@Entity(tableName = "tutor")
data class Tutor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val telefone: String,
    val observacoes: String? = null,
    val ativo: Boolean = true,
)

@Entity(
    tableName = "pet",
    foreignKeys = [
        ForeignKey(
            entity = Tutor::class,
            parentColumns = ["id"],
            childColumns = ["tutorId"],
        ),
    ],
    indices = [Index("tutorId")],
)
data class Pet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tutorId: Long,
    val nome: String,
    val raca: String? = null,
    val porte: String? = null,
    val observacoes: String? = null,
    val ativo: Boolean = true,
)

@Entity(tableName = "servico")
data class Servico(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    /** Valores sempre em centavos, para não haver erro de arredondamento. */
    val precoSugeridoCentavos: Long,
    val ativo: Boolean = true,
)
