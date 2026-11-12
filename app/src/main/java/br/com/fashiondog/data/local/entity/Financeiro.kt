package br.com.fashiondog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categoria")
data class Categoria(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
)

@Entity(
    tableName = "despesa",
    foreignKeys = [
        ForeignKey(entity = Categoria::class, parentColumns = ["id"], childColumns = ["categoriaId"]),
        ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["usuarioId"]),
    ],
    indices = [Index("categoriaId"), Index("usuarioId")],
)
data class Despesa(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoriaId: Long,
    val descricao: String,
    val valorTotalCentavos: Long,
    val formaPagamento: FormaPagamentoDespesa,
    val numParcelas: Int = 1,
    /** Data em milissegundos (epoch). */
    val dataCompra: Long,
    val usuarioId: Long,
)

/**
 * Toda despesa tem ao menos uma parcela: uma compra à vista é uma despesa
 * com 1 parcela já paga. O balanço soma as parcelas pelo vencimento.
 */
@Entity(
    tableName = "parcela",
    foreignKeys = [
        ForeignKey(
            entity = Despesa::class,
            parentColumns = ["id"],
            childColumns = ["despesaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("despesaId"), Index(value = ["vencimento", "status"])],
)
data class Parcela(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val despesaId: Long,
    val numero: Int,
    val valorCentavos: Long,
    /** Vencimento em milissegundos (epoch). */
    val vencimento: Long,
    val status: StatusParcela = StatusParcela.PENDENTE,
    val dataPagamento: Long? = null,
)
