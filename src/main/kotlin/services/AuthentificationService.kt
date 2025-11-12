package com.example.services

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.authentification.JwtConfig
import com.example.model.Users
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class AuthentificationService {

    fun gentok(userId: Int): AuthResponse {
        val accessToken = JwtConfig.mktok(userId)
        val refreshToken = "$userId:${UUID.randomUUID()}"

        return AuthResponse(
            access_token = accessToken,
            refresh_token = refreshToken
        )
    }

    fun authenticate(email: String, password: String): Int? {
        return transaction {
            Users.select { Users.email eq email }.singleOrNull()?.let { user ->
                val passwordValid = BCrypt.verifyer()
                    .verify(password.toCharArray(), user[Users.password])
                    .verified

                if (passwordValid) {
                    user[Users.id]
                } else {
                    null
                }
            }
        }
    }

    fun validateRefreshToken(refreshToken: String): Int? {
        val parts = refreshToken.split(":")
        if (parts.size != 2) {
            return null
        }

        val userId = parts[0].toIntOrNull() ?: return null

        return transaction {
            Users.select { Users.id eq userId }.singleOrNull()?.let { _ -> userId }
        }
    }
}

@Serializable
data class AuthResponse(val access_token: String, val refresh_token: String)