package com.example

import com.example.utils.RequestHelpers
import com.example.services.AuthentificationService
import com.example.services.CreateOrderRequest
import com.example.services.CreateProductRequest
import com.example.services.CreateUserRequest
import com.example.services.OrderService
import com.example.services.ProductService
import com.example.services.TokenBlacklistService
import com.example.services.UpdateOrderRequest
import com.example.services.UpdateProductRequest
import com.example.services.UpdateUserRequest
import com.example.services.UserService
import com.example.utils.AuthHelpers
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.serialization.Serializable

fun Application.configureRouting() {
    val orderService = OrderService()
    val authService = AuthentificationService()
    val blService = TokenBlacklistService()
    val productService = ProductService()
    val userService = UserService()

    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        route("/authentication") {
            post("/login") {
                loginUser(call, authService)
            }
            post("/refresh-token") {
                refreshToken(call, authService)
            }

            authenticate("auth-jwt") {
                post("/logout") {
                    logoutUser(call, blService)
                }
            }
        }
        route("/users") {
            authenticate("auth-jwt") {
                put("/{id}") {
                    updateUser(call, userService)
                }

                delete("/{id}") {
                    deleteUser(call, userService)
                }
            }

            post {
                createUser(call, userService)
            }

            get {
                getUsers(call, userService)
            }

            get("/{id}") {
                getUserById(call, userService)
            }
        }
        route("/products") {
            get {
                getProducts(call, productService)
            }

            get("/{id}") {
                getProductById(call, productService)
            }
            authenticate("auth-jwt") {
                post {
                    createProduct(call, productService)
                }

                put("/{id}") {
                    updateProduct(call, productService)
                }

                delete("/{id}") {
                    deleteProduct(call, productService)
                }
            }
        }
        route("/orders") {
            authenticate("auth-jwt") {
                post {
                    createOrder(call, orderService)
                }
                get {
                    getUserOrders(call, orderService)
                }
                get("/{id}") {
                    getOrderById(call, orderService)
                }

                put("/{id}") {
                    updateOrder(call, orderService)
                }

                delete("/{id}") {
                    deleteOrder(call, orderService)
                }
            }
        }
    }
}

private suspend fun loginUser(call: ApplicationCall, authService: AuthentificationService) {
    try {
        val jsonText = call.receiveText()
        val cleanedJson = RequestHelpers.cleanJson(jsonText)
        val request = RequestHelpers.parseJson<LoginRequest>(cleanedJson)

        if (request.email.isBlank() || request.password.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, MessageResponse("Both email and password are required"))
            return
        }

        val userId = authService.authenticate(request.email, request.password)
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized, MessageResponse("Invalid credentials"))
            return
        }

        val tokens = authService.gentok(userId)
        call.respond(tokens)

    } catch (e: Exception) {
        call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid request: ${e.message}"))
    }
}

private suspend fun refreshToken(call: ApplicationCall, authService: AuthentificationService) {
    try {
        val jsonText = call.receiveText()
        val cleanedJson = RequestHelpers.cleanJson(jsonText)
        val request = RequestHelpers.parseJson<RefreshTokenRequest>(cleanedJson)

        if (request.refresh_token.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, MessageResponse("Refresh token is required"))
            return
        }

        val userId = authService.validateRefreshToken(request.refresh_token)
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized, MessageResponse("Invalid refresh token"))
            return
        }

        val tokens = authService.gentok(userId)
        call.respond(tokens)
    } catch (e: Exception) {
        call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid request: ${e.message}"))
    }
}


private suspend fun logoutUser(call: ApplicationCall, tokenBlacklistService: TokenBlacklistService) = try {
    val token = RequestHelpers.extractTokenFromHeader(call) ?: run {
        call.respond(HttpStatusCode.BadRequest, MessageResponse("Token is required"))
        return
    }

    tokenBlacklistService.blacklist(token)
    call.respond(MessageResponse("Logged out successfully"))
} catch (e: Exception) {
    call.respond(HttpStatusCode.BadRequest, MessageResponse("Logout failed: ${e.message}"))
}


