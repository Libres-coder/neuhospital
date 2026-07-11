package com.example.neusoft_hospital.feature.followup.presentation

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
import com.example.neusoft_hospital.feature.auth.domain.FollowUpTask
import com.example.neusoft_hospital.feature.followup.data.FollowUpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FollowUpPlanUiState(
    val tasks: List<FollowUpTask> = emptyList(),
    val selectedTask: FollowUpTask? = null,
    val answers: Map<String, String> = emptyMap(),
    val loading: Boolean = false
)

@HiltViewModel
class FollowUpPlanViewModel @Inject constructor(
    private val repo: FollowUpRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val planId: String = savedStateHandle.get<String>("planId") ?: ""
    private val _ui = MutableStateFlow(FollowUpPlanUiState())
    val ui: StateFlow<FollowUpPlanUiState> = _ui

    init { observe() }

    private fun observe() {
        viewModelScope.launch {
            repo.observeTasks(planId).collect { _ui.value = _ui.value.copy(tasks = it) }
        }
    }

    fun selectTask(task: FollowUpTask) {
        _ui.value = _ui.value.copy(selectedTask = task, answers = task.answers ?: emptyMap())
    }

    fun dismiss() { _ui.value = _ui.value.copy(selectedTask = null) }

    fun onAnswer(q: String, a: String) {
        _ui.value = _ui.value.copy(answers = _ui.value.answers + (q to a))
    }

    fun submit() {
        val task = _ui.value.selectedTask ?: return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            repo.completeTask(task.id, _ui.value.answers)
            _ui.value = _ui.value.copy(loading = false, selectedTask = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowUpPlanScreen(navController: NavController, vm: FollowUpPlanViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("随访计划") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { pad ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ui.tasks) { task ->
                Card(modifier = Modifier.fillMaxWidth(), colors = if (task.completed) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant) else CardDefaults.cardColors()) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (task.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("术后第 ${task.dayIndex} 天 · ${task.targetDate}", style = MaterialTheme.typography.titleSmall)
                            Text("${task.questions.size} 个问题", style = MaterialTheme.typography.bodySmall)
                            if (task.doctorReply != null) {
                                Spacer(Modifier.height(4.dp))
                                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                                    Text("医生建议：${task.doctorReply}", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        TextButton(onClick = { vm.selectTask(task) }) { Text(if (task.completed) "查看" else "填写") }
                    }
                }
            }
        }
    }

    ui.selectedTask?.let { task ->
        AlertDialog(
            onDismissRequest = vm::dismiss,
            title = { Text("术后第 ${task.dayIndex} 天问卷") },
            text = {
                Column {
                    task.questions.forEach { q ->
                        OutlinedTextField(
                            value = ui.answers[q].orEmpty(),
                            onValueChange = { vm.onAnswer(q, it) },
                            label = { Text(q) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = vm::submit) { Text("提交") } },
            dismissButton = { TextButton(onClick = vm::dismiss) { Text("取消") } }
        )
    }
}