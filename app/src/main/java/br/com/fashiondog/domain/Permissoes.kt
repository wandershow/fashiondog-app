package br.com.fashiondog.domain

import br.com.fashiondog.data.local.entity.Perfil
import java.time.LocalDate

/**
 * O que cada perfil pode fazer (requisito do CLAUDE.md):
 * - Proprietário: tudo.
 * - Funcionário: agenda, pets, tutores, atendimentos e fotos; SEM financeiro, SEM
 *   editar/excluir registros antigos, SEM backup, SEM gerenciar usuários.
 *
 * "Registro antigo" para o funcionário: tutor e pet já cadastrados (não edita nem
 * desativa) e atendimento de dias anteriores. O atendimento de HOJE ele pode corrigir,
 * para consertar um erro de digitação logo depois de salvar.
 */
object Permissoes {

    fun veFinanceiro(perfil: Perfil) = perfil == Perfil.PROPRIETARIO

    fun fazBackup(perfil: Perfil) = perfil == Perfil.PROPRIETARIO

    fun gerenciaUsuarios(perfil: Perfil) = perfil == Perfil.PROPRIETARIO

    /** Preço sugerido dos serviços também é decisão do dono. */
    fun alteraPrecos(perfil: Perfil) = perfil == Perfil.PROPRIETARIO

    fun alteraCadastroExistente(perfil: Perfil) = perfil == Perfil.PROPRIETARIO

    fun editaAtendimento(perfil: Perfil, dataDoAtendimento: LocalDate, hoje: LocalDate = LocalDate.now()) =
        perfil == Perfil.PROPRIETARIO || dataDoAtendimento == hoje
}
