package com.example.discbase

// Screens_ProMode.kt en üstüne ekleyin:
import android.content.pm.ActivityInfo // <-- BU IMPORT'U EKLEMEYİ UNUTMA
import androidx.compose.ui.draw.clip // <-- clip hatası için
import coil.compose.AsyncImage      // <-- AsyncImage hatası için
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.discbase.ui.theme.*
import java.util.UUID
import kotlin.math.pow
import kotlin.math.sqrt
import androidx.compose.foundation.lazy.LazyRow // <-- BU EKSİKTİ
import androidx.compose.foundation.BorderStroke // <-- Border hatası için
import androidx.compose.material3.FilterChip // <-- Chip hatası için
import androidx.compose.material3.FilterChipDefaults // <-- Chip renkleri için
// --- OYUN DURUMU ENUM ---
private enum class ProGameState {
    LINE_SELECTION, START_SELECTION,
    PULL_RECEIVE_LOC, PULL_RECEIVER, OFFENSE_PLAY, WAITING_ACTION,
    PULL_THROWER, PULL_DESTINATION, DEFENSE_WAITING, TURNOVER_LOC, TURNOVER_PICKUP
}

enum class SelectionMode { SELECT, ACTION }

private enum class AttackDirection { UP, DOWN }
// --- YEREL DURUM VE MODELLER (GÜNCELLENDİ) ---

private enum class AnalysisState {
    LINE_SELECTION,
    START_MODE_SELECTION,
    DIRECTION_SELECTION, // <-- BU SATIRI EKLEYİN
    PULL_SETUP,
    PULL_LANDING,
    DEFENSE_PHASE,
    SET_DISC_START,
    ACTION_PHASE
}
private enum class SelectorType { NONE, SET_THROWER, PASS_TARGET, SET_PULLER }

// Görselleştirme için yeni model
private data class ProVisualEvent(
    val id: String = UUID.randomUUID().toString(),
    val from: Offset?,
    val to: Offset,
    val type: String, // PASS, GOAL, DROP, THROWAWAY, PULL, PICKUP
    val color: Color,
    val throwerId: String?,
    val receiverId: String?
)

// --- YEREL GÖRSEL MODEL ---
private data class VisualEvent(
    val from: Offset?,
    val to: Offset,
    val color: Color,
    val isGoal: Boolean = false,
    val type: String = "PASS",
    val distance: Double = 0.0,
    val throwerName: String? = null,
    val receiverName: String? = null
)

// --- SAHA ÖLÇÜLERİ ---
const val FIELD_WIDTH_YARDS = 40.0
const val FIELD_LENGTH_YARDS = 110.0
const val ENDZONE_TOP_RATIO = 20.0 / 110.0
const val ENDZONE_BOTTOM_RATIO = 1.0 - (20.0 / 110.0)

