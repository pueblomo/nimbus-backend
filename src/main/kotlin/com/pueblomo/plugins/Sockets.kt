package com.pueblomo.plugins

import com.pueblomo.models.ConnectionState
import com.pueblomo.models.MessageType
import com.pueblomo.models.MessageWrapper
import com.pueblomo.models.WebsocketConnection
import com.pueblomo.schemas.Client
import com.pueblomo.schemas.Clients
import com.pueblomo.services.ConnectionStateHandler
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
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

            fun sendFileDelete(path: String) {
                connections.filter { it.name != thisConnection.name }.forEach {
                    launch { it.sendFileDelete(path) }
                }
            }

            fun sendFileUpdate(path: String, type: MessageType) {
                connections.filter { it.name != thisConnection.name }.forEach {
                    launch { it.sendFileUpdate(path, type) }
                }
            }

            val connectionStateHandler =
                ConnectionStateHandler(
                    thisConnection,
                    sendFileDelete = { path -> sendFileDelete(path) },
                    sendFileUpdate = { path, type -> sendFileUpdate(path, type) })

            try {
                while (true) {
                    val messageWrapper = receiveDeserialized<MessageWrapper>()
                    when (thisConnection.state) {
                        ConnectionState.INIT -> {
                            connectionStateHandler.handleInitState(messageWrapper)
                            // ToDo search new files
                        }

                        ConnectionState.AUTHORIZED -> connectionStateHandler.handleAuthorizedState(messageWrapper)
                        else -> {}
                    }
                    thisConnection.session.flush()
                }
            } catch (ex: Exception) {
                logger.warn("Catched: ", ex)
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
