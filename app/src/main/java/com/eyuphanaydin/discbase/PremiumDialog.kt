package com.eyuphanaydin.discbase

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyuphanaydin.discbase.ui.theme.StitchColor

@Composable
fun PremiumRequiredDialog(
    price: String, // Fiyat parametresi eklendi
    onDismiss: () -> Unit,
    onBuyClick: () -> Unit
) {
    val buttonText = if (price.isNotEmpty()) "Abone Ol ($price)" else "Abonelik Başlat"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = StitchColor.Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Advanced Mode")
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
                Text(buttonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}