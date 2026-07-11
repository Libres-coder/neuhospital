package com.example.neusoft_hospital.feature.ai.data

import com.example.neusoft_hospital.core.data.prefs.UserPreferences
import com.example.neusoft_hospital.feature.auth.domain.ChatSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Chat repository. Backed by the Spring Boot service.
 *
 * `observeSessions` returns a [Flow] that is driven by an internal StateFlow.
 * Each call to [refreshSessions] fetches the latest list from the server and
 * updates the StateFlow — the UI subscribes once and stays reactive.
 */
@Singleton
class ChatRepository @Inject constructor(
    private val qwen: QwenClient,
    private val api: AiChatApiServiceRetrofit,
    private val prefs: UserPreferences
) {
    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessionsFlow: Flow<List<ChatSession>> get() = _sessions.asStateFlow()

    /** Backwards-compatible alias used by the existing screens. */
    fun observeSessions(): Flow<List<ChatSession>> = sessionsFlow

    suspend fun refreshSessions(): Result<List<ChatSession>> = runCatching {
        val resp = api.sessions()
        if (resp.code != 0) error(resp.message ?: "load sessions failed")
        val list = resp.data.orEmpty().map {
            ChatSession(it.id, it.title, it.lastMessage.orEmpty(), it.lastTime)
        }
        _sessions.value = list
        list
    }

    suspend fun createSession(uid: String, title: String): String {
        // Marker for the local UI only — the real session is created lazily
        // inside /api/aichat/send. We intentionally return null-like value so
        // the view model treats the screen as "no session yet".
        return ""
    }

    suspend fun deleteSession(id: String) {
        runCatching { api.deleteSession(id) }
        _sessions.value = _sessions.value.filterNot { it.id == id }
    }

    suspend fun updateLastMessage(sessionId: String, message: String) {
        // Server already updates lastMessage on each /send; nothing else to do here.
    }

    suspend fun sendMessage(sessionId: String, userText: String): Result<String> = runCatching {
        val reqSessionId = sessionId.takeIf { it.isNotBlank() && !it.startsWith("cs_local_") }
        val resp = api.send(SendRequestDto(reqSessionId, null, userText))
        if (resp.code != 0) error(resp.message ?: "send failed")
        resp.data?.reply ?: error("empty reply")
    }.recoverCatching { t ->
        qwen.chatSimple(userText).getOrElse { "网络异常，请稍后再试" }
    }
}