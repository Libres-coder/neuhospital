package com.example.neusoft_hospital.feature.auth.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.neusoft_hospital.core.data.prefs.UserPreferences
import com.example.neusoft_hospital.core.util.IdCardValidator
import com.example.neusoft_hospital.feature.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyAddUiState(
    val name: String = "",
    val phone: String = "",
    val idCard: String = "",
    val relation: String = "子女",
    val loading: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class FamilyAddViewModel @Inject constructor(
    private val repo: AuthRepository,
    private val prefs: UserPreferences
) : ViewModel() {
    private val _ui = MutableStateFlow(FamilyAddUiState())
    val ui: StateFlow<FamilyAddUiState> = _ui

    fun onNameChange(v: String) { _ui.value = _ui.value.copy(name = v, error = null) }
    fun onPhoneChange(v: String) { _ui.value = _ui.value.copy(phone = v, error = null) }
    fun onIdCardChange(v: String) { _ui.value = _ui.value.copy(idCard = v, error = null) }
    fun onRelationChange(v: String) { _ui.value = _ui.value.copy(relation = v) }

    fun save() {
        val state = _ui.value
        if (state.name.isBlank() || state.phone.length != 11 || state.idCard.length != 18) {
            _ui.value = _ui.value.copy(error = "请填写完整正确的信息")
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            val uid = prefs.tokenFlow.first()
            repo.addFamilyMember(uid, state.name, state.phone, state.idCard, state.relation).fold(
                onSuccess = { _ui.value = _ui.value.copy(loading = false, saved = true) },
                onFailure = { _ui.value = _ui.value.copy(loading = false, error = it.message) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyAddScreen(navController: NavController, vm: FamilyAddViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    val relations = listOf("父母", "子女", "配偶", "其他")

    LaunchedEffect(ui.saved) {
        if (ui.saved) navController.popBackStack()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("添加就诊人") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }) }
    ) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(value = ui.name, onValueChange = vm::onNameChange, label = { Text("姓名") }, leadingIcon = { Icon(Icons.Default.Person, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = ui.phone, onValueChange = vm::onPhoneChange, label = { Text("手机号") }, leadingIcon = { Icon(Icons.Default.Phone, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = ui.idCard, onValueChange = vm::onIdCardChange, label = { Text("身份证号") }, leadingIcon = { Icon(Icons.Default.Badge, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            Text("与您的关系", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                relations.forEach { rel ->
                    FilterChip(selected = ui.relation == rel, onClick = { vm.onRelationChange(rel) }, label = { Text(rel) })
                }
            }

            if (ui.error != null) Text(ui.error!!, color = MaterialTheme.colorScheme.error)

            Spacer(Modifier.weight(1f))
            Button(onClick = vm::save, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = !ui.loading) {
                if (ui.loading) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text("保存")
            }
        }
    }
}
