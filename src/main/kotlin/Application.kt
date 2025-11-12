package com.example

import at.favre.lib.crypto.bcrypt.BCrypt
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing
import com.example.authentification.*;
import com.example.model.BlacklistedTokens
import com.example.model.OrderProducts
import com.example.model.Orders
import com.example.model.Products
import com.example.model.Users
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun main(args: Array<String>) {
//    io.ktor.server.netty.EngineMain.main(args)
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    configureAuthentication()
    configureDatabase()
    configureSerialization()
    configureSwagger()
    configureRouting()
    println("inited");
}

private fun Application.configureSwagger() {
    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
    }
}

@OptIn(ExperimentalSerializationApi::class)
private fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            explicitNulls = false
        })
    }
}

private fun Application.configureDatabase() {
    Database.connect("jdbc:sqlite:data.db", driver = "org.sqlite.JDBC")

    transaction {
        SchemaUtils.create(Users, Orders, Products, OrderProducts, BlacklistedTokens)

        if (Users.selectAll().empty()) {
            val hashedPassword = BCrypt.withDefaults().hashToString(12, "admin".toCharArray())
            Users.insert {
                it[name] = "Admin"
                it[email] = "admin@example.com"
                it[password] = hashedPassword
            }
        }
    }
}
