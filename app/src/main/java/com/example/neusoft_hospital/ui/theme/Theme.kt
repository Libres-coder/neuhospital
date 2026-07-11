package com.example.neusoft_hospital.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.neusoft_hospital.core.ui.theme.Blue30
import com.example.neusoft_hospital.core.ui.theme.Blue40
import com.example.neusoft_hospital.core.ui.theme.Blue80
import com.example.neusoft_hospital.core.ui.theme.Blue90
import com.example.neusoft_hospital.core.ui.theme.Teal30
import com.example.neusoft_hospital.core.ui.theme.Teal40
import com.example.neusoft_hospital.core.ui.theme.Teal80
import com.example.neusoft_hospital.core.ui.theme.Teal90

private val LightColors = lightColorScheme(
    primary = Blue30,
    onPrimary = Color.White,
    primaryContainer = Blue90,
    onPrimaryContainer = Blue30,
    secondary = Teal30,
    onSecondary = Color.White,
    secondaryContainer = Teal90,
    onSecondaryContainer = Teal30,
    tertiary = Color(0xFF7B1FA2),
    background = Color(0xFFF6F8FB),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),
)

private val DarkColors = darkColorScheme(
    primary = Blue80,
    onPrimary = Blue30,
    primaryContainer = Blue40,
    onPrimaryContainer = Blue90,
    secondary = Teal80,
    onSecondary = Teal30,
    secondaryContainer = Teal40,
    onSecondaryContainer = Teal90,
)

@Composable
fun Neusoft_hospitalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}