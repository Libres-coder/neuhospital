package com.example.neusoft_hospital.feature.appointment.presentation

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
import com.example.neusoft_hospital.feature.auth.domain.DoctorRecommendation
import com.example.neusoft_hospital.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SmartRecommendUiState(
    val symptoms: String = "",
    val recommendations: List<DoctorRecommendation> = emptyList(),
    val loading: Boolean = false
)

@HiltViewModel
class SmartRecommendViewModel @Inject constructor(
    private val repo: AppointmentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val initial: String = savedStateHandle.get<String>("symptoms")?.takeIf { it.isNotBlank() } ?: ""
    private val _ui = MutableStateFlow(SmartRecommendUiState(symptoms = initial))
    val ui: StateFlow<SmartRecommendUiState> = _ui

    init { if (initial.isNotBlank()) search() }

    fun onSymptomsChange(v: String) { _ui.value = _ui.value.copy(symptoms = v) }

    fun search() {
        val q = _ui.value.symptoms
        if (q.isBlank()) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            repo.recommend(q).onSuccess {
                _ui.value = _ui.value.copy(loading = false, recommendations = it)
            }.onFailure {
                _ui.value = _ui.value.copy(loading = false)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartRecommendScreen(navController: NavController, vm: SmartRecommendViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("智能号源推荐") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            OutlinedTextField(
                value = ui.symptoms,
                onValueChange = vm::onSymptomsChange,
                label = { Text("症状或疾病") },
                placeholder = { Text("如：高血压、头痛、胃痛") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = vm::search, modifier = Modifier.fillMaxWidth(), enabled = !ui.loading && ui.symptoms.isNotBlank()) {
                Text("智能推荐")
            }
            Spacer(Modifier.height(16.dp))
            if (ui.loading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (ui.recommendations.isEmpty()) {
                Text("请输入症状后点击推荐", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(ui.recommendations) { rec ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                navController.navigate(Routes.DoctorDetail.create(rec.doctor.id))
                            }
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                                    Icon(Icons.Default.Person, null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(rec.doctor.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${rec.doctor.departmentName} | ${rec.doctor.title}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("擅长：${rec.doctor.expertise}", style = MaterialTheme.typography.bodySmall)
                                    if (rec.reasons.isNotEmpty()) {
                                        Spacer(Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            rec.reasons.forEach { reason ->
                                                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                                                    Text(reason, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("匹配度", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${(rec.score * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}