package com.example.authentification

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond

fun Application.configureAuthentication() {
    install(Authentication.Companion) {
        jwt("auth-jwt") {
            realm = JwtConfig.REALM
            verifier {
                JWT.require(Algorithm.HMAC256(JwtConfig.SECRET))
                    .withIssuer(JwtConfig.ISSUER)
                    .withAudience(JwtConfig.AUDIENCE)
                    .build()
            }
            validate { credential ->
                if (credential.payload.getClaim("userId").asInt() != null) {
                    JWTPrincipal(credential.payload)
                } else null
            }
            challenge { _, _ -> call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "Token is invalid or expired")
                )
            }
        }
    }
}