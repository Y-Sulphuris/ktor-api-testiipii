package com.example.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Users : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 32)
    val email = varchar("email", 255).uniqueIndex()
    val password = varchar("passwd", 255)
    val creationDate = datetime("creation_date").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}