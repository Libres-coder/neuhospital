package com.example.neusoft_hospital.feature.ai.data

import com.example.neusoft_hospital.core.network.ApiEnvelope
import com.example.neusoft_hospital.feature.auth.domain.ChatSession
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class SendRequestDto(val sessionId: String?, val title: String?, val content: String)
data class HistoryItemDto(val role: String, val content: String, val time: Long)
data class SendResponseDto(val sessionId: String, val reply: String, val history: List<HistoryItemDto>)
data class SessionSummaryDto(val id: String, val title: String, val lastMessage: String?, val lastTime: Long)

interface AiChatApiServiceRetrofit {
    @POST("api/aichat/send")
    suspend fun send(@Body req: SendRequestDto): ApiEnvelope<SendResponseDto>

    @GET("api/aichat/sessions")
    suspend fun sessions(): ApiEnvelope<List<SessionSummaryDto>>

    @DELETE("api/aichat/sessions/{id}")
    suspend fun deleteSession(@Path("id") id: String): ApiEnvelope<Unit>

    @GET("api/aichat/sessions/{id}/history")
    suspend fun history(@Path("id") id: String): ApiEnvelope<List<HistoryItemDto>>

    fun toSessionSummary(): List<ChatSession> = emptyList()
}