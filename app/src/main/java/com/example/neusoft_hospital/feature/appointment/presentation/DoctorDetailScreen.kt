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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.neusoft_hospital.core.ui.components.SectionTitle
import com.example.neusoft_hospital.feature.appointment.data.AppointmentRepository
import com.example.neusoft_hospital.feature.auth.domain.Doctor
import com.example.neusoft_hospital.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DoctorDetailUiState(
    val doctor: Doctor? = null,
    val loading: Boolean = false
)

@HiltViewModel
class DoctorDetailViewModel @Inject constructor(
    private val repo: AppointmentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val doctorId: String = savedStateHandle.get<String>("doctorId") ?: ""
    private val _ui = MutableStateFlow(DoctorDetailUiState())
    val ui: StateFlow<DoctorDetailUiState> = _ui

    init { load() }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            repo.getDoctor(doctorId).onSuccess { _ui.value = _ui.value.copy(doctor = it, loading = false) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDetailScreen(navController: NavController, vm: DoctorDetailViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    val doc = ui.doctor
    // Re-fetch availability whenever the screen comes back to the foreground
    // (e.g. after returning from BookingScreen or cancelling an appointment).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("医生详情") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }
            )
        },
        bottomBar = {
            if (doc != null) {
                BottomAppBar {
                    Button(
                        onClick = { navController.navigate(Routes.Schedule.create(doc.id)) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, null)
                        Spacer(Modifier.width(8.dp))
                        Text("查看出诊时间并预约")
                    }
                }
            }
        }
    ) { pad ->
        if (ui.loading || doc == null) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(72.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.padding(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(doc.name, style = MaterialTheme.typography.headlineSmall)
                            Text(doc.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                                Text("${doc.rating}", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.width(8.dp))
                                Text(doc.departmentName, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            item { SectionTitle("专家简介") }
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text(doc.profile, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
            item { SectionTitle("擅长领域") }
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text(doc.expertise, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
            item { SectionTitle("本周出诊") }
            items(doc.schedule.take(3)) { schedule ->
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(schedule.date, style = MaterialTheme.typography.titleSmall)
                            Text(schedule.dayOfWeek, style = MaterialTheme.typography.bodySmall)
                        }
                        val avail = schedule.slots.count { it.available > 0 }
                        Text("$avail 个时段可约", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}