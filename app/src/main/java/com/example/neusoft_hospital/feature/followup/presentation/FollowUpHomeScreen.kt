package com.example.neusoft_hospital.feature.followup.presentation

import androidx.compose.foundation.clickable
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
import com.example.neusoft_hospital.core.ui.components.ConfirmDialog
import com.example.neusoft_hospital.feature.auth.domain.ChronicType
import com.example.neusoft_hospital.feature.followup.data.FollowUpRepository
import com.example.neusoft_hospital.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FollowUpHomeUiState(
    val pendingCount: Int = 0,
    val planCount: Int = 0,
    val alertCount: Int = 0,
    val showCreatePlan: Boolean = false,
    val disease: String = "",
    val surgeryDate: String = "",
    val loading: Boolean = false
)

@HiltViewModel
class FollowUpHomeViewModel @Inject constructor(
    private val repo: FollowUpRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(FollowUpHomeUiState())
    val ui: StateFlow<FollowUpHomeUiState> = _ui

    init { observe() }

    private fun observe() {
        viewModelScope.launch {
            repo.observePendingTasks().collect { list ->
                _ui.value = _ui.value.copy(pendingCount = list.size)
            }
        }
        viewModelScope.launch {
            repo.observePlans().collect { list ->
                _ui.value = _ui.value.copy(planCount = list.size)
            }
        }
    }

    fun showCreate() { _ui.value = _ui.value.copy(showCreatePlan = true) }
    fun hideCreate() { _ui.value = _ui.value.copy(showCreatePlan = false) }
    fun onDiseaseChange(v: String) { _ui.value = _ui.value.copy(disease = v) }
    fun onSurgeryDateChange(v: String) { _ui.value = _ui.value.copy(surgeryDate = v) }

    fun create() {
        val s = _ui.value
        if (s.disease.isBlank() || s.surgeryDate.isBlank()) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            repo.createPlan(s.disease, s.surgeryDate)
            _ui.value = _ui.value.copy(loading = false, showCreatePlan = false, disease = "", surgeryDate = "")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowUpHomeScreen(navController: NavController, vm: FollowUpHomeViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text("健康管理") }) },
        floatingActionButton = { FloatingActionButton(onClick = { vm.showCreate() }) { Icon(Icons.Default.Add, null) } }
    ) { pad ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(modifier = Modifier.weight(1f).clickable { navController.navigate(Routes.ChronicDashboard.create("HYPERTENSION")) }) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(4.dp))
                            Text("高血压管理", style = MaterialTheme.typography.titleSmall)
                            Text("血压/心率趋势", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Card(modifier = Modifier.weight(1f).clickable { navController.navigate(Routes.ChronicDashboard.create("DIABETES")) }) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.WaterDrop, null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.height(4.dp))
                            Text("糖尿病管理", style = MaterialTheme.typography.titleSmall)
                            Text("血糖/HbA1c", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Routes.ChronicAlerts.route) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("异常告警", style = MaterialTheme.typography.titleMedium)
                            Text("${ui.alertCount} 条未处理", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Routes.RehabGuide.create("骨折术后")) }) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("康复指导", style = MaterialTheme.typography.titleMedium)
                            Text("按病种查看图文/视频指引", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
            item {
                Text("随访计划 (${ui.planCount})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (ui.showCreatePlan) {
        AlertDialog(
            onDismissRequest = vm::hideCreate,
            title = { Text("创建随访计划") },
            text = {
                Column {
                    OutlinedTextField(value = ui.disease, onValueChange = vm::onDiseaseChange, label = { Text("病种 / 术式") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = ui.surgeryDate, onValueChange = vm::onSurgeryDateChange, label = { Text("手术日期 (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(onClick = vm::create) { Text("创建") } },
            dismissButton = { TextButton(onClick = vm::hideCreate) { Text("取消") } }
        )
    }
}