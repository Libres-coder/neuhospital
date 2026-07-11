package com.example.neusoft_hospital

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.neusoft_hospital.core.data.prefs.UserPreferences
import com.example.neusoft_hospital.navigation.MainScaffold
import com.example.neusoft_hospital.navigation.Routes
import com.example.neusoft_hospital.ui.theme.Neusoft_hospitalTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefs: UserPreferences

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* ignore result, app still works without */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            Neusoft_hospitalTheme {
                val navController = rememberNavController()
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val logged = prefs.isLoggedInFlow.first()
                    startDestination = if (logged) Routes.AppointmentHome.route else Routes.Login.route
                }

                startDestination?.let { start ->
                    MainScaffold(navController = navController, startDestination = start)
                }
            }
        }
    }
}