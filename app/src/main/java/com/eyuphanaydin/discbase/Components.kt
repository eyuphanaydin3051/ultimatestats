package com.eyuphanaydin.discbase

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import com.eyuphanaydin.discbase.ui.theme.IlkuygulamamTheme
import com.eyuphanaydin.discbase.ui.theme.StitchColor
import com.eyuphanaydin.discbase.ui.theme.StitchDefense
import com.eyuphanaydin.discbase.ui.theme.StitchPrimary
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

@Composable
fun PlayerAvatar(
    name: String,
    jerseyNumber: Int? = null,
    photoUrl: String? = null,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp
) {
    // Baş harfleri veya numarayı hazırlayalım
    val initials = if (name.isNotBlank()) {
        name.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.toString() }
            .joinToString("")
            .uppercase()
    } else "?"

    val displayText = if (jerseyNumber != null) "$jerseyNumber" else initials

    // Ortak Tasarım (Baş Harfler Kutusu)
    val InitialsBox = @Composable {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayText,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }

    // --- DÜZELTME BURADA ---
    // photoUrl değişmediği sürece bitmap'i hafızada tut (Performans için kritik)
    val imageModel = remember(photoUrl) { getLogoModel(photoUrl) }

    if (imageModel != null) {
        SubcomposeAsyncImage(
            model = imageModel, // <-- Artık işlenmiş Bitmap veya Uri kullanıyoruz
            contentDescription = name,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(1.dp, Color.LightGray, CircleShape)
                .background(Color.White), // Arka planı beyaz yapalım
            contentScale = ContentScale.Crop,
            error = { InitialsBox() },   // Hata olursa (ör: bozuk veri) kutuyu göster
            loading = { InitialsBox() }  // Yüklenirken kutuyu göster
        )
    } else {
        InitialsBox()
    }
}
// --- 2. SKOR TABELASI (SCOREBOARD) ---
@Composable
fun Scoreboard(
    ourTeamName: String,
    theirTeamName: String,
    scoreUs: Int,
    scoreThem: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = ourTeamName.uppercase(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(text = "$scoreUs", fontSize = 48.sp, fontWeight = FontWeight.Bold)
        }
        Text(text = "VS", fontSize = 24.sp)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = theirTeamName.uppercase(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(text = "$scoreThem", fontSize = 48.sp, fontWeight = FontWeight.Bold)
        }
    }
}
// --- 3. MODERN İKON BUTONU ---
@Composable
fun ModernIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    color: Color,
    contentDescription: String
) {
    // Dışarıdan tıklanabilir, içi renkli ve yuvarlak buton
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = color.copy(alpha = 0.1f), // Hafif arka plan (Rengin %10'u)
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = color, // İkonun kendisi canlı renk
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
// --- 4. İSTATİSTİK ROZETİ (BADGE) ---
@Composable
fun StatBadge(
    text: String,
    count: Int,
    color: Color,
    icon: ImageVector? = null,
    isError: Boolean = false
) {
    Surface(
        color = if (isError) color.copy(alpha = 0.1f) else color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = if (isError) BorderStroke(1.dp, color.copy(0.5f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = if (count > 1) "$text ($count)" else text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
// --- 5. KÜÇÜK İSTATİSTİK KUTUSU ---
@Composable
fun StitchStatBox(title: String, value: String, color: Color, modifier: Modifier = Modifier,valueFontSize: TextUnit = 20.sp) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
            Text(value, fontSize = valueFontSize, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
// --- 6. PERFORMANS ÇUBUĞU (PROGRESS BAR) ---
@Composable
fun PerformanceStatRow(
    title: String,
    percentage: String,
    ratio: String,
    progress: Float,
    progressColor: Color = StitchPrimary,
    teamAverage: Float? = null
) {
    Column(Modifier.padding(vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
            Text(percentage, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StitchColor.TextPrimary)
        }
        Spacer(Modifier.height(8.dp))

        // Özel Tasarım Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFF0F0F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(progressColor) // İstersen buraya Brush (gradyan) verebilirsin
            )
        }

        // Alt Metinler
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(ratio, fontSize = 11.sp, color = Color.LightGray)
            if (teamAverage != null) {
                val avgText = "${String.format("%.1f", teamAverage * 100)}%"
                val isAbove = progress >= teamAverage
                Text(
                    "Ort: $avgText",
                    fontSize = 11.sp,
                    color = if (isAbove) com.eyuphanaydin.discbase.StitchOffense else Color.Gray,
                    fontWeight = if (isAbove) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
// --- 7. PAS AĞI DİYAGRAMI (CHORD DIAGRAM) ---
@Composable
fun PassNetworkChordDiagram(
    mainPlayerName: String,
    passDistribution: Map<String, Int>,
    allPlayers: List<Player>
) {
    val totalPasses = passDistribution.values.sum()
    if (totalPasses == 0) return

    // En çok pas atılan 8 kişiyi al
    val topConnections = passDistribution.toList()
        .sortedByDescending { it.second }
        .take(8)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.title_pass_network), fontWeight = FontWeight.Bold, color = StitchColor.TextPrimary)
        Spacer(Modifier.height(16.dp))

        // Canvas Boyutu
        Canvas(modifier = Modifier.size(340.dp)) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            // Yarıçapı biraz küçülttük ki isimler kenarlara sığsın
            val radius = size.width / 3.5f

            // --- Paint Hazırlığı (Yazı İçin) ---
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK // Siyah yazı
                textSize = 32f // Yazı boyutu
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT,
                    android.graphics.Typeface.BOLD
                )
            }

            val countPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 24f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }

            // 1. MERKEZ (SEÇİLİ OYUNCU)
            drawCircle(
                color = StitchPrimary.copy(alpha = 0.1f),
                radius = 45.dp.toPx(),
                center = center
            )
            drawCircle(
                color = StitchPrimary,
                radius = 8.dp.toPx(),
                center = center
            )
            // Merkezde oyuncunun baş harfleri (Opsiyonel)
            // val initials = mainPlayerName.split(" ").take(2).joinToString("") { it.take(1) }
            // drawContext.canvas.nativeCanvas.drawText(initials, centerX, centerY + 10f, textPaint.apply { color = android.graphics.Color.WHITE })


            // 2. BAĞLANTILAR VE İSİMLER
            val angleStep = (2 * Math.PI) / topConnections.size

            topConnections.forEachIndexed { index, (receiverId, count) ->
                val receiver = allPlayers.find { it.id == receiverId }
                val isHandler =
                    receiver?.position == "Handler" || receiver?.position == "Hybrid"
                val nodeColor =
                    if (isHandler) StitchPrimary else com.eyuphanaydin.discbase.StitchOffense // Mor veya Yeşil
                // İsmin sadece ilk kelimesini al (uzun isimler taşmasın)
                val shortName = receiver?.name?.trim()?.split(" ")?.firstOrNull() ?: "?"

                // Açıyı hesapla (-PI/2 yukarıdan başlaması için)
                val angle = index * angleStep - (Math.PI / 2)

                // Node (Daire) Konumu
                val nodeX = centerX + (radius * Math.cos(angle)).toFloat()
                val nodeY = centerY + (radius * Math.sin(angle)).toFloat()

                // Çizgi Kalınlığı (Pas sayısına göre)
                val percentage = count.toFloat() / totalPasses
                val strokeWidth = 4.dp.toPx() + (14.dp.toPx() * percentage)

                // Çizgiyi Çiz
                drawLine(
                    color = nodeColor.copy(alpha = 0.6f),
                    start = center,
                    end = androidx.compose.ui.geometry.Offset(nodeX, nodeY),
                    strokeWidth = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )

                // Hedef Noktayı Çiz
                drawCircle(
                    color = nodeColor,
                    radius = 10.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(nodeX, nodeY)
                )

                // --- İSİMLERİ YAZDIR ---
                // İsmi dairenin biraz daha dışına konumlandır
                val labelRadius = radius + 40.dp.toPx()
                val labelX = centerX + (labelRadius * Math.cos(angle)).toFloat()
                val labelY = centerY + (labelRadius * Math.sin(angle)).toFloat()

                // İsmi Yaz
                drawContext.canvas.nativeCanvas.drawText(shortName, labelX, labelY, textPaint)

                // Pas Sayısını Altına Yaz
                drawContext.canvas.nativeCanvas.drawText(
                    "($count)",
                    labelX,
                    labelY + 30f,
                    countPaint
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "${stringResource(R.string.label_center)}: $mainPlayerName",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}
// --- 8. BASİT BAŞLIK ---
@Composable
fun SectionHeader(text: String) {
    Surface(
        color = Color(0xFFECEFF1),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(8.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
    }
}
// --- 9. FİLTRE BUTONU (Leaderboard için) ---
@Composable
fun FilterButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(40.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
        border = BorderStroke(1.dp, Color.LightGray)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
        }
    }
}


// ===============================================================
// MAIN ACTIVITY'DEN TAŞINAN YARDIMCI BİLEŞENLER
// ===============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentCard(
    tournament: Tournament,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface), // Beyaz kart
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sol tarafta renkli bir kutu (Tarih veya Baş harf)
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StitchSecondary.copy(alpha = 0.2f)), // Hafif mor
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.EmojiEvents, null, tint = StitchColor.Primary)
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tournament.tournamentName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = tournament.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchCard(
    match: Match,
    tournamentId: String,
    navController: NavController
) {
    // Sonuç Rengi Belirle
    val isWin = match.scoreUs > match.scoreThem
    val isLoss = match.scoreThem > match.scoreUs
    val resultColor = if (isWin) com.eyuphanaydin.discbase.StitchOffense else if (isLoss) StitchDefense else Color.Gray
    val resultText = if (isWin) stringResource(R.string.match_result_win) else if (isLoss) stringResource(R.string.match_result_loss) else stringResource(R.string.match_result_draw)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface), // <-- Koyu/Açık kart rengi
        onClick = { navController.navigate("match_detail/$tournamentId/${match.id}") }
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Sol Renk Şeridi
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(8.dp)
                    .background(resultColor)
            )

            // İçerik
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol: Rakip İsmi
                Column {
                    Text(
                        text = "vs ${match.opponentName}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StitchColor.TextPrimary // <-- Dinamik Renk
                    )
                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = resultColor
                    )
                }

                // Sağ: Skor
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${match.scoreUs}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isWin) com.eyuphanaydin.discbase.StitchOffense else StitchColor.TextPrimary // <-- Kazandıysak yeşil, yoksa tema rengi
                    )
                    Text(
                        text = " - ",
                        fontSize = 24.sp,
                        color = StitchColor.TextSecondary // <-- Tire işareti
                    )
                    Text(
                        text = "${match.scoreThem}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLoss) StitchDefense else StitchColor.TextPrimary // <-- Kaybettiysek kırmızı, yoksa tema rengi
                    )

                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Default.ChevronRight, null, tint = StitchColor.TextSecondary)
                }
            }
        }
    }
}

