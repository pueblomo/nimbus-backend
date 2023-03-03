package com.pueblomo

import com.pueblomo.plugins.configureDatabases
import com.pueblomo.plugins.configureRouting
import com.pueblomo.plugins.configureSerialization
import com.pueblomo.plugins.configureSockets
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSockets()
    configureSerialization()
    configureDatabases()
    configureRouting()
}