@Composable
fun ProModeHost(
    mainViewModel: MainViewModel,
    tournaments: List<Tournament>,
    allPlayers: List<Player>,
    onFullScreenToggle: (Boolean) -> Unit // <-- YENİ PARAMETRE
) {
    val proNavController = rememberNavController()

    NavHost(navController = proNavController, startDestination = "pro_tournament_list") {
        // Diğer ekranlarda Tam Ekran KAPALI (false)
        composable("pro_tournament_list") {
            // Listeye geri dönüldüğünde barların geri geldiğinden emin olalım
            LaunchedEffect(Unit) { onFullScreenToggle(false) }
            ProTournamentListScreen(
                tournaments = tournaments,
                onTournamentSelected = { id -> proNavController.navigate("pro_tournament_detail/$id") }
            )
        }

        composable("pro_tournament_detail/{tournamentId}") { backStack ->
            // Detaya dönüldüğünde barların geri geldiğinden emin olalım
            LaunchedEffect(Unit) { onFullScreenToggle(false) }

            val tId = backStack.arguments?.getString("tournamentId")
            val tournament = tournaments.find { it.id == tId }
            if (tournament != null) {
                ProTournamentDetailScreen(
                    tournament = tournament,
                    onBack = { proNavController.popBackStack() },
                    onNewMatch = { proNavController.navigate("pro_match_setup/${tournament.id}") },
                    onMatchClick = { mId -> proNavController.navigate("pro_point_history/${tournament.id}/$mId") },
                    onDeleteMatch = { mId -> mainViewModel.deleteMatch(tournament.id, mId) }
                )
            }
        }

        composable("pro_point_history/{tournamentId}/{matchId}") { backStack ->
            // Geçmiş ekranında da barlar açık olsun
            LaunchedEffect(Unit) { onFullScreenToggle(false) }
            val tId = backStack.arguments?.getString("tournamentId")
            val mId = backStack.arguments?.getString("matchId")
            val tournament = tournaments.find { it.id == tId }
            val match = tournament?.matches?.find { it.id == mId }

            if (match != null && tournament != null) {
                ProMatchHistoryScreen(
                    match = match,
                    viewModel = mainViewModel,
                    onBack = { proNavController.popBackStack() },
                    onContinueMatch = { proNavController.navigate("pro_analysis/${tournament.id}/${match.id}") }
                )
            }
        }

        composable("pro_match_setup/{tournamentId}") { backStack ->
            LaunchedEffect(Unit) { onFullScreenToggle(false) }
            val tId = backStack.arguments?.getString("tournamentId")
            val tournament = tournaments.find { it.id == tId }
            if (tournament != null) {
                ProMatchSetupScreen(
                    tournament = tournament,
                    onBack = { proNavController.popBackStack() },
                    onStartMatch = { opponent ->
                        val newMatch = Match(
                            id = UUID.randomUUID().toString(),
                            opponentName = opponent,
                            ourTeamName = tournament.ourTeamName,
                            isProMode = true
                        )
                        mainViewModel.saveMatch(tournament.id, newMatch)
                        proNavController.navigate("pro_analysis/${tournament.id}/${newMatch.id}") {
                            popUpTo("pro_tournament_detail/${tournament.id}") { inclusive = false }
                        }
                    }
                )
            }
        }

        // --- KRİTİK NOKTA: SADECE ANALİZ EKRANINDA GİZLİYORUZ ---
        // --- PRO ANALİZ EKRANI ---
        composable("pro_analysis/{tournamentId}/{matchId}") { backStack ->
            val tId = backStack.arguments?.getString("tournamentId")
            val mId = backStack.arguments?.getString("matchId")
            val tournament = tournaments.find { it.id == tId }
            val match = tournament?.matches?.find { it.id == mId }
            val roster = allPlayers.filter { tournament?.rosterPlayerIds?.contains(it.id) == true }

            if (match != null && tournament != null) {
                ProAnalysisScreen(
                    match = match,
                    roster = roster,
                    // YENİ: Hazır setleri gönderiyoruz
                    presetLines = tournament.presetLines,
                    onUpdateMatch = { updatedMatch -> mainViewModel.saveMatch(tournament.id, updatedMatch) },
                    onExit = { proNavController.popBackStack("pro_tournament_detail/$tId", inclusive = false) },
                    onToggleFullScreen = onFullScreenToggle
                )
            }
        }
    }
}
private fun getDistanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val l2 = (a.x - b.x).pow(2) + (a.y - b.y).pow(2)
    if (l2 == 0f) return sqrt((p.x - a.x).pow(2) + (p.y - a.y).pow(2)) // A ve B aynı nokta ise

    // Tıklamanın doğru üzerindeki izdüşümünü (t) bul
    var t = ((p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y)) / l2
    t = t.coerceIn(0f, 1f) // Doğru parçasının dışına taşmasını engelle

    val projectionX = a.x + t * (b.x - a.x)
    val projectionY = a.y + t * (b.y - a.y)

    return sqrt((p.x - projectionX).pow(2) + (p.y - projectionY).pow(2))
}
// --- MAÇ GEÇMİŞİ VE İZLEME EKRANI ---
// --- MAÇ GEÇMİŞİ VE İZLEME EKRANI (DÜZELTİLMİŞ) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProMatchHistoryScreen(
    match: Match,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onContinueMatch: () -> Unit
) {
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    var isEventListExpanded by remember { mutableStateOf(false) }
    var selectedEventDetail by remember { mutableStateOf<VisualEvent?>(null) }
    var isFilterMenuVisible by remember { mutableStateOf(false) }
    var selectedPlayerFilter by remember { mutableStateOf<String?>(null) }

    if (selectedPointIndex != null) {
        val pointData = match.pointsArchive[selectedPointIndex!!]
        val visuals = remember(pointData) {
            pointData.proEvents.map { event ->
                VisualEvent(
                    from = if (event.fromX != null && event.fromY != null) Offset(event.fromX, event.fromY) else null,
                    to = Offset(event.toX, event.toY),
                    color = when (event.type) {
                        "GOAL" -> StitchOffense
                        "THROWAWAY", "DROP" -> StitchDefense
                        "PULL" -> Color.Gray
                        "DEFENSE" -> StitchDefense
                        else -> Color.White
                    },
                    isGoal = event.type == "GOAL",
                    type = event.type,
                    distance = event.distanceYards,
                    throwerName = event.throwerName,
                    receiverName = event.receiverName
                )
            }
        }

        // FİLTRELEME MANTIĞI
        val filteredVisuals = remember(visuals, selectedPlayerFilter) {
            if (selectedPlayerFilter == null) {
                visuals
            } else {
                visuals.filter { event ->
                    event.throwerName == selectedPlayerFilter || event.receiverName == selectedPlayerFilter
                }
            }
        }

        // AKTİF OYUNCULAR LİSTESİ
        val activePlayersInPoint = remember(pointData) {
            val names = pointData.proEvents.flatMap { listOfNotNull(it.throwerName, it.receiverName) }.distinct().sorted()
            names.mapNotNull { name -> getPlayerByName(name, viewModel) }
        }

        // DETAY DİYALOĞU
        if (selectedEventDetail != null) {
            val event = selectedEventDetail!!
            val isGoal = event.type == "GOAL"
            val isError = event.type == "THROWAWAY" || event.type == "DROP"

            val throwerPlayer = getPlayerByName(event.throwerName, viewModel)
            val receiverPlayer = getPlayerByName(event.receiverName, viewModel)

            AlertDialog(
                onDismissRequest = { selectedEventDetail = null },
                containerColor = StitchColor.Surface,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val icon = when {
                            isGoal -> Icons.Default.EmojiEvents
                            isError -> Icons.Default.Warning
                            else -> Icons.Default.SportsHandball
                        }
                        val color = when {
                            isGoal -> StitchOffense
                            isError -> StitchDefense
                            else -> StitchColor.Primary
                        }
                        Icon(icon, null, tint = color)
                        Spacer(Modifier.width(8.dp))
                        Text(text = if (isGoal) "Sayı Detayı" else "Olay Detayı", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (event.throwerName != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (throwerPlayer != null) {
                                    PlayerAvatar(name = throwerPlayer.name, jerseyNumber = throwerPlayer.jerseyNumber, photoUrl = throwerPlayer.photoUrl, size = 48.dp)
                                } else {
                                    Surface(shape = CircleShape, color = Color.Gray, modifier = Modifier.size(48.dp)) {
                                        Box(contentAlignment = Alignment.Center) { Text(event.throwerName.take(1), color = Color.White, fontWeight = FontWeight.Bold) }
                                    }
                                }
                                Column {
                                    Text(if (isGoal) "ASİST" else "ATAN", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Text(throwerPlayer?.name ?: event.throwerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            HorizontalDivider(color = Color.LightGray.copy(0.3f))
                        }

                        if (event.receiverName != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (receiverPlayer != null) {
                                    PlayerAvatar(name = receiverPlayer.name, jerseyNumber = receiverPlayer.jerseyNumber, photoUrl = receiverPlayer.photoUrl, size = 48.dp)
                                } else {
                                    Surface(shape = CircleShape, color = if (isGoal) StitchOffense else Color.Gray, modifier = Modifier.size(48.dp)) {
                                        Box(contentAlignment = Alignment.Center) { Text(event.receiverName.take(1), color = Color.White, fontWeight = FontWeight.Bold) }
                                    }
                                }
                                Column {
                                    Text(if (isGoal) "GOL" else "TUTAN", style = MaterialTheme.typography.labelSmall, color = if (isGoal) StitchOffense else Color.Gray, fontWeight = FontWeight.Bold)
                                    Text(receiverPlayer?.name ?: event.receiverName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Text("Olay: ${event.type}", fontSize = 12.sp, color = Color.Gray)
                            if (event.distance > 0) {
                                Text("Mesafe: ${String.format("%.1f", event.distance)} yd", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedEventDetail = null }) { Text("Kapat") }
                }
            )
        }

        Scaffold(
            containerColor = Color(0xFF121212),
            topBar = {
                TopAppBar(
                    title = { Text("Sayı ${selectedPointIndex!! + 1} Detayı", color = Color.White) },
                    navigationIcon = { IconButton(onClick = { selectedPointIndex = null }) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(0.4f))
                )
            }
        ) { padding ->
            BoxWithConstraints(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // Saha Çizim Boyutlarını Hesapla
                val fieldAspectRatio = (FIELD_WIDTH_YARDS / FIELD_LENGTH_YARDS).toFloat()
                val screenWidth = maxWidth.value
                val screenHeight = maxHeight.value

                var drawWidth = screenWidth
                var drawHeight = screenWidth / fieldAspectRatio
                if (drawHeight > screenHeight) {
                    drawHeight = screenHeight
                    drawWidth = screenHeight * fieldAspectRatio
                }

                val offsetX = (screenWidth - drawWidth) / 2f
                val offsetY = (screenHeight - drawHeight) / 2f

                // 1. ZEMİN VE ÇİZGİLER (CANVAS)
                FieldCanvas(
                    visualEvents = filteredVisuals,
                    onTap = { clickedNormPos ->
                        val circleThreshold = 0.05f
                        var found = visuals.findLast { event -> // Orijinal listeden arıyoruz ki görünmeyenlere tıklanmasın
                            // Ama sadece filtrelenmiş listede varsa kabul et
                            if (filteredVisuals.contains(event)) {
                                val dx = event.to.x - clickedNormPos.x
                                val dy = event.to.y - clickedNormPos.y
                                sqrt(dx * dx + dy * dy) < circleThreshold
                            } else false
                        }
                        if (found == null) {
                            val lineThreshold = 0.025f
                            found = visuals.findLast { event ->
                                if (filteredVisuals.contains(event) && event.from != null) {
                                    val dist = getDistanceToSegment(clickedNormPos, event.from, event.to)
                                    dist < lineThreshold
                                } else false
                            }
                        }
                        if (found != null) selectedEventDetail = found
                    },
                    currentThrowerPos = null
                )

                // 2. KATMAN: GOAL ve ASIST AVATARLARI
                filteredVisuals.filter { it.type == "GOAL" }.forEach { event ->
                    val receiverPlayer = getPlayerByName(event.receiverName, viewModel)
                    val goalX = (offsetX + (event.to.x * drawWidth)).dp
                    val goalY = (offsetY + (event.to.y * drawHeight)).dp
                    val avatarSize = 36.dp

                    Box(
                        modifier = Modifier
                            .offset(x = goalX - (avatarSize / 2), y = goalY - (avatarSize / 2))
                            .size(avatarSize)
                    ) {
                        if (receiverPlayer != null) {
                            PlayerAvatar(name = receiverPlayer.name, jerseyNumber = receiverPlayer.jerseyNumber, photoUrl = receiverPlayer.photoUrl, size = avatarSize, fontSize = 12.sp)
                        } else {
                            Surface(shape = CircleShape, color = StitchOffense, modifier = Modifier.fillMaxSize()) {
                                Box(contentAlignment = Alignment.Center) { Text("G", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }

                    if (event.from != null) {
                        val assistPlayer = getPlayerByName(event.throwerName, viewModel)
                        val assistX = (offsetX + (event.from.x * drawWidth)).dp
                        val assistY = (offsetY + (event.from.y * drawHeight)).dp

                        Box(
                            modifier = Modifier
                                .offset(x = assistX - (avatarSize / 2), y = assistY - (avatarSize / 2))
                                .size(avatarSize)
                        ) {
                            if (assistPlayer != null) {
                                PlayerAvatar(name = assistPlayer.name, jerseyNumber = assistPlayer.jerseyNumber, photoUrl = assistPlayer.photoUrl, size = avatarSize, fontSize = 12.sp)
                            } else {
                                Surface(shape = CircleShape, color = StitchColor.Primary, modifier = Modifier.fillMaxSize()) {
                                    Box(contentAlignment = Alignment.Center) { Text("A", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                }

                // 3. FİLTRE MENÜSÜ VE BUTONU
                if (activePlayersInPoint.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, end = 8.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        FilledIconButton(
                            onClick = { isFilterMenuVisible = !isFilterMenuVisible },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isFilterMenuVisible) StitchColor.Primary else Color.Black.copy(alpha = 0.6f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = if (isFilterMenuVisible) Icons.Default.Close else if (selectedPlayerFilter != null) Icons.Default.FilterAlt else Icons.Default.FilterList,
                                contentDescription = "Filtrele"
                            )
                        }
                    }
                }

                // 4. AÇILIR FİLTRE PANELİ
                if (isFilterMenuVisible && activePlayersInPoint.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp)
                            .padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E1E1E).copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, Color.Gray.copy(0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Odaklanacak Oyuncuyu Seç",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    val isTotalSelected = selectedPlayerFilter == null
                                    FilterChip(
                                        selected = isTotalSelected,
                                        onClick = { selectedPlayerFilter = null },
                                        label = { Text("Tümü") },
                                        enabled = true,
                                        leadingIcon = if (isTotalSelected) { { Icon(Icons.Default.Check, null) } } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color.White,
                                            selectedLabelColor = Color.Black,
                                            containerColor = Color.Black,
                                            labelColor = Color.White
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isTotalSelected,
                                            borderColor = Color.Gray
                                        )
                                    )
                                }

                                items(items = activePlayersInPoint) { player ->
                                    val isSelected = selectedPlayerFilter == player.name
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedPlayerFilter = if (isSelected) null else player.name },
                                        label = { Text(player.name.split(" ").first()) },
                                        enabled = true,
                                        leadingIcon = {
                                            if (player.photoUrl != null) {
                                                AsyncImage(model = player.photoUrl, contentDescription = null, modifier = Modifier.size(20.dp).clip(CircleShape))
                                            }
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = StitchOffense,
                                            selectedLabelColor = Color.White,
                                            containerColor = Color.Black,
                                            labelColor = Color.White
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = if (isSelected) StitchOffense else Color.Gray
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. ALT MENÜ (OLAY LİSTESİ)
                val sheetHeight by animateDpAsState(targetValue = if (isEventListExpanded) 400.dp else 60.dp)
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(sheetHeight)
                        .clickable(enabled = !isEventListExpanded) { isEventListExpanded = true },
                    color = Color.Black.copy(0.85f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .clickable { isEventListExpanded = !isEventListExpanded }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Olay Akışı (${pointData.proEvents.size})", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            Icon(
                                imageVector = if (isEventListExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        HorizontalDivider(color = Color.Gray.copy(0.5f))
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            itemsIndexed(visuals) { idx, event ->
                                val color = event.color
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedEventDetail = event }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${idx + 1}", color = Color.Gray, modifier = Modifier.width(24.dp), fontSize = 12.sp)
                                    Text(event.type, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.width(90.dp), fontSize = 14.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        if (event.throwerName != null && event.receiverName != null) {
                                            Text("${event.throwerName} -> ${event.receiverName}", color = Color.White, fontSize = 14.sp)
                                        } else if (event.receiverName != null) {
                                            Text(event.receiverName, color = Color.White, fontSize = 14.sp)
                                        }
                                    }
                                    if (event.distance > 0) {
                                        Text(String.format("%.0f yd", event.distance), color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                                HorizontalDivider(color = Color.Gray.copy(0.2f))
                            }
                        }
                    }
                }
            }
        }
    } else {
        // --- MAÇ LİSTESİ GÖRÜNÜMÜ ---
        Scaffold(
            containerColor = StitchColor.Background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("${match.scoreUs} - ${match.scoreThem}", fontWeight = FontWeight.Black, fontSize = 24.sp) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") } }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(onClick = onContinueMatch, containerColor = StitchPrimary, contentColor = Color.White) {
                    Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("MAÇA DEVAM ET")
                }
            }
        ) { padding ->
            LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(match.pointsArchive) { index, point ->
                    Card(
                        onClick = { selectedPointIndex = index },
                        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).background(if (point.whoScored == "US") StitchOffense else StitchDefense, CircleShape), contentAlignment = Alignment.Center) {
                                Text("${index + 1}", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (point.whoScored == "US") "BİZİM SAYI" else "RAKİP SAYI", fontWeight = FontWeight.Bold)
                                Text(
                                    "${point.stats.sumOf { it.successfulPass + it.assist }} Pas • ${point.proEvents.size} Olay",
                                    fontSize = 12.sp, color = Color.Gray
                                )
                            }
                            Icon(Icons.Default.Map, null, tint = StitchPrimary)
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}


// --- EKRAN: ANALİZ SAHASI (YATAY MOD TASARIMI) ---
@Composable
fun ProAnalysisScreen(
    match: Match,
    roster: List<Player>,
    presetLines: List<PresetLine>, // <-- YENİ PARAMETRE
    onUpdateMatch: (Match) -> Unit,
    onExit: () -> Unit,
    onToggleFullScreen: (Boolean) -> Unit
) {

    var analysisState by remember { mutableStateOf(AnalysisState.LINE_SELECTION) }
    var isOffenseStart by remember { mutableStateOf(true) }
    var isLeftToRight by remember { mutableStateOf(true) }

    // --- 1. EKRAN YÖNÜ AYARI (DÜZELTME) ---
    // Kadro ve Mod seçiminde DİKEY, Saha analizinde YATAY
    // ... (State tanımları scoreUs, scoreThem vs. aynı kalsın) ...

    // --- 1. EKRAN YÖNÜ VE BAR KONTROLÜ (DÜZELTİLDİ) ---
    // Sadece SAHA aşamalarında YATAY, Seçim aşamalarında DİKEY
    val isLandscapeMode = when (analysisState) {
        AnalysisState.LINE_SELECTION,
        AnalysisState.START_MODE_SELECTION,
        AnalysisState.DIRECTION_SELECTION -> false // Dikey Mod
        else -> true // Yatay Mod (Saha)
    }

    val targetOrientation = if (isLandscapeMode) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    LockScreenOrientation(targetOrientation)

    LaunchedEffect(isLandscapeMode) {
        onToggleFullScreen(isLandscapeMode)
    }

    // --- 2. VERİ GÜNCELLEME (İSTATİSTİK HATASI DÜZELTİLDİ) ---
    // Match nesnesi değiştiğinde (sayı bitince) bu listeler yeniden hesaplanmalı.
    // remember(match) ekleyerek tetiklenmesini sağlıyoruz.
    val rosterForSelection = remember(match, roster) {
        val statsMap = match.pointsArchive.flatMap { it.stats }.groupingBy { it.playerId }.eachCount()
        roster.map { player ->
            Pair(player, AdvancedPlayerStats(basicStats = PlayerStats(playerId = player.id, name = player.name, pointsPlayed = statsMap[player.id] ?: 0)))
        }
    }

    // Son oynanan seti hatırlama (Same Line butonu için)
    val lastPointLine = remember(match) {
        match.pointsArchive.lastOrNull()?.stats?.mapNotNull { stat ->
            roster.find { it.id == stat.playerId }
        } ?: emptyList()
    }
    // --- EKRAN AYARLARI ---
    DisposableEffect(Unit) {
        onToggleFullScreen(true)
        onDispose { onToggleFullScreen(false) }
    }

    // STATE
    var scoreUs by remember { mutableStateOf(match.scoreUs) }
    var scoreThem by remember { mutableStateOf(match.scoreThem) }
    var currentLine by remember { mutableStateOf<List<Player>>(emptyList()) }

    // Olay Zinciri
    val currentPointEvents = remember { mutableStateListOf<ProVisualEvent>() }

    // Saha Mantığı
    var lastDiscLocation by remember { mutableStateOf<Offset?>(null) }
    var currentThrower by remember { mutableStateOf<Player?>(null) }
    var tempTargetLocation by remember { mutableStateOf<Offset?>(null) }

    // UI Kontrol
    var showPlayerSelector by remember { mutableStateOf(false) }
    var selectorType by remember { mutableStateOf(SelectorType.NONE) }


    // --- FONKSİYONLAR ---
    fun savePoint(whoScored: String) {
        if (whoScored == "US") scoreUs++ else scoreThem++

        val proEventsData = currentPointEvents.map { viz ->
            ProEventData(
                fromX = viz.from?.x, fromY = viz.from?.y,
                toX = viz.to.x, toY = viz.to.y,
                type = viz.type,
                throwerName = roster.find { it.id == viz.throwerId }?.name,
                receiverName = roster.find { it.id == viz.receiverId }?.name,
                distanceYards = 0.0
            )
        }

        val newPoint = PointData(
            whoScored = whoScored,
            captureMode = CaptureMode.PRO,
            proEvents = proEventsData,
            stats = currentLine.map { PlayerStats(playerId = it.id, name = it.name, pointsPlayed = 1) }
        )

        val updatedMatch = match.copy(scoreUs = scoreUs, scoreThem = scoreThem, pointsArchive = match.pointsArchive + newPoint)
        onUpdateMatch(updatedMatch)

        // Sıfırla
        currentPointEvents.clear()
        currentLine = emptyList()
        lastDiscLocation = null
        currentThrower = null
        tempTargetLocation = null
        analysisState = AnalysisState.LINE_SELECTION // Tekrar seçim ekranına dön
    }

    fun handleFieldTap(tapLoc: Offset) {
        when (analysisState) {
            AnalysisState.LINE_SELECTION, AnalysisState.START_MODE_SELECTION,AnalysisState.DIRECTION_SELECTION -> { /* İşlem yok */ }
            AnalysisState.PULL_SETUP -> {
                lastDiscLocation = tapLoc
                tempTargetLocation = tapLoc
                selectorType = SelectorType.SET_PULLER
                showPlayerSelector = true
            }
            AnalysisState.PULL_LANDING -> {
                val startLoc = lastDiscLocation
                currentPointEvents.add(
                    ProVisualEvent(from = startLoc, to = tapLoc, type = "PULL", color = Color.Gray, throwerId = currentThrower?.id, receiverId = null)
                )
                analysisState = AnalysisState.DEFENSE_PHASE // Defansa geç
                currentThrower = null
                lastDiscLocation = null
            }
            AnalysisState.DEFENSE_PHASE -> {
                // Defans modunda sahaya tıklayınca şimdilik bir şey yapmıyoruz, butonları kullanıyoruz.
            }
            AnalysisState.SET_DISC_START -> {
                lastDiscLocation = tapLoc
                tempTargetLocation = tapLoc
                selectorType = SelectorType.SET_THROWER
                showPlayerSelector = true
            }
            AnalysisState.ACTION_PHASE -> {
                tempTargetLocation = tapLoc
                selectorType = SelectorType.PASS_TARGET
                showPlayerSelector = true
            }
        }
    }

    fun handlePlayerSelect(player: Player?, actionType: String) {
        showPlayerSelector = false
        val startLoc = lastDiscLocation
        val endLoc = tempTargetLocation ?: Offset(0.5f, 0.5f)

        if (selectorType == SelectorType.SET_THROWER) {
            currentThrower = player
            lastDiscLocation = endLoc
            analysisState = AnalysisState.ACTION_PHASE
            currentPointEvents.add(
                ProVisualEvent(from = null, to = endLoc, type = "PICKUP", color = Color.Gray, throwerId = null, receiverId = player?.id)
            )
        } else if (selectorType == SelectorType.PASS_TARGET) {
            when (actionType) {
                "PASS" -> {
                    if (player != null && currentThrower != null) {
                        currentPointEvents.add(
                            ProVisualEvent(from = startLoc, to = endLoc, type = "PASS", color = Color.White, throwerId = currentThrower!!.id, receiverId = player.id)
                        )
                        currentThrower = player
                        lastDiscLocation = endLoc
                    }
                }
                "GOAL" -> {
                    if (player != null && currentThrower != null) {
                        currentPointEvents.add(
                            ProVisualEvent(from = startLoc, to = endLoc, type = "GOAL", color = StitchOffense, throwerId = currentThrower!!.id, receiverId = player.id)
                        )
                        savePoint("US")
                    }
                }
                "DROP", "THROWAWAY" -> {
                    // Hata -> Turnover -> Defans Moduna Geç
                    if (currentThrower != null) {
                        currentPointEvents.add(
                            ProVisualEvent(from = startLoc, to = endLoc, type = actionType, color = StitchDefense, throwerId = currentThrower?.id, receiverId = player?.id)
                        )
                    }
                    analysisState = AnalysisState.DEFENSE_PHASE // <-- ÖNEMLİ: Defansa geçiş
                    currentThrower = null
                    lastDiscLocation = null
                }
            }
        }
    }

    // --- ARAYÜZ MANTIĞI (DEĞİŞTİ) ---

    if (analysisState == AnalysisState.LINE_SELECTION) {
        // 1. MOD: TAM EKRAN KADRO SEÇİMİ (Advanced ile aynı component)
        // Landscape modda olduğumuz için LineSelectionScreen içindeki Column'un scrollable olduğundan emin olmalıyız (zaten öyle).
        Box(modifier = Modifier.fillMaxSize().background(StitchBackground)) {
            LineSelectionScreen(
                fullRosterWithStats = rosterForSelection,
                ourTeamName = match.ourTeamName,
                theirTeamName = match.opponentName,
                scoreUs = scoreUs,
                scoreThem = scoreThem,
                initialSelectedPlayerIds = emptySet(),
                presetLines = presetLines,
                lastPointLine = emptyList(), // İstersen son oynanan line'ı buraya ekleyebilirsin
                onLineConfirmed = { selectedPairs ->
                    currentLine = selectedPairs.map { it.first }
                    // DÜZELTME: Direkt sahaya değil, Başlangıç Modu seçimine git
                    analysisState = AnalysisState.START_MODE_SELECTION
                }
            )


            // Çıkış butonu (Kadro seçiminden vazgeçip ana menüye dönmek için)
            IconButton(
                onClick = onExit,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
            ) {
                Icon(Icons.Default.ArrowBack, null)
            }
        }
        // ... (if LINE_SELECTION bloğu bittikten sonra) ...

    } else if (analysisState == AnalysisState.START_MODE_SELECTION) {
        // --- 2. ADIM: HÜCUM MU DEFANS MI? (DİKEY) ---
        Column(
            modifier = Modifier.fillMaxSize().background(StitchBackground).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Başlangıç Durumu", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = StitchColor.TextPrimary)
            Spacer(Modifier.height(40.dp))

            Button(
                onClick = {
                    isOffenseStart = true
                    analysisState = AnalysisState.DIRECTION_SELECTION // Yön seçimine git
                },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StitchOffense)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("HÜCUM (OFFENSE)", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Disk bizde başlıyor", fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    isOffenseStart = false
                    analysisState = AnalysisState.DIRECTION_SELECTION // Yön seçimine git
                },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StitchDefense)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DEFANS (PULL)", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Pulu biz atıyoruz", fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = { analysisState = AnalysisState.LINE_SELECTION }) { Text("Geri", color = Color.Gray) }
        }

    } else if (analysisState == AnalysisState.DIRECTION_SELECTION) {
        // --- 3. ADIM: HÜCUM YÖNÜ (DİKEY) ---
        Column(
            modifier = Modifier.fillMaxSize().background(StitchBackground).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Hücum Yönü", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = StitchColor.TextPrimary)
            Spacer(Modifier.height(40.dp))

            // Sol -> Sağ
            Button(
                onClick = {
                    isLeftToRight = true
                    // Seçime göre sahayı başlat
                    analysisState = if(isOffenseStart) AnalysisState.SET_DISC_START else AnalysisState.PULL_SETUP
                },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Icon(Icons.Default.ArrowForward, null)
                Spacer(Modifier.width(16.dp))
                Text("SOL -> SAĞ", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))

            // Sağ -> Sol
            Button(
                onClick = {
                    isLeftToRight = false
                    analysisState = if(isOffenseStart) AnalysisState.SET_DISC_START else AnalysisState.PULL_SETUP
                },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text("SAĞ <- SOL", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(16.dp))
                Icon(Icons.Default.ArrowBack, null)
            }
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = { analysisState = AnalysisState.START_MODE_SELECTION }) { Text("Geri", color = Color.Gray) }
        }

    } else {
        // ... (Buradan sonrası Saha/Yatay görünüm) ...
        // 2. MOD: SAHA VE KONTROL PANELİ (Yatay Row)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E1E))
        ) {
            // SOL: SAHA (%75)
            Box(
                modifier = Modifier
                    .weight(0.75f)
                    .fillMaxHeight()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2E7D32))
            ) {
                ProFieldCanvas(
                    visualEvents = currentPointEvents,
                    lastDiscPos = lastDiscLocation,
                    onTap = { handleFieldTap(it) }
                )

                Surface(color = Color.Black.copy(0.6f), modifier = Modifier.align(Alignment.TopStart).padding(8.dp), shape = RoundedCornerShape(8.dp)) {
                    Text("${match.opponentName}: $scoreThem  |  BİZ: $scoreUs", color = Color.White, modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold)
                }

                // Saha içindeki Sol Alt Köşe
                if (currentPointEvents.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            if (currentPointEvents.isNotEmpty()) {
                                // HATA VEREN KISMI BÖYLE DEĞİŞTİRİN:
                                val removed = currentPointEvents.removeAt(currentPointEvents.size - 1)

                                // ... geri kalan mantık aynı ...
                                if (removed.type == "PASS" || removed.type == "PICKUP") {
                                    currentThrower = null
                                    analysisState = AnalysisState.SET_DISC_START
                                } else if (removed.type == "PULL") {
                                    analysisState = AnalysisState.PULL_SETUP
                                } else if (removed.type == "DROP" || removed.type == "THROWAWAY") {
                                    analysisState = AnalysisState.SET_DISC_START
                                    currentThrower = null
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).background(Color.Black.copy(0.5f), CircleShape)
                    ) { Icon(Icons.Default.Undo, null, tint = Color.White) }
                }
            }

            // SAĞ: KONTROL PANELİ (%25)
            Surface(
                modifier = Modifier.weight(0.25f).fillMaxHeight(),
                color = Color(0xFF252525),
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    Text("Analiz Modu", color = Color.Gray, fontSize = 12.sp)
                    if (analysisState == AnalysisState.DEFENSE_PHASE) {
                        Spacer(Modifier.weight(1f))
                        Text("SAVUNMA MODU", color = StitchDefense, fontWeight = FontWeight.Bold)

                        Button(onClick = { analysisState = AnalysisState.SET_DISC_START }, modifier = Modifier.fillMaxWidth()) {
                            Text("BLOK / TURNOVER") // Biz aldık
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { savePoint("THEM") }, colors = ButtonDefaults.buttonColors(containerColor = StitchDefense), modifier = Modifier.fillMaxWidth()) {
                            Text("RAKİP GOL")
                        }
                    } else {
                        Text(if(analysisState == AnalysisState.SET_DISC_START) "Disk Başlangıç Noktasını Seçin" else "Pas Hedefini Seçin", color = Color.White)
                    }

                    // Sahadaki Oyuncular Listesi (Küçük Hatırlatma)
                    Divider(color = Color.Gray)
                    Text("Sahadakiler:", color = Color.Gray, fontSize = 10.sp)
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(currentLine) { p ->
                            Text(p.name, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }


                    Button(onClick = { savePoint("THEM") }, colors = ButtonDefaults.buttonColors(containerColor = StitchDefense), modifier = Modifier.fillMaxWidth()) {
                        Text("RAKİP GOL")
                    }
                    OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                        Text("Kaydet & Çık", color = Color.Gray)
                    }
                }
            }
        }
    }

    // OYUNCU SEÇİM POPUP'I
    if (showPlayerSelector && currentLine.isNotEmpty()) {
        PlayerSelectionDialog(
            players = currentLine,
            currentThrower = currentThrower,
            selectorType = selectorType,
            onSelect = { p, action -> handlePlayerSelect(p, action) },
            onDismiss = { showPlayerSelector = false }
        )
    }
}

// --- FIELD CANVAS ---
@Composable
private fun FieldCanvas(
    visualEvents: List<VisualEvent>,
    onTap: (Offset) -> Unit,
    currentThrowerPos: Offset?,
    attackDirection: AttackDirection? = null
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val fieldAspectRatio = (FIELD_WIDTH_YARDS / FIELD_LENGTH_YARDS).toFloat()
                    val screenWidth = size.width.toFloat()
                    val screenHeight = size.height.toFloat()
                    var drawWidth = screenWidth
                    var drawHeight = screenWidth / fieldAspectRatio
                    if (drawHeight > screenHeight) {
                        drawHeight = screenHeight
                        drawWidth = screenHeight * fieldAspectRatio
                    }
                    val offsetX = (screenWidth - drawWidth) / 2f
                    val offsetY = (screenHeight - drawHeight) / 2f

                    if (tapOffset.x >= offsetX && tapOffset.x <= offsetX + drawWidth &&
                        tapOffset.y >= offsetY && tapOffset.y <= offsetY + drawHeight
                    ) {
                        val normalizedX = (tapOffset.x - offsetX) / drawWidth
                        val normalizedY = (tapOffset.y - offsetY) / drawHeight
                        onTap(Offset(normalizedX, normalizedY))
                    }
                }
            }
    ) {
        val fieldAspectRatio = (FIELD_WIDTH_YARDS / FIELD_LENGTH_YARDS).toFloat()
        val screenWidth = maxWidth.value
        val screenHeight = maxHeight.value

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            var drawWidth = canvasW
            var drawHeight = canvasW / fieldAspectRatio
            if (drawHeight > canvasH) {
                drawHeight = canvasH
                drawWidth = canvasH * fieldAspectRatio
            }
            val offsetX = (canvasW - drawWidth) / 2f
            val offsetY = (canvasH - drawHeight) / 2f

            translate(left = offsetX, top = offsetY) {
                drawRect(color = Color(0xFF2E7D32), size = Size(drawWidth, drawHeight))
                val endzoneHeightPx = drawHeight * ENDZONE_TOP_RATIO.toFloat()
                drawLine(Color.White.copy(0.8f), Offset(0f, endzoneHeightPx), Offset(drawWidth, endzoneHeightPx), 2.dp.toPx())
                drawLine(Color.White.copy(0.8f), Offset(0f, drawHeight - endzoneHeightPx), Offset(drawWidth, drawHeight - endzoneHeightPx), 2.dp.toPx())
                val playingFieldHeight = drawHeight - (2 * endzoneHeightPx)
                val tenYardHeight = playingFieldHeight / 7f
                for (i in 1..6) {
                    val y = endzoneHeightPx + (i * tenYardHeight)
                    drawLine(Color.White.copy(0.2f), Offset(0f, y), Offset(drawWidth, y), 1.dp.toPx())
                }
                val brickTopY = endzoneHeightPx + (2 * tenYardHeight)
                val brickBottomY = drawHeight - endzoneHeightPx - (2 * tenYardHeight)
                val centerX = drawWidth / 2
                drawLine(Color.White.copy(0.5f), Offset(centerX-10f, brickTopY-10f), Offset(centerX+10f, brickTopY+10f), 2.dp.toPx())
                drawLine(Color.White.copy(0.5f), Offset(centerX+10f, brickTopY-10f), Offset(centerX-10f, brickTopY+10f), 2.dp.toPx())
                drawLine(Color.White.copy(0.5f), Offset(centerX-10f, brickBottomY-10f), Offset(centerX+10f, brickBottomY+10f), 2.dp.toPx())
                drawLine(Color.White.copy(0.5f), Offset(centerX+10f, brickBottomY-10f), Offset(centerX-10f, brickBottomY+10f), 2.dp.toPx())

                if (attackDirection != null) {
                    val arrowColor = Color.White.copy(0.15f)
                    val arrowStart = if(attackDirection == AttackDirection.UP)
                        Offset(centerX, drawHeight * 0.7f) else Offset(centerX, drawHeight * 0.3f)
                    val arrowEnd = if(attackDirection == AttackDirection.UP)
                        Offset(centerX, drawHeight * 0.3f) else Offset(centerX, drawHeight * 0.7f)
                    drawLine(arrowColor, arrowStart, arrowEnd, 20.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    if(attackDirection == AttackDirection.UP) {
                        drawLine(arrowColor, arrowEnd, Offset(centerX - 40f, arrowEnd.y + 40f), 20.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        drawLine(arrowColor, arrowEnd, Offset(centerX + 40f, arrowEnd.y + 40f), 20.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    } else {
                        drawLine(arrowColor, arrowEnd, Offset(centerX - 40f, arrowEnd.y - 40f), 20.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        drawLine(arrowColor, arrowEnd, Offset(centerX + 40f, arrowEnd.y - 40f), 20.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    }
                }

                visualEvents.forEach { event ->
                    val start = event.from?.let { Offset(it.x * drawWidth, it.y * drawHeight) }
                    val end = Offset(event.to.x * drawWidth, event.to.y * drawHeight)
                    if (start != null) {
                        val isError = event.type == "DROP" || event.type == "THROWAWAY"
                        drawLine(
                            color = event.color,
                            start = start,
                            end = end,
                            strokeWidth = if (isError) 2.dp.toPx() else 3.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            pathEffect = if (isError) PathEffect.dashPathEffect(floatArrayOf(15f, 15f)) else null
                        )
                    }
                    val radius = if (event.isGoal) 6.dp.toPx() else 4.dp.toPx()
                    drawCircle(event.color, radius, end)
                }
                if (currentThrowerPos != null) {
                    drawCircle(Color(0xFFFFD700), 8.dp.toPx(), Offset(currentThrowerPos.x * drawWidth, currentThrowerPos.y * drawHeight))
                    drawCircle(Color.Black, 8.dp.toPx(), Offset(currentThrowerPos.x * drawWidth, currentThrowerPos.y * drawHeight), style = Stroke(width = 1.dp.toPx()))
                }
            }
        }
    }
}

// --- POPUP GÜNCELLEMESİ ---
@Composable
fun PlayerSelectionPopup(
    players: List<Player>,
    mode: SelectionMode,
    isPotentialGoal: Boolean = false,
    onSelect: (Player?, String) -> Unit
) {
    var showDropSelection by remember { mutableStateOf(false) }
    var showTurnoverSelection by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = {}) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = StitchColor.Surface)) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (showDropSelection) {
                    Text("Kim Düşürdü? (Drop)", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))
                    LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items = players) { player ->
                            Card(onClick = { onSelect(player, "DROP") }, colors = CardDefaults.cardColors(containerColor = StitchDefense.copy(alpha = 0.1f)), modifier = Modifier.height(70.dp)) {
                                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(player.jerseyNumber?.toString() ?: "", fontWeight = FontWeight.Bold, color = StitchDefense)
                                    Text(player.name.split(" ").first(), fontSize = 12.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showDropSelection = false }) { Text("Geri", color = Color.Gray) }
                } else if (showTurnoverSelection) {
                    Text("Hata Türü Seçin", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))
                    Button(onClick = { onSelect(null, "THROWAWAY") }, colors = ButtonDefaults.buttonColors(containerColor = StitchDefense), modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(12.dp)) { Text("THROWAWAY (Atıcı Hatası)", fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { showDropSelection = true }, colors = ButtonDefaults.buttonColors(containerColor = StitchDefense), modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(12.dp)) { Text("DROP (Alıcı Hatası)", fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showTurnoverSelection = false }) { Text("Geri", color = Color.Gray) }
                } else {
                    val title = if (isPotentialGoal) "GOL MÜ? (Kim Yakaladı?)" else if (mode == SelectionMode.ACTION) "Pas Hedefi" else "Oyuncu Seç"
                    val titleColor = if (isPotentialGoal) StitchOffense else StitchColor.TextPrimary
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp), color = titleColor)
                    if (mode == SelectionMode.ACTION) {
                        Button(onClick = { showTurnoverSelection = true }, colors = ButtonDefaults.buttonColors(containerColor = StitchDefense), modifier = Modifier.fillMaxWidth()) { Text("HATA (TURNOVER)") }
                        Spacer(Modifier.height(8.dp))
                    }
                    LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items = players) { player ->
                            Card(onClick = { if (mode == SelectionMode.ACTION) { if (isPotentialGoal) onSelect(player, "GOAL") else onSelect(player, "CATCH") } else { onSelect(player, "SELECT") } }, colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)), modifier = Modifier.height(70.dp)) {
                                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(player.jerseyNumber?.toString() ?: "", fontWeight = FontWeight.Bold, color = StitchPrimary)
                                    Text(player.name.split(" ").first(), fontSize = 12.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- DİĞER STANDART FONKSİYONLAR ---
@Composable
private fun StartModeDialog(onDefense: () -> Unit, onStartSelected: (mode: String, direction: AttackDirection) -> Unit) {
    Dialog(onDismissRequest = {}) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = StitchColor.Surface)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Başlangıç Durumu", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = StitchColor.TextPrimary)
                Spacer(Modifier.height(16.dp))
                Text("YUKARI HÜCUM (↑)", fontWeight = FontWeight.Bold, color = StitchColor.Primary)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onStartSelected("OFFENSE", AttackDirection.UP) }, colors = ButtonDefaults.buttonColors(containerColor = StitchOffense), modifier = Modifier.weight(1f)) { Text("Hücum (Biz)") }
                    Button(onClick = { onStartSelected("DEFENSE", AttackDirection.UP) }, colors = ButtonDefaults.buttonColors(containerColor = StitchDefense), modifier = Modifier.weight(1f)) { Text("Defans (Rakip)") }
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                Text("AŞAĞI HÜCUM (↓)", fontWeight = FontWeight.Bold, color = StitchColor.Primary)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onStartSelected("OFFENSE", AttackDirection.DOWN) }, colors = ButtonDefaults.buttonColors(containerColor = StitchOffense), modifier = Modifier.weight(1f)) { Text("Hücum (Biz)") }
                    Button(onClick = { onStartSelected("DEFENSE", AttackDirection.DOWN) }, colors = ButtonDefaults.buttonColors(containerColor = StitchDefense), modifier = Modifier.weight(1f)) { Text("Defans (Rakip)") }
                }
            }
        }
    }
}