@Composable
fun PointSummaryCard(
    pointIndex: Int,
    pointData: PointData,
    onViewClick: () -> Unit,
    onEditClick: () -> Unit,
    showEditButton: Boolean
) {
    val startModeLabel = when (pointData.startMode) {
        PointStartMode.OFFENSE -> stringResource(R.string.start_mode_offense)
        PointStartMode.DEFENSE -> stringResource(R.string.start_mode_defense)
        null -> "-"
    }
    val isUs = pointData.whoScored == "US"
    val scoreColor = if (isUs) com.eyuphanaydin.discbase.StitchOffense else StitchDefense
    val titleText = if (isUs) stringResource(R.string.point_ours) else stringResource(R.string.point_theirs)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Sol Renk Şeridi
            Box(modifier = Modifier.fillMaxHeight().width(6.dp).background(scoreColor))

            Column(Modifier.padding(16.dp).weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sayı ${pointIndex + 1}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = StitchColor.TextPrimary)
                    Surface(color = scoreColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text(titleText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = scoreColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(startModeLabel, fontSize = 12.sp, color = Color.Gray)

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onViewClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(stringResource(R.string.btn_detail), fontSize = 12.sp)
                    }
                    if (showEditButton) {
                        Button(
                            onClick = onEditClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StitchColor.Primary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(stringResource(R.string.btn_edit), fontSize = 12.sp) // btn_edit zaten vardı
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandablePassNetworkCard(
    playerStat: PlayerStats,
    allPlayers: List<Player>
) {
    var expanded by remember { mutableStateOf(false) }
    val totalPasses = playerStat.successfulPass + playerStat.assist

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize().clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Başlık Satırı
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlayerAvatar(name = playerStat.name, size = 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(playerStat.name, fontWeight = FontWeight.Bold)
                        Text("$totalPasses Pas", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ArrowDropDown, null, tint = Color.Gray)
            }

            // Açılır İçerik (Diyagram)
            if (expanded) {
                Spacer(Modifier.height(16.dp))
                Divider()
                Spacer(Modifier.height(16.dp))

                PassNetworkChordDiagram(
                    mainPlayerName = playerStat.name,
                    passDistribution = playerStat.passDistribution,
                    allPlayers = allPlayers
                )

                // Metin Olarak Liste
                Spacer(Modifier.height(12.dp))
                playerStat.passDistribution.toList().sortedByDescending { it.second }.take(3).forEach { (rid, count) ->
                    val rName = allPlayers.find { it.id == rid }?.name ?: "?"
                    Text("-> $rName: $count", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
fun MatchPassNetworkCard(
    topPasserStats: PlayerStats,
    allPlayers: List<Player>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Maçın Pas Ağı (En Aktif: ${topPasserStats.name})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StitchColor.TextPrimary
            )
            Spacer(Modifier.height(16.dp))

            // Diyagram
            PassNetworkChordDiagram(
                mainPlayerName = topPasserStats.name,
                passDistribution = topPasserStats.passDistribution,
                allPlayers = allPlayers
            )

            Spacer(Modifier.height(16.dp))

            // En Güçlü 3 Bağlantı
            Text("En Güçlü Bağlantılar:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(Modifier.height(8.dp))

            val topConnections = topPasserStats.passDistribution.toList()
                .sortedByDescending { it.second }
                .take(3)

            topConnections.forEach { (receiverId, count) ->
                val receiverName = allPlayers.find { it.id == receiverId }?.name ?: "Bilinmeyen"
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${topPasserStats.name} -> $receiverName", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("$count Pas", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StitchPrimary)
                }
                Divider(color = Color.LightGray.copy(0.2f))
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerStatCard(
    playerWithStats: AdvancedPlayerStats,
    onClick: () -> Unit
) {
    val stats = playerWithStats.basicStats
    val plusMinus = playerWithStats.plusMinus
    val plusMinusColor = if (plusMinus > 0) com.eyuphanaydin.discbase.StitchOffense else if (plusMinus < 0) StitchDefense else Color.Gray

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            PlayerAvatar(name = stats.name, size = 48.dp)
            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(stats.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = StitchColor.TextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("G: ${stats.goal} A: ${stats.assist} D: ${stats.block}", fontSize = 12.sp, color = Color.Gray)
                }
            }

            // Plus/Minus Göstergesi
            Surface(
                color = plusMinusColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    // String.format kullanarak ondalıklı göster
                    text = if (plusMinus > 0) "+${String.format("%.2f", plusMinus)}" else String.format("%.2f", plusMinus),
                    color = plusMinusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun PointPlayerStatCard(
    playerStat: PlayerStats
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Avatar
            PlayerAvatar(name = playerStat.name, size = 48.dp, fontSize = 16.sp)

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // İsim
                Text(
                    text = playerStat.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = StitchColor.TextPrimary
                )

                Spacer(Modifier.height(8.dp))

                // --- İSTATİSTİK ETİKETLERİ (FlowRow ile yan yana dökülür) ---
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // --- YENİ EKLENEN: CALLAHAN ETİKETİ ---
                    if (playerStat.callahan > 0) {
                        StatBadge(
                            text = "CALLAHAN",
                            count = playerStat.callahan,
                            color = Color(0xFFFFC107), // Altın Sarısı
                            icon = Icons.Default.Star // veya Star
                        )
                    }
                    // -------------------------------------
                    // GOL (Mor)
                    if (playerStat.goal > 0) {
                        StatBadge(
                            text = "GOL",
                            count = playerStat.goal,
                            color = StitchPrimary,
                            icon = Icons.Default.Star
                        )
                    }
                    // ASİST (Koyu Mavi/Mor)
                    if (playerStat.assist > 0) {
                        StatBadge(
                            text = "ASİST",
                            count = playerStat.assist,
                            color = Color(0xFF3F51B5),
                            icon = Icons.Default.AddCircle
                        )
                    }
                    // BLOK (Turuncu/Kırmızı)
                    if (playerStat.block > 0) {
                        StatBadge(
                            text = "BLOK",
                            count = playerStat.block,
                            color = StitchDefense,
                            icon = Icons.Default.Shield
                        )
                    }
                    // THROW (Gri) - Sadece başarılı pas
                    if (playerStat.successfulPass > 0) {
                        StatBadge(
                            text = "Pas",
                            count = playerStat.successfulPass,
                            color = Color.Gray,
                            icon = null
                        )
                    }
                    // CATCH (Yeşilimsi)
                    if (playerStat.catchStat > 0) {
                        StatBadge(
                            text = "Catch",
                            count = playerStat.catchStat,
                            color = Color(0xFF4CAF50),
                            icon = null
                        )
                    }
                    // HATALAR (Kırmızı)
                    if (playerStat.throwaway > 0) {
                        StatBadge(
                            text = "Turn",
                            count = playerStat.throwaway,
                            color = Color(0xFFD32F2F),
                            isError = true
                        )
                    }
                    if (playerStat.drop > 0) {
                        StatBadge(
                            text = "Drop",
                            count = playerStat.drop,
                            color = Color(0xFFD32F2F),
                            isError = true
                        )
                    }
                }
            }
        }
    }
}

// Components.kt dosyasında PassingStatsCard fonksiyonunu bul ve güncelle:

@Composable
fun PassingStatsCard(
    passSuccessRate: StatPercentage,
    stats: PlayerStats,
    teamAverages: Map<String, Double>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.TrendingUp,
                    null,
                    tint = StitchColor.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Pas Performansı", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Divider(Modifier.padding(vertical = 12.dp).alpha(0.1f))

            PerformanceStatRow(
                title = "Başarı Yüzdesi",
                percentage = passSuccessRate.text,
                ratio = passSuccessRate.ratio,
                progress = passSuccessRate.progress,
                progressColor = StitchPrimary,
                teamAverage = teamAverages["passRate"]?.toFloat()
            )

            // --- YENİ: Tempo Hesaplama ---
            val totalThrows = stats.successfulPass + stats.assist + stats.throwaway
            val avgTempo = if (totalThrows > 0) stats.totalTempoSeconds.toDouble() / totalThrows else 0.0
            val tempoColor = if (avgTempo < 3.0) Color(0xFF4CAF50) else if (avgTempo < 6.0) Color(0xFFFF9800) else Color(0xFFF44336)

            Spacer(Modifier.height(8.dp))

            // 3'lü Grid Haline Getirdik
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StitchStatBox(
                    "Asist",
                    stats.assist.toString(),
                    StitchPrimary,
                    Modifier.weight(1f)
                )
                StitchStatBox(
                    "Hata",
                    stats.throwaway.toString(),
                    StitchDefense,
                    Modifier.weight(1f)
                )
                // --- YENİ KUTU ---
                StitchStatBox(
                    "Tempo (sn)",
                    String.format("%.2f", avgTempo),
                    tempoColor,
                    Modifier.weight(1f)
                )
            }
        }
    }
}



@Composable
fun IconActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    color: Color
) {
    FilledIconButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = color.copy(alpha = 0.2f),
            contentColor = color
        ),
        modifier = Modifier.size(width = 90.dp, height = 45.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(25.dp)
        )
    }
}


