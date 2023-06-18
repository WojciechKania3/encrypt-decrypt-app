import java.nio.ByteBuffer
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
        val plaintextBytes = plaintext.toByteArray(Charsets.UTF_8)
        return encryptBytes(plaintextBytes, passphrase).map { encryptedBytes ->
            Base64.getEncoder().encodeToString(encryptedBytes)
        }
    }

    fun decryptText(ciphertext: String, passphrase: String): Result<String> {
        val ciphertextBytes = Base64.getDecoder().decode(ciphertext)
        return decryptBytes(ciphertextBytes, passphrase).map { decryptedBytes ->
            String(decryptedBytes, Charsets.UTF_8)
        }
    }

    fun encryptBytes(plaintext: ByteArray, passphrase: String): Result<ByteArray> {
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, generateKey(passphrase))
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plaintext)
            val ivLength = iv.size
            ByteBuffer.allocate(4 + ivLength + encryptedBytes.size)
                .putInt(ivLength)
                .put(iv)
                .put(encryptedBytes)
                .array()
        }
    }

    fun decryptBytes(ciphertext: ByteArray, passphrase: String): Result<ByteArray> {
        return runCatching {
            val bb = ByteBuffer.wrap(ciphertext)
            val ivLength = bb.int
            if (ivLength < 0 || ivLength >= ciphertext.size) {
                throw IllegalArgumentException("Invalid iv length")
            }
            val iv = ByteArray(ivLength)
            bb.get(iv)
            val encryptedBytes = ByteArray(bb.remaining())
            bb.get(encryptedBytes)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, generateKey(passphrase), spec)
            cipher.doFinal(encryptedBytes)
        }
    }
}
