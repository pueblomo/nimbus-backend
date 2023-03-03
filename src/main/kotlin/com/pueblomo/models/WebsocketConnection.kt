package com.pueblomo.models

import io.ktor.websocket.*

class WebsocketConnection(val session: DefaultWebSocketSession) {
    var name = "Not Known"
}