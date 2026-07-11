package com.example.neusoft_hospital.feature.appointment.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.neusoft_hospital.feature.appointment.data.AppointmentRepository
import com.example.neusoft_hospital.feature.auth.domain.Schedule
import com.example.neusoft_hospital.feature.auth.domain.TimeSlot
import com.example.neusoft_hospital.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScheduleUiState(
    val doctorName: String = "",
    val schedules: List<Schedule> = emptyList(),
    val selectedDate: String = "",
    val selectedSlot: TimeSlot? = null,
    val loading: Boolean = false
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repo: AppointmentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val doctorId: String = savedStateHandle.get<String>("doctorId") ?: ""
    private val _ui = MutableStateFlow(ScheduleUiState())
    val ui: StateFlow<ScheduleUiState> = _ui

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            repo.getDoctor(doctorId).onSuccess { doc ->
                val firstDate = doc.schedule.firstOrNull()?.date ?: ""
                _ui.value = _ui.value.copy(doctorName = doc.name, schedules = doc.schedule, selectedDate = firstDate, loading = false)
            }
        }
    }

    fun selectDate(date: String) { _ui.value = _ui.value.copy(selectedDate = date, selectedSlot = null) }
    fun selectSlot(slot: TimeSlot) { _ui.value = _ui.value.copy(selectedSlot = slot) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(navController: NavController, vm: ScheduleViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    val currentSchedule = ui.schedules.firstOrNull { it.date == ui.selectedDate }
    val morningSlots = currentSchedule?.slots?.filter { it.startTime.substringBefore(":").toInt() < 12 } ?: emptyList()
    val afternoonSlots = currentSchedule?.slots?.filter { it.startTime.substringBefore(":").toInt() >= 12 } ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择就诊时间") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }
            )
        },
        bottomBar = {
            if (ui.selectedSlot != null) {
                BottomAppBar {
                    Button(
                        onClick = {
                            ui.selectedSlot?.let {
                                navController.navigate(Routes.Booking.create(
                                    doctorId = vm.doctorId,
                                    date = ui.selectedDate,
                                    slotId = it.id
                                ))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        enabled = ui.selectedSlot != null
                    ) { Text("下一步") }
                }
            }
        }
    ) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad)) {
            Text(ui.doctorName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))

            // Date picker
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ui.schedules) { schedule ->
                    val selected = schedule.date == ui.selectedDate
                    Surface(
                        modifier = Modifier.clickable { vm.selectDate(schedule.date) },
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(schedule.dayOfWeek.take(2), color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(schedule.date.takeLast(5), color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (morningSlots.isNotEmpty()) {
                Text("上午", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                SlotsGrid(morningSlots, ui.selectedSlot, vm::selectSlot)
            }

            if (afternoonSlots.isNotEmpty()) {
                Text("下午", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                SlotsGrid(afternoonSlots, ui.selectedSlot, vm::selectSlot)
            }
        }
    }
}

@Composable
private fun SlotsGrid(slots: List<TimeSlot>, selected: TimeSlot?, onSelect: (TimeSlot) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        slots.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { slot ->
                    val isSelected = selected?.id == slot.id
                    val enabled = slot.available > 0
                    OutlinedButton(
                        onClick = { if (enabled) onSelect(slot) },
                        modifier = Modifier.weight(1f),
                        enabled = enabled,
                        colors = if (isSelected) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primary) else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(slot.startTime, style = MaterialTheme.typography.bodySmall)
                            Text("余${slot.available}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}