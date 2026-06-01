package com.duallayersecurity.app.data.models

sealed class CryptoStegoResult<out T> {
    data class Success<T>(val data: T) : CryptoStegoResult<T>()
    data class Error(val message: String, val exception: Exception? = null) : CryptoStegoResult<Nothing>()
}
