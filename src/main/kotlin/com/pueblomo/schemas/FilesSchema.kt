package com.pueblomo.schemas

import com.pueblomo.models.MessageType
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.*

object Files : UUIDTable() {
    val fileName = varchar("file_name", 250)
    val lastUpdated = timestamp("last_updated")
    val action = enumerationByName<MessageType>("action", 20)
}

class File(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, File>(Files)

    var fileName by Files.fileName
    var lastUpdated by Files.lastUpdated
    var action by Files.action
}