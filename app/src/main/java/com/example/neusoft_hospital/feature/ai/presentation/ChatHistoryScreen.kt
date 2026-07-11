package com.example.neusoft_hospital.feature.ai.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.neusoft_hospital.feature.ai.data.ChatRepository
import com.example.neusoft_hospital.feature.auth.domain.ChatSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatHistoryUiState(val sessions: List<ChatSession> = emptyList())

@HiltViewModel
class ChatHistoryViewModel @Inject constructor(
    private val repo: ChatRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(ChatHistoryUiState())
    val ui: StateFlow<ChatHistoryUiState> = _ui

    init {
        viewModelScope.launch {
            // refresh first, then observe further updates
            repo.refreshSessions()
            repo.observeSessions().collect { _ui.value = ChatHistoryUiState(it) }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { repo.deleteSession(id) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryScreen(navController: NavController, vm: ChatHistoryViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("对话历史") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { pad ->
        if (ui.sessions.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Text("暂无对话历史", modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ui.sessions) { session ->
                Card {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Chat, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(session.title, style = MaterialTheme.typography.titleSmall)
                            Text(session.lastMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        IconButton(onClick = { vm.delete(session.id) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}