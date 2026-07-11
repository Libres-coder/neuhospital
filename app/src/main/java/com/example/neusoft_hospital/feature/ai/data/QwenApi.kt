package com.example.neusoft_hospital.feature.ai.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class QwenMessage(
    val role: String, // system / user / assistant
    val content: String
)

@JsonClass(generateAdapter = true)
data class QwenRequest(
    val model: String = "qwen-plus",
    val messages: List<QwenMessage>,
    val stream: Boolean = false,
    val temperature: Double = 0.3
)

@JsonClass(generateAdapter = true)
data class QwenChoice(
    val index: Int,
    val message: QwenMessage,
    @Json(name = "finish_reason") val finishReason: String?
)

@JsonClass(generateAdapter = true)
data class QwenResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<QwenChoice>,
    val usage: Map<String, Int>? = null
)

interface QwenApiService {
    @POST("chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") authorization: String,
        @Body request: QwenRequest
    ): QwenResponse
}