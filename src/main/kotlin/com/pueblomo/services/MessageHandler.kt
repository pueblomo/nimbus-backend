package com.pueblomo.services

import com.pueblomo.models.FileMessage
import com.pueblomo.models.MessageType
import com.pueblomo.schemas.File
import com.pueblomo.schemas.Files
import de.sharpmind.ktor.EnvConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.FileOutputStream
import java.time.Instant

class MessageHandler(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    companion object {
        val LIMIT = 3
    }

    private val logger = KotlinLogging.logger {}
    private var givenConnectionName: String = ""
    fun handleWrapperTypeFile(
        data: String,
        sendChunks: (fileMessage: FileMessage) -> Unit,
        connectionName: String
    ) {
        givenConnectionName = connectionName
        val fileMessage = Json.decodeFromString<FileMessage>(data)
        when (fileMessage.type) {
            MessageType.CREATE, MessageType.UPDATE -> {
                val basePath = EnvConfig.getString("base_path")
                if (!fileMessage.isLast) {
                    FileOutputStream("$basePath/${fileMessage.fileName}").use { it.write(fileMessage.decodeData()) }
                } else {
                    transaction {
                        File.find { Files.fileName eq fileMessage.fileName }.firstOrNull()?.let {
                            it.lastUpdated = Instant.now()
                            it.action = MessageType.UPDATE
                        } ?: kotlin.run {
                            File.new {
                                lastUpdated = Instant.now()
                                action = MessageType.CREATE
                                fileName = fileMessage.fileName
                            }
                        }
                    }
                }
                sendChunks(fileMessage)
            }

            MessageType.DELETE -> {
                // ToDo delete file
            }
        }
    }

    fun isConnectionAuthorized(data: String): Boolean {
        return data.isNotBlank()
    }
}