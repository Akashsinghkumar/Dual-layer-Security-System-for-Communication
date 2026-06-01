package com.duallayersecurity.app.ui.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duallayersecurity.app.data.crypto.CryptoManager
import com.duallayersecurity.app.data.database.AppDatabase
import com.duallayersecurity.app.data.database.UserEntity
import com.duallayersecurity.app.data.models.CryptoStegoResult
import com.duallayersecurity.app.domain.usecases.CryptoStegoUseCase
import com.duallayersecurity.app.domain.usecases.ExtractedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class CryptoStegoViewModel(context: Context) : ViewModel() {

    private val userDao = AppDatabase.getDatabase(context).userDao()
    private val cryptoStegoUseCase = CryptoStegoUseCase()
    private val cryptoManager = CryptoManager()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.LoggedOut)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            if (username.isBlank() || password.isBlank()) {
                _authState.value = AuthState.Error("Username and password cannot be empty")
                return@launch
            }

            val result = withContext(Dispatchers.IO) {
                try {
                    val user = userDao.getUserByUsername(username)
                    if (user == null) {
                        AuthState.Error("User not found")
                    } else {
                        // Calculate password hash using salt stored in db
                        val salt = user.salt
                        val hashInput = password + salt
                        val calculatedHash = cryptoManager.calculateSHA256(hashInput.toByteArray())
                        
                        if (calculatedHash == user.passwordHash) {
                            AuthState.LoggedIn(user.username)
                        } else {
                            AuthState.Error("Incorrect password")
                        }
                    }
                } catch (e: Exception) {
                    AuthState.Error("Authentication failed: ${e.message}")
                }
            }
            _authState.value = result
        }
    }

    fun register(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            if (username.isBlank() || password.isBlank()) {
                _authState.value = AuthState.Error("Username and password cannot be empty")
                return@launch
            }

            val result = withContext(Dispatchers.IO) {
                try {
                    val userExists = userDao.doesUserExist(username) > 0
                    if (userExists) {
                        AuthState.Error("Username already taken")
                    } else {
                        // Generate random salt for password storage
                        val saltBytes = cryptoManager.generateSalt()
                        val saltString = android.util.Base64.encodeToString(saltBytes, android.util.Base64.NO_WRAP)
                        
                        val hashInput = password + saltString
                        val passwordHash = cryptoManager.calculateSHA256(hashInput.toByteArray())
                        
                        val newUser = UserEntity(
                            username = username,
                            passwordHash = passwordHash,
                            salt = saltString
                        )
                        userDao.registerUser(newUser)
                        AuthState.RegisterSuccess
                    }
                } catch (e: Exception) {
                    AuthState.Error("Registration failed: ${e.message}")
                }
            }
            _authState.value = result
        }
    }

    fun logout() {
        _authState.value = AuthState.LoggedOut
        _uiState.value = UiState.Idle
    }

    fun embedMessage(message: String, password: String, coverImage: Bitmap) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            if (password.isEmpty()) {
                _uiState.value = UiState.Error("Encryption password cannot be empty")
                return@launch
            }

            val result = withContext(Dispatchers.Default) {
                cryptoStegoUseCase.performCryptoStegoEmbed(message, password, coverImage)
            }

            _uiState.value = when (result) {
                is CryptoStegoResult.Success -> UiState.EmbedSuccess(result.data)
                is CryptoStegoResult.Error -> UiState.Error(result.message)
            }
        }
    }

    fun extractMessage(stegoImage: Bitmap, password: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            if (password.isEmpty()) {
                _uiState.value = UiState.Error("Decryption password cannot be empty")
                return@launch
            }

            Log.d("DLS_DEBUG", "extractMessage: bitmap=${stegoImage.width}x${stegoImage.height}, config=${stegoImage.config}, password length=${password.length}")

            val result = withContext(Dispatchers.Default) {
                cryptoStegoUseCase.performCryptoStegoExtract(stegoImage, password)
            }

            Log.d("DLS_DEBUG", "extractMessage result: ${result::class.simpleName} -> ${if (result is CryptoStegoResult.Error) result.message else "OK"}")

            _uiState.value = when (result) {
                is CryptoStegoResult.Success -> UiState.ExtractSuccess(result.data)
                is CryptoStegoResult.Error -> UiState.Error(result.message)
            }
        }
    }

    fun embedFile(fileBytes: ByteArray, fileName: String, password: String, coverImage: Bitmap) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            if (password.isEmpty()) {
                _uiState.value = UiState.Error("Encryption password cannot be empty")
                return@launch
            }

            Log.d("DLS_DEBUG", "embedFile: fileName=$fileName, fileSize=${fileBytes.size}, imageSize=${coverImage.width}x${coverImage.height}")

            val result = withContext(Dispatchers.Default) {
                cryptoStegoUseCase.performFileEmbed(fileBytes, fileName, password, coverImage)
            }

            _uiState.value = when (result) {
                is CryptoStegoResult.Success -> UiState.FileEmbedSuccess(result.data, fileName)
                is CryptoStegoResult.Error -> UiState.Error(result.message)
            }
        }
    }

    fun extractFile(stegoImage: Bitmap, password: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            if (password.isEmpty()) {
                _uiState.value = UiState.Error("Decryption password cannot be empty")
                return@launch
            }

            Log.d("DLS_DEBUG", "extractFile: bitmap=${stegoImage.width}x${stegoImage.height}, config=${stegoImage.config}")

            val result = withContext(Dispatchers.Default) {
                cryptoStegoUseCase.performFileExtract(stegoImage, password)
            }

            _uiState.value = when (result) {
                is CryptoStegoResult.Success -> UiState.FileExtractSuccess(result.data)
                is CryptoStegoResult.Error -> UiState.Error(result.message)
            }
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }

    fun clearAuthError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.LoggedOut
        }
    }

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class EmbedSuccess(val stegoBitmap: Bitmap) : UiState()
        data class ExtractSuccess(val message: String) : UiState()
        data class FileEmbedSuccess(val stegoBitmap: Bitmap, val fileName: String) : UiState()
        data class FileExtractSuccess(val extractedFile: ExtractedFile) : UiState()
        data class Error(val message: String) : UiState()
    }

    sealed class AuthState {
        object LoggedOut : AuthState()
        object Loading : AuthState()
        object RegisterSuccess : AuthState()
        data class LoggedIn(val username: String) : AuthState()
        data class Error(val message: String) : AuthState()
    }
}
