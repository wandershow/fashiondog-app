package br.com.fashiondog.domain

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Senha nunca é gravada: guardamos só um "hash" dela. Hash é uma conta de mão única —
 * dá para conferir se a senha digitada bate, mas não dá para descobrir a senha a partir dele.
 *
 * Usamos PBKDF2 (padrão da indústria, já vem no Java) com:
 * - um "sal" aleatório por senha: duas pessoas com a mesma senha têm hashes diferentes;
 * - milhares de repetições: tentar adivinhar senhas por força bruta fica muito lento.
 *
 * Formato gravado: `pbkdf2$<repetições>$<sal em base64>$<hash em base64>`.
 */
object HashDeSenha {

    private const val ALGORITMO = "PBKDF2WithHmacSHA256"
    private const val REPETICOES = 120_000
    private const val TAMANHO_SAL = 16
    private const val TAMANHO_HASH_BITS = 256
    private val aleatorio = SecureRandom()

    fun gerar(senha: String, repeticoes: Int = REPETICOES): String {
        val sal = ByteArray(TAMANHO_SAL).also(aleatorio::nextBytes)
        val hash = calcular(senha, sal, repeticoes)
        val b64 = Base64.getEncoder()
        return "pbkdf2\$$repeticoes\$${b64.encodeToString(sal)}\$${b64.encodeToString(hash)}"
    }

    fun confere(senha: String, guardado: String): Boolean {
        val partes = guardado.split('$')
        if (partes.size != 4 || partes[0] != "pbkdf2") return false
        val repeticoes = partes[1].toIntOrNull() ?: return false
        val b64 = Base64.getDecoder()
        val sal = runCatching { b64.decode(partes[2]) }.getOrNull() ?: return false
        val esperado = runCatching { b64.decode(partes[3]) }.getOrNull() ?: return false
        // Comparação em tempo constante: não "vaza" quantos bytes acertou.
        return MessageDigest.isEqual(calcular(senha, sal, repeticoes), esperado)
    }

    private fun calcular(senha: String, sal: ByteArray, repeticoes: Int): ByteArray {
        val especificacao = PBEKeySpec(senha.toCharArray(), sal, repeticoes, TAMANHO_HASH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacao).encoded
        } finally {
            especificacao.clearPassword()
        }
    }
}

/**
 * Sem servidor não há "e-mail de recuperação". No lugar, o proprietário recebe um
 * código ao criar a conta (e um novo a cada uso). Com ele, cria uma senha nova.
 * O código também é guardado só como hash.
 */
object CodigoDeRecuperacao {

    /** Sem 0/O, 1/I/L: letras que se confundem ao anotar no papel. */
    private const val ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    private val aleatorio = SecureRandom()

    /** Ex.: "K7QM-3XRP-T9WD" (12 caracteres, ~59 bits de aleatoriedade). */
    fun gerar(): String = (1..3).joinToString("-") {
        (1..4).map { ALFABETO[aleatorio.nextInt(ALFABETO.length)] }.joinToString("")
    }

    /** Aceita o código digitado com minúsculas, espaços ou sem os traços. */
    fun normalizar(digitado: String): String =
        digitado.uppercase().filter { it in ALFABETO }.chunked(4).joinToString("-")
}

object ValidacaoDeConta {

    const val TAMANHO_MINIMO_SENHA = 6
    private val loginValido = Regex("^[a-z0-9._]{3,20}$")

    /** Login em minúsculas, sem espaço nem acento: "maria", "joao.silva". */
    fun normalizarLogin(login: String): String = login.trim().lowercase()

    fun erroLogin(login: String): String? =
        if (!loginValido.matches(normalizarLogin(login))) "Use 3 a 20 letras minúsculas, números, ponto ou _" else null

    fun erroSenha(senha: String): String? =
        if (senha.length < TAMANHO_MINIMO_SENHA) "A senha precisa ter pelo menos $TAMANHO_MINIMO_SENHA caracteres" else null

    fun erroConfirmacao(senha: String, confirmacao: String): String? =
        if (senha != confirmacao) "As senhas não são iguais" else null
}
