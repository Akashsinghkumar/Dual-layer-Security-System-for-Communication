package com.duallayersecurity.app

import com.duallayersecurity.app.data.crypto.CryptoManager
import com.duallayersecurity.app.data.models.CryptoStegoResult
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CryptoManagerTest {

    private lateinit var cryptoManager: CryptoManager

    @Before
    fun setup() {
        cryptoManager = CryptoManager()
    }

    @Test
    fun testAESKeyGeneration() {
        val key = cryptoManager.generateAESKey()
        assertNotNull(key)
        assertEquals("AES", key.algorithm)
        assertEquals(32, key.encoded.size) // 256 bits = 32 bytes
    }

    @Test
    fun testEncryptDecryptMessage() {
        val message = "Hello, this is a secret message!"
        val key = cryptoManager.generateAESKey()

        val encryptResult = cryptoManager.encryptMessage(message, key)
        assertTrue(encryptResult is CryptoStegoResult.Success)

        val encryptedMessage = (encryptResult as CryptoStegoResult.Success).data
        assertNotNull(encryptedMessage.encryptedData)
        assertNotNull(encryptedMessage.checksum)

        val decryptResult = cryptoManager.decryptMessage(encryptedMessage, key)
        assertTrue(decryptResult is CryptoStegoResult.Success)

        val decryptedMessage = (decryptResult as CryptoStegoResult.Success).data
        assertEquals(message, decryptedMessage)
    }

    @Test
    fun testIntegrityCheckFails() {
        val message = "Test message"
        val key = cryptoManager.generateAESKey()

        val encryptResult = cryptoManager.encryptMessage(message, key)
        val encryptedMessage = (encryptResult as CryptoStegoResult.Success).data

        // Tamper with the data
        val tamperedData = encryptedMessage.encryptedData.copyOf()
        tamperedData[0] = (tamperedData[0].toInt() xor 0xFF).toByte()

        val tamperedMessage = encryptedMessage.copy(encryptedData = tamperedData)
        val decryptResult = cryptoManager.decryptMessage(tamperedMessage, key)

        assertTrue(decryptResult is CryptoStegoResult.Error)
        assertTrue((decryptResult as CryptoStegoResult.Error).message.contains("Integrity check failed"))
    }

    @Test
    fun testKeyToBase64AndBack() {
        val originalKey = cryptoManager.generateAESKey()
        val base64Key = cryptoManager.keyToBase64(originalKey)
        val restoredKey = cryptoManager.base64ToKey(base64Key)

        assertArrayEquals(originalKey.encoded, restoredKey.encoded)
    }

    @Test
    fun testPBKDF2KeyDerivation() {
        val password = "StrongPassword789"
        val salt = cryptoManager.generateSalt()
        val key1 = cryptoManager.deriveKeyFromPassword(password, salt)
        val key2 = cryptoManager.deriveKeyFromPassword(password, salt)

        assertNotNull(key1)
        assertNotNull(key2)
        assertArrayEquals(key1.encoded, key2.encoded)
        assertEquals("AES", key1.algorithm)
        assertEquals(32, key1.encoded.size) // 256 bits
    }

    @Test
    fun testSHA256Calculation() {
        val data = "Test data".toByteArray()
        val hash1 = cryptoManager.calculateSHA256(data)
        val hash2 = cryptoManager.calculateSHA256(data)

        assertEquals(hash1, hash2)
        assertNotNull(hash1)
        assertTrue(hash1.isNotEmpty())
    }
}
