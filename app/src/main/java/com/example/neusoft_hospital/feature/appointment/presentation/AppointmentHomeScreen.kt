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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.neusoft_hospital.core.ui.components.SectionTitle
import com.example.neusoft_hospital.feature.appointment.data.AppointmentRepository
import com.example.neusoft_hospital.feature.auth.domain.Department
import com.example.neusoft_hospital.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppointmentHomeUiState(
    val parents: List<Department> = emptyList(),
    val all: List<Department> = emptyList(),
    val recommended: List<String> = listOf("高血压", "糖尿病", "冠心病", "感冒发烧", "失眠", "胃痛", "过敏", "腰腿痛"),
    val searchQuery: String = "",
    val loading: Boolean = false
)

@HiltViewModel
class AppointmentHomeViewModel @Inject constructor(
    private val repo: AppointmentRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(AppointmentHomeUiState())
    val ui: StateFlow<AppointmentHomeUiState> = _ui

    init { load() }

    fun load() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            repo.getDepartments().onSuccess { parents ->
                repo.getAllDepartments().onSuccess { all ->
                    _ui.value = _ui.value.copy(parents = parents, all = all, loading = false)
                }
            }
        }
    }

    fun onSearchChange(v: String) {
        _ui.value = _ui.value.copy(searchQuery = v)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentHomeScreen(navController: NavController, vm: AppointmentHomeViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("东软医院 · 在线挂号") },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.MyAppointments.route) }) {
                        Icon(Icons.Default.EventNote, contentDescription = "我的预约")
                    }
                    IconButton(onClick = { navController.navigate(Routes.FamilyManage.route) }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "就诊人")
                    }
                }
            )
        }
    ) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad)) {
            OutlinedTextField(
                value = ui.searchQuery,
                onValueChange = vm::onSearchChange,
                placeholder = { Text("搜索科室、医生、症状") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            LazyColumn {
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(72.dp),
                        onClick = { navController.navigate(Routes.PreConsultHome.route) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("AI 智能预问诊", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("不知道挂哪科？描述症状，AI 帮你推荐", style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Default.ChevronRight, null)
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
                item { SectionTitle("智能推荐") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ui.recommended) { keyword ->
                            AssistChip(
                                onClick = { navController.navigate(Routes.SmartRecommend.create(keyword)) },
                                label = { Text(keyword) },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
                item { SectionTitle("科室分类") }
                item {
                    val parentList = ui.parents
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        parentList.chunked(4).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { dept ->
                                    Card(
                                        modifier = Modifier.weight(1f).clickable {
                                            navController.navigate(Routes.DepartmentList.create(dept.id))
                                        },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.LocalHospital, null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.height(4.dp))
                                            Text(dept.name, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                                repeat(4 - row.size) { Box(Modifier.weight(1f)) {} }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}