package com.duallayersecurity.app

import android.graphics.Bitmap
import com.duallayersecurity.app.data.models.CryptoStegoResult
import com.duallayersecurity.app.domain.usecases.CryptoStegoUseCase
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CryptoStegoUseCaseTest {

    private lateinit var cryptoStegoUseCase: CryptoStegoUseCase

    @Before
    fun setup() {
        cryptoStegoUseCase = CryptoStegoUseCase()
    }

    @Test
    fun testFullCryptoStegoWorkflow() {
        val message = "This is a secret message that will be encrypted and hidden!"
        val password = "MySecurePassword123"
        val coverImage = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)

        // Embed
        val embedResult = cryptoStegoUseCase.performCryptoStegoEmbed(message, password, coverImage)
        assertTrue(embedResult is CryptoStegoResult.Success)

        val stegoBitmap = (embedResult as CryptoStegoResult.Success).data

        // Extract
        val extractResult = cryptoStegoUseCase.performCryptoStegoExtract(stegoBitmap, password)
        assertTrue(extractResult is CryptoStegoResult.Success)

        val extractedMessage = (extractResult as CryptoStegoResult.Success).data
        assertEquals(message, extractedMessage)
    }

    @Test
    fun testWrongPasswordDecryption() {
        val message = "Secret message"
        val correctPassword = "CorrectPassword"
        val wrongPassword = "WrongPassword"
        val coverImage = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)

        val embedResult = cryptoStegoUseCase.performCryptoStegoEmbed(message, correctPassword, coverImage)
        val stegoBitmap = (embedResult as CryptoStegoResult.Success).data

        val extractResult = cryptoStegoUseCase.performCryptoStegoExtract(stegoBitmap, wrongPassword)
        assertTrue(extractResult is CryptoStegoResult.Error)
    }

    @Test
    fun testImageTooSmall() {
        val longMessage = "A".repeat(10000)
        val password = "MySecurePassword123"
        val smallImage = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)

        val embedResult = cryptoStegoUseCase.performCryptoStegoEmbed(longMessage, password, smallImage)
        assertTrue(embedResult is CryptoStegoResult.Error)
        assertTrue((embedResult as CryptoStegoResult.Error).message.contains("too small"))
    }
}
