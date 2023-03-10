package com.pueblomo.services

import com.pueblomo.exceptions.MessageHandlerException
import com.pueblomo.models.FileMessage
import com.pueblomo.models.MessageType
import com.pueblomo.schemas.File
import com.pueblomo.schemas.Files
import de.sharpmind.ktor.EnvConfig
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.FileOutputStream
import java.time.Instant
import kotlin.io.path.Path

class MessageHandler {
    private val logger = KotlinLogging.logger {}
    fun handleWrapperTypeFile(
        data: String,
        sendFileDelete: (String) -> Unit,
        sendFileUpdate: (String, MessageType) -> Unit
    ) {
        val fileMessage = Json.decodeFromString<FileMessage>(data)
        when (fileMessage.type) {
            MessageType.CREATE, MessageType.UPDATE -> {
                try {
                    val basePath = EnvConfig.getString("base_path")
                    createFolderStructure("$basePath/${fileMessage.filePath}")
                    if (!fileMessage.isLast) {
                        FileOutputStream("$basePath/${fileMessage.filePath}").use { it.write(fileMessage.decodeData()) }
                    } else {
                        transaction {
                            File.find { Files.filePath eq fileMessage.filePath }.firstOrNull()?.let {
                                it.lastUpdated = Instant.now()
                                it.action = MessageType.UPDATE
                            } ?: kotlin.run {
                                File.new {
                                    lastUpdated = Instant.now()
                                    action = MessageType.CREATE
                                    filePath = fileMessage.filePath
                                }
                            }
                        }
                        logger.info("Saved File at ${fileMessage.filePath}")
                        sendFileUpdate(fileMessage.filePath, fileMessage.type)
                    }
                } catch (ex: Exception) {
                    throw MessageHandlerException(fileMessage.type, fileMessage.filePath, ex)
                }
            }

            MessageType.DELETE -> {
                val basePath = EnvConfig.getString("base_path")
                val deleted = java.io.File("$basePath/${fileMessage.filePath}").delete()
                if (deleted) {
                    logger.info("Deleted file ${fileMessage.filePath}")
                    transaction {
                        File.find { Files.filePath eq fileMessage.filePath }.first().let {
                            it.lastUpdated = Instant.now()
                            it.action = MessageType.DELETE
                        }
                    }
                    sendFileDelete(fileMessage.filePath)
                } else {
                    throw MessageHandlerException(MessageType.DELETE, fileMessage.filePath, null)
                }
            }
        }
    }

    private fun createFolderStructure(path: String) {
        val folders = path.removeSuffix(Path(path).fileName.toString())
        java.io.File(folders).mkdirs()
    }

    fun isConnectionAuthorized(data: String): Boolean {
        return data.isNotBlank()
    }
}