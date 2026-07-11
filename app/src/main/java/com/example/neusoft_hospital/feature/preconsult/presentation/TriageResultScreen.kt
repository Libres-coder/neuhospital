package com.example.neusoft_hospital.feature.preconsult.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.neusoft_hospital.feature.appointment.data.AppointmentRepository
import com.example.neusoft_hospital.feature.appointment.data.toDomain
import com.example.neusoft_hospital.feature.auth.domain.Appointment
import com.example.neusoft_hospital.feature.auth.domain.DepartmentRecommendation
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
    val loading: Boolean = false,
    val error: String? = null,
    val booking: Boolean = false,
    val bookedAppointment: Appointment? = null,
    val bookedDoctorName: String? = null,
    val bookedSlot: String? = null
)

/** Maps raw 0..1 confidence into a 0..1 strength bucket + label the UI can show. */
private data class MatchStrength(val label: String, val ratio: Float, val color: Color)

@Composable
private fun matchStrength(confidence: Float): MatchStrength {
    val scheme = MaterialTheme.colorScheme
    return when {
        confidence >= 0.6f -> MatchStrength("强烈推荐", (confidence / 0.9f).coerceIn(0f, 1f), scheme.primary)
        confidence >= 0.3f -> MatchStrength("可能匹配", (confidence / 0.6f).coerceIn(0f, 1f), scheme.tertiary)
        confidence >= 0.15f -> MatchStrength("建议尝试", (confidence / 0.3f).coerceIn(0f, 1f), scheme.secondary)
        else -> MatchStrength("参考", confidence.coerceIn(0f, 1f), scheme.outline)
    }
}

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
            _ui.value = _ui.value.copy(loading = true, error = null)
            val list = symptoms.split("、", "，", ",").map { it.trim() }.filter { it.isNotEmpty() }
            repo.triage(list)
                .onSuccess { _ui.value = _ui.value.copy(result = it, loading = false) }
                .onFailure { e ->
                    _ui.value = _ui.value.copy(
                        loading = false,
                        error = e.message ?: "分诊失败，请稍后重试"
                    )
                }
        }
    }

    fun retry() {
        if (_ui.value.symptoms.isNotBlank()) triage(_ui.value.symptoms)
    }

    fun oneClickBook() {
        val symptoms = _ui.value.symptoms
        if (symptoms.isBlank()) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(booking = true, error = null)
            apptRepo.recommendAndBook(symptoms)
                .onSuccess { ap ->
                    val domain = ap.toDomain()
                    _ui.value = _ui.value.copy(
                        booking = false,
                        bookedAppointment = domain,
                        bookedDoctorName = ap.doctorName,
                        bookedSlot = "${ap.date} ${ap.timeSlot}"
                    )
                }
                .onFailure { e ->
                    _ui.value = _ui.value.copy(
                        booking = false,
                        error = "一键挂号失败：${e.message ?: "未知错误"}"
                    )
                }
        }
    }

    fun dismissBookedBanner() {
        _ui.value = _ui.value.copy(bookedAppointment = null)
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
        if (ui.loading || (result == null && ui.error == null)) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        val errorMsg = ui.error
        if (errorMsg != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(pad).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text(errorMsg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { navController.popBackStack() }) { Text("返回补充") }
                    Button(onClick = vm::retry) { Text("重试") }
                }
            }
            return@Scaffold
        }

        // result is non-null here
        val safeResult = result!!
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

            if (safeResult.possibleDiseases.isNotEmpty()) {
                item { Text("可能疾病", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
                items(safeResult.possibleDiseases) { d ->
                    Card {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(d.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                Text(
                                    text = "${(d.probability * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(d.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                Text(
                    "推荐科室（共 ${safeResult.recommendedDepartments.size} 个）",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(safeResult.recommendedDepartments) { rec ->
                RecommendedDepartmentCard(rec = rec, onClick = {
                    navController.navigate(Routes.DoctorList.create(rec.department.id))
                })
            }

            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = vm::oneClickBook,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = safeResult.recommendedDepartments.isNotEmpty() && !ui.booking
                ) {
                    if (ui.booking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("正在匹配最佳医生并预约…")
                    } else {
                        Icon(Icons.Default.EventAvailable, null)
                        Spacer(Modifier.width(8.dp))
                        Text("一键挂号：${safeResult.recommendedDepartments.firstOrNull()?.department?.name ?: "—"}")
                    }
                }
                if (ui.bookedAppointment != null) {
                    Spacer(Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "挂号成功：${ui.bookedDoctorName}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "时间：${ui.bookedSlot}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "可在「我的预约」中查看",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            IconButton(onClick = vm::dismissBookedBanner) {
                                Icon(Icons.Default.Close, contentDescription = "关闭")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val topDept = safeResult.recommendedDepartments.firstOrNull()?.department
                        if (topDept != null) navController.navigate(Routes.DoctorList.create(topDept.id))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ListAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("或：手动选择科室和医生")
                }
            }
        }
    }
}

@Composable
private fun RecommendedDepartmentCard(rec: DepartmentRecommendation, onClick: () -> Unit) {
    val strength = matchStrength(rec.confidence)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocalHospital, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(rec.department.name, style = MaterialTheme.typography.titleMedium)
                Text(rec.department.desc, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(strength.ratio)
                            .height(4.dp)
                            .background(strength.color)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(strength.label, color = strength.color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Text("${(rec.confidence * 100).toInt()}%", color = strength.color, style = MaterialTheme.typography.labelSmall)
            }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}