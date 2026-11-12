# Sprint 2 — Atendimentos, fotos e histórico do pet

**Branch:** `feature/sprint-2-atendimentos` (criada a partir da Sprint 1) · **Status:** concluída

## Objetivo

Registrar cada banho/tosa com valor, pagamento e até 2 fotos (antes/depois), e mostrar na
ficha do pet o **último banho em destaque** e o **histórico completo**.

## O que foi entregue

| Tela | O que faz |
|---|---|
| **Ficha do pet** (atualizada) | Cartão amarelo "Último banho · há N dias" com data, serviço e valor; lista do histórico (o mais recente primeiro); botão **Registrar atendimento**. |
| **Novo / editar atendimento** | Serviço (Banho / Banho e tosa), valor cobrado (já vem com o preço sugerido), forma de pagamento (Pix, Dinheiro, Cartão), data, observações e fotos **Antes** e **Depois** (câmera ou galeria). |
| **Detalhe do atendimento** | Serviço, data e hora, valor e pagamento, observações e as fotos; tocar numa foto amplia. |
| **Mais → Serviços e preços** | Muda o preço sugerido de cada serviço. |

## Decisões (e por quê)

1. **Valor gravado no atendimento.** Ao escolher o serviço, o campo de valor é preenchido com o
   preço sugerido, mas o que fica salvo é o valor do campo. Mudar o preço em "Serviços e preços"
   não altera o histórico nem o balanço (regra do CLAUDE.md).

2. **Campo de dinheiro estilo maquininha.** Os números entram pela direita (5 → R$ 0,05,
   500 → R$ 5,00). Não tem vírgula para errar, e o valor já sai em **centavos (Long)**. A
   formatação (`Dinheiro.formatar`) é feita à mão para não depender do idioma do celular.

3. **Câmera do sistema, não câmera própria.** O app pede para o app de câmera do Android tirar
   a foto (`TakePicture`) e gravar num arquivo nosso via **FileProvider**. Não precisa da
   permissão de câmera nem de biblioteca extra. A **galeria** usa o seletor de fotos do Android,
   que também dispensa permissão de armazenamento.

4. **Fotos comprimidas na pasta interna.** Uma foto de câmera tem uns 4000×3000 px (3–5 MB).
   O `ArmazenamentoDeFotos`:
   - lê só o tamanho primeiro, depois abre a imagem já reduzida (economiza memória);
   - reduz para no máximo **1280 px** no lado maior e corrige a rotação (EXIF);
   - grava JPEG qualidade 80 (~150–250 KB) em `files/fotos/`, que outros apps não acessam.

5. **O banco guarda o caminho relativo** (`fotos/<id>.jpg`), não o absoluto. Assim o backup da
   Sprint 5 pode ser restaurado em outro celular sem quebrar as fotos.

6. **Limite de 2 fotos garantido pelo formato**: no máximo uma foto por tipo (Antes, Depois).
   Trocar ou remover uma foto apaga o arquivo antigo — **só depois** que o banco confirmou a
   gravação, para não perder a foto se algo der errado no meio.

7. **Sem lixo no celular.** Se a pessoa tira fotos e sai sem salvar, o ViewModel apaga as fotos
   comprimidas ao ser destruído (`onCleared`). As capturas originais da câmera são apagadas logo
   depois de comprimidas.

8. **Serviços iniciais.** Na primeira abertura, `DadosIniciais` cria "Banho" (R$ 50,00) e
   "Banho e tosa" (R$ 80,00). São só sugestões, ajustáveis em Mais.

9. **Usuário provisório (`Sessao`).** Atendimento exige o id de quem registrou, mas o login é da
   Sprint 5. Por enquanto, tudo fica no nome de um "proprietário" padrão. Na Sprint 5 esse mesmo
   registro vira a conta real, então o histórico continua correto.

10. **Data do atendimento:** de hoje grava a hora atual; de outro dia, meio-dia (no histórico só a
    data importa e o meio-dia evita problemas de fuso). Editar sem mudar o dia mantém a hora original.

## Arquivos principais

- `domain/Dinheiro.kt`, `domain/Datas.kt`, `domain/RedimensionamentoDeFoto.kt`
- `data/fotos/ArmazenamentoDeFotos.kt`, `res/xml/caminhos_arquivos.xml` + `<provider>` no manifesto
- `data/repository/AtendimentoRepository.kt`, `data/DadosIniciais.kt`, `di/Sessao.kt`
- `data/local/dao/AtendimentoDao.kt` — histórico com serviço, detalhe, fotos
- `ui/atendimento/` — formulário, detalhe e componentes de foto
- `ui/componentes/CamposEspeciais.kt` — `CampoDinheiro`, `CampoData`, `CampoHora`
- `ui/mais/` — aba Mais e Serviços e preços

## Testes automatizados

`AtendimentoRegrasTest.kt` — 11 testes novos (29 no total):
- **Dinheiro:** milhar e centavos, negativo, dígitos entrando pela direita, backspace, texto vazio.
- **Datas:** ida e volta millis ↔ data, "hoje / ontem / há N dias", ponte com o seletor (UTC).
- **Fotos:** fator de redução de uma foto de câmera, foto pequena intacta, proporção em retrato e paisagem.

## Como testar à mão

1. Abra um pet → **Registrar atendimento**.
2. Escolha **Banho e tosa**: o valor vira R$ 80,00. Apague e digite `7500` → R$ 75,00.
3. Escolha **Pix**, tire a foto **Antes** com a câmera e escolha a **Depois** da galeria. Salve.
4. A ficha mostra "Último banho · hoje", R$ 75,00, e o atendimento no histórico.
5. Em **Mais → Serviços e preços**, mude "Banho e tosa" para R$ 90,00. Volte à ficha: o
   atendimento continua R$ 75,00.
6. Abra o atendimento → toque na foto (amplia) → editar → remova a foto Antes → salvar.

## Dependências adicionadas

- `io.coil-kt:coil-compose` 2.7.0 — mostra as fotos sem travar a tela
- `androidx.exifinterface` 1.3.7 — lê a rotação gravada pela câmera

## Pendências

- Sem permissões por perfil ainda (Sprint 5): todos podem editar atendimentos.
- Atendimentos vindos da agenda (Sprint 3) já têm os campos `agendamentoId` e `servicoId` na rota.
