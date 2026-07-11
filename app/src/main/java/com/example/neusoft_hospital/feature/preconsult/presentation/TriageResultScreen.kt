package com.example.neusoft_hospital.feature.preconsult.presentation

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.neusoft_hospital.feature.appointment.data.AppointmentRepository
import com.example.neusoft_hospital.feature.auth.domain.TriageResult
import com.example.neusoft_hospital.feature.preconsult.data.PreConsultRepository
import com.example.neusoft_hospital.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TriageUiState(
    val result: TriageResult? = null,
    val symptoms: String = "",
    val loading: Boolean = false
)

@HiltViewModel
class TriageResultViewModel @Inject constructor(
    private val repo: PreConsultRepository,
    private val apptRepo: AppointmentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _ui = MutableStateFlow(TriageUiState())
    val ui: StateFlow<TriageUiState> = _ui

    init {
        val encoded = savedStateHandle.get<String>("symptoms").orEmpty()
        val symptoms = java.net.URLDecoder.decode(encoded, "UTF-8")
        _ui.value = _ui.value.copy(symptoms = symptoms)
        if (symptoms.isNotBlank()) triage(symptoms)
    }

    fun triage(symptoms: String) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            val list = symptoms.split("、", "，", ",").map { it.trim() }.filter { it.isNotEmpty() }
            repo.triage(list).onSuccess { _ui.value = _ui.value.copy(result = it, loading = false) }
                .onFailure { _ui.value = _ui.value.copy(loading = false) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriageResultScreen(navController: NavController, vm: TriageResultViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    val result = ui.result

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("智能分诊结果") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { pad ->
        if (ui.loading || result == null) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("AI 建议", fontWeight = FontWeight.Bold)
                            Text("以下结果仅供参考，不能替代医生诊断", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (result.possibleDiseases.isNotEmpty()) {
                item { Text("可能疾病", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
                items(result.possibleDiseases) { d ->
                    Card {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(d.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                Text("${(d.probability * 100).toInt()}%", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(d.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item { Text("推荐科室", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            items(result.recommendedDepartments) { rec ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        navController.navigate(Routes.DoctorList.create(rec.department.id))
                    }
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalHospital, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(rec.department.name, style = MaterialTheme.typography.titleMedium)
                            Text(rec.department.desc, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("${(rec.confidence * 100).toInt()}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }

            item {
                Button(onClick = {
                    val topDept = result.recommendedDepartments.firstOrNull()?.department
                    if (topDept != null) navController.navigate(Routes.DoctorList.create(topDept.id))
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("一键挂号")
                }
            }
        }
    }
}