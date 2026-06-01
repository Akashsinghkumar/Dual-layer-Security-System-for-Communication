package com.duallayersecurity.app

import android.graphics.Bitmap
import com.duallayersecurity.app.data.models.CryptoStegoResult
import com.duallayersecurity.app.data.steganography.SteganographyManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SteganographyManagerTest {

    private lateinit var steganographyManager: SteganographyManager

    @Before
    fun setup() {
        steganographyManager = SteganographyManager()
    }

    @Test
    fun testCalculateImageCapacity() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val capacity = steganographyManager.calculateImageCapacity(bitmap)
        
        // 100x100 pixels * 3 channels = 30000 bits = 3750 bytes - 4 bytes header = 3746 bytes
        assertEquals(3746, capacity)
    }

    @Test
    fun testEmbedAndExtractData() {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val testData = "This is test data for steganography".toByteArray()

        val embedResult = steganographyManager.embedDataInImage(bitmap, testData)
        assertTrue(embedResult is CryptoStegoResult.Success)

        val stegoBitmap = (embedResult as CryptoStegoResult.Success).data

        val extractResult = steganographyManager.extractDataFromImage(stegoBitmap)
        assertTrue(extractResult is CryptoStegoResult.Success)

        val extractedData = (extractResult as CryptoStegoResult.Success).data
        assertArrayEquals(testData, extractedData)
    }

    @Test
    fun testInsufficientCapacity() {
        val smallBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val largeData = ByteArray(1000) { it.toByte() }

        val embedResult = steganographyManager.embedDataInImage(smallBitmap, largeData)
        assertTrue(embedResult is CryptoStegoResult.Error)
        assertTrue((embedResult as CryptoStegoResult.Error).message.contains("capacity insufficient"))
    }

    @Test
    fun testEmptyDataEmbedding() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val emptyData = ByteArray(0)

        val embedResult = steganographyManager.embedDataInImage(bitmap, emptyData)
        assertTrue(embedResult is CryptoStegoResult.Success)

        val stegoBitmap = (embedResult as CryptoStegoResult.Success).data
        val extractResult = steganographyManager.extractDataFromImage(stegoBitmap)
        assertTrue(extractResult is CryptoStegoResult.Success)

        val extractedData = (extractResult as CryptoStegoResult.Success).data
        assertEquals(0, extractedData.size)
    }
}
