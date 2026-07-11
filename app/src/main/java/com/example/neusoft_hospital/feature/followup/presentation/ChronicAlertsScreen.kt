package com.example.neusoft_hospital.feature.followup.presentation

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
import com.example.neusoft_hospital.feature.auth.domain.ChronicAlert
import com.example.neusoft_hospital.feature.followup.data.ChronicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChronicAlertsUiState(val alerts: List<ChronicAlert> = emptyList())

@HiltViewModel
class ChronicAlertsViewModel @Inject constructor(
    private val repo: ChronicRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(ChronicAlertsUiState())
    val ui: StateFlow<ChronicAlertsUiState> = _ui

    init {
        viewModelScope.launch {
            repo.observeAlerts().collect { _ui.value = ChronicAlertsUiState(it) }
        }
    }

    fun ack(id: String) { viewModelScope.launch { repo.ackAlert(id) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChronicAlertsScreen(navController: NavController, vm: ChronicAlertsViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text("异常告警") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }) }
    ) { pad ->
        if (ui.alerts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("暂无异常告警", modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ui.alerts) { alert ->
                val (color, label) = when (alert.level) {
                    3 -> MaterialTheme.colorScheme.error to "危险"
                    2 -> MaterialTheme.colorScheme.secondary to "警告"
                    else -> MaterialTheme.colorScheme.tertiary to "注意"
                }
                Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = color)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(alert.message, style = MaterialTheme.typography.bodyMedium, color = color)
                            Text(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA).format(java.util.Date(alert.createTime)), style = MaterialTheme.typography.bodySmall)
                        }
                        Surface(color = color, shape = MaterialTheme.shapes.small) {
                            Text(label, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onError, style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.width(4.dp))
                        TextButton(onClick = { vm.ack(alert.id) }) { Text("已读") }
                    }
                }
            }
        }
    }
}