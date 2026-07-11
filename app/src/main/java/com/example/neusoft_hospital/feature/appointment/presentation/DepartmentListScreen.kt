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
import com.example.neusoft_hospital.feature.auth.domain.Department
import com.example.neusoft_hospital.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DepartmentListUiState(
    val departments: List<Department> = emptyList(),
    val title: String = "科室",
    val loading: Boolean = false
)

@HiltViewModel
class DepartmentListViewModel @Inject constructor(
    private val repo: AppointmentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val parentId: String? = savedStateHandle.get<String>("parentId")
    private val _ui = MutableStateFlow(DepartmentListUiState())
    val ui: StateFlow<DepartmentListUiState> = _ui

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            repo.getDepartments(parentId).onSuccess { list ->
                val title = if (parentId == null) "科室分类" else "选择子科室"
                _ui.value = _ui.value.copy(departments = list, title = title, loading = false)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentListScreen(navController: NavController, vm: DepartmentListViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ui.title) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { pad ->
        if (ui.loading && ui.departments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(ui.departments) { dept ->
                Card(modifier = Modifier.fillMaxWidth().clickable {
                    // If has children, navigate; otherwise go to doctor list
                    navController.navigate(Routes.DoctorList.create(dept.id))
                }) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalHospital, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(dept.name, style = MaterialTheme.typography.titleMedium)
                            Text(dept.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        }
    }
}