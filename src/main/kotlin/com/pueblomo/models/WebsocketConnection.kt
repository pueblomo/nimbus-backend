package com.pueblomo.models

import de.sharpmind.ktor.EnvConfig
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.util.*

class WebsocketConnection(val session: DefaultWebSocketSession) {
    var name = "Not Known"
    var state = ConnectionState.INIT

    suspend fun sendMessage(message: String) {
        session.send(Frame.Text(message))
    }

    suspend fun sendFileUpdate(path: String, type: MessageType) {
        val basePath = EnvConfig.getString("base_path")
        val buffer = ByteArray(1024)
        var bytesRead = 0
        withContext(Dispatchers.IO) {
            FileInputStream("$basePath/$path").use { input ->
                while (bytesRead != -1) {
                    bytesRead = input.read(buffer)
                    if (bytesRead != -1) {
                        val fileMessage = FileMessage(
                            type,
                            path,
                            Base64.getEncoder().encodeToString(buffer.copyOfRange(0, bytesRead)),
                            false
                        )
                        val messageWrapper = MessageWrapper(WrapperType.FILE, fileMessage.toBase64())
                        session.send(Frame.Text(messageWrapper.toString()))
                    }
                }
                val fileMessage = FileMessage(type, path, "", true)
                val messageWrapper = MessageWrapper(WrapperType.FILE, fileMessage.toBase64())
                session.send(Frame.Text(messageWrapper.toString()))
            }
        }
    }

    suspend fun sendFileDelete(path: String) {
        val fileMessage = FileMessage(MessageType.DELETE, path, "", false)
        val messageWrapper = MessageWrapper(WrapperType.FILE, fileMessage.toBase64())
        session.send(Frame.Text(messageWrapper.toString()))
    }
}

enum class ConnectionState {
    INIT, AUTHORIZED, SYNCING
}