private suspend fun createOrder(call: ApplicationCall, orderService: OrderService) {
    try {
        val userId = AuthHelpers.getUserIdFromPrincipal(call)
        if (AuthHelpers.respondIfUnauthorized(call, userId)) return

        val jsonText = call.receiveText()
        val cleanedJson = RequestHelpers.cleanJson(jsonText)
        val request = RequestHelpers.parseJson<CreateOrderRequest>(cleanedJson)

        if (request.items.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, MessageResponse("Order must contain items"))
            return
        }

        val orderItems = request.items.map { it.productId to it.quantity }
        val orderResponse = orderService.createOrder(userId!!, orderItems)

        call.respond(HttpStatusCode.Created, orderResponse)

    } catch (e: Exception) {
        call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid request: ${e.message}"))
    }
}

private suspend fun getUserOrders(call: ApplicationCall, orderService: OrderService) {
    val userId = AuthHelpers.getUserIdFromPrincipal(call)
    if (AuthHelpers.respondIfUnauthorized(call, userId)) return

    val orders = orderService.getUserOrders(userId!!)
    call.respond(orders)
}

private suspend fun getOrderById(call: ApplicationCall, orderService: OrderService) {
    val userId = AuthHelpers.getUserIdFromPrincipal(call)
    if (AuthHelpers.respondIfUnauthorized(call, userId)) return

    val orderId = call.parameters["id"]?.toIntOrNull()
        ?: return call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid order ID"))

    val order = orderService.getUserOrder(userId!!, orderId)
        ?: return call.respond(HttpStatusCode.NotFound, MessageResponse("Order not found"))

    call.respond(order)
}

private suspend fun updateOrder(call: ApplicationCall, orderService: OrderService) {
    try {
        val userId = AuthHelpers.getUserIdFromPrincipal(call)
        if (AuthHelpers.respondIfUnauthorized(call, userId)) return

        val orderId = call.parameters["id"]?.toIntOrNull()
            ?: return call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid order ID"))

        val jsonText = call.receiveText()
        val cleanedJson = RequestHelpers.cleanJson(jsonText)
        val request = RequestHelpers.parseJson<UpdateOrderRequest>(cleanedJson)

        val orderItems = request.items?.map { it.productId to it.quantity }
        val updatedOrder = orderService.updateOrder(userId!!, orderId, orderItems)
            ?: return call.respond(HttpStatusCode.NotFound, MessageResponse("Order not found"))

        call.respond(updatedOrder)

    } catch (e: Exception) {
        call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid request: ${e.message}"))
    }
}

private suspend fun deleteOrder(call: ApplicationCall, orderService: OrderService) {
    val userId = AuthHelpers.getUserIdFromPrincipal(call)
    if (AuthHelpers.respondIfUnauthorized(call, userId)) return

    val orderId = call.parameters["id"]?.toIntOrNull()
        ?: return call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid order ID"))

    val deleted = orderService.deleteOrder(userId!!, orderId)
    if (!deleted) {
        return call.respond(HttpStatusCode.NotFound, MessageResponse("Order not found"))
    }

    call.respond(MessageResponse("Order deleted"))
}


private suspend fun createProduct(call: ApplicationCall, productService: ProductService) {
    try {
        val jsonText = call.receiveText()
        val cleanedJson = RequestHelpers.cleanJson(jsonText)
        val request = RequestHelpers.parseJson<CreateProductRequest>(cleanedJson)

        if (request.name.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, MessageResponse("Product name is required"))
            return
        }

        if (request.price <= 0) {
            call.respond(HttpStatusCode.BadRequest, MessageResponse("Price must be positive"))
            return
        }

        val productResponse = productService.createProduct(
            name = request.name,
            description = request.description,
            price = request.price
        )

        call.respond(HttpStatusCode.Created, productResponse)

    } catch (e: Exception) {
        call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid request: ${e.message}"))
    }
}

private suspend fun getProducts(call: ApplicationCall, productService: ProductService) {
    val nameFilter = call.request.queryParameters["name"]
    val products = productService.getAllProducts(nameFilter)
    call.respond(products)
}

private suspend fun getProductById(call: ApplicationCall, productService: ProductService) {
    val productId = call.parameters["id"]?.toIntOrNull()
        ?: return call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid product ID"))

    val product = productService.getProductById(productId)
        ?: return call.respond(HttpStatusCode.NotFound, MessageResponse("Product not found"))

    call.respond(product)
}

private suspend fun updateProduct(call: ApplicationCall, productService: ProductService) {
    try {
        val productId = call.parameters["id"]?.toIntOrNull()
            ?: return call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid product ID"))

        val jsonText = call.receiveText()
        val cleanedJson = RequestHelpers.cleanJson(jsonText)
        val request = RequestHelpers.parseJson<UpdateProductRequest>(cleanedJson)

        val updated = productService.updateProduct(
            id = productId,
            name = request.name,
            description = request.description,
            price = request.price
        )

        if (updated) {
            call.respond(MessageResponse("Product updated"))
        } else {
            call.respond(HttpStatusCode.NotFound, MessageResponse("Product not found"))
        }

    } catch (e: Exception) {
        call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid request: ${e.message}"))
    }
}

private suspend fun deleteProduct(call: ApplicationCall, productService: ProductService) {
    val productId = call.parameters["id"]?.toIntOrNull()
        ?: return call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid product ID"))

    val deleted = productService.deleteProduct(productId)
    if (deleted) {
        call.respond(MessageResponse("Product Deleted"))
    } else {
        call.respond(HttpStatusCode.NotFound, MessageResponse("Product Not Found"))
    }
}

@Serializable
data class RefreshTokenRequest(val refresh_token: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
@Schema(description = "Generic message response")
data class MessageResponse(
    @Schema(description = "Response message", example = "Operation completed successfully")
    val message: String
)


private suspend fun createUser(call: ApplicationCall, userService: UserService) {
    try {
        val jsonText = call.receiveText()
        val cleanedJson = RequestHelpers.cleanJson(jsonText)
        val request = RequestHelpers.parseJson<CreateUserRequest>(cleanedJson)

        if (request.name.isBlank() || request.email.isBlank() || request.password.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, MessageResponse("You missed something"))
            return
        }

        if (userService.emailExists(request.email)) {
            call.respond(HttpStatusCode.Conflict, MessageResponse("User with this email already exist"))
            return
        }

        val userResponse = userService.createUser(
            name = request.name,
            email = request.email,
            password = request.password
        )

        call.respond(HttpStatusCode.Created, userResponse)

    } catch (e: Exception) {
        call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid user request: ${e.message}"))
    }
}

private suspend fun getUsers(call: ApplicationCall, userService: UserService) {
    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10

    val users = userService.getAllUsers(page, limit)
    call.respond(users)
}

private suspend fun getUserById(call: ApplicationCall, userService: UserService) {
    val userId = call.parameters["id"]?.toIntOrNull()
        ?: return call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid user ID"))

    val user = userService.getUserById(userId)
        ?: return call.respond(HttpStatusCode.NotFound, MessageResponse("User not found"))

    call.respond(user)
}

private suspend fun updateUser(call: ApplicationCall, userService: UserService) {
    try {
        val userId = call.parameters["id"]?.toIntOrNull()
            ?: return call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid user ID"))

        val currentUserId = AuthHelpers.getUserIdFromPrincipal(call)
        if (AuthHelpers.respondIfUnauthorized(call, currentUserId)) return

        val jsonText = call.receiveText()
        val cleanedJson = RequestHelpers.cleanJson(jsonText)
        val request = RequestHelpers.parseJson<UpdateUserRequest>(cleanedJson)

        // Проверяем, что email не занят другими пользователями
        if (request.email != null && userService.emailExists(request.email, excludeUserId = userId)) {
            call.respond(HttpStatusCode.Conflict, MessageResponse("User with this email already exists"))
            return
        }

        val updated = userService.updateUser(
            id = userId,
            name = request.name,
            email = request.email
        )

        if (updated) {
            call.respond(MessageResponse("User updated"))
        } else {
            call.respond(HttpStatusCode.NotFound, MessageResponse("User not found"))
        }

    } catch (e: Exception) {
        call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid request: ${e.message}"))
    }
}

private suspend fun deleteUser(call: ApplicationCall, userService: UserService) {
    val userId = call.parameters["id"]?.toIntOrNull()
        ?: return call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid user ID"))

    val currentUserId = AuthHelpers.getUserIdFromPrincipal(call)
    if (AuthHelpers.respondIfUnauthorized(call, currentUserId)) return

    val deleted = userService.deleteUser(userId)
    if (deleted) {
        call.respond(MessageResponse("User deleted"))
    } else {
        call.respond(HttpStatusCode.NotFound, MessageResponse("No such user"))
    }
}