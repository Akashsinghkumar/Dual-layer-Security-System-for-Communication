package com.duallayersecurity.app.domain.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.duallayersecurity.app.data.crypto.CryptoManager
import javax.crypto.SecretKey

class KeyRepository(context: Context) {
    
    private val cryptoManager = CryptoManager()
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveKey(keyAlias: String, key: SecretKey) {
        val keyBase64 = cryptoManager.keyToBase64(key)
        sharedPreferences.edit().putString(keyAlias, keyBase64).apply()
    }

    fun getKey(keyAlias: String): SecretKey? {
        val keyBase64 = sharedPreferences.getString(keyAlias, null) ?: return null
        return cryptoManager.base64ToKey(keyBase64)
    }

    fun generateAndSaveKey(keyAlias: String): SecretKey {
        val key = cryptoManager.generateAESKey()
        saveKey(keyAlias, key)
        return key
    }

    fun hasKey(keyAlias: String): Boolean {
        return sharedPreferences.contains(keyAlias)
    }

    fun deleteKey(keyAlias: String) {
        sharedPreferences.edit().remove(keyAlias).apply()
    }
}
