package com.pueblomo.models

import io.ktor.websocket.*

class WebsocketConnection(val session: DefaultWebSocketSession) {
    var name = "Not Known"
    var state = ConnectionState.INIT

    suspend fun sendMessage(message: String) {
        session.send(Frame.Text(message))
    }
}

enum class ConnectionState {
    INIT, AUTHORIZED, SYNCING
}