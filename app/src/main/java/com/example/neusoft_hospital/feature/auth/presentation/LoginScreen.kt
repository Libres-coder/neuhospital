package com.example.neusoft_hospital.feature.auth.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.neusoft_hospital.feature.auth.data.AuthRepository
import com.example.neusoft_hospital.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val phone: String = "",
    val code: String = "",
    val codeSent: Boolean = false,
    val mockCode: String? = null,  // Mock 模式下返回的验证码，方便用户直接使用
    val loading: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(private val repo: AuthRepository) : ViewModel() {
    private val _ui = MutableStateFlow(LoginUiState())
    val ui: StateFlow<LoginUiState> = _ui

    fun onPhoneChange(v: String) { _ui.value = _ui.value.copy(phone = v, error = null) }
    fun onCodeChange(v: String) { _ui.value = _ui.value.copy(code = v, error = null) }

    fun sendCode() {
        val phone = _ui.value.phone
        if (phone.length != 11) {
            _ui.value = _ui.value.copy(error = "请输入11位手机号")
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            repo.sendSms(phone).fold(
                onSuccess = { code ->
                    _ui.value = _ui.value.copy(loading = false, codeSent = true, mockCode = code, code = code)
                },
                onFailure = { _ui.value = _ui.value.copy(loading = false, error = it.message) }
            )
        }
    }

    fun login() {
        val phone = _ui.value.phone
        val code = _ui.value.code
        if (code.length != 6) {
            _ui.value = _ui.value.copy(error = "请输入6位验证码")
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            repo.verifySms(phone, code).fold(
                onSuccess = { _ui.value = _ui.value.copy(loading = false, loggedIn = true) },
                onFailure = { _ui.value = _ui.value.copy(loading = false, error = it.message) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    vm: LoginViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    var showPass by remember { mutableStateOf(false) }

    LaunchedEffect(ui.loggedIn) {
        if (ui.loggedIn) {
            navController.navigate(Routes.AppointmentHome.route) { popUpTo(0) { inclusive = true } }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("登录/注册") })
        }
    ) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("欢迎使用东软医院", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("请输入手机号登录", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Mock 模式提示
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("开发模式提示", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                        Text("当前为 Mock 环境，验证码固定为 123456，无需真实手机接收。", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            OutlinedTextField(
                value = ui.phone,
                onValueChange = vm::onPhoneChange,
                label = { Text("手机号") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Phone, null) }
            )

            OutlinedTextField(
                value = ui.code,
                onValueChange = vm::onCodeChange,
                label = { Text("验证码") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    TextButton(
                        onClick = vm::sendCode,
                        enabled = !ui.loading && ui.phone.length == 11
                    ) {
                        Text(
                            if (ui.codeSent) "已发送 (${ui.code})" else "获取验证码",
                            color = if (ui.codeSent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            if (ui.error != null) {
                Text(ui.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = vm::login,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !ui.loading && ui.codeSent && ui.code.length == 6
            ) {
                if (ui.loading) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text("登录")
            }

            TextButton(
                onClick = { navController.navigate(Routes.Verify.route) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("实名/医保认证")
            }
        }
    }
}
