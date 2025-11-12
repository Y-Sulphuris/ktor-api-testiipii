package com.example.utils

import com.example.MessageResponse
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.http.*
import io.ktor.server.auth.principal

object AuthHelpers {

    fun getUserIdFromPrincipal(call: ApplicationCall): Int? {
        return call.principal<JWTPrincipal>()?.getClaim("userId", Int::class)
    }

    suspend fun respondIfUnauthorized(call: ApplicationCall, userId: Int?): Boolean {
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized, MessageResponse("Invalid token"))
            return true
        }
        return false
    }
}