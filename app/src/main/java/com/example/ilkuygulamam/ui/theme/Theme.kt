package com.example.ilkuygulamam.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.ilkuygulamam.AppTheme // ViewModel'deki Enum'ı import et

// Koyu Tema Renkleri
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = Pink80,
    background = DarkBackground,
    surface = DarkSurface,
    error = DarkError,
    onPrimary = DarkOnPrimary,
    onSecondary = DarkOnSecondary,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    onError = DarkOnError
)

// Açık Tema Renkleri
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

@Composable
fun IlkuygulamamTheme(
    appTheme: AppTheme = AppTheme.LIGHT, // Varsayılan
    dynamicColor: Boolean = false, // DÜZELTME: Varsayılanı false yapalım ki bizim renklerimiz baskın olsun
    content: @Composable () -> Unit
) {
    // 1. Temanın Koyu mu Açık mı olacağına karar ver
    val darkTheme = when (appTheme) {
        AppTheme.LIGHT -> false // Kesinlikle Açık
        AppTheme.DARK -> true   // Kesinlikle Koyu
        AppTheme.SYSTEM -> isSystemInDarkTheme() // Sisteme bak
    }

    // 2. Renk şemasını seç
    val colorScheme = when {
        // Android 12+ dinamik renkleri (Duvar kağıdına göre değişen renkler)
        // Eğer kendi marka renklerini (Mor/Yeşil) kullanmak istiyorsan dynamicColor'ı false yapmalısın.
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // 3. Status Bar (Bildirim çubuğu) rengini ayarla
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar arka plan rengi
            window.statusBarColor = colorScheme.background.toArgb()

            // İkon renkleri (Saat, Pil vs.)
            // Eğer koyu temadaysak ikonlar açık renk olmalı (!darkTheme = false -> light status bar false)
            // Eğer açık temadaysak ikonlar koyu renk olmalı (!darkTheme = true -> light status bar true)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}