package br.com.fashiondog.ui.preview

import androidx.compose.runtime.Composable
import br.com.fashiondog.data.local.entity.Agendamento
import br.com.fashiondog.data.local.entity.Atendimento
import br.com.fashiondog.data.local.entity.Categoria
import br.com.fashiondog.data.local.entity.FormaPagamento
import br.com.fashiondog.data.local.entity.FormaPagamentoDespesa
import br.com.fashiondog.data.local.entity.Parcela
import br.com.fashiondog.data.local.entity.Pet
import br.com.fashiondog.data.local.entity.Servico
import br.com.fashiondog.data.local.entity.StatusAgendamento
import br.com.fashiondog.data.local.entity.StatusParcela
import br.com.fashiondog.data.local.entity.Tutor
import br.com.fashiondog.data.local.model.AgendamentoDetalhado
import br.com.fashiondog.data.local.model.AtendimentoComServico
import br.com.fashiondog.data.local.model.ParcelaDetalhada
import br.com.fashiondog.data.local.model.PetComTutor
import br.com.fashiondog.data.local.model.TutorComQuantidade
import br.com.fashiondog.domain.Datas
import br.com.fashiondog.ui.theme.FashionDogTheme
import java.time.LocalDate
import java.time.LocalTime

/**
 * Dados de mentira usados só nos @Preview do Android Studio.
 * As prévias desenham a parte "sem estado" de cada tela (XxxConteudo) com estes dados,
 * sem banco nem ViewModel.
 */
object DadosDePreview {
    private val hoje = LocalDate.now()

    val maria = Tutor(id = 1, nome = "Maria Souza", telefone = "53999998888", observacoes = "Prefere WhatsApp")
    val joao = Tutor(id = 2, nome = "João Pereira", telefone = "5332221111")

    val boris = Pet(id = 1, tutorId = 1, nome = "Bóris", raca = "Poodle", porte = "Pequeno", observacoes = "Alergia a shampoo de aveia")
    val thor = Pet(id = 2, tutorId = 1, nome = "Thor", raca = "Golden", porte = "Grande")
    val mel = Pet(id = 3, tutorId = 2, nome = "Mel", raca = "SRD", porte = "Médio")

    val petsComTutor = listOf(
        PetComTutor(boris, maria.nome, maria.telefone),
        PetComTutor(mel, joao.nome, joao.telefone),
        PetComTutor(thor, maria.nome, maria.telefone),
    )

    val tutores = listOf(TutorComQuantidade(joao, 1), TutorComQuantidade(maria, 2))

    val banho = Servico(id = 1, nome = "Banho", precoSugeridoCentavos = 5_000)
    val banhoETosa = Servico(id = 2, nome = "Banho e tosa", precoSugeridoCentavos = 8_000)
    val servicos = listOf(banho, banhoETosa)

    fun atendimento(id: Long, diasAtras: Long, servico: Servico, valor: Long, forma: FormaPagamento) = AtendimentoComServico(
        Atendimento(
            id = id,
            petId = boris.id,
            servicoId = servico.id,
            data = Datas.paraMillis(hoje.minusDays(diasAtras)),
            valorCentavos = valor,
            formaPagamento = forma,
            usuarioId = 1,
        ),
        servico.nome,
    )

    val historico = listOf(
        atendimento(3, 6, banhoETosa, 7_500, FormaPagamento.PIX),
        atendimento(2, 36, banho, 5_000, FormaPagamento.DINHEIRO),
        atendimento(1, 70, banhoETosa, 8_000, FormaPagamento.CARTAO),
    )

    fun agendamento(id: Long, hora: Int, pet: Pet, tutor: Tutor, servico: Servico, status: StatusAgendamento) =
        AgendamentoDetalhado(
            Agendamento(
                id = id,
                petId = pet.id,
                servicoId = servico.id,
                dataHora = Datas.paraMillis(hoje, LocalTime.of(hora, 0)),
                status = status,
                usuarioId = 1,
            ),
            pet.nome,
            tutor.nome,
            tutor.telefone,
            servico.nome,
        )

    val agendaDoDia = listOf(
        agendamento(1, 9, boris, maria, banhoETosa, StatusAgendamento.CONCLUIDO),
        agendamento(2, 11, mel, joao, banho, StatusAgendamento.AGENDADO),
        agendamento(3, 15, thor, maria, banho, StatusAgendamento.CANCELADO),
    )

    val categorias = listOf(
        Categoria(1, "Produtos de banho"), Categoria(2, "Aluguel"), Categoria(3, "Água e luz"), Categoria(4, "Outros"),
    )

    fun parcela(id: Long, descricao: String, numero: Int, total: Int, valor: Long, dias: Long, paga: Boolean, categoria: String) =
        ParcelaDetalhada(
            Parcela(
                id = id,
                despesaId = id,
                numero = numero,
                valorCentavos = valor,
                vencimento = Datas.paraMillis(hoje.plusDays(dias)),
                status = if (paga) StatusParcela.PAGA else StatusParcela.PENDENTE,
            ),
            descricao,
            total,
            if (total > 1 || !paga) FormaPagamentoDespesa.BOLETO_PARCELADO else FormaPagamentoDespesa.A_VISTA,
            categoria,
        )

    val parcelas = listOf(
        parcela(1, "Secador profissional", 2, 3, 30_000, -2, paga = false, categoria = "Equipamentos"),
        parcela(2, "Shampoo 5 L", 1, 1, 15_990, -5, paga = true, categoria = "Produtos de banho"),
        parcela(3, "Aluguel", 1, 1, 120_000, 8, paga = false, categoria = "Aluguel"),
    )
}

/** Envolve a prévia no tema do app (cores da marca). */
@Composable
fun PreviewNoTema(conteudo: @Composable () -> Unit) {
    FashionDogTheme(content = conteudo)
}
