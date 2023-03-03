package com.pueblomo.schemas

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.*

object Clients : UUIDTable() {
    val name = varchar("name", 250)
    val lastConnected = timestamp("last_connected")
}

class Client(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, Client>(Clients)

    var name by Clients.name
    var lastConnected by Clients.lastConnected
}