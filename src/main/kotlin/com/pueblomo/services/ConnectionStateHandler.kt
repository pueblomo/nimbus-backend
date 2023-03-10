package com.pueblomo.services

import com.pueblomo.models.*
import io.ktor.websocket.*
import mu.KotlinLogging

class ConnectionStateHandler(
    private val connection: WebsocketConnection,
    private val sendFileUpdate: (String, MessageType) -> Unit,
    private val sendFileDelete: (String) -> Unit
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
                    connection.session.close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "Not Authorized"))
                }
            }

            else -> {
                connection.session.close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "Not Authorized"))
            }
        }
    }

    suspend fun handleAuthorizedState(messageWrapper: MessageWrapper) {
        when (messageWrapper.type) {
            WrapperType.FILE -> {
                try {
                    messageHandler.handleWrapperTypeFile(
                        messageWrapper.decodeData(),
                        sendFileDelete,
                        sendFileUpdate
                    )
                } catch (ex: Exception) {
                    logger.error("Failed file handling to ${connection.name}", ex)
                    connection.sendMessage("Error on sending")
                }
            }

            else -> connection.sendMessage("Wrong wrapper type")
        }
    }
}