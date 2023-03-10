package com.pueblomo.models

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class MessageWrapper(
    val type: WrapperType,
    val data: String,
) {
    fun decodeData(): String = String(Base64.getDecoder().decode(data))
}

@Serializable
data class FileMessage(
    val type: MessageType,
    val filePath: String,
    val data: String,
    val isLast: Boolean
) {
    fun decodeData(): ByteArray = Base64.getDecoder().decode(data)
    fun toBase64(): String = Base64.getEncoder().encodeToString(this.toString().encodeToByteArray())
}

enum class MessageType {
    CREATE, UPDATE, DELETE
}

enum class WrapperType {
    INITIAL, FILE
}