@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    color: Color? = null,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = if (color != null) {
            ButtonDefaults.buttonColors(containerColor = color)
        } else {
            ButtonDefaults.buttonColors()
        },
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, fontSize = 12.sp)
    }
}





// Components.kt dosyasındaki GenderSelector ve PositionSelector fonksiyonlarını bul ve bunlarla değiştir:

@Composable
fun GenderSelector(
    selectedGender: String,
    onGenderSelect: (String) -> Unit,
    enabled: Boolean = true
) {
    // Veritabanı Değeri -> Ekranda Görünecek Resource ID
    val genderOptions = listOf(
        "Erkek" to R.string.gender_male,
        "Kadın" to R.string.gender_female
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        genderOptions.forEach { (dbValue, labelResId) ->
            FilterChip(
                selected = selectedGender == dbValue,
                onClick = { if (enabled) onGenderSelect(dbValue) },
                label = { Text(stringResource(labelResId)) },
                enabled = enabled,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StitchColor.Primary,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
fun PositionSelector(
    selectedPosition: String,
    onPositionSelect: (String) -> Unit,
    enabled: Boolean = true
) {
    // Veritabanı Değeri -> Ekranda Görünecek Resource ID
    val positions = listOf(
        "Handler" to R.string.pos_handler,
        "Cutter" to R.string.pos_cutter,
        "Hybrid" to R.string.pos_hybrid
    )

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        positions.forEach { (dbValue, labelResId) ->
            FilterChip(
                selected = selectedPosition == dbValue,
                onClick = { if (enabled) onPositionSelect(dbValue) },
                label = { Text(stringResource(labelResId)) },
                enabled = enabled,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StitchColor.Primary,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineSetupScreen(
    navController: NavController,
    tournament: Tournament,
    onNavigateToEdit: (lineId: String?) -> Unit
) {
    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Hazır Line Yönetimi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    ModernIconButton(
                        Icons.Default.ArrowBack,
                        { navController.popBackStack() },
                        StitchTextPrimary,
                        "Geri"
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEdit(null) },
                containerColor = StitchColor.Primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Yeni Line Ekle")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (tournament.presetLines.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Groups,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.LightGray
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Kayıtlı hazır line yok.", color = Color.Gray)
                        Text(
                            "Oyun hızlandırmak için (+) ile ekleyin.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tournament.presetLines) { line ->
                        PresetLineCard(
                            line = line,
                            onClick = { onNavigateToEdit(line.id) }
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetLineCard(
    line: PresetLine,
    onClick: () -> Unit
) {
    // Line türüne göre renk ve ikon belirle
    val (icon, color, label) = when (line.type) {
        LineType.FULL -> Triple(Icons.Default.Groups, StitchPrimary, "Tam Kadro")
        LineType.HANDLER_SET -> Triple(
            Icons.Default.PanTool,
            Color(0xFF1976D2),
            "Handler Seti"
        ) // Mavi
        LineType.CUTTER_SET -> Triple(
            Icons.Default.DirectionsRun,
            Color(0xFF388E3C),
            "Cutter Seti"
        ) // Yeşil
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sol: İkon Kutusu
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color)
            }

            Spacer(Modifier.width(16.dp))

            // Orta: İsim ve Tür
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    line.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = StitchColor.TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Tür Etiketi
                    Surface(
                        color = color.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("${line.playerIds.size} oyuncu", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}



// --- 31. ÖNİZLEME ---
@Preview(showBackground = true)
@Composable
fun StatTrackerPreview() {
    IlkuygulamamTheme {
        UltimateStatsApp()
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PresetLineAddScreen(
    navController: NavController,
    tournament: Tournament,
    lineToEdit: PresetLine?,
    viewModel: MainViewModel = viewModel()
) {
    var lineName by remember { mutableStateOf(lineToEdit?.name ?: "") }
    var selectedPlayerIds by remember {
        mutableStateOf(
            lineToEdit?.playerIds?.toSet() ?: emptySet()
        )
    }
    var selectedType by remember { mutableStateOf(lineToEdit?.type ?: LineType.FULL) }

    val context = LocalContext.current
    val allPlayers by viewModel.players.collectAsState()
    // Sadece turnuva kadrosundakileri göster, isme göre sırala
    val rosterPlayers = allPlayers
        .filter { tournament.rosterPlayerIds.contains(it.id) }
        .sortedBy { it.name }

    val limitText = when (selectedType) {
        LineType.FULL -> "7 kişi önerilir (Maç başlangıcı için)"
        LineType.HANDLER_SET -> "2-4 kişi önerilir (Handler değişimi için)"
        LineType.CUTTER_SET -> "3-5 kişi önerilir (Cutter değişimi için)"
    }

    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (lineToEdit == null) "Yeni Line" else "Line Düzenle",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    ModernIconButton(
                        Icons.Default.ArrowBack,
                        { navController.popBackStack() },
                        StitchTextPrimary,
                        "Geri"
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // 1. AYARLAR KARTI
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = lineName,
                        onValueChange = { lineName = it },
                        label = { Text("Line Adı (Örn: Ofans A)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text("Line Türü:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedType == LineType.FULL,
                            onClick = { selectedType = LineType.FULL },
                            label = { Text("Tam Kadro (7)") },
                            leadingIcon = if (selectedType == LineType.FULL) {
                                { Icon(Icons.Default.Check, null) }
                            } else null
                        )
                        FilterChip(
                            selected = selectedType == LineType.HANDLER_SET,
                            onClick = { selectedType = LineType.HANDLER_SET },
                            label = { Text("Handler Seti") },
                            leadingIcon = if (selectedType == LineType.HANDLER_SET) {
                                { Icon(Icons.Default.Check, null) }
                            } else null
                        )
                        FilterChip(
                            selected = selectedType == LineType.CUTTER_SET,
                            onClick = { selectedType = LineType.CUTTER_SET },
                            label = { Text("Cutter Seti") },
                            leadingIcon = if (selectedType == LineType.CUTTER_SET) {
                                { Icon(Icons.Default.Check, null) }
                            } else null
                        )
                    }
                    Text(
                        limitText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 2. OYUNCU LİSTESİ
            Text(
                "Oyuncuları Seç (${selectedPlayerIds.size})",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rosterPlayers) { player ->
                    val isSelected = selectedPlayerIds.contains(player.id)
                    val cardColor =
                        if (isSelected) StitchPrimary.copy(alpha = 0.1f) else Color.White

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPlayerIds =
                                    if (isSelected) selectedPlayerIds - player.id else selectedPlayerIds + player.id
                            },
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlayerAvatar(
                                name = player.name,
                                jerseyNumber = player.jerseyNumber,
                                size = 40.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(player.name, fontWeight = FontWeight.Bold)
                                Text(player.position, fontSize = 12.sp, color = Color.Gray)
                            }
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = StitchPrimary)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // KAYDET BUTONU
            Button(
                onClick = {
                    if (lineName.isNotBlank() && selectedPlayerIds.isNotEmpty()) {
                        val newLine = PresetLine(
                            id = lineToEdit?.id ?: UUID.randomUUID().toString(),
                            name = lineName,
                            playerIds = selectedPlayerIds.toList(),
                            type = selectedType
                        )
                        val currentLines = tournament.presetLines.toMutableList()
                        if (lineToEdit != null) {
                            val index = currentLines.indexOfFirst { it.id == lineToEdit.id }
                            if (index != -1) currentLines[index] = newLine
                        } else {
                            currentLines.add(newLine)
                        }
                        viewModel.saveTournament(tournament.copy(presetLines = currentLines))
                        navController.popBackStack()
                    } else {
                        Toast.makeText(
                            context,
                            "Ad ve en az 1 oyuncu gerekli.",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StitchColor.Primary)
            ) { Text("Kaydet", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}


@Composable
fun RosterSelectionRow(
    pair: Pair<Player, AdvancedPlayerStats>,
    isSelected: Boolean,
    onToggle: (Pair<Player, AdvancedPlayerStats>) -> Unit
) {
    val player = pair.first
    val stats = pair.second

    // Seçiliyse veya Pozisyona göre arka plan
    val bgColor =
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else when (player.position) {
            "Handler" -> Color(0xFFE3F2FD) // Açık Mavi
            "Cutter" -> Color(0xFFE8F5E9) // Açık Yeşil
            else -> Color.White
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(pair) },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(8.dp), // Hafif köşe
        elevation = CardDefaults.cardElevation(0.dp) // Flat görünüm
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sol: Avatar ve İsim
            PlayerAvatar(
                name = player.name,
                jerseyNumber = player.jerseyNumber,
                size = 40.dp,
                fontSize = 14.sp
            )
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(player.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "${player.position} (${player.gender})",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Orta: İstatistik Özeti (Fotoğraftaki gibi Oyn: 9, O: 7 | D: 2)
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("Oyn: ${stats.basicStats.pointsPlayed}", fontSize = 12.sp)
                Text(
                    text = "O: ${stats.oPointsPlayed} | D: ${stats.dPointsPlayed}", // Doğrusu bu
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchPrimary
                )
            }

            // Sağ: Checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = null, // Card tıklamasını kullanıyoruz
                colors = CheckboxDefaults.colors(checkedColor = StitchPrimary)
            )
        }
    }
}



@Composable
fun IconButtonBox(
    icon: ImageVector,
    bgColor: Color,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp), // Hafif karemsi
        color = bgColor,
        modifier = modifier.height(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp) // İkon boyutu
            )
        }
    }
}



@Composable
fun CompactStatButton(text: String, color: Color, textColor: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.width(60.dp).height(40.dp) // Sabit genişlik
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}


@Composable
fun ActionIconButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    isFilled: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isFilled) color else color.copy(alpha = 0.1f),
        border = if (!isFilled) BorderStroke(1.dp, color.copy(alpha = 0.3f)) else null,
        modifier = Modifier.height(40.dp).defaultMinSize(minWidth = 60.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isFilled) Color.White else color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFilled) Color.White else color
            )
        }
    }
}

@Composable
fun CompactIconButton(icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = color,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CompactActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    isFilled: Boolean = false
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isFilled) color else color.copy(alpha = 0.1f),
            contentColor = if (isFilled) Color.White else color
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = Modifier.defaultMinSize(minWidth = 60.dp, minHeight = 40.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatRowWithAverage(
    title: String,
    value: Int,
    teamAverage: Double,
    isErrorStat: Boolean = false
) {
    val isGood = if (isErrorStat) value < teamAverage else value > teamAverage
    val valueColor = if (isGood && value != 0) Color(0xFF4CAF50)
    else if (isErrorStat && value > teamAverage) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onSurface

    Column(Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "(Ort: ${String.format("%.1f", teamAverage)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray.copy(alpha = 0.7f)
                )
            }
        }
        Divider(
            Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    }
}
@Composable
fun GameTimeCard(
    totalPoints: Int,
    offensePoints: Int,
    defensePoints: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Başlık
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccessTime,
                    null,
                    tint = Color(0xFF673AB7),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Oyun Süresi (Sayı)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchColor.TextPrimary
                )
            }
            Divider(Modifier.padding(vertical = 16.dp).alpha(0.1f))

            // 3'lü Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "$totalPoints",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = StitchColor.TextPrimary
                    )
                    Text("Toplam", fontSize = 12.sp, color = Color.Gray)
                }
                Box(
                    modifier = Modifier.width(1.dp).height(40.dp)
                        .background(Color.LightGray.copy(0.3f))
                        .align(Alignment.CenterVertically)
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "$offensePoints",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.eyuphanaydin.discbase.StitchOffense
                    )
                    Text("Ofans", fontSize = 12.sp, color = com.eyuphanaydin.discbase.StitchOffense)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "$defensePoints",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchDefense
                    )
                    Text("Defans", fontSize = 12.sp, color = StitchDefense)
                }
            }
        }
    }
}

@Composable
fun ReceivingStatsCard(
    catchRate: StatPercentage,
    stats: PlayerStats,
    teamAverages: Map<String, Double>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.PanTool,
                    null,
                    tint = com.eyuphanaydin.discbase.StitchOffense,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Yakalama (Receiving)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchColor.TextPrimary
                )
            }
            Divider(Modifier.padding(vertical = 12.dp).alpha(0.1f))

            PerformanceStatRow(
                title = "Tutuş Yüzdesi",
                percentage = catchRate.text,
                ratio = catchRate.ratio,
                progress = catchRate.progress,
                progressColor = com.eyuphanaydin.discbase.StitchOffense,
                teamAverage = teamAverages["catchRate"]?.toFloat()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StitchStatBox("Gol", stats.goal.toString(), StitchPrimary, Modifier.weight(1f))
                StitchStatBox(
                    "Catch",
                    stats.catchStat.toString(),
                    com.eyuphanaydin.discbase.StitchOffense,
                    Modifier.weight(1f)
                )
                StitchStatBox("Drop", stats.drop.toString(), StitchDefense, Modifier.weight(1f))
            }
        }
    }
}
@Composable
fun DefenseStatsCard(
    stats: PlayerStats,
    teamAverages: Map<String, Double>
) {
    val avgPullTime = if (stats.pullAttempts > 0) {
        stats.totalPullTimeSeconds / stats.pullAttempts
    } else 0.0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)) // Açık Mor Zemin
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Shield,
                    null,
                    tint = Color(0xFF7B1FA2),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Savunma (Defense)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A148C)
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${stats.block}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF7B1FA2)
                    )
                    Text(
                        "Blok (D)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7B1FA2)
                    )
                    Text(
                        "(Ort: ${String.format("%.1f", teamAverages["block"] ?: 0.0)})",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Pull İstatistikleri",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(4.dp))

                    Text(
                        "Toplam: ${stats.pullAttempts} (Başarılı: ${stats.successfulPulls})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // --- YENİ: ORTALAMA SÜRE ---
                    if (stats.pullAttempts > 0) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime,
                                null,
                                modifier = Modifier.size(12.dp),
                                tint = StitchColor.Primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Ort. Süre: ${String.format("%.2f", avgPullTime)} sn",
                                fontWeight = FontWeight.Bold,
                                color = StitchColor.Primary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun PlayerAttendanceDetailDialog(
    player: Player,
    allTrainings: List<Training>,
    onDismiss: () -> Unit
) {
    // --- VERİ HESAPLAMA ---
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // 1. Tarihe göre sırala (En yeniden en eskiye)
    val sortedTrainings = remember(allTrainings) {
        allTrainings.sortedByDescending {
            try {
                dateFormat.parse(it.date)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }

    // 2. Son 5 Antrenman
    val last5Trainings = sortedTrainings.take(5)

    // 3. Aylık İstatistikler
    val monthlyStats = remember(sortedTrainings) {
        sortedTrainings.groupBy {
            try {
                val date = dateFormat.parse(it.date)
                SimpleDateFormat("MMMM yyyy", Locale("tr")).format(date ?: 0L) // Türkçe Ay Adı
            } catch (e: Exception) {
                "Bilinmeyen Tarih"
            }
        }.mapValues { entry ->
            val total = entry.value.size
            val attended = entry.value.count { it.attendeeIds.contains(player.id) }
            Pair(attended, total)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null, // Özel başlık kullanacağız
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // BAŞLIK: Avatar ve İsim
                PlayerAvatar(
                    name = player.name,
                    jerseyNumber = player.jerseyNumber,
                    size = 64.dp,
                    fontSize = 24.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    player.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(player.position, color = Color.Gray, fontSize = 14.sp)

                Spacer(Modifier.height(24.dp))

                // BÖLÜM 1: SON 5 ANTRENMAN
                Text(
                    "Son 5 Antrenman",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchPrimary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    last5Trainings.forEach { training ->
                        val isAttended = training.attendeeIds.contains(player.id)
                        val datePart =
                            training.date.split("/").take(2).joinToString("/") // Gün/Ay

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Durum İkonu
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isAttended) com.eyuphanaydin.discbase.StitchOffense.copy(0.2f) else StitchDefense.copy(
                                            0.1f
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAttended) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (isAttended) com.eyuphanaydin.discbase.StitchOffense else StitchDefense,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(datePart, fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }

                if (last5Trainings.isEmpty()) {
                    Text("Kayıtlı antrenman yok.", fontSize = 12.sp, color = Color.Gray)
                }

                Spacer(Modifier.height(24.dp))
                Divider()
                Spacer(Modifier.height(16.dp))

                // BÖLÜM 2: AYLIK RAPOR
                Text(
                    "Aylık Performans",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchPrimary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp), // Yükseklik sınırı
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(monthlyStats.toList()) { (month, stats) ->
                        val (attended, total) = stats
                        val ratio = attended.toFloat() / total

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                month,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(100.dp)
                            )

                            // Progress Bar
                            Box(
                                modifier = Modifier.weight(1f).height(8.dp)
                                    .clip(RoundedCornerShape(50)).background(Color(0xFFF0F0F0))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio)
                                        .fillMaxHeight()
                                        .background(if (ratio > 0.7) com.eyuphanaydin.discbase.StitchOffense else StitchPrimary)
                                )
                            }

                            Spacer(Modifier.width(12.dp))
                            Text(
                                "$attended/$total",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = StitchColor.Primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Kapat")
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = StitchColor.Surface
    )
}




data class MatchDisplayData(
    val match: Match,
    val tournamentName: String,
    val tournamentId: String,
    val ourTeamName: String
)


@Composable
fun EfficiencyCard(
    efficiencyScore: Double,
    onInfoClick: () -> Unit
) {
    // Rengi puana göre belirle
    val scoreColor = when {
        efficiencyScore >= 10.0 -> Color(0xFF00C853) // Çok İyi (Koyu Yeşil)
        efficiencyScore >= 5.0 -> com.eyuphanaydin.discbase.StitchOffense      // İyi (Teal)
        efficiencyScore >= 0.0 -> StitchPrimary      // Normal (Mor)
        else -> StitchDefense                        // Kötü (Kırmızı)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BarChart, null, tint = scoreColor)
                    Spacer(Modifier.width(8.dp))
                    Text("Verimlilik Puanı", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onInfoClick) {
                    Icon(Icons.Default.Info, contentDescription = "Bilgi", tint = Color.Gray)
                }
            }

            Divider(Modifier.padding(vertical = 12.dp).alpha(0.1f))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format("%.2f", efficiencyScore),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = scoreColor
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "puan",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}