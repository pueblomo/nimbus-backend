package com.pueblomo.services

import com.pueblomo.models.*
import mu.KotlinLogging

class ConnectionStateHandler(
    private val connection: WebsocketConnection,
    private val sendChunks: (fileMessage: FileMessage) -> Unit
) {
    private val messageHandler: MessageHandler =
        MessageHandler()
    private val logger = KotlinLogging.logger {}

    suspend fun handleInitState(messageWrapper: MessageWrapper) {
        when (messageWrapper.type) {
            WrapperType.INITIAL -> {
                val isAuthorized = messageHandler.isConnectionAuthorized(messageWrapper.decodeData())
                if (isAuthorized) {
                    connection.name = messageWrapper.decodeData()
                    connection.state = ConnectionState.AUTHORIZED
                } else {
                    connection.sendMessage("Not Authorized")
                }
            }

            else -> connection.sendMessage("Not Authorized")
        }
    }

    suspend fun handleAuthorizedState(messageWrapper: MessageWrapper) {
        when (messageWrapper.type) {
            WrapperType.FILE -> {
                try {
                    messageHandler.handleWrapperTypeFile(
                        messageWrapper.decodeData(), sendChunks, connection.name
                    )
                } catch (ex: Exception) {
                    logger.error("Failed sending chunks to ${connection.name}", ex)
                }
            }

            else -> connection.sendMessage("Wrong wrapper type")
        }
    }
}