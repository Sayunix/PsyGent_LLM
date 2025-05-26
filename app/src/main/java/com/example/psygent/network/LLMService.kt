package com.example.psygent.network


import com.example.psygent.network.GenerateRequest
import com.example.psygent.network.GenerateResponse

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// zum Parsen von Fehler-Bodies
@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
private data class ErrorResponse(val error: String)

object LLMService {
    private val client = HttpClient(CIO) {
        // HTTP-Logging aktivieren (Request/Response Body)
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.BODY
        }

        // JSON-Serialisierung
        install(ContentNegotiation) {
            // unbekannte Felder (z.B. "error") werden hier ignoriert
            json(Json {
                ignoreUnknownKeys = true
            })
        }

        // Timeouts
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000   // 2 Minuten insgesamt
            connectTimeoutMillis = 10_000    // 10 Sekunden bis zur Verbindung
            socketTimeoutMillis  = 120_000   // 2 Minuten für Socket-IO
        }
    }

    /**
     * Sendet den Prompt an lokalen Inference-Server
     * unter http://10.0.2.2:5000/generate
     * und gibt nur das Feld `response` zurück.
     */
    suspend fun generate(prompt: String): String {
        try {
            // sende Request
            val response: HttpResponse = client.post("http://10.0.2.2:5000/generate") {
                contentType(ContentType.Application.Json)
                setBody(GenerateRequest(prompt))
                accept(ContentType.Application.Json)
            }

            return if (response.status.isSuccess()) {
                // 2xx → body in GenerateResponse parsen
                response.body<GenerateResponse>().response
            } else {
                // 4xx/5xx → Fehlertext aus dem JSON lesen
                val text = response.bodyAsText()
                // versuch, das error-Feld zu parsen
                runCatching {
                    Json.decodeFromString<ErrorResponse>(text).error
                }.getOrElse { fallback ->
                    // falls selbst das scheitert, return rohen Body
                    text
                }.let { errMsg ->
                    "Fehler: $errMsg"
                }
            }
        } catch (e: Exception) {
            // Netzwerk- oder Parser-Exception
            return "Fehler: ${e.localizedMessage}"
        }
    }
}
