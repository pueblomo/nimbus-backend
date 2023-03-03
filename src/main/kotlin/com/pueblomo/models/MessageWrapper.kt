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
data class InitialMessage(
    val clientName: String
)

@Serializable
data class FileMessage(
    val type: MessageType,
    val fileName: String,
    val data: String,
    val number: Int,
    val isLast: Boolean
) {
    fun decodeData(): ByteArray = Base64.getDecoder().decode(data)
}

enum class MessageType {
    CREATE, UPDATE, DELETE
}

enum class WrapperType {
    INITIAL, FILE
}
