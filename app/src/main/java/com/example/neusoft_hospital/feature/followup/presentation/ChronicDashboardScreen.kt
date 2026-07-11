package com.example.neusoft_hospital.feature.followup.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.neusoft_hospital.core.data.prefs.UserPreferences
import com.example.neusoft_hospital.feature.auth.domain.ChronicRecord
import com.example.neusoft_hospital.feature.auth.domain.ChronicType
import com.example.neusoft_hospital.feature.followup.data.ChronicRepository
import com.example.neusoft_hospital.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChronicUiState(
    val type: ChronicType = ChronicType.HYPERTENSION,
    val records: List<ChronicRecord> = emptyList(),
    val loading: Boolean = false
)

@HiltViewModel
class ChronicDashboardViewModel @Inject constructor(
    private val repo: ChronicRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val typeStr: String = savedStateHandle.get<String>("type") ?: "HYPERTENSION"
    private val _ui = MutableStateFlow(ChronicUiState(type = ChronicType.valueOf(typeStr)))
    val ui: StateFlow<ChronicUiState> = _ui

    init { observe() }

    private fun observe() {
        viewModelScope.launch {
            repo.observeRecords(_ui.value.type).collect { list ->
                _ui.value = _ui.value.copy(records = list)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChronicDashboardScreen(navController: NavController, vm: ChronicDashboardViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (ui.type == ChronicType.HYPERTENSION) "高血压管理" else "糖尿病管理") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { navController.navigate(Routes.ChronicInput.create(ui.type.name)) }) { Icon(Icons.Default.Add, null) } }
    ) { pad ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("近 30 天趋势", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        SimpleLineChart(
                            values = ui.records.takeLast(20).map { record ->
                                when (ui.type) {
                                    ChronicType.HYPERTENSION -> record.systolic?.toFloat() ?: 0f
                                    ChronicType.DIABETES -> record.fastingGlucose ?: 0f
                                }
                            }.reversed(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            item { Text("历史记录", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            items(ui.records) { r -> ChronicRecordItem(r) }
        }
    }
}

@Composable
private fun SimpleLineChart(values: List<Float>, color: Color) {
    if (values.isEmpty()) {
        Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val max = values.max().coerceAtLeast(1f)
    val min = values.min()
    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        val w = size.width; val h = size.height
        val path = Path()
        val range = (max - min).coerceAtLeast(1f)
        values.forEachIndexed { i, v ->
            val x = w * i / (values.size - 1).coerceAtLeast(1)
            val y = h - (h * (v - min) / range)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = Stroke(width = 4f))
        // draw points
        values.forEachIndexed { i, v ->
            val x = w * i / (values.size - 1).coerceAtLeast(1)
            val y = h - (h * (v - min) / range)
            drawCircle(color, radius = 5f, center = Offset(x, y))
        }
    }
}

@Composable
private fun ChronicRecordItem(r: ChronicRecord) {
    val (color, levelText) = when (r.alertLevel) {
        0 -> MaterialTheme.colorScheme.primary to "正常"
        1 -> MaterialTheme.colorScheme.tertiary to "注意"
        2 -> MaterialTheme.colorScheme.secondary to "警告"
        3 -> MaterialTheme.colorScheme.error to "危险"
        else -> MaterialTheme.colorScheme.outline to "-"
    }
    Card {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(r.date, style = MaterialTheme.typography.titleSmall)
                when (r) {
                    else -> {
                        if (r.systolic != null) Text("血压 ${r.systolic}/${r.diastolic} mmHg · 心率 ${r.heartRate ?: 0}", style = MaterialTheme.typography.bodySmall)
                        if (r.fastingGlucose != null) Text("空腹 ${r.fastingGlucose} mmol/L", style = MaterialTheme.typography.bodySmall)
                        if (r.postprandialGlucose != null) Text("餐后 ${r.postprandialGlucose} mmol/L", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Surface(color = color.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
                Text(levelText, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = color, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}