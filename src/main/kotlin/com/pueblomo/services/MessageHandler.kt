package com.pueblomo.services

import com.pueblomo.models.FileMessage
import com.pueblomo.models.MessageType
import com.pueblomo.models.WebsocketConnection
import com.pueblomo.schemas.File
import com.pueblomo.schemas.Files
import com.pueblomo.schemas.OngoingTransfer
import com.pueblomo.schemas.OngoingTransfers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.FileOutputStream
import java.time.Instant

class MessageHandler(private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    private val logger = KotlinLogging.logger {}
    fun handleWrapperTypeFile(
        data: String,
        sendMessage: (message: String) -> Job,
        sendChunks: (fileMessage: FileMessage) -> Unit
    ) {
        val fileMessage = Json.decodeFromString<FileMessage>(data)
        when (fileMessage.type) {
            MessageType.CREATE, MessageType.UPDATE -> {
                saveFileMessage(fileMessage)
                sendChunks(fileMessage)
                if (fileMessage.isLast) updateFile(fileMessage.fileName).also { sendMessage("File sync completed") }
            }

            MessageType.DELETE -> {
                // ToDo delete file
            }

            else -> sendMessage("Unknown FileMessageType: ${fileMessage.type}")
        }
    }

    private fun saveFileMessage(fileMessage: FileMessage) {
        logger.info { "Save chunk number ${fileMessage.number} for ${fileMessage.fileName}" }
        transaction {
            OngoingTransfer.new {
                fileName = fileMessage.fileName
                content = fileMessage.decodeData()
                number = fileMessage.number
                isLast = fileMessage.isLast
            }
        }
    }

    private fun updateFile(givenFileName: String) {
        logger.info { "Update File $givenFileName" }

        transaction {
            val data =
                OngoingTransfer.find { OngoingTransfers.fileName eq givenFileName }.sortedBy { it.number }
                    .map { it.content }
                    .reduce { acc, bytes -> acc.plus(bytes) }

            FileOutputStream(givenFileName).use { it.write(data) }

            OngoingTransfer.find { OngoingTransfers.fileName eq givenFileName }.forEach { it.delete() }
                .also { logger.info { "Delete updated File $givenFileName from Ongoing_transactions" } }
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

    fun handleWrapperTypeInitial(data: String, thisConnection: WebsocketConnection) {
        thisConnection.name = data
    }
}