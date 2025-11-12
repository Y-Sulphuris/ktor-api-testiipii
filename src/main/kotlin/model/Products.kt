package com.example.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Products : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val description = text("description")
    val price = decimal("price", 10, 2)
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}