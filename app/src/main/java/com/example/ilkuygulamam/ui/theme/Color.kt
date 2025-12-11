package com.example.ilkuygulamam.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import com.example.ilkuygulamam.StitchSecondary

// --- SABİT RENK PALETİ (Bu renkler değişmez, referans içindir) ---
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Stitch Marka Renkleri
private val StitchLightPrimary = Color(0xFF6200EA) // Koyu Mor (Açık modda butonlar)
private val StitchDarkPrimary = Color(0xFFBB86FC)  // Açık Mor (Koyu modda butonlar parlasın diye)

private val StitchLightBg = Color(0xFFF9F9F9)      // Açık Gri Zemin
private val StitchDarkBg = Color(0xFF121212)       // Koyu Siyah Zemin

private val StitchLightCard = Color(0xFFFFFFFF)    // Beyaz Kart
private val StitchDarkCard = Color(0xFF1E1E1E)     // Koyu Gri Kart

private val StitchLightText = Color(0xFF212121)    // Siyah Yazı
private val StitchDarkText = Color(0xFFE0E0E0)     // Beyazımsı Yazı

// Aksiyon Renkleri (Hücum/Defans)
val StitchOffense = Color(0xFF00C49A) // Teal/Yeşil
val StitchDefense = Color(0xFFFF6B6B) // Kırmızı/Turuncu
val StitchGradientStart = Color(0xFF3A1078)
val StitchGradientEnd = Color(0xFF2F58CD)

// --- AKILLI RENK NESNESİ (Dinamik Renkler) ---
// Uygulamanın geri kalanında ARTIK BUNLARI kullanacağız.
object StitchColor {
    val Primary: Color
        @Composable get() = MaterialTheme.colorScheme.primary

    val Background: Color
        @Composable get() = MaterialTheme.colorScheme.background

    val Surface: Color // Kart Rengi
        @Composable get() = MaterialTheme.colorScheme.surface

    val TextPrimary: Color
        @Composable get() = MaterialTheme.colorScheme.onBackground

    val TextSecondary: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
}

// Eski kodların bozulmaması için (Geriye uyumluluk)
// Ama bunları yavaş yavaş StitchColor.Background ile değiştireceğiz.
val StitchPrimary = StitchLightPrimary
val StitchBackground = StitchLightBg
val StitchCardBg = StitchLightCard
val StitchTextPrimary = StitchLightText
// Theme.kt içindeki DarkColorScheme tarafından aranan eksik renkler:
val DarkPrimary = StitchDarkPrimary
val DarkSecondary = StitchSecondary
val DarkBackground = StitchDarkBg
val DarkSurface = StitchDarkCard
val DarkError = StitchDefense

val DarkOnPrimary = Color.Black
val DarkOnSecondary = Color.White
val DarkOnBackground = StitchDarkText
val DarkOnSurface = StitchDarkText
val DarkOnError = Color.White