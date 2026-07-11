package com.example.neusoft_hospital.feature.preconsult.presentation

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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.neusoft_hospital.feature.ai.data.QwenClient
import com.example.neusoft_hospital.feature.preconsult.data.PreConsultRepository
import com.example.neusoft_hospital.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SymptomMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // user / assistant
    val text: String,
    val isQuestion: Boolean = false // 是否是关键追问
)

data class SymptomUiState(
    val mode: String = "text",
    val messages: List<SymptomMessage> = emptyList(),
    val input: String = "",
    val loading: Boolean = false,
    val done: Boolean = false,
    val symptoms: List<String> = emptyList()
)

@HiltViewModel
class SymptomInputViewModel @Inject constructor(
    private val repo: PreConsultRepository,
    private val qwen: QwenClient,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val mode: String = savedStateHandle.get<String>("mode") ?: "text"
    private val _ui = MutableStateFlow(SymptomUiState(mode = mode))
    val ui: StateFlow<SymptomUiState> = _ui

    init {
        // Initial greeting
        addAssistant("您好，我是您的 AI 智能预问诊助手。请描述您目前最明显的不适症状（如\"头痛3天，伴有恶心\"）。")
    }

    fun onInputChange(v: String) { _ui.value = _ui.value.copy(input = v) }

    fun send() {
        val text = _ui.value.input.trim()
        if (text.isBlank()) return
        val userMsg = SymptomMessage(role = "user", text = text)
        val newMessages = _ui.value.messages + userMsg
        val newSymptoms = (_ui.value.symptoms + text).distinct()
        _ui.value = _ui.value.copy(messages = newMessages, symptoms = newSymptoms, input = "", loading = true)

        viewModelScope.launch {
            // Call Qwen for next question
            val prompt = buildPrompt(newSymptoms)
            val aiText = qwen.chatSimple(prompt).getOrDefault(
                "请问这个症状持续多久了？是否有其他伴随症状？"
            )
            addAssistant(aiText)
            // Heuristic: if user said enough, end
            if (newSymptoms.size >= 2) {
                _ui.value = _ui.value.copy(loading = false, done = true)
            } else {
                _ui.value = _ui.value.copy(loading = false)
            }
        }
    }

    fun submitSymptoms() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(done = true, symptoms = _ui.value.symptoms.distinct())
        }
    }

    private fun addAssistant(text: String) {
        _ui.value = _ui.value.copy(messages = _ui.value.messages + SymptomMessage(role = "assistant", text = text))
    }

    private fun buildPrompt(symptoms: List<String>): String {
        return "你是医疗预问诊助手。患者描述了：${symptoms.joinToString("；")}。请用一句话追问一个关键问题帮助判断就诊科室。"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomInputScreen(navController: NavController, vm: SymptomInputViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()

    LaunchedEffect(ui.done) {
        if (ui.done) {
            val symptoms = ui.symptoms.joinToString("、")
            navController.navigate(Routes.TriageResult.create(symptoms))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (ui.mode == "image") "图文问诊" else "症状采集") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp) {
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = ui.input,
                        onValueChange = vm::onInputChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("描述症状…") },
                        maxLines = 3
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
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ui.messages) { msg ->
                    MessageBubble(msg)
                }
                if (ui.loading) {
                    item {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("AI 正在思考…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (ui.symptoms.size >= 2 && !ui.done) {
                    item {
                        Button(onClick = vm::submitSymptoms, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text("完成问诊，查看推荐")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: SymptomMessage) {
    val isUser = msg.role == "user"
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(msg.text, modifier = Modifier.padding(12.dp), color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
        }
    }
}