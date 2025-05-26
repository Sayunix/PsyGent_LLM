package com.example.psygent.network

import kotlinx.serialization.Serializable

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class GenerateResponse(val response: String)