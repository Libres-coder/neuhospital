package com.example.neusoft_hospital.feature.auth.presentation

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.neusoft_hospital.core.data.local.entity.FamilyMemberEntity
import com.example.neusoft_hospital.core.data.prefs.UserPreferences
import com.example.neusoft_hospital.feature.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyUiState(
    val members: List<FamilyMemberEntity> = emptyList(),
    val currentId: String = "",
    val loading: Boolean = false
)

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val repo: AuthRepository,
    private val prefs: UserPreferences
) : ViewModel() {
    private val _ui = MutableStateFlow(FamilyUiState())
    val ui: StateFlow<FamilyUiState> = _ui

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val uid = prefs.tokenFlow.first()
            _ui.value = _ui.value.copy(currentId = prefs.currentPatientIdFlow.first())
            repo.getFamilyMembers().collect { list ->
                _ui.value = _ui.value.copy(members = list)
            }
        }
    }

    fun switchPatient(member: FamilyMemberEntity) {
        viewModelScope.launch {
            val uid = prefs.tokenFlow.first()
            repo.setDefaultMember(member.id, uid)
            _ui.value = _ui.value.copy(currentId = member.id)
        }
    }

    fun deleteMember(member: FamilyMemberEntity) {
        viewModelScope.launch { repo.deleteFamilyMember(member) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyManageScreen(navController: NavController, vm: FamilyViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("亲情账户管理") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                actions = { IconButton(onClick = { navController.navigate("family_add") }) { Icon(Icons.Default.PersonAdd, null) } }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { navController.navigate("family_add") }) { Icon(Icons.Default.Add, null) } }
    ) { pad ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("当前就诊人", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
            items(ui.members) { member ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { vm.switchPatient(member) },
                    colors = if (member.id == ui.currentId) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                             else CardDefaults.cardColors()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(member.name, style = MaterialTheme.typography.titleMedium)
                            Text("${member.relation} | ${member.phone}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (member.id == ui.currentId) {
                            Surface(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small) {
                                Text("当前", modifier = Modifier.padding(horizontal=8.dp, vertical=4.dp), color=MaterialTheme.colorScheme.onPrimary, style=MaterialTheme.typography.labelSmall)
                            }
                        }
                        IconButton(onClick = { vm.deleteMember(member) }) { Icon(Icons.Default.Delete, null, tint=MaterialTheme.colorScheme.error) }
                    }
                }
            }
            item {
                if (ui.members.isEmpty()) {
                    Text("暂无就诊人，点击右下角添加", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
