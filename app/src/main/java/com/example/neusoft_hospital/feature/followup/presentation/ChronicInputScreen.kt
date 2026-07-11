package com.example.neusoft_hospital.feature.followup.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChronicInputUiState(
    val type: ChronicType = ChronicType.HYPERTENSION,
    val systolic: String = "",
    val diastolic: String = "",
    val heartRate: String = "",
    val fastingGlucose: String = "",
    val postprandialGlucose: String = "",
    val hba1c: String = "",
    val note: String = "",
    val loading: Boolean = false,
    val saved: Boolean = false
)

@HiltViewModel
class ChronicInputViewModel @Inject constructor(
    private val repo: ChronicRepository,
    private val prefs: UserPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val typeStr: String = savedStateHandle.get<String>("type") ?: "HYPERTENSION"
    private val _ui = MutableStateFlow(ChronicInputUiState(type = ChronicType.valueOf(typeStr)))
    val ui: StateFlow<ChronicInputUiState> = _ui

    fun onSystolicChange(v: String) { _ui.value = _ui.value.copy(systolic = v) }
    fun onDiastolicChange(v: String) { _ui.value = _ui.value.copy(diastolic = v) }
    fun onHeartRateChange(v: String) { _ui.value = _ui.value.copy(heartRate = v) }
    fun onFastingChange(v: String) { _ui.value = _ui.value.copy(fastingGlucose = v) }
    fun onPostprandialChange(v: String) { _ui.value = _ui.value.copy(postprandialGlucose = v) }
    fun onHba1cChange(v: String) { _ui.value = _ui.value.copy(hba1c = v) }
    fun onNoteChange(v: String) { _ui.value = _ui.value.copy(note = v) }

    fun submit() {
        val s = _ui.value
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            val uid = prefs.tokenFlow.first()
            val record = if (s.type == ChronicType.HYPERTENSION) {
                ChronicRecord(
                    id = "", patientId = uid, type = s.type, date = com.example.neusoft_hospital.core.util.DateExt.today(),
                    systolic = s.systolic.toIntOrNull(), diastolic = s.diastolic.toIntOrNull(),
                    heartRate = s.heartRate.toIntOrNull(), note = s.note
                )
            } else {
                ChronicRecord(
                    id = "", patientId = uid, type = s.type, date = com.example.neusoft_hospital.core.util.DateExt.today(),
                    fastingGlucose = s.fastingGlucose.toFloatOrNull(),
                    postprandialGlucose = s.postprandialGlucose.toFloatOrNull(),
                    hba1c = s.hba1c.toFloatOrNull(), note = s.note
                )
            }
            repo.submit(record)
            _ui.value = _ui.value.copy(loading = false, saved = true)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChronicInputScreen(navController: NavController, vm: ChronicInputViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    LaunchedEffect(ui.saved) { if (ui.saved) navController.popBackStack() }
    Scaffold(
        topBar = { TopAppBar(title = { Text(if (ui.type == ChronicType.HYPERTENSION) "录入血压" else "录入血糖") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }) }
    ) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (ui.type == ChronicType.HYPERTENSION) {
                OutlinedTextField(value = ui.systolic, onValueChange = vm::onSystolicChange, label = { Text("收缩压 (mmHg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = ui.diastolic, onValueChange = vm::onDiastolicChange, label = { Text("舒张压 (mmHg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = ui.heartRate, onValueChange = vm::onHeartRateChange, label = { Text("心率 (次/分)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
            } else {
                OutlinedTextField(value = ui.fastingGlucose, onValueChange = vm::onFastingChange, label = { Text("空腹血糖 (mmol/L)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = ui.postprandialGlucose, onValueChange = vm::onPostprandialChange, label = { Text("餐后血糖 (mmol/L)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = ui.hba1c, onValueChange = vm::onHba1cChange, label = { Text("糖化血红蛋白 (%)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            OutlinedTextField(value = ui.note, onValueChange = vm::onNoteChange, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.weight(1f))
            Button(onClick = vm::submit, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = !ui.loading) {
                if (ui.loading) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text("提交")
            }
        }
    }
}