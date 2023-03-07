package com.pueblomo

import com.pueblomo.plugins.configureDatabases
import com.pueblomo.plugins.configureRouting
import com.pueblomo.plugins.configureSerialization
import com.pueblomo.plugins.configureSockets
import de.sharpmind.ktor.EnvConfig
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    configureSockets()
    configureSerialization()
    configureDatabases()
    configureRouting()
    EnvConfig.initConfig(environment.config)
}
