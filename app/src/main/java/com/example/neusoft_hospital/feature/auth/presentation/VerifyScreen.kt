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
import com.example.neusoft_hospital.core.util.IdCardValidator
import com.example.neusoft_hospital.feature.auth.data.AuthRepository
import com.example.neusoft_hospital.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VerifyUiState(
    val step: Int = 0, // 0=实名 1=医保
    val name: String = "",
    val idCard: String = "",
    val idCardFront: String? = null,
    val idCardBack: String? = null,
    val ehsCardNo: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val idCardValid: Boolean = false
)

@HiltViewModel
class VerifyViewModel @Inject constructor(private val repo: AuthRepository) : ViewModel() {
    private val _ui = MutableStateFlow(VerifyUiState())
    val ui: StateFlow<VerifyUiState> = _ui

    fun onNameChange(v: String) { _ui.value = _ui.value.copy(name = v, error = null) }

    fun onIdCardChange(v: String) {
        val valid = IdCardValidator.isValid(v)
        _ui.value = _ui.value.copy(idCard = v, idCardValid = valid, error = null)
    }

    fun onEhsChange(v: String) { _ui.value = _ui.value.copy(ehsCardNo = v, error = null) }

    fun submitVerify() {
        val state = _ui.value
        if (state.name.isBlank() || state.idCard.length != 18) {
            _ui.value = _ui.value.copy(error = "请填写完整真实信息")
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            repo.verifyIdCard(state.name, state.idCard).fold(
                onSuccess = { _ui.value = _ui.value.copy(loading = false, step = 1) },
                onFailure = { _ui.value = _ui.value.copy(loading = false, error = it.message) }
            )
        }
    }

    fun bindEhs() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            repo.bindEhsCard("", _ui.value.ehsCardNo).fold(
                onSuccess = { _ui.value = _ui.value.copy(loading = false, success = true) },
                onFailure = { _ui.value = _ui.value.copy(loading = false, error = it.message) }
            )
        }
    }

    fun skipEhs() { _ui.value = _ui.value.copy(success = true) }

    fun back() { _ui.value = _ui.value.copy(step = 0) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyScreen(navController: NavController, vm: VerifyViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()

    LaunchedEffect(ui.success) {
        if (ui.success) navController.navigate(Routes.AppointmentHome.route) { popUpTo(0) { inclusive = true } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (ui.step == 0) "实名认证" else "绑定医保电子凭证") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (ui.step == 0) navController.popBackStack()
                        else vm.back()
                    }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).padding(24.dp).verticalScroll(
                rememberScrollState()
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (ui.step == 0) {
                Text("请填写真实信息用于实名认证", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = ui.name,
                    onValueChange = vm::onNameChange,
                    label = { Text("真实姓名") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = ui.idCard,
                    onValueChange = vm::onIdCardChange,
                    label = { Text("身份证号（18位）") },
                    leadingIcon = { Icon(Icons.Default.Badge, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        if (ui.idCard.length == 18) {
                            Text(if (ui.idCardValid) "身份证格式正确" else "身份证校验位错误", color = if (ui.idCardValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        }
                    }
                )
                Text("身份证照片（Mock 阶段跳过）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (ui.error != null) Text(ui.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

                Spacer(Modifier.weight(1f))
                Button(
                    onClick = vm::submitVerify,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !ui.loading && ui.name.isNotBlank() && ui.idCardValid
                ) {
                    if (ui.loading) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("下一步：绑定医保")
                }
            } else {
                Text("绑定医保电子凭证", style = MaterialTheme.typography.titleMedium)
                Text("医保电子凭证由国家医保信息平台统一签发，与实体医保卡一一对应。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = ui.ehsCardNo,
                    onValueChange = vm::onEhsChange,
                    label = { Text("医保电子凭证号") },
                    leadingIcon = { Icon(Icons.Default.LocalHospital, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (ui.error != null) Text(ui.error!!, color = MaterialTheme.colorScheme.error)

                Spacer(Modifier.weight(1f))
                Button(onClick = vm::bindEhs, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = !ui.loading && ui.ehsCardNo.isNotBlank()) {
                    if (ui.loading) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("绑定")
                }
                TextButton(onClick = vm::skipEhs, modifier = Modifier.fillMaxWidth()) {
                    Text("暂不绑定")
                }
            }
        }
    }
}
