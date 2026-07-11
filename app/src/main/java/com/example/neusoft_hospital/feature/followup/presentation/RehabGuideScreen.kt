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
import com.example.neusoft_hospital.feature.followup.data.FollowUpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RehabUiState(
    val disease: String = "",
    val items: List<String> = emptyList(),
    val logs: List<com.example.neusoft_hospital.core.data.local.entity.RehabLogEntity> = emptyList()
)

@HiltViewModel
class RehabViewModel @Inject constructor(
    private val repo: FollowUpRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val disease: String = savedStateHandle.get<String>("disease") ?: "骨折术后"
    private val _ui = MutableStateFlow(RehabUiState(disease = disease, items = rehabPlanFor(disease)))
    val ui: StateFlow<RehabUiState> = _ui

    init {
        viewModelScope.launch {
            repo.observeRehabLogs().collect { _ui.value = _ui.value.copy(logs = it) }
        }
    }

    fun checkin() {
        viewModelScope.launch {
            repo.createRehabLog(_ui.value.disease, _ui.value.items)
        }
    }

    fun complete(id: String) {
        viewModelScope.launch { repo.completeRehab(id) }
    }
}

private fun rehabPlanFor(disease: String): List<String> = when {
    disease.contains("骨折") -> listOf("踝泵运动 5min", "股四头肌等长收缩 3 组×10 次", "直腿抬高 3 组×10 次", "助行器下床活动 10min")
    disease.contains("心脏") -> listOf("深呼吸训练 5min", "床边慢走 10min", "心率监测")
    disease.contains("糖尿病") -> listOf("餐后散步 30min", "足部检查", "血糖记录", "药物服用")
    else -> listOf("散步 15min", "呼吸训练", "饮食记录")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RehabGuideScreen(navController: NavController, vm: RehabViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("康复指导 · ${ui.disease}") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { pad ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("今日康复计划", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        ui.items.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = vm::checkin) { Text("完成今日打卡") }
                    }
                }
            }
            item { Text("历史打卡", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            items(ui.logs) { log ->
                Card {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (log.completed) Icons.Default.CheckCircle else Icons.Default.Pending, null, tint = if (log.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${log.disease} · ${log.date}", style = MaterialTheme.typography.titleSmall)
                            Text(log.items.split("|").joinToString("，"), style = MaterialTheme.typography.bodySmall)
                        }
                        if (!log.completed) {
                            TextButton(onClick = { vm.complete(log.id) }) { Text("完成") }
                        }
                    }
                }
            }
        }
    }
}