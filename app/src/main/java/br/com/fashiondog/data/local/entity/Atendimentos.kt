package br.com.fashiondog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "agendamento",
    foreignKeys = [
        ForeignKey(entity = Pet::class, parentColumns = ["id"], childColumns = ["petId"]),
        ForeignKey(entity = Servico::class, parentColumns = ["id"], childColumns = ["servicoId"]),
        ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["usuarioId"]),
    ],
    indices = [Index("petId"), Index("servicoId"), Index("usuarioId"), Index("dataHora")],
)
data class Agendamento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val petId: Long,
    val servicoId: Long,
    /** Data e hora em milissegundos (epoch). */
    val dataHora: Long,
    val lembreteMinutos: Int = 60,
    val status: StatusAgendamento = StatusAgendamento.AGENDADO,
    val usuarioId: Long,
)

@Entity(
    tableName = "atendimento",
    foreignKeys = [
        ForeignKey(entity = Pet::class, parentColumns = ["id"], childColumns = ["petId"]),
        ForeignKey(entity = Servico::class, parentColumns = ["id"], childColumns = ["servicoId"]),
        ForeignKey(
            entity = Agendamento::class,
            parentColumns = ["id"],
            childColumns = ["agendamentoId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["usuarioId"]),
    ],
    indices = [
        Index(value = ["petId", "data"]),
        Index("servicoId"),
        Index("agendamentoId"),
        Index("usuarioId"),
    ],
)
data class Atendimento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val petId: Long,
    val servicoId: Long,
    val agendamentoId: Long? = null,
    /** Data em milissegundos (epoch). */
    val data: Long,
    /** Valor cobrado no dia; fica gravado mesmo se o preço do serviço mudar depois. */
    val valorCentavos: Long,
    val formaPagamento: FormaPagamento,
    val observacoes: String? = null,
    val usuarioId: Long,
)

@Entity(
    tableName = "foto",
    foreignKeys = [
        ForeignKey(
            entity = Atendimento::class,
            parentColumns = ["id"],
            childColumns = ["atendimentoId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("atendimentoId")],
)
data class Foto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val atendimentoId: Long,
    /** Caminho do arquivo na pasta interna do app. */
    val caminho: String,
    val tipo: TipoFoto,
)
