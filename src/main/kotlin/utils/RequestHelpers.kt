package com.example.utils

import io.ktor.server.application.*
import kotlinx.serialization.json.Json

object RequestHelpers {

    fun extractTokenFromHeader(call: ApplicationCall): String? {
        return call.request.headers["Authorization"]?.removePrefix("Bearer ")
    }

    fun cleanJson(jsonText: String): String {
        return jsonText.replace("\r", "").replace("\n", "").trim()
    }

    inline fun <reified T> parseJson(jsonText: String): T {
        val json = Json {
            ignoreUnknownKeys = true
        }
        return json.decodeFromString<T>(jsonText)
    }
}