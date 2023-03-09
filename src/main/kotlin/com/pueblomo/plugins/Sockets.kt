package com.pueblomo.plugins

import com.pueblomo.models.*
import com.pueblomo.schemas.Client
import com.pueblomo.schemas.Clients
import com.pueblomo.services.ConnectionStateHandler
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction
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

            val connectionStateHandler =
                ConnectionStateHandler(thisConnection, sendChunks)

            try {
                while (true) {
                    val messageWrapper = receiveDeserialized<MessageWrapper>()
                    when (thisConnection.state) {
                        ConnectionState.INIT -> connectionStateHandler.handleInitState(messageWrapper)
                        ConnectionState.AUTHORIZED -> connectionStateHandler.handleAuthorizedState(messageWrapper)
                        else -> {}
                    }
                    thisConnection.session.flush()
                }
            } catch (ex: Exception) {
                logger.info("Catched: ", ex)
            } finally {
                if (thisConnection.state != ConnectionState.INIT) {
                    updateClientLastConnected(thisConnection.name)
                }
                logger.info { "Removing ${thisConnection.name}" }
                connections -= thisConnection
            }
        }
    }
}

fun updateClientLastConnected(connectionName: String) {
    transaction {
        Client.find { Clients.name eq connectionName }.firstOrNull()?.let {
            it.lastConnected = Instant.now()
        } ?: run {
            Client.new {
                name = connectionName
                lastConnected = Instant.now()
            }
        }
    }
}
