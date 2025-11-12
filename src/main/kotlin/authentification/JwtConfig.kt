package com.example.authentification

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

object JwtConfig {
    const val REALM = "Ktor Testiipii"
    const val SECRET = "top-sicret-key"
    const val ISSUER = "ktor-test-api"
    const val AUDIENCE = "postman-client"
    const val EXPIRATION = 1000 * 60 * 60

    fun mktok(userId: Int): String {
        return JWT.create()
            .withAudience(AUDIENCE).withIssuer(ISSUER)
            .withClaim("userId", userId)
            .withExpiresAt(Date(System.currentTimeMillis() + EXPIRATION))
            .sign(m_algo)
    }

    fun verify(token: String): Int? {
        try {
            val verifier = JWT.require(m_algo)
                .withAudience(AUDIENCE).withIssuer(ISSUER)
                .build()

            return verifier
                .verify(token.replace("Bearer ", ""))
                .getClaim("userId").asInt()
        } catch (e: Exception) {
            e.printStackTrace();
            return null;
        }
    }

    private val m_algo = Algorithm.HMAC256(SECRET)
}
