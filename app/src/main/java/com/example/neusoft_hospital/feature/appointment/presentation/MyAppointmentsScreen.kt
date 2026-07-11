package com.example.neusoft_hospital.feature.appointment.presentation

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
import com.example.neusoft_hospital.core.ui.components.StatusChip
import com.example.neusoft_hospital.feature.appointment.data.AppointmentRepository
import com.example.neusoft_hospital.feature.auth.domain.Appointment
import com.example.neusoft_hospital.feature.auth.domain.AppointmentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyAppointmentsUiState(
    val appointments: List<Appointment> = emptyList(),
    val loading: Boolean = false,
    val showCancelDialog: Appointment? = null,
    val errorMessage: String? = null
) {
    fun consumeError() = copy(errorMessage = null)
}

@HiltViewModel
class MyAppointmentsViewModel @Inject constructor(
    private val repo: AppointmentRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(MyAppointmentsUiState())
    val ui: StateFlow<MyAppointmentsUiState> = _ui

    init {
        observe()
        refresh()
    }

    private fun observe() {
        viewModelScope.launch {
            repo.observeAppointments().collect { list ->
                _ui.value = _ui.value.copy(appointments = list)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            repo.refreshMyAppointments()
            _ui.value = _ui.value.copy(loading = false)
        }
    }

    fun cancelAppointment(apt: Appointment) { _ui.value = _ui.value.copy(showCancelDialog = apt) }
    fun dismissCancel() { _ui.value = _ui.value.copy(showCancelDialog = null) }
    fun consumeError() { _ui.value = _ui.value.copy(errorMessage = null) }

    fun confirmCancel() {
        val apt = _ui.value.showCancelDialog ?: return
        viewModelScope.launch {
            val res = repo.cancel(apt.id)
            _ui.value = _ui.value.copy(
                showCancelDialog = null,
                errorMessage = res.exceptionOrNull()?.message
            )
        }
    }

    fun toggleReminder(apt: Appointment) {
        viewModelScope.launch { repo.setReminder(apt.id, !apt.status.name.lowercase().contains("complete")) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppointmentsScreen(navController: NavController, vm: MyAppointmentsViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()

        val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("我的预约") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { vm.refresh() }) { Icon(Icons.Default.Refresh, contentDescription = "刷新") }
                }
            )
        }
    ) { pad ->
        LaunchedEffect(ui.errorMessage) {
            val msg = ui.errorMessage
            if (msg != null) {
                snackbarHostState.showSnackbar(msg)
                vm.consumeError()
            }
        }
        if (ui.loading && ui.appointments.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        if (ui.appointments.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EventBusy, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Text("暂无预约", modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(ui.appointments) { apt ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(apt.doctorName, style = MaterialTheme.typography.titleMedium)
                            StatusChip(apt.status.name)
                        }
                        Text("${apt.departmentName} | ${apt.date} ${apt.timeSlot}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("就诊人：${apt.patientName}", style = MaterialTheme.typography.bodySmall)
                        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val cancellable = when (apt.status) {
                                AppointmentStatus.PENDING, AppointmentStatus.PAYED, AppointmentStatus.CONFIRMED -> true
                                else -> false
                            }
                            if (cancellable) {
                                OutlinedButton(onClick = { vm.cancelAppointment(apt) }) {
                                    Text("退号", color = MaterialTheme.colorScheme.error)
                                }
                            }
                            OutlinedButton(onClick = { vm.toggleReminder(apt) }) {
                                Icon(Icons.Default.Notifications, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("提醒")
                            }
                        }
                    }
                }
            }
        }
    }

    ui.showCancelDialog?.let { apt ->
        ConfirmDialog(
            title = "确认退号",
            message = "确定要取消 ${apt.date} ${apt.timeSlot} ${apt.doctorName} 医生的预约吗？",
            onConfirm = { vm.confirmCancel() },
            onDismiss = { vm.dismissCancel() }
        )
    }
}