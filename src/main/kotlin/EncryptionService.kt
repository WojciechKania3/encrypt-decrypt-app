import java.security.Key
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class EncryptionService {
    companion object {
        private const val SALT = "RandomSalt"
        private const val ITERATION_COUNT = 65536
        private const val KEY_LENGTH = 256
    }

    private fun generateKey(passphrase: String): Key {
        val keySpec = PBEKeySpec(passphrase.toCharArray(), SALT.toByteArray(), ITERATION_COUNT, KEY_LENGTH)
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val keyBytes = secretKeyFactory.generateSecret(keySpec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encryptText(plaintext: String, passphrase: String): Result<String> {
        return encryptBytes(plaintext.toByteArray(), passphrase).map { ciphertext ->
            val iv = Base64.getEncoder().encodeToString(ciphertext.first)
            val encryptedText = Base64.getEncoder().encodeToString(ciphertext.second)
            "$iv:$encryptedText"
        }
    }

    fun decryptText(ciphertext: String, passphrase: String): Result<String> {
        val parts = ciphertext.split(":")
        val iv = Base64.getDecoder().decode(parts[0])
        val encryptedBytes = Base64.getDecoder().decode(parts[1])
        return decryptBytes(iv to encryptedBytes, passphrase).map { plaintext -> String(plaintext) }
    }

    fun encryptBytes(plaintext: ByteArray, passphrase: String): Result<Pair<ByteArray, ByteArray>> {
        return Result.runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, generateKey(passphrase))
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plaintext)
            iv to encryptedBytes
        }
    }

    fun decryptBytes(ciphertext: Pair<ByteArray, ByteArray>, passphrase: String): Result<ByteArray> {
        return Result.runCatching {
            val (iv, encryptedBytes) = ciphertext
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, generateKey(passphrase), spec)
            cipher.doFinal(encryptedBytes)
        }
    }
}
