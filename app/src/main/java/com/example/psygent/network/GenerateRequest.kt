package com.example.psygent.network

import kotlinx.serialization.Serializable

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class GenerateRequest(
    val prompt: String,
    val history: List<ChatTurn>
)

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class ChatTurn(val user: String, val assistant: String)