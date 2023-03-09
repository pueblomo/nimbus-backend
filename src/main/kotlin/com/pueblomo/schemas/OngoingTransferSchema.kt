package com.pueblomo.schemas

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object OngoingTransfers : UUIDTable() {
    val fileName = varchar("file_name", 250)
    val content = binary("content", 100000)
    val number = integer("number")
    val isLast = bool("is_last")
    val connectionName = varchar("connection_name", 250)
}

class OngoingTransfer(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, OngoingTransfer>(OngoingTransfers)

    var fileName by OngoingTransfers.fileName
    var content by OngoingTransfers.content
    var number by OngoingTransfers.number
    var isLast by OngoingTransfers.isLast
    var connectionName by OngoingTransfers.connectionName
}