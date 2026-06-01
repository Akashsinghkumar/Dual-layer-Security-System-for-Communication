package com.duallayersecurity.app.data.crypto

import android.util.Base64
import com.duallayersecurity.app.data.models.CryptoStegoResult
import com.duallayersecurity.app.data.models.EncryptedMessage
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class CryptoManager {

    companion object {
        private const val AES_ALGORITHM = "AES"
        private const val AES_TRANSFORMATION = "AES/CBC/PKCS5Padding"
        private const val AES_KEY_SIZE = 256
        private const val IV_SIZE = 16
    }

    fun generateAESKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM)
        keyGenerator.init(AES_KEY_SIZE, SecureRandom())
        return keyGenerator.generateKey()
    }

    fun encryptMessage(message: String, secretKey: SecretKey): CryptoStegoResult<EncryptedMessage> {
        return try {
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val iv = ByteArray(IV_SIZE)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encryptedBytes = cipher.doFinal(message.toByteArray(Charsets.UTF_8))
            
            // Combine IV and encrypted data
            val combined = iv + encryptedBytes
            
            // Calculate checksum
            val checksum = calculateSHA256(combined)
            
            CryptoStegoResult.Success(
                EncryptedMessage(
                    encryptedData = combined,
                    checksum = checksum
                )
            )
        } catch (e: Exception) {
            CryptoStegoResult.Error("Encryption failed: ${e.message}", e)
        }
    }

    fun decryptMessage(encryptedMessage: EncryptedMessage, secretKey: SecretKey): CryptoStegoResult<String> {
        return try {
            // Verify checksum
            val calculatedChecksum = calculateSHA256(encryptedMessage.encryptedData)
            if (calculatedChecksum != encryptedMessage.checksum) {
                return CryptoStegoResult.Error("Integrity check failed: Data has been tampered with")
            }

            if (encryptedMessage.encryptedData.size < IV_SIZE) {
                return CryptoStegoResult.Error("Invalid encrypted data: Too short to contain IV")
            }

            // Extract IV and encrypted data
            val iv = encryptedMessage.encryptedData.copyOfRange(0, IV_SIZE)
            val encryptedBytes = encryptedMessage.encryptedData.copyOfRange(IV_SIZE, encryptedMessage.encryptedData.size)
            
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            val decryptedMessage = String(decryptedBytes, Charsets.UTF_8)
            
            CryptoStegoResult.Success(decryptedMessage)
        } catch (e: Exception) {
            CryptoStegoResult.Error("Decryption failed: ${e.message}", e)
        }
    }

    fun calculateSHA256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun keyToBase64(key: SecretKey): String {
        return Base64.encodeToString(key.encoded, Base64.NO_WRAP)
    }

    fun base64ToKey(base64Key: String): SecretKey {
        val decodedKey = Base64.decode(base64Key, Base64.NO_WRAP)
        return SecretKeySpec(decodedKey, 0, decodedKey.size, AES_ALGORITHM)
    }

    fun deriveKeyFromPassword(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, AES_ALGORITHM)
    }

    fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }

    /**
     * Encrypts raw byte data (files) using AES-256-CBC.
     */
    fun encryptRawBytes(data: ByteArray, secretKey: SecretKey): CryptoStegoResult<EncryptedMessage> {
        return try {
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val iv = ByteArray(IV_SIZE)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encryptedBytes = cipher.doFinal(data)

            val combined = iv + encryptedBytes
            val checksum = calculateSHA256(combined)

            CryptoStegoResult.Success(
                EncryptedMessage(
                    encryptedData = combined,
                    checksum = checksum
                )
            )
        } catch (e: Exception) {
            CryptoStegoResult.Error("Raw encryption failed: ${e.message}", e)
        }
    }

    /**
     * Decrypts raw byte data (files) previously encrypted with encryptRawBytes.
     */
    fun decryptRawBytes(encryptedMessage: EncryptedMessage, secretKey: SecretKey): CryptoStegoResult<ByteArray> {
        return try {
            val calculatedChecksum = calculateSHA256(encryptedMessage.encryptedData)
            if (calculatedChecksum != encryptedMessage.checksum) {
                return CryptoStegoResult.Error("Integrity check failed: Data has been tampered with")
            }

            if (encryptedMessage.encryptedData.size < IV_SIZE) {
                return CryptoStegoResult.Error("Invalid encrypted data: Too short to contain IV")
            }

            val iv = encryptedMessage.encryptedData.copyOfRange(0, IV_SIZE)
            val encryptedBytes = encryptedMessage.encryptedData.copyOfRange(IV_SIZE, encryptedMessage.encryptedData.size)

            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            CryptoStegoResult.Success(decryptedBytes)
        } catch (e: Exception) {
            CryptoStegoResult.Error("Raw decryption failed: ${e.message}", e)
        }
    }
}
