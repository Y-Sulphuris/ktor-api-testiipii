package com.example.services

import com.example.model.Products
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.name
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal
import kotlin.collections.set
import kotlin.text.toDouble

class ProductService {

    fun createProduct(name: String, description: String, price: Double): ProductResponse {
        return transaction {
            val productId = Products.insert {
                it[Products.name] = name
                it[Products.description] = description
                it[Products.price] = BigDecimal.valueOf(price)
            } get Products.id

            ProductResponse(
                id = productId,
                name = name,
                description = description,
                price = price,
                createdAt = "now"
            )
        }
    }

    fun getAllProducts(nameFilter: String? = null): List<ProductResponse> {
        return transaction {
            val query = if (!nameFilter.isNullOrBlank()) {
                Products.select { Products.name like "%$nameFilter%" }
            } else {
                Products.selectAll()
            }

            query.map { rowToProductResponse(it) }
        }
    }

    fun getProductById(id: Int): ProductResponse? {
        return transaction {
            Products.select { Products.id eq id }.singleOrNull()?.let { rowToProductResponse(it) }
        }
    }

    fun updateProduct(id: Int, name: String?, description: String?, price: Double?): Boolean {
        return transaction {
            Products.update({ Products.id eq id }) {
                if (name != null) it[Products.name] = name
                if (description != null) it[Products.description] = description
                if (price != null) it[Products.price] = BigDecimal.valueOf(price)
            } > 0
        }
    }

    fun deleteProduct(id: Int): Boolean {
        return transaction {
            Products.deleteWhere { Products.id eq id } > 0
        }
    }

    fun productExists(id: Int): Boolean {
        return transaction {
            Products.select { Products.id eq id }.singleOrNull() != null
        }
    }

    private fun rowToProductResponse(row: ResultRow): ProductResponse {
        return ProductResponse(
            id = row[Products.id],
            name = row[Products.name],
            description = row[Products.description],
            price = row[Products.price].toDouble(),
            createdAt = row[Products.createdAt].toString()
        )
    }
}

@Serializable
data class CreateProductRequest(val name: String, val description: String, val price: Double)

@Serializable
data class UpdateProductRequest(val name: String?, val description: String?, val price: Double?)

@Serializable
data class ProductResponse(
    val id: Int,
    val name: String,
    val description: String,
    val price: Double,
    val createdAt: String
)