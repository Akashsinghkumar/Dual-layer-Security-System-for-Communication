package com.duallayersecurity.app.data.models

data class EncryptedMessage(
    val encryptedData: ByteArray,
    val checksum: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedMessage

        if (!encryptedData.contentEquals(other.encryptedData)) return false
        if (checksum != other.checksum) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encryptedData.contentHashCode()
        result = 31 * result + checksum.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
