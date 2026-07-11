package com.example.neusoft_hospital.feature.appointment.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.neusoft_hospital.core.data.prefs.UserPreferences
import com.example.neusoft_hospital.feature.appointment.data.AppointmentRepository
import com.example.neusoft_hospital.feature.auth.domain.Doctor
import com.example.neusoft_hospital.feature.auth.domain.Schedule
import com.example.neusoft_hospital.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookingUiState(
    val doctor: Doctor? = null,
    val schedule: Schedule? = null,
    val slotId: String = "",
    val duration: Int = 15,
    val fee: Double = 0.0,
    val currentPatientName: String = "",
    val loading: Boolean = false,
    val booked: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val repo: AppointmentRepository,
    private val prefs: UserPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val doctorId: String = savedStateHandle.get<String>("doctorId") ?: ""
    private val date: String = savedStateHandle.get<String>("date") ?: ""
    private val slotId: String = savedStateHandle.get<String>("slotId") ?: ""

    private val _ui = MutableStateFlow(BookingUiState(slotId = slotId))
    val ui: StateFlow<BookingUiState> = _ui

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            repo.getDoctor(doctorId).onSuccess { doc ->
                val schedule = doc.schedule.firstOrNull { it.date == date }
                val slot = schedule?.slots?.firstOrNull { it.id == slotId }
                val baseFee = when (doc.title) {
                    "主任医师" -> 50.0
                    "副主任医师" -> 30.0
                    else -> 15.0
                }
                val fee = baseFee * (_ui.value.duration / 15.0)
                _ui.value = _ui.value.copy(doctor = doc, schedule = schedule, fee = fee, currentPatientName = prefs.userNameFlow.first())
                _ui.value = _ui.value.copy(loading = false)
            }
        }
    }

    fun setDuration(d: Int) {
        val baseFee = when (_ui.value.doctor?.title) {
            "主任医师" -> 50.0
            "副主任医师" -> 30.0
            else -> 15.0
        }
        val fee = baseFee * (d / 15.0)
        _ui.value = _ui.value.copy(duration = d, fee = fee)
    }

    fun confirm() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            val state = _ui.value
val uid = prefs.userIdFlow.first()
        val patientId = prefs.currentPatientIdFlow.first().ifBlank { uid }
            val patientName = state.currentPatientName.ifBlank { "本人" }
            repo.book(doctorId, date, "${state.schedule?.slots?.firstOrNull { it.id == slotId }?.startTime ?: ""}-${state.schedule?.slots?.firstOrNull { it.id == slotId }?.endTime ?: ""}", state.duration, patientId, patientName).fold(
                onSuccess = { _ui.value = _ui.value.copy(loading = false, booked = true) },
                onFailure = { _ui.value = _ui.value.copy(loading = false, error = it.message) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(navController: NavController, vm: BookingViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    LaunchedEffect(ui.booked) {
        if (ui.booked) {
            navController.navigate(Routes.MyAppointments.route) {
                popUpTo(Routes.AppointmentHome.route)
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("确认预约") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ui.doctor?.let { doc ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(doc.name, style = MaterialTheme.typography.titleMedium)
                        Text("${doc.departmentName} | ${doc.title}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("就诊时间：${ui.schedule?.date} ${ui.schedule?.slots?.firstOrNull { it.id == ui.slotId }?.startTime}", style = MaterialTheme.typography.bodyMedium)
                        Text("就诊人：${ui.currentPatientName}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Text("就诊时长", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 45, 60).forEach { d ->
                    FilterChip(selected = ui.duration == d, onClick = { vm.setDuration(d) }, label = { Text("${d}分钟") })
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("挂号费")
                        Text("¥${"%.2f".format(ui.fee)}", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("支持医保电子凭证结算", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (ui.error != null) Text(ui.error!!, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.weight(1f))
            Button(onClick = vm::confirm, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = !ui.loading) {
                if (ui.loading) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text("确认支付")
            }
        }
    }
}