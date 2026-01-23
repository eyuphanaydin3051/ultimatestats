package com.eyuphanaydin.discbase

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyuphanaydin.discbase.ui.theme.StitchColor // Renk teman buradaysa
// Eğer StitchColor bulunamazsa yerine Color.Blue vb. geçici bir renk koyabilirsin.

@Composable
fun PremiumRequiredDialog (onDismiss: () -> Unit,onBuyClick: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = StitchColor.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Advanced Mode (Premium)")
            }
        },
        text = {
            Column {
                Text("Gelişmiş istatistik modunu kullanmak için Premium üyeliğe geçmelisiniz.")
                Spacer(modifier = Modifier.height(16.dp))
                Text("• Detaylı istatistikler (Drop, Turnover, Blok)", fontSize = 14.sp)
                Text("• PDF Raporlama", fontSize = 14.sp)
                Text("• Reklamsız deneyim", fontSize = 14.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = onBuyClick,
                colors = ButtonDefaults.buttonColors(containerColor = StitchColor.Primary)
            ) {
                Text("Abonelik Başlat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}