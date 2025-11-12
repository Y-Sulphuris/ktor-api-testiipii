package com.example.services

import com.example.model.OrderProducts
import com.example.model.Orders
import com.example.model.Products
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.name
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal
import kotlin.text.toDouble

class OrderService {

    fun createOrder(userId: Int, items: List<Pair<Int, Int>>): OrderResponse {
        return transaction {
            val productPrices = getProductPrices(items.map { it.first })
            validateProductsExist(items.map { it.first }, productPrices.keys)

            val totalPrice = calculateTotalPrice(items, productPrices)
            val orderId = createOrderRecord(userId, totalPrice)
            createOrderItems(orderId, items, productPrices)

            buildOrderResponse(orderId, userId, totalPrice)
        }
    }

    fun getUserOrders(userId: Int): List<OrderResponse> {
        return transaction {
            Orders.select { Orders.userId eq userId }.map { orderRow ->
                buildOrderResponse(
                    orderRow[Orders.id],
                    userId,
                    orderRow[Orders.totalPrice].toDouble()
                )
            }
        }
    }

    fun getUserOrder(userId: Int, orderId: Int): OrderResponse? {
        return transaction {
            Orders.select { (Orders.id eq orderId) and (Orders.userId eq userId) }
                .singleOrNull()
                ?.let { orderRow ->
                    buildOrderResponse(
                        orderId,
                        userId,
                        orderRow[Orders.totalPrice].toDouble()
                    )
                }
        }
    }

    fun updateOrder(userId: Int, orderId: Int, items: List<Pair<Int, Int>>?): OrderResponse? {
        return transaction {
            val orderExists = Orders.select { (Orders.id eq orderId) and (Orders.userId eq userId) }
                .singleOrNull() != null

            if (!orderExists) return@transaction null

            if (items != null) {
                updateOrderItems(orderId, items)
                val totalPrice = recalculateOrderTotal(orderId)
                updateOrderTotal(orderId, totalPrice)
            }

            buildOrderResponse(orderId, userId, getOrderTotal(orderId))
        }
    }

    fun deleteOrder(userId: Int, orderId: Int): Boolean {
        return transaction {
            val deleted = Orders.deleteWhere { (Orders.id eq orderId) and (Orders.userId eq userId) } > 0
            if (deleted) {
                OrderProducts.deleteWhere { OrderProducts.orderId eq orderId }
            }
            deleted
        }
    }

    fun orderExists(userId: Int, orderId: Int): Boolean {
        return transaction {
            Orders.select { (Orders.id eq orderId) and (Orders.userId eq userId) }.singleOrNull() != null
        }
    }

    private fun getProductPrices(productIds: List<Int>): Map<Int, Double> {
        return Products
            .select { Products.id inList productIds }
            .associate { it[Products.id] to it[Products.price].toDouble() }
    }

    private fun validateProductsExist(requestedIds: List<Int>, existingIds: Collection<Int>) {
        val missingProducts = requestedIds - existingIds
        if (missingProducts.isNotEmpty()) {
            throw IllegalArgumentException("Products not found: $missingProducts")
        }
    }

    private fun calculateTotalPrice(items: List<Pair<Int, Int>>, prices: Map<Int, Double>): Double {
        return items.sumOf { (productId, quantity) ->
            prices[productId]!! * quantity
        }
    }

    private fun createOrderRecord(userId: Int, totalPrice: Double): Int {
        return Orders.insert {
            it[Orders.userId] = userId
            it[Orders.totalPrice] = BigDecimal.valueOf(totalPrice)
        } get Orders.id
    }

    private fun createOrderItems(orderId: Int, items: List<Pair<Int, Int>>, prices: Map<Int, Double>) {
        items.forEach { (productId, quantity) ->
            OrderProducts.insert {
                it[OrderProducts.orderId] = orderId
                it[OrderProducts.productId] = productId
                it[OrderProducts.quantity] = quantity
                it[OrderProducts.price] = BigDecimal.valueOf(prices[productId]!!)
            }
        }
    }

    private fun updateOrderItems(orderId: Int, items: List<Pair<Int, Int>>) {
        val productPrices = getProductPrices(items.map { it.first })
        validateProductsExist(items.map { it.first }, productPrices.keys)

        OrderProducts.deleteWhere { OrderProducts.orderId eq orderId }

        items.forEach { (productId, quantity) ->
            OrderProducts.insert {
                it[OrderProducts.orderId] = orderId
                it[OrderProducts.productId] = productId
                it[OrderProducts.quantity] = quantity
                it[OrderProducts.price] = BigDecimal.valueOf(productPrices[productId]!!)
            }
        }
    }

    private fun recalculateOrderTotal(orderId: Int): Double {
        val items = OrderProducts
            .select { OrderProducts.orderId eq orderId }
            .map { it[OrderProducts.quantity] to it[OrderProducts.price].toDouble() }

        return items.sumOf { (quantity, price) -> quantity * price }
    }

    private fun updateOrderTotal(orderId: Int, totalPrice: Double) {
        Orders.update({ Orders.id eq orderId }) {
            it[Orders.totalPrice] = BigDecimal.valueOf(totalPrice)
        }
    }

    private fun getOrderTotal(orderId: Int): Double {
        return Orders.select { Orders.id eq orderId }.single()[Orders.totalPrice].toDouble()
    }

    private fun buildOrderResponse(orderId: Int, userId: Int, totalPrice: Double): OrderResponse {
        val items = getOrderItems(orderId)
        val orderRow = Orders.select { Orders.id eq orderId }.single()

        return OrderResponse(
            id = orderId,
            userId = userId,
            totalPrice = totalPrice,
            items = items,
            createdAt = orderRow[Orders.createdAt].toString()
        )
    }

    private fun getOrderItems(orderId: Int): List<OrderItemResponse> {
        return (OrderProducts innerJoin Products)
            .select { OrderProducts.orderId eq orderId }
            .map { itemRow ->
                OrderItemResponse(
                    productId = itemRow[OrderProducts.productId],
                    name = itemRow[Products.name],
                    quantity = itemRow[OrderProducts.quantity],
                    price = itemRow[OrderProducts.price].toDouble()
                )
            }
    }
}

@Serializable
data class OrderItemRequest(val productId: Int, val quantity: Int)

@Serializable
data class CreateOrderRequest(val items: List<OrderItemRequest>)

@Serializable
data class UpdateOrderRequest(val items: List<OrderItemRequest>?)

@Serializable
data class OrderItemResponse(
    val productId: Int,
    val name: String,
    val quantity: Int,
    val price: Double
)

@Serializable
data class OrderResponse(
    val id: Int,
    val userId: Int,
    val totalPrice: Double,
    val items: List<OrderItemResponse>,
    val createdAt: String
)