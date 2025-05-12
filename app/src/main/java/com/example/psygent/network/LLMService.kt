package com.example.psygent.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable

@Serializable
data class PromptRequest(val prompt: String)

@Serializable
data class PromptResponse(val response: String)

object LLMService {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
        install(HttpTimeout) {                // hier Timeout hinzufügen
            requestTimeoutMillis = 120_000    // 2 Minuten
            connectTimeoutMillis = 10_000     // 10 Sekunden
            socketTimeoutMillis  = 120_000    // 2 Minuten
        }
    }

    suspend fun generate(prompt: String): String {
        val resp: PromptResponse = client.post("http://10.0.2.2:5000/generate") {
            contentType(ContentType.Application.Json)
            setBody(PromptRequest(prompt))
        }.body()
        return resp.response
    }
}
