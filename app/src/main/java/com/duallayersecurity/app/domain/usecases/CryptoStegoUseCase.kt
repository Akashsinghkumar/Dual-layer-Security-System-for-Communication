package com.duallayersecurity.app.domain.usecases

import android.graphics.Bitmap
import android.util.Log
import com.duallayersecurity.app.data.crypto.CryptoManager
import com.duallayersecurity.app.data.models.CryptoStegoResult
import com.duallayersecurity.app.data.models.EncryptedMessage
import com.duallayersecurity.app.data.steganography.SteganographyManager

/**
 * Data class to hold extracted file information.
 */
data class ExtractedFile(
    val fileName: String,
    val fileBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ExtractedFile
        return fileName == other.fileName && fileBytes.contentEquals(other.fileBytes)
    }
    override fun hashCode(): Int = 31 * fileName.hashCode() + fileBytes.contentHashCode()
}

class CryptoStegoUseCase(
    private val cryptoManager: CryptoManager = CryptoManager(),
    private val steganographyManager: SteganographyManager = SteganographyManager()
) {

    // ==================== TEXT MESSAGE METHODS ====================

    fun performCryptoStegoEmbed(
        message: String,
        password: String,
        coverImage: Bitmap
    ): CryptoStegoResult<Bitmap> {
        return try {
            val salt = cryptoManager.generateSalt()
            val key = cryptoManager.deriveKeyFromPassword(password, salt)

            val encryptResult = cryptoManager.encryptMessage(message, key)
            if (encryptResult is CryptoStegoResult.Error) {
                return CryptoStegoResult.Error("Encryption failed: ${encryptResult.message}")
            }
            
            val encryptedMessage = (encryptResult as CryptoStegoResult.Success).data
            
            // Type marker 0x01 = text message
            val serializedData = serializePayload(salt, encryptedMessage, TYPE_TEXT, "message.txt")
            
            val requiredCapacity = serializedData.size
            val availableCapacity = steganographyManager.calculateImageCapacity(coverImage)
            
            if (requiredCapacity > availableCapacity) {
                return CryptoStegoResult.Error(
                    "Image too small. Required: $requiredCapacity bytes, Available: $availableCapacity bytes"
                )
            }
            
            val embedResult = steganographyManager.embedDataInImage(coverImage, serializedData)
            if (embedResult is CryptoStegoResult.Error) {
                return CryptoStegoResult.Error("Embedding failed: ${embedResult.message}")
            }
            
            embedResult
        } catch (e: Exception) {
            CryptoStegoResult.Error("Embed failed: ${e.message}", e)
        }
    }

    fun performCryptoStegoExtract(
        stegoImage: Bitmap,
        password: String
    ): CryptoStegoResult<String> {
        return try {
            Log.d("DLS_DEBUG", "Extract Step 1: Extracting raw data from stego image...")
            val extractResult = steganographyManager.extractDataFromImage(stegoImage)
            if (extractResult is CryptoStegoResult.Error) {
                Log.e("DLS_DEBUG", "Extract Step 1 FAILED: ${extractResult.message}")
                return CryptoStegoResult.Error("Extraction failed: ${extractResult.message}")
            }
            
            val extractedData = (extractResult as CryptoStegoResult.Success).data
            Log.d("DLS_DEBUG", "Extract Step 1 OK: ${extractedData.size} bytes extracted")
            
            Log.d("DLS_DEBUG", "Extract Step 2: Deserializing payload...")
            val deserializeResult = deserializePayload(extractedData)
            if (deserializeResult is CryptoStegoResult.Error) {
                Log.e("DLS_DEBUG", "Extract Step 2 FAILED: ${deserializeResult.message}")
                return CryptoStegoResult.Error("Deserialization failed: ${deserializeResult.message}")
            }
            
            val payload = (deserializeResult as CryptoStegoResult.Success).data
            Log.d("DLS_DEBUG", "Extract Step 2 OK: type=${payload.type}, name=${payload.fileName}, salt=${payload.salt.size} bytes")

            if (payload.type != TYPE_TEXT) {
                return CryptoStegoResult.Error("This image contains a file, not a text message. Use 'Extract File from Photo' instead.")
            }
            
            Log.d("DLS_DEBUG", "Extract Step 3: Deriving key...")
            val key = cryptoManager.deriveKeyFromPassword(password, payload.salt)
            Log.d("DLS_DEBUG", "Extract Step 3 OK: Key derived")

            Log.d("DLS_DEBUG", "Extract Step 4: Decrypting message...")
            val decryptResult = cryptoManager.decryptMessage(payload.encryptedMessage, key)
            if (decryptResult is CryptoStegoResult.Error) {
                Log.e("DLS_DEBUG", "Extract Step 4 FAILED: ${decryptResult.message}")
                return CryptoStegoResult.Error("Decryption failed. Please verify your password.", decryptResult.exception)
            }
            
            Log.d("DLS_DEBUG", "Extract Step 4 OK: Decryption successful!")
            decryptResult
        } catch (e: Exception) {
            Log.e("DLS_DEBUG", "Extract EXCEPTION: ${e.message}", e)
            CryptoStegoResult.Error("Extraction failed: ${e.message}", e)
        }
    }

    // ==================== FILE METHODS ====================

    /**
     * Encrypts a file and embeds it inside a cover image.
     */
    fun performFileEmbed(
        fileBytes: ByteArray,
        fileName: String,
        password: String,
        coverImage: Bitmap
    ): CryptoStegoResult<Bitmap> {
        return try {
            Log.d("DLS_DEBUG", "FileEmbed: fileName=$fileName, fileSize=${fileBytes.size} bytes")

            // Step 1: Generate salt and derive key
            val salt = cryptoManager.generateSalt()
            val key = cryptoManager.deriveKeyFromPassword(password, salt)

            // Step 2: Encrypt the raw file bytes using AES
            val encryptResult = cryptoManager.encryptRawBytes(fileBytes, key)
            if (encryptResult is CryptoStegoResult.Error) {
                return CryptoStegoResult.Error("File encryption failed: ${encryptResult.message}")
            }

            val encryptedMessage = (encryptResult as CryptoStegoResult.Success).data

            // Step 3: Serialize with type marker TYPE_FILE
            val serializedData = serializePayload(salt, encryptedMessage, TYPE_FILE, fileName)
            Log.d("DLS_DEBUG", "FileEmbed: serialized payload size=${serializedData.size} bytes")

            // Step 4: Check image capacity
            val availableCapacity = steganographyManager.calculateImageCapacity(coverImage)
            if (serializedData.size > availableCapacity) {
                return CryptoStegoResult.Error(
                    "Image too small for this file. Required: ${serializedData.size} bytes, Available: $availableCapacity bytes. Use a larger image."
                )
            }

            // Step 5: Embed in image
            val embedResult = steganographyManager.embedDataInImage(coverImage, serializedData)
            if (embedResult is CryptoStegoResult.Error) {
                return CryptoStegoResult.Error("Embedding failed: ${embedResult.message}")
            }

            Log.d("DLS_DEBUG", "FileEmbed: SUCCESS")
            embedResult
        } catch (e: Exception) {
            Log.e("DLS_DEBUG", "FileEmbed EXCEPTION: ${e.message}", e)
            CryptoStegoResult.Error("File embed failed: ${e.message}", e)
        }
    }

    /**
     * Extracts a hidden file from a stego image.
     */
    fun performFileExtract(
        stegoImage: Bitmap,
        password: String
    ): CryptoStegoResult<ExtractedFile> {
        return try {
            Log.d("DLS_DEBUG", "FileExtract Step 1: Extracting raw data...")
            val extractResult = steganographyManager.extractDataFromImage(stegoImage)
            if (extractResult is CryptoStegoResult.Error) {
                return CryptoStegoResult.Error("Extraction failed: ${extractResult.message}")
            }

            val extractedData = (extractResult as CryptoStegoResult.Success).data
            Log.d("DLS_DEBUG", "FileExtract Step 1 OK: ${extractedData.size} bytes")

            Log.d("DLS_DEBUG", "FileExtract Step 2: Deserializing...")
            val deserializeResult = deserializePayload(extractedData)
            if (deserializeResult is CryptoStegoResult.Error) {
                return CryptoStegoResult.Error("Deserialization failed: ${deserializeResult.message}")
            }

            val payload = (deserializeResult as CryptoStegoResult.Success).data
            Log.d("DLS_DEBUG", "FileExtract Step 2 OK: type=${payload.type}, name=${payload.fileName}")

            if (payload.type != TYPE_FILE) {
                return CryptoStegoResult.Error("This image contains a text message, not a file. Use 'Extract & Decrypt' instead.")
            }

            Log.d("DLS_DEBUG", "FileExtract Step 3: Deriving key...")
            val key = cryptoManager.deriveKeyFromPassword(password, payload.salt)

            Log.d("DLS_DEBUG", "FileExtract Step 4: Decrypting file bytes...")
            val decryptResult = cryptoManager.decryptRawBytes(payload.encryptedMessage, key)
            if (decryptResult is CryptoStegoResult.Error) {
                return CryptoStegoResult.Error("Decryption failed. Please verify your password.", decryptResult.exception)
            }

            val decryptedBytes = (decryptResult as CryptoStegoResult.Success).data
            Log.d("DLS_DEBUG", "FileExtract OK: ${decryptedBytes.size} bytes, fileName=${payload.fileName}")

            CryptoStegoResult.Success(ExtractedFile(payload.fileName, decryptedBytes))
        } catch (e: Exception) {
            Log.e("DLS_DEBUG", "FileExtract EXCEPTION: ${e.message}", e)
            CryptoStegoResult.Error("File extraction failed: ${e.message}", e)
        }
    }

    // ==================== SERIALIZATION ====================

    companion object {
        const val TYPE_TEXT: Byte = 0x01
        const val TYPE_FILE: Byte = 0x02
    }

    /**
     * Unified serialization format:
     * [type (1 byte)][salt (16 bytes)][fileName_length (4)][fileName][checksum_length (4)][checksum][data_length (4)][data]
     */
    private fun serializePayload(
        salt: ByteArray,
        encryptedMessage: EncryptedMessage,
        type: Byte,
        fileName: String
    ): ByteArray {
        val fileNameBytes = fileName.toByteArray(Charsets.UTF_8)
        val checksumBytes = encryptedMessage.checksum.toByteArray(Charsets.UTF_8)
        val dataBytes = encryptedMessage.encryptedData

        // 1 (type) + 16 (salt) + 4 (fnLen) + fnLen + 4 (csLen) + csLen + 4 (dataLen) + dataLen
        val totalSize = 1 + 16 + 4 + fileNameBytes.size + 4 + checksumBytes.size + 4 + dataBytes.size
        val buffer = ByteArray(totalSize)
        var offset = 0

        // Type marker
        buffer[offset++] = type

        // Salt
        System.arraycopy(salt, 0, buffer, offset, 16)
        offset += 16

        // File name
        writeInt(buffer, offset, fileNameBytes.size); offset += 4
        System.arraycopy(fileNameBytes, 0, buffer, offset, fileNameBytes.size)
        offset += fileNameBytes.size

        // Checksum
        writeInt(buffer, offset, checksumBytes.size); offset += 4
        System.arraycopy(checksumBytes, 0, buffer, offset, checksumBytes.size)
        offset += checksumBytes.size

        // Encrypted data
        writeInt(buffer, offset, dataBytes.size); offset += 4
        System.arraycopy(dataBytes, 0, buffer, offset, dataBytes.size)

        return buffer
    }

    private data class DeserializedPayload(
        val type: Byte,
        val salt: ByteArray,
        val fileName: String,
        val encryptedMessage: EncryptedMessage
    )

    private fun deserializePayload(data: ByteArray): CryptoStegoResult<DeserializedPayload> {
        return try {
            var offset = 0

            // Read type
            if (offset + 1 > data.size) {
                return CryptoStegoResult.Error("Payload too short: missing type marker")
            }
            val type = data[offset++]

            // Read salt
            if (offset + 16 > data.size) {
                return CryptoStegoResult.Error("Payload too short: missing salt")
            }
            val salt = ByteArray(16)
            System.arraycopy(data, offset, salt, 0, 16)
            offset += 16

            // Read file name
            if (offset + 4 > data.size) {
                return CryptoStegoResult.Error("Payload too short: missing file name length")
            }
            val fnLen = readInt(data, offset); offset += 4
            if (fnLen < 0 || offset + fnLen > data.size) {
                return CryptoStegoResult.Error("Invalid file name length: $fnLen (payload size: ${data.size})")
            }
            val fileNameBytes = ByteArray(fnLen)
            System.arraycopy(data, offset, fileNameBytes, 0, fnLen)
            offset += fnLen
            val fileName = String(fileNameBytes, Charsets.UTF_8)

            // Read checksum
            if (offset + 4 > data.size) {
                return CryptoStegoResult.Error("Payload too short: missing checksum length")
            }
            val csLen = readInt(data, offset); offset += 4
            if (csLen < 0 || offset + csLen > data.size) {
                return CryptoStegoResult.Error("Invalid checksum length: $csLen (payload size: ${data.size})")
            }
            val checksumBytes = ByteArray(csLen)
            System.arraycopy(data, offset, checksumBytes, 0, csLen)
            offset += csLen
            val checksum = String(checksumBytes, Charsets.UTF_8)

            // Read encrypted data
            if (offset + 4 > data.size) {
                return CryptoStegoResult.Error("Payload too short: missing data length")
            }
            val dataLen = readInt(data, offset); offset += 4
            if (dataLen < 0 || offset + dataLen > data.size) {
                return CryptoStegoResult.Error("Invalid data length: $dataLen (payload size: ${data.size})")
            }
            val encryptedData = ByteArray(dataLen)
            System.arraycopy(data, offset, encryptedData, 0, dataLen)

            CryptoStegoResult.Success(
                DeserializedPayload(
                    type = type,
                    salt = salt,
                    fileName = fileName,
                    encryptedMessage = EncryptedMessage(encryptedData, checksum)
                )
            )
        } catch (e: Exception) {
            CryptoStegoResult.Error("Deserialization failed: ${e.message}", e)
        }
    }

    private fun writeInt(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset]     = (value shr 24).toByte()
        buffer[offset + 1] = (value shr 16).toByte()
        buffer[offset + 2] = (value shr 8).toByte()
        buffer[offset + 3] = value.toByte()
    }

    private fun readInt(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0xFF) shl 24) or
                ((data[offset + 1].toInt() and 0xFF) shl 16) or
                ((data[offset + 2].toInt() and 0xFF) shl 8) or
                (data[offset + 3].toInt() and 0xFF)
    }
}
