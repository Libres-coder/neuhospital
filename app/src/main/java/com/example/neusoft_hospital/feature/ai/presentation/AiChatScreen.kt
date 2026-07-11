package com.example.neusoft_hospital.feature.ai.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.neusoft_hospital.core.data.prefs.UserPreferences
import com.example.neusoft_hospital.feature.ai.data.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMsg(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // user / assistant
    val text: String,
    val time: Long = System.currentTimeMillis()
)

data class AiChatUiState(
    val sessionId: String? = null,
    val messages: List<ChatMsg> = emptyList(),
    val input: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val chatRepo: ChatRepository,
    private val prefs: UserPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val initialSessionId: String? = savedStateHandle.get<String>("sessionId")
    private val _ui = MutableStateFlow(AiChatUiState(sessionId = initialSessionId))
    val ui: StateFlow<AiChatUiState> = _ui

    init {
        if (initialSessionId == null) {
            viewModelScope.launch {
                val uid = prefs.tokenFlow.first()
                val newId = chatRepo.createSession(uid, "新会话")
                _ui.value = _ui.value.copy(sessionId = newId,
                    messages = listOf(ChatMsg(role = "assistant", text = "您好，我是东软医院 AI 医疗助手。请问您哪里不舒服？"))
                )
            }
        } else {
            _ui.value = _ui.value.copy(
                messages = listOf(ChatMsg(role = "assistant", text = "您好，请问您需要什么帮助？"))
            )
        }
    }

    fun onInputChange(v: String) { _ui.value = _ui.value.copy(input = v, error = null) }

    fun send() {
        val text = _ui.value.input.trim()
        if (text.isBlank()) return
        val sessionId = _ui.value.sessionId ?: return
        _ui.value = _ui.value.copy(
            messages = _ui.value.messages + ChatMsg(role = "user", text = text),
            input = "",
            loading = true
        )
        viewModelScope.launch {
            chatRepo.sendMessage(sessionId, text).fold(
                onSuccess = { reply ->
                    _ui.value = _ui.value.copy(
                        messages = _ui.value.messages + ChatMsg(role = "assistant", text = reply),
                        loading = false
                    )
                },
                onFailure = { _ui.value = _ui.value.copy(loading = false, error = it.message) }
            )
        }
    }

    fun startNew() {
        viewModelScope.launch {
            val uid = prefs.tokenFlow.first()
            val newId = chatRepo.createSession(uid, "新会话")
            _ui.value = AiChatUiState(sessionId = newId, messages = listOf(ChatMsg(role = "assistant", text = "新会话已开启，请问您需要什么帮助？")))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(navController: NavController, vm: AiChatViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(ui.messages.size) {
        if (ui.messages.isNotEmpty()) listState.animateScrollToItem(ui.messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 医疗助手") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { navController.navigate("chat_history") }) { Icon(Icons.Default.History, null) }
                    IconButton(onClick = vm::startNew) { Icon(Icons.Default.Add, null) }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp) {
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = ui.input,
                        onValueChange = vm::onInputChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("请描述您的健康问题…") },
                        maxLines = 4
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = vm::send, enabled = !ui.loading && ui.input.isNotBlank()) {
                        Icon(Icons.Default.Send, null)
                    }
                }
            }
        }
    ) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad)) {
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("AI 回答仅供参考，不能替代医生诊断", style = MaterialTheme.typography.bodySmall)
                }
            }
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ui.messages) { msg -> ChatBubble(msg) }
                if (ui.loading) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("AI 正在思考…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMsg) {
    val isUser = msg.role == "user"
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(msg.text, modifier = Modifier.padding(12.dp), color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
        }
    }
}