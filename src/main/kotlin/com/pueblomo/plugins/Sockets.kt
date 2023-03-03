package com.pueblomo.plugins

import com.pueblomo.models.FileMessage
import com.pueblomo.models.MessageWrapper
import com.pueblomo.models.WebsocketConnection
import com.pueblomo.models.WrapperType
import com.pueblomo.schemas.Client
import com.pueblomo.schemas.Clients
import com.pueblomo.services.MessageHandler
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant
import java.util.*

fun Application.configureSockets() {
    val logger = KotlinLogging.logger {}
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    val messageHandler = MessageHandler()
    routing {
        val connections = Collections.synchronizedSet<WebsocketConnection>(LinkedHashSet())
        webSocket("/ws") {
            val thisConnection = WebsocketConnection(this)
            connections += thisConnection

            val sendChunks = { fileMessage: FileMessage ->
                connections.filter { it != thisConnection }.forEach {
                    val messageWrapper = MessageWrapper(
                        WrapperType.FILE,
                        Base64.getEncoder().encodeToString(fileMessage.toString().encodeToByteArray())
                    )
                    launch {
                        it.session.send(Frame.Binary(true, messageWrapper.toString().encodeToByteArray()))
                    }
                }
            }

            val sendMessage = { message: String -> launch { thisConnection.session.send(Frame.Text(message)) } }

            try {
                while (true) {
                    val messageWrapper = receiveDeserialized<MessageWrapper>()
                    when (messageWrapper.type) {
                        WrapperType.FILE ->
                            messageHandler.handleWrapperTypeFile(
                                messageWrapper.decodeData(),
                                sendMessage,
                                sendChunks
                            ).also { logger.info("Handling file for ${thisConnection.name}") }

                        WrapperType.INITIAL -> messageHandler.handleWrapperTypeInitial(
                            messageWrapper.decodeData(),
                            thisConnection
                        )
                    }
                }
            } catch (ex: Exception) {
                logger.info { ex.message }
            } finally {
                logger.info { "Removing ${thisConnection.name}" }
                updateClientLastConnected(thisConnection.name)
                connections -= thisConnection
            }
        }
    }
}

fun updateClientLastConnected(connectionName: String) {
    Client.find { Clients.name eq connectionName }.firstOrNull()?.let {
        it.lastConnected = Instant.now()
    } ?: run {
        Client.new {
            name = connectionName
            lastConnected = Instant.now()
        }
    }
}
