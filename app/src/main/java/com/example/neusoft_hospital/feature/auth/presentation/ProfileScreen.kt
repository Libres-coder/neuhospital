package com.example.neusoft_hospital.feature.auth.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.neusoft_hospital.core.data.prefs.UserPreferences
import com.example.neusoft_hospital.feature.appointment.data.AppointmentRepository
import com.example.neusoft_hospital.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val name: String = "",
    val phone: String = "",
    val isVerified: Boolean = false,
    val hasEhs: Boolean = false,
    val clearMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val repo: AppointmentRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(ProfileUiState())
    val ui: StateFlow<ProfileUiState> = _ui

    init {
        viewModelScope.launch {
            _ui.value = ProfileUiState(
                name = prefs.userNameFlow.first(),
                phone = prefs.phoneFlow.first(),
                isVerified = prefs.isVerifiedFlow.first(),
                hasEhs = prefs.hasEhsFlow.first()
            )
        }
    }

    fun logout(nav: NavController) {
        viewModelScope.launch {
            prefs.logout()
            nav.navigate(Routes.Login.route) { popUpTo(0) { inclusive = true } }
        }
    }

    /** Wipes every appointment row owned by the current user. Debug helper. */
    fun clearAppointments() {
        viewModelScope.launch {
            val n = repo.clearMyAppointments()
            _ui.value = _ui.value.copy(clearMessage = "已清除 $n 条预约记录")
        }
    }

    fun consumeClearMessage() { _ui.value = _ui.value.copy(clearMessage = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, vm: ProfileViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text("个人中心") }) }
    ) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(64.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primary) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(ui.name.ifBlank { "未登录" }, style = MaterialTheme.typography.titleLarge)
                        Text(ui.phone, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            ListItem(
                headlineContent = { Text("我的预约") },
                leadingContent = { Icon(Icons.Default.EventNote, null) },
                modifier = Modifier.clickable { navController.navigate(Routes.MyAppointments.route) }
            )
            ListItem(
                headlineContent = { Text("亲情账户") },
                leadingContent = { Icon(Icons.Default.AccountCircle, null) },
                modifier = Modifier.clickable { navController.navigate(Routes.FamilyManage.route) }
            )
            ListItem(
                headlineContent = { Text("实名认证") },
                supportingContent = { Text(if (ui.isVerified) "已认证" else "未认证") },
                leadingContent = { Icon(Icons.Default.Badge, null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                modifier = Modifier.clickable { navController.navigate(Routes.Verify.route) }
            )
            ListItem(
                headlineContent = { Text("医保电子凭证") },
                supportingContent = { Text(if (ui.hasEhs) "已绑定" else "未绑定") },
                leadingContent = { Icon(Icons.Default.LocalHospital, null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                modifier = Modifier.clickable { navController.navigate(Routes.Verify.route) }
            )
            Spacer(Modifier.weight(1f))
            val msg = ui.clearMessage
            if (msg != null) {
                Text(msg, color = MaterialTheme.colorScheme.primary)
                vm.consumeClearMessage()
            }
            OutlinedButton(onClick = { vm.clearAppointments() }, modifier = Modifier.fillMaxWidth()) {
                Text("清空我的预约（调试用）")
            }
            OutlinedButton(onClick = { vm.logout(navController) }, modifier = Modifier.fillMaxWidth()) {
                Text("退出登录", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}