package com.example.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime


object Orders : Table() {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val totalPrice = decimal("total_price", 16, 2)
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}

object OrderProducts : Table() {
    val orderId = integer("order_id").references(Orders.id)
    val productId = integer("product_id").references(Products.id)
    val quantity = integer("quantity").default(1)
    val price = decimal("price", 16, 2)
}