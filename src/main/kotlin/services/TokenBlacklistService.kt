package com.example.services

import com.example.authentification.JwtConfig
import com.example.model.BlacklistedTokens
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class TokenBlacklistService {

    fun blacklist(token: String) {
        transaction {
            val expiresAt = System.currentTimeMillis() + JwtConfig.EXPIRATION

            try {
                BlacklistedTokens.insert {
                    it[BlacklistedTokens.token] = token
                    it[BlacklistedTokens.expiresAt] = expiresAt
                }
            } catch (_: Exception) { }
        }
    }

    fun isBlacklisted(token: String): Boolean {
        return transaction {
            BlacklistedTokens.select {
                BlacklistedTokens.token eq token
            }.singleOrNull() != null
        }
    }
}