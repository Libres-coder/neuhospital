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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.neusoft_hospital.feature.appointment.data.AppointmentRepository
import com.example.neusoft_hospital.feature.auth.domain.Doctor
import com.example.neusoft_hospital.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DoctorListUiState(
    val doctors: List<Doctor> = emptyList(),
    val departmentName: String = "",
    val loading: Boolean = false
)

@HiltViewModel
class DoctorListViewModel @Inject constructor(
    private val repo: AppointmentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val departmentId: String = savedStateHandle.get<String>("departmentId") ?: ""
    private val _ui = MutableStateFlow(DoctorListUiState())
    val ui: StateFlow<DoctorListUiState> = _ui

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            repo.getDoctors(departmentId).onSuccess { list ->
                _ui.value = _ui.value.copy(doctors = list, departmentName = list.firstOrNull()?.departmentName ?: "", loading = false)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorListScreen(navController: NavController, vm: DoctorListViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ui.departmentName.ifEmpty { "医生" }) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { pad ->
        if (ui.loading && ui.doctors.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(ui.doctors) { doc ->
                Card(modifier = Modifier.fillMaxWidth().clickable {
                    navController.navigate(Routes.DoctorDetail.create(doc.id))
                }) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(56.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(doc.name, style = MaterialTheme.typography.titleMedium)
                            Text(doc.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("擅长：${doc.expertise}", style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                Text("${doc.rating}", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        }
    }
}