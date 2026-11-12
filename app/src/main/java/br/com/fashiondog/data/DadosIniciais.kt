package br.com.fashiondog.data

import br.com.fashiondog.data.local.AppDatabase
import br.com.fashiondog.data.local.entity.Servico

/**
 * Cadastros que o app precisa ter desde a primeira vez que abre.
 * Só insere se a tabela estiver vazia, então pode ser chamado a cada abertura.
 * Os preços são sugestões: o proprietário ajusta em Mais → Serviços e preços.
 */
object DadosIniciais {

    suspend fun garantir(db: AppDatabase) {
        val servicos = db.servicoDao()
        if (servicos.contar() == 0) {
            servicos.inserir(Servico(nome = "Banho", precoSugeridoCentavos = 5_000))
            servicos.inserir(Servico(nome = "Banho e tosa", precoSugeridoCentavos = 8_000))
        }
    }
}
