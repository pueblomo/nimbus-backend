package com.pueblomo.services

import com.pueblomo.models.FileMessage
import com.pueblomo.models.MessageType
import com.pueblomo.schemas.File
import com.pueblomo.schemas.Files
import com.pueblomo.schemas.OngoingTransfer
import com.pueblomo.schemas.OngoingTransfers
import de.sharpmind.ktor.EnvConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.FileOutputStream
import java.time.Instant

class MessageHandler(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
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
                saveFileMessage(fileMessage)
                sendChunks(fileMessage)
                if (fileMessage.isLast) updateFile(fileMessage.fileName)
                    .also { logger.info { "File sync completed for ${fileMessage.fileName}" } }
            }

            MessageType.DELETE -> {
                // ToDo delete file
            }
        }
    }

    private fun saveFileMessage(fileMessage: FileMessage) {
        logger.info { "Save chunk number ${fileMessage.number}from ${fileMessage.fileName} for $givenConnectionName" }
        transaction {
            OngoingTransfer.new {
                fileName = fileMessage.fileName
                content = fileMessage.decodeData()
                number = fileMessage.number
                isLast = fileMessage.isLast
                connectionName = givenConnectionName
            }
        }
    }

    private fun updateFile(givenFileName: String) {
        logger.info { "Update File $givenFileName" }
        val basePath = EnvConfig.getString("base_path")
        transaction {
            val data =
                OngoingTransfer.find {
                    (OngoingTransfers.fileName eq givenFileName) and (OngoingTransfers.connectionName eq givenConnectionName)
                }
                    .sortedBy { it.number }
                    .map { it.content }
                    .reduce { acc, bytes -> acc.plus(bytes) }

            FileOutputStream("$basePath/$givenFileName").use { it.write(data) }

            OngoingTransfer.find {
                (OngoingTransfers.fileName eq givenFileName) and (OngoingTransfers.connectionName eq givenConnectionName)
            }.forEach { it.delete() }
                .also {
                    logger.info { "Delete updated File $givenFileName from Ongoing_transactions for connection $givenConnectionName" }
                }

            File.find { Files.fileName eq givenFileName }.firstOrNull()?.let {
                it.lastUpdated = Instant.now()
                it.action = MessageType.UPDATE
            } ?: kotlin.run {
                File.new {
                    lastUpdated = Instant.now()
                    action = MessageType.CREATE
                    fileName = givenFileName
                }
            }
        }

    }

    fun isConnectionAuthorized(data: String): Boolean {
        return data.isNotBlank()
    }
}