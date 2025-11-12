package com.example.model

import org.jetbrains.exposed.sql.Table

object BlacklistedTokens : Table("blacklisted_tokens") {
    val token = varchar("token", 512).uniqueIndex()
    val expiresAt = long("expires_at")
}