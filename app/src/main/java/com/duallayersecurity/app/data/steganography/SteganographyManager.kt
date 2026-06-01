package com.duallayersecurity.app.data.steganography

import android.graphics.Bitmap
import android.graphics.Color
import com.duallayersecurity.app.data.models.CryptoStegoResult
import java.nio.ByteBuffer

class SteganographyManager {

    companion object {
        private const val HEADER_SIZE = 4 // 4 bytes for data length
    }

    fun embedDataInImage(coverBitmap: Bitmap, encryptedData: ByteArray): CryptoStegoResult<Bitmap> {
        return try {
            // Calculate required capacity
            val totalBitsNeeded = (HEADER_SIZE + encryptedData.size) * 8
            val availableBits = coverBitmap.width * coverBitmap.height * 3 // RGB channels
            
            if (totalBitsNeeded > availableBits) {
                return CryptoStegoResult.Error(
                    "Image capacity insufficient. Need $totalBitsNeeded bits, have $availableBits bits available"
                )
            }

            // Create mutable copy
            val stegoBitmap = coverBitmap.copy(Bitmap.Config.ARGB_8888, true)
            
            // Prepare data with length header
            val dataWithHeader = ByteBuffer.allocate(HEADER_SIZE + encryptedData.size)
            dataWithHeader.putInt(encryptedData.size)
            dataWithHeader.put(encryptedData)
            val fullData = dataWithHeader.array()
            
            // Convert to bits
            val bits = bytesToBits(fullData)
            
            var bitIndex = 0
            outerLoop@ for (y in 0 until stegoBitmap.height) {
                for (x in 0 until stegoBitmap.width) {
                    if (bitIndex >= bits.size) break@outerLoop
                    
                    val pixel = stegoBitmap.getPixel(x, y)
                    var red = Color.red(pixel)
                    var green = Color.green(pixel)
                    var blue = Color.blue(pixel)
                    val alpha = Color.alpha(pixel)
                    
                    // Embed in red channel
                    if (bitIndex < bits.size) {
                        red = (red and 0xFE) or bits[bitIndex]
                        bitIndex++
                    }
                    
                    // Embed in green channel
                    if (bitIndex < bits.size) {
                        green = (green and 0xFE) or bits[bitIndex]
                        bitIndex++
                    }
                    
                    // Embed in blue channel
                    if (bitIndex < bits.size) {
                        blue = (blue and 0xFE) or bits[bitIndex]
                        bitIndex++
                    }
                    
                    val newPixel = Color.argb(alpha, red, green, blue)
                    stegoBitmap.setPixel(x, y, newPixel)
                }
            }
            
            CryptoStegoResult.Success(stegoBitmap)
        } catch (e: Exception) {
            CryptoStegoResult.Error("Steganography embedding failed: ${e.message}", e)
        }
    }

    fun extractDataFromImage(stegoBitmap: Bitmap): CryptoStegoResult<ByteArray> {
        return try {
            // First, extract the length header (4 bytes = 32 bits)
            val headerBits = mutableListOf<Int>()
            var bitCount = 0
            val headerBitSize = HEADER_SIZE * 8
            
            outerLoop@ for (y in 0 until stegoBitmap.height) {
                for (x in 0 until stegoBitmap.width) {
                    if (bitCount >= headerBitSize) break@outerLoop
                    
                    val pixel = stegoBitmap.getPixel(x, y)
                    val red = Color.red(pixel)
                    val green = Color.green(pixel)
                    val blue = Color.blue(pixel)
                    
                    // Extract from red channel
                    if (bitCount < headerBitSize) {
                        headerBits.add(red and 0x01)
                        bitCount++
                    }
                    
                    // Extract from green channel
                    if (bitCount < headerBitSize) {
                        headerBits.add(green and 0x01)
                        bitCount++
                    }
                    
                    // Extract from blue channel
                    if (bitCount < headerBitSize) {
                        headerBits.add(blue and 0x01)
                        bitCount++
                    }
                }
            }
            
            // Convert header bits to length
            val headerBytes = bitsToBytes(headerBits)
            val dataLength = ByteBuffer.wrap(headerBytes).int
            
            if (dataLength <= 0 || dataLength > 10_000_000) { // Sanity check
                return CryptoStegoResult.Error("Invalid data length detected: $dataLength")
            }
            
            // Now extract the actual data
            val totalBitsNeeded = (HEADER_SIZE + dataLength) * 8
            val allBits = mutableListOf<Int>()
            bitCount = 0
            
            outerLoop2@ for (y in 0 until stegoBitmap.height) {
                for (x in 0 until stegoBitmap.width) {
                    if (bitCount >= totalBitsNeeded) break@outerLoop2
                    
                    val pixel = stegoBitmap.getPixel(x, y)
                    val red = Color.red(pixel)
                    val green = Color.green(pixel)
                    val blue = Color.blue(pixel)
                    
                    if (bitCount < totalBitsNeeded) {
                        allBits.add(red and 0x01)
                        bitCount++
                    }
                    
                    if (bitCount < totalBitsNeeded) {
                        allBits.add(green and 0x01)
                        bitCount++
                    }
                    
                    if (bitCount < totalBitsNeeded) {
                        allBits.add(blue and 0x01)
                        bitCount++
                    }
                }
            }
            
            // Convert all bits to bytes and extract data (skip header)
            val allBytes = bitsToBytes(allBits)
            val extractedData = allBytes.copyOfRange(HEADER_SIZE, HEADER_SIZE + dataLength)
            
            CryptoStegoResult.Success(extractedData)
        } catch (e: Exception) {
            CryptoStegoResult.Error("Steganography extraction failed: ${e.message}", e)
        }
    }

    private fun bytesToBits(bytes: ByteArray): List<Int> {
        val bits = mutableListOf<Int>()
        for (byte in bytes) {
            for (i in 7 downTo 0) {
                bits.add((byte.toInt() shr i) and 0x01)
            }
        }
        return bits
    }

    private fun bitsToBytes(bits: List<Int>): ByteArray {
        val bytes = ByteArray((bits.size + 7) / 8)
        for (i in bits.indices) {
            if (bits[i] == 1) {
                bytes[i / 8] = (bytes[i / 8].toInt() or (1 shl (7 - (i % 8)))).toByte()
            }
        }
        return bytes
    }

    fun calculateImageCapacity(bitmap: Bitmap): Int {
        // Each pixel has 3 channels (RGB), each can store 1 bit
        val totalBits = bitmap.width * bitmap.height * 3
        // Subtract header size
        return (totalBits / 8) - HEADER_SIZE
    }
}