@Composable
fun InstructionOverlay(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.background(Color.Black.copy(0.6f), RoundedCornerShape(8.dp)).padding(16.dp))
    }
}

@Composable
fun DefenseActionOverlay(onOpponentScore: () -> Unit, onTurnover: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onOpponentScore, colors = ButtonDefaults.buttonColors(containerColor = StitchDefense), modifier = Modifier.weight(1f).height(60.dp), shape = RoundedCornerShape(12.dp)) { Text("RAKİP GOL", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            Button(onClick = onTurnover, colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary), modifier = Modifier.weight(1f).height(60.dp), shape = RoundedCornerShape(12.dp)) { Text("RAKİP HATA\n(TURNOVER)", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProLineSelectionScreen(
    fullRosterWithStats: List<Pair<Player, AdvancedPlayerStats>>,
    ourTeamName: String,
    theirTeamName: String,
    scoreUs: Int,
    scoreThem: Int,
    onLineConfirmed: (List<Pair<Player, AdvancedPlayerStats>>) -> Unit,
    presetLines: List<PresetLine>,
    lastPointLine: List<String>
) {
    var selectedPlayerIds by remember { mutableStateOf(emptySet<String>()) }
    var activeFilterIds by remember { mutableStateOf<Set<String>?>(null) }
    var isQuickSetExpanded by remember { mutableStateOf(false) }
    val displayedRoster = remember(fullRosterWithStats, activeFilterIds) {
        val list = if (activeFilterIds != null) fullRosterWithStats.filter { activeFilterIds!!.contains(it.first.id) } else fullRosterWithStats
        list.sortedWith(compareBy<Pair<Player, AdvancedPlayerStats>> { it.first.position != "Handler" && it.first.position != "Hybrid" }.thenByDescending { it.second.basicStats.pointsPlayed })
    }
    val handlers = displayedRoster.filter { it.first.position == "Handler" || it.first.position == "Hybrid" }
    val cutters = displayedRoster.filter { it.first.position == "Cutter" }
    Column(Modifier.fillMaxSize().background(StitchBackground).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("$scoreUs - $scoreThem", fontSize = 24.sp, fontWeight = FontWeight.Black, color = StitchColor.TextPrimary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Kadro (${selectedPlayerIds.size}/7)", fontWeight = FontWeight.Bold, color = if(selectedPlayerIds.size == 7) StitchOffense else Color.Gray)
                if (selectedPlayerIds.isNotEmpty()) { Spacer(Modifier.width(8.dp)); TextButton(onClick = { selectedPlayerIds = emptySet(); activeFilterIds = null }) { Text("Temizle", color = StitchDefense) } }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (presetLines.isNotEmpty() || lastPointLine.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth().animateContentSize().clickable { isQuickSetExpanded = !isQuickSetExpanded }, colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Hızlı Set Yükle", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0)); Icon(if (isQuickSetExpanded) Icons.Default.ExpandLess else Icons.Default.ArrowDropDown, null, tint = Color(0xFF1565C0))
                    }
                    if (isQuickSetExpanded) {
                        Spacer(Modifier.height(12.dp))
                        if (lastPointLine.isNotEmpty()) {
                            val lastSet = lastPointLine.toSet()
                            val isActive = selectedPlayerIds.containsAll(lastSet) && selectedPlayerIds.size == lastSet.size
                            QuickSetChip(line = PresetLine(name = "AYNI KADRO (${lastPointLine.size})", playerIds = lastPointLine), isFilter = false, isActive = isActive) { if (isActive) selectedPlayerIds = emptySet() else selectedPlayerIds = lastSet }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            presetLines.forEach { line ->
                                val lineSet = line.playerIds.toSet()
                                val isActive = if(line.playerIds.size > 7) activeFilterIds == lineSet else selectedPlayerIds.containsAll(lineSet)
                                QuickSetChip(line, isFilter = line.playerIds.size > 7, isActive = isActive) {
                                    if(line.playerIds.size > 7) { activeFilterIds = if(isActive) null else lineSet; selectedPlayerIds = emptySet() } else { if(isActive) selectedPlayerIds -= lineSet else if(selectedPlayerIds.size + lineSet.size <= 7) selectedPlayerIds += lineSet }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if(handlers.isNotEmpty()) { item { SectionHeader("HANDLERS") }; items(items = handlers) { p -> RosterSelectionRow(p, selectedPlayerIds.contains(p.first.id), selectedPlayerIds.size>=7) { if(selectedPlayerIds.contains(it.first.id)) selectedPlayerIds-=it.first.id else if(selectedPlayerIds.size<7) selectedPlayerIds+=it.first.id } } }
            if(cutters.isNotEmpty()) { item { Spacer(Modifier.height(12.dp)); SectionHeader("CUTTERS") }; items(items = cutters) { p -> RosterSelectionRow(p, selectedPlayerIds.contains(p.first.id), selectedPlayerIds.size>=7) { if(selectedPlayerIds.contains(it.first.id)) selectedPlayerIds-=it.first.id else if(selectedPlayerIds.size<7) selectedPlayerIds+=it.first.id } } }
        }
        Button(onClick = { onLineConfirmed(fullRosterWithStats.filter { selectedPlayerIds.contains(it.first.id) }) }, enabled = selectedPlayerIds.size == 7, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = StitchColor.Primary)) {
            Text(if (selectedPlayerIds.size == 7) "MAÇI BAŞLAT" else "${7 - selectedPlayerIds.size} KİŞİ DAHA SEÇ", fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProMatchSetupScreen(tournament: Tournament, onBack: () -> Unit, onStartMatch: (String) -> Unit) {
    var opponentName by remember { mutableStateOf("") }
    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Yeni Maç") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(value = opponentName, onValueChange = { opponentName = it }, label = { Text("Rakip Adı") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { if (opponentName.isNotBlank()) onStartMatch(opponentName) }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("BAŞLAT") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProTournamentDetailScreen(tournament: Tournament, onBack: () -> Unit, onNewMatch: () -> Unit, onMatchClick: (String) -> Unit, onDeleteMatch: (String) -> Unit) {
    val proMatches = tournament.matches.filter { it.isProMode }
    var matchToDelete by remember { mutableStateOf<Match?>(null) }
    if (matchToDelete != null) {
        AlertDialog(onDismissRequest = { matchToDelete = null }, title = { Text("Maçı Sil") }, text = { Text("Bu maçı silmek istediğinize emin misiniz?") }, confirmButton = { Button(onClick = { onDeleteMatch(matchToDelete!!.id); matchToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Sil") } }, dismissButton = { TextButton(onClick = { matchToDelete = null }) { Text("İptal") } })
    }
    Scaffold(containerColor = StitchColor.Background, topBar = { CenterAlignedTopAppBar(title = { Text(tournament.tournamentName, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") } }) }, floatingActionButton = { ExtendedFloatingActionButton(onClick = onNewMatch, containerColor = StitchPrimary, contentColor = Color.White, icon = { Icon(Icons.Default.Add, null) }, text = { Text("YENİ MAÇ") }) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (proMatches.isEmpty()) item { Text("Henüz maç yok.", color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
            items(items = proMatches) { match ->
                Card(onClick = { onMatchClick(match.id) }, colors = CardDefaults.cardColors(containerColor = StitchColor.Surface)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text( "${match.ourTeamName} vs ${match.opponentName}", fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("${match.scoreUs} - ${match.scoreThem}", color = StitchPrimary, fontWeight = FontWeight.Bold) }
                        IconButton(onClick = { matchToDelete = match }) { Icon(Icons.Default.Delete, "Sil", tint = Color.LightGray) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProTournamentListScreen(tournaments: List<Tournament>, onTournamentSelected: (String) -> Unit) {
    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Pro Mod") }) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp)) {
            items(items = tournaments) { t -> Card(Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onTournamentSelected(t.id) }) { Row(Modifier.padding(16.dp)) { Text(t.tournamentName, fontWeight = FontWeight.Bold) } } }
        }
    }
}
// --- YENİ YARDIMCI FONKSİYON ---
// Verilen oyuncu ID'sine göre viewModel'daki tüm takımları tarayarak Player nesnesini bulur.

private fun getPlayerByName(name: String?, viewModel: MainViewModel): Player? {
    if (name.isNullOrBlank() || name == "Anonymous") return null
    return viewModel.players.value.find { it.name.equals(name, ignoreCase = true) }
}
@Composable
private fun ProFieldCanvas(
    visualEvents: List<ProVisualEvent>,
    lastDiscPos: Offset?,
    onTap: (Offset) -> Unit
) {
    Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
        detectTapGestures { tapOffset ->
            val w = size.width.toFloat(); val h = size.height.toFloat()
            onTap(Offset((tapOffset.x / w).coerceIn(0f, 1f), (tapOffset.y / h).coerceIn(0f, 1f)))
        }
    }) {
        val w = size.width; val h = size.height
        // Endzonelar (%18 genişlik)
        val ezW = w * 0.18f
        drawRect(Color.White.copy(0.1f), topLeft = Offset(0f, 0f), size = Size(ezW, h))
        drawRect(Color.White.copy(0.1f), topLeft = Offset(w - ezW, 0f), size = Size(ezW, h))
        drawLine(Color.White, Offset(ezW, 0f), Offset(ezW, h), 2.dp.toPx())
        drawLine(Color.White, Offset(w - ezW, 0f), Offset(w - ezW, h), 2.dp.toPx())

        // Olaylar
        visualEvents.forEach { event ->
            val end = Offset(event.to.x * w, event.to.y * h)
            if (event.from != null) {
                val start = Offset(event.from.x * w, event.from.y * h)
                val isError = event.type == "THROWAWAY" || event.type == "DROP"
                drawLine(
                    color = event.color, start = start, end = end, strokeWidth = 3.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    pathEffect = if (isError) PathEffect.dashPathEffect(floatArrayOf(20f, 20f)) else null
                )
            }
            drawCircle(event.color, 6.dp.toPx(), end)
        }
        // Aktif Disk
        if (lastDiscPos != null) {
            drawCircle(Color(0xFFFFD700), 8.dp.toPx(), Offset(lastDiscPos.x * w, lastDiscPos.y * h))
        }
    }
}

@Composable
private fun PlayerSelectionDialog(
    players: List<Player>,
    currentThrower: Player?,
    selectorType: SelectorType,
    onSelect: (Player?, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedAction by remember { mutableStateOf("PASS") } // Varsayılan

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF252525))) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                val title = if(selectorType == SelectorType.SET_PULLER) "Pull Atan Kim?" else if(selectorType == SelectorType.SET_THROWER) "Diski Kim Aldı?" else "Pas Kime?"
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

                // Pas Hedefi seçiyorsak AKSİYON TİPİNİ Soralım
                if (selectorType == SelectorType.PASS_TARGET) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        FilterChip(selected = selectedAction == "PASS", onClick = { selectedAction = "PASS" }, label = { Text("Pas") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color.Gray))
                        FilterChip(selected = selectedAction == "GOAL", onClick = { selectedAction = "GOAL" }, label = { Text("Gol") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = StitchOffense))
                        FilterChip(selected = selectedAction == "DROP", onClick = { selectedAction = "DROP" }, label = { Text("Drop") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = StitchDefense))
                    }
                }

                // Oyuncu Listesi
                val targets = if (selectorType == SelectorType.PASS_TARGET) players.filter { it.id != currentThrower?.id } else players
                LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(targets) { player ->
                        Card(
                            onClick = {
                                val action = if(selectorType == SelectorType.PASS_TARGET) selectedAction else "PICKUP"
                                onSelect(player, action)
                            },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF444444))
                        ) {
                            Column(Modifier.padding(8.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(player.jerseyNumber?.toString() ?: "#", color = StitchPrimary, fontWeight = FontWeight.Bold)
                                Text(player.name.split(" ").first(), color = Color.White, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
