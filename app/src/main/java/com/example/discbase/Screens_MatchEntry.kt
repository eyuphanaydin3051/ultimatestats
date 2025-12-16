package com.example.discbase

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.discbase.ui.theme.*
import java.util.UUID
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.OutlinedTextField

// --- 1. MAÇ KURULUM (İSİM GİRME) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchSetupScreen(
    navController: NavController,
    ourTeamName: String,
    onMatchStarted: (opponentName: String) -> Unit
) {
    var opponentName by remember { mutableStateOf("") }
    val isReady = opponentName.isNotBlank()

    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Yeni Maç", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    ModernIconButton(Icons.Default.ArrowBack, { navController.popBackStack() }, StitchTextPrimary, "Geri")
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // VS Kartı
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(ourTeamName.uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = com.example.discbase.ui.theme.StitchPrimary)
                    Spacer(Modifier.height(16.dp))

                    // VS İkonu
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(StitchBackground, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("VS", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                    }

                    Spacer(Modifier.height(16.dp))

                    // Rakip Girişi
                    OutlinedTextField(
                        value = opponentName,
                        onValueChange = { opponentName = it },
                        label = { Text("Rakip Takım") },
                        placeholder = { Text("Örn: ODTUPUS") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = com.example.discbase.ui.theme.StitchPrimary,
                            focusedLabelColor = com.example.discbase.ui.theme.StitchPrimary
                        ),
                        singleLine = true
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // --- GÜNCELLENEN MAÇI BAŞLAT BUTONU ---
            Button(
                onClick = { onMatchStarted(opponentName) },
                enabled = isReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(
                        elevation = if (isReady) 8.dp else 0.dp,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StitchColor.Primary, // Aktif Renk (Mor)
                    contentColor = Color.White,
                    // --- PASİF DURUM AYARLARI ---
                    disabledContainerColor = Color(0xFFE0E0E0), // Açık Gri
                    disabledContentColor = Color.Gray // Koyu Gri Yazı
                )
            ) {
                Text(
                    text = if (isReady) "MAÇI BAŞLAT" else "RAKİP İSMİ GİRİNİZ",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isReady) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Sports, contentDescription = null)
                }
            }
        }
    }
}
// --- 2. MAÇ OYNATMA HOST (CANLI MAÇ YÖNETİCİSİ) ---
@Composable
fun MatchPlaybackHost(
    mainViewModel: MainViewModel, // <-- DEĞİŞİKLİK 1: Parametre olarak eklendi
    ourTeamName: String,
    opponentName: String,
    fullRoster: List<Player>,
    matchToEdit: Match?,
    presetLines: List<PresetLine>,
    onMatchFinished: (Match) -> Unit,
    onMatchReset: () -> Unit,
    onMatchUpdated: (Match) -> Unit
) {
    // --- STATE TANIMLARI ---
    var currentLine by remember { mutableStateOf<List<Player>>(emptyList()) }
    var gameMode by remember { mutableStateOf(GameMode.IDLE) }
    var currentPointStats by remember { mutableStateOf<List<PlayerStats>>(emptyList()) }
    var pointsArchive by remember { mutableStateOf(matchToEdit?.pointsArchive ?: emptyList()) }
    var previousPointsDuration by remember {
        mutableStateOf(matchToEdit?.pointsArchive?.sumOf { it.durationSeconds } ?: 0L)
    }

    var currentPointSeconds by remember { mutableStateOf(0L) }

    val totalMatchTimeSeconds = previousPointsDuration + currentPointSeconds

    var isTimerRunning by remember { mutableStateOf(false) }



    val lastPointLineIds = remember(pointsArchive) {
        pointsArchive.lastOrNull()?.stats?.map { it.playerId } ?: emptyList()
    }
    var matchTimerSeconds by remember { mutableStateOf(matchToEdit?.matchDurationSeconds ?: 0L) }
    var lastPointTimeSeconds by remember { mutableStateOf(0L) }
    var pointStartTimeSeconds by remember { mutableStateOf(matchTimerSeconds) }

    // Anlık Skor ve Pasör
    var activePasserId by remember { mutableStateOf<String?>(null) }
    var scoreUs by remember { mutableStateOf(matchToEdit?.scoreUs ?: 0) }
    var scoreThem by remember { mutableStateOf(matchToEdit?.scoreThem ?: 0) }

    // Tarihçe ve Sakatlık
    var currentPointStartMode by remember { mutableStateOf<PointStartMode?>(null) }
    var pointHistoryStack by remember { mutableStateOf<List<PointStateSnapshot>>(emptyList()) }
    var subbedOutStats by remember { mutableStateOf<List<PlayerStats>>(emptyList()) }
    var possessionStartTime by remember { mutableStateOf<Long?>(null) }
    var showInjuryDialog by remember { mutableStateOf(false) }

    // Pull için geçici
    var tempPullerId by remember { mutableStateOf<String?>(null) }

    // DEĞİŞİKLİK 2: ViewModel'i içeride tekrar OLUŞTURMUYORUZ. Parametreyi kullanıyoruz.
    val isLeftHanded by mainViewModel.isLeftHanded.collectAsState()
    val nameFormat by mainViewModel.nameFormat.collectAsState()

    // --- BU SATIRI EKLEYİN: Ayarlardaki Modu Çekiyoruz ---
    val captureModeSettings by mainViewModel.captureMode.collectAsState()
    var playerEntryTimes by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    // Kariyer verilerini hesapla
    val rosterWithMatchStats = remember(pointsArchive, fullRoster) {
        fullRoster.map { player ->
            val stats = calculateStatsFromPoints(player.id, pointsArchive)
            Pair(player, stats)
        }.sortedByDescending { it.second.basicStats.pointsPlayed } // Önce puana göre
            .sortedBy {
                // Sonra pozisyona göre (Handler/Hybrid başa gelsin diye false/true mantığı)
                it.first.position != "Handler" && it.first.position != "Hybrid"
            }
    }

    val activePasserName = currentPointStats.find { it.playerId == activePasserId }?.name

    // --- TARİHÇE YÖNETİMİ ---
    fun saveCurrentStateToHistory() {
        pointHistoryStack = pointHistoryStack + PointStateSnapshot(
            pointStats = currentPointStats,
            activePasserId = activePasserId,
            gameMode = gameMode,
            subbedOutStats = subbedOutStats
        )
    }
    // --- ZAMANLAMA STATE'LERİ ---
    // Maç süresi (Saniye cinsinden)

    // Zamanlayıcı çalışıyor mu? (Varsayılan false, maç başlayınca true olacak)

    val isTimeTrackingEnabled by mainViewModel.timeTrackingEnabled.collectAsState()
    val keepScreenOn by mainViewModel.keepScreenOn.collectAsState()
    KeepScreenOn(keepScreenOn)
    // Pull Zamanlaması için geçici değişken
    var pullStartTimeMs by remember { mutableStateOf<Long?>(null) }
    var currentPointPullDuration by remember { mutableStateOf(0.0) }
    // --- DURAKSAMA STATE'LERİ ---
    var activeStoppageType by remember { mutableStateOf<StoppageType?>(null) }
    var stoppageStartTimeMs by remember { mutableStateOf<Long?>(null) }
    var currentStoppageSeconds by remember { mutableStateOf(0L) }
    // O anki sayıya ait birikmiş duraksamalar
    var currentPointStoppages by remember { mutableStateOf<List<Stoppage>>(emptyList()) }
    // --- KRONOMETRE MOTORU ---
    // isTimerRunning true olduğu sürece her saniye çalışır
    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning) {
            kotlinx.coroutines.delay(1000L)
            // Sadece şu anki sayının süresini artırıyoruz
            currentPointSeconds++

            // --- EKSİK OLAN SATIR BURASI (BUNU EKLEYİN) ---
            // 2. Ana Maç Sayacını artır (Oyuncu sürelerini hesaplayan gizli saat)
            matchTimerSeconds++
        }
    }
    LaunchedEffect(activeStoppageType) {
        if (activeStoppageType != null) {
            val startTime = System.currentTimeMillis()
            while (activeStoppageType != null) {
                val now = System.currentTimeMillis()
                currentStoppageSeconds = (now - startTime) / 1000
                kotlinx.coroutines.delay(1000L)
            }
        }
    }
    fun startStoppage(type: StoppageType) {
        saveCurrentStateToHistory()
        activeStoppageType = type
        stoppageStartTimeMs = System.currentTimeMillis()
        currentStoppageSeconds = 0
        isTimerRunning = false
        // İsterseniz burada maç saatini durdurabilirsiniz: isTimerRunning = false
        // Ama Ultimate'de genelde maç saati molada da akar (Soft cap hariç).
        // Şimdilik maç saatine dokunmuyoruz.
    }

    fun endStoppage() {
        val type = activeStoppageType ?: return

        // Kayıt oluştur
        val newStoppage = Stoppage(
            type = type,
            durationSeconds = currentStoppageSeconds,
            startTimeSeconds = matchTimerSeconds // Maçın o anki saniyesi
        )

        currentPointStoppages = currentPointStoppages + newStoppage

        // Sıfırla
        activeStoppageType = null
        stoppageStartTimeMs = null
        currentStoppageSeconds = 0
    }
    fun recordTempoForPasser() {
        val passerId = activePasserId ?: return
        val startTime = possessionStartTime ?: return

        // Eğer süre takibi kapalıysa veya sayaç çalışmıyorsa hesaplama yapma
        if (!isTimeTrackingEnabled) return

        val duration = if (matchTimerSeconds >= startTime) matchTimerSeconds - startTime else 0

        // Pasörün istatistiğine süreyi ekle
        currentPointStats = currentPointStats.map {
            if (it.playerId == passerId) {
                it.copy(totalTempoSeconds = it.totalTempoSeconds + duration)
            } else it
        }

        // Süreyi sıfırla (Bir sonraki pasör için null yap, catch olunca tekrar dolacak)
        possessionStartTime = null
    }
    fun handleResetMatch() {
        currentLine = emptyList()
        currentPointStats = emptyList()
        subbedOutStats = emptyList()
        activePasserId = null
        gameMode = GameMode.IDLE
        currentPointStartMode = null
        pointHistoryStack = emptyList()
        onMatchReset()
    }

    // --- AKSİYON FONKSİYONLARI (Kısaltıldı, mantık aynı) ---
    fun handleUndoLastPoint() {
        if (gameMode == GameMode.SIMPLE_ENTRY) return
        if (pointHistoryStack.isEmpty()) {
            handleResetMatch()
            return
        }
        val previousState = pointHistoryStack.last()
        currentPointStats = previousState.pointStats
        activePasserId = previousState.activePasserId
        gameMode = previousState.gameMode
        subbedOutStats = previousState.subbedOutStats
        pointHistoryStack = pointHistoryStack.dropLast(1)
    }



    fun handleInjurySub(playerOutId: String, playerInId: String) {
        saveCurrentStateToHistory()

        // --- ÇIKAN OYUNCUNUN SÜRESİNİ HESAPLA ---
        if (isTimeTrackingEnabled) {
            val entryTime = playerEntryTimes[playerOutId] ?: matchTimerSeconds
            val duration = matchTimerSeconds - entryTime

            // Çıkan oyuncunun stats'ına süreyi ekle
            currentPointStats = currentPointStats.map {
                if (it.playerId == playerOutId) it.copy(secondsPlayed = it.secondsPlayed + duration)
                else it
            }

            // Çıkan oyuncuyu giriş listesinden sil, gireni ekle
            val newEntryTimes = playerEntryTimes.toMutableMap()
            newEntryTimes.remove(playerOutId)
            newEntryTimes[playerInId] = matchTimerSeconds // Giren şimdi girdi
            playerEntryTimes = newEntryTimes
        }
        // --------------------------------------
        val playerOutStats = currentPointStats.find { it.playerId == playerOutId } ?: return
        subbedOutStats = subbedOutStats + playerOutStats
        val playerIn = fullRoster.find { it.id == playerInId } ?: return
        val playerInStats = PlayerStats(playerId = playerIn.id, name = playerIn.name, pointsPlayed = 1)
        currentPointStats = currentPointStats.filter { it.playerId != playerOutId } + playerInStats
        if (activePasserId == playerOutId) activePasserId = playerInId
        showInjuryDialog = false
    }

    fun handleLineSelected(line: List<Pair<Player, AdvancedPlayerStats>>) {
        val playerList = line.map { it.first }
        currentLine = playerList

        // İstatistikleri oluştur
        currentPointStats = playerList.map {
            PlayerStats(playerId = it.id, name = it.name, pointsPlayed = 1)
        }
        pointStartTimeSeconds = matchTimerSeconds

        // --- SÜRE TAKİBİ BAŞLAT ---
        // Sahadaki herkes şu an (matchTimerSeconds) oyuna girdi sayılır
        if (isTimeTrackingEnabled) {
            playerEntryTimes = playerList.associate { it.id to matchTimerSeconds }
        }
        // --------------------------
        currentPointSeconds = 0

        gameMode = GameMode.IDLE
        pointHistoryStack = emptyList()
        subbedOutStats = emptyList()
    }

    fun handleGameModeSelected(mode: GameMode) {
        saveCurrentStateToHistory()

        // 1. Başlangıç Modunu Kaydet (Hücum mu Defans mı?)
        if (mode == GameMode.OFFENSE) currentPointStartMode = PointStartMode.OFFENSE
        else if (mode == GameMode.DEFENSE_PULL) currentPointStartMode = PointStartMode.DEFENSE

        // 2. AYARLARA GÖRE YÖNLENDİR
        if (captureModeSettings == CaptureMode.SIMPLE) {
            // Ayarlar BASİT ise -> Direkt Basit Ekrana git, soru sorma
            gameMode = GameMode.SIMPLE_ENTRY
        } else {
            // Ayarlar GELİŞMİŞ ise -> Direkt Detaylı Ekrana git
            if (currentPointStartMode == PointStartMode.OFFENSE) {
                gameMode = GameMode.OFFENSE
                activePasserId = null
            } else {
                gameMode = GameMode.DEFENSE_PULL // Pull atan oyuncuyu seçme ekranı
            }
        }
    }


    fun handleStatModeSelected(isDetailed: Boolean) {
        saveCurrentStateToHistory()
        if (isDetailed) {
            if (currentPointStartMode == PointStartMode.OFFENSE) { gameMode = GameMode.OFFENSE; activePasserId = null }
            else { gameMode = GameMode.DEFENSE_PULL }
        } else { gameMode = GameMode.SIMPLE_ENTRY }
    }

    fun handlePull(playerId: String) {
        saveCurrentStateToHistory()
        tempPullerId = playerId

        // Pull Zamanlayıcısını Başlat
        pullStartTimeMs = System.currentTimeMillis()
        gameMode = GameMode.DEFENSE_PULL_RESULT
    }
    fun handlePullResult(isSuccess: Boolean) {
        val pullerId = tempPullerId ?: return
        saveCurrentStateToHistory()

        // --- YENİ: Süreyi Hesapla ---
        val durationSeconds: Double
        if (pullStartTimeMs != null) {
            val durationMs = System.currentTimeMillis() - pullStartTimeMs!!
            durationSeconds = durationMs / 1000.0
            currentPointPullDuration = durationSeconds
            pullStartTimeMs = null
        } else {
            durationSeconds = 0.0
            currentPointPullDuration = 0.0
        }
        // ----------------------------

        currentPointStats = currentPointStats.map {
            if (it.playerId == pullerId) {
                it.copy(
                    pullAttempts = 1,
                    successfulPulls = if (isSuccess) 1 else 0,
                    // --- YENİ: Oyuncuya Süreyi İşle ---
                    totalPullTimeSeconds = durationSeconds
                    // ----------------------------------
                )
            } else {
                it.copy(pullAttempts = 0, successfulPulls = 0, totalPullTimeSeconds = 0.0)
            }
        }
        gameMode = GameMode.DEFENSE
        tempPullerId = null
    }
    fun handleBlock(playerId: String) {
        saveCurrentStateToHistory()
        currentPointStats = currentPointStats.map { if (it.playerId == playerId) it.copy(block = it.block + 1) else it }
        gameMode = GameMode.OFFENSE; activePasserId = null
    }
    fun handleOpponentTurnover() { saveCurrentStateToHistory(); gameMode = GameMode.OFFENSE; activePasserId = null }
    fun selectPasser(playerId: String) {
        saveCurrentStateToHistory()
        activePasserId = playerId
        // Pasör seçildiği an süreyi başlat
        possessionStartTime = matchTimerSeconds // <-- EKLENDİ
    }

    fun endPoint(finalStats: List<PlayerStats>, whoScored: String) {
        // 1. SÜREYİ OTOMATİK DURDUR (İsteğin - Madde 1)
        isTimerRunning = false // Süreyi durdur

        // 2. SAYI SÜRESİNİ HESAPLA
        val pointDuration = if (matchTimerSeconds >= pointStartTimeSeconds)
            matchTimerSeconds - pointStartTimeSeconds
        else 0L
        var processedStats = finalStats

        // --- SAHADAKİLERİN SÜRESİNİ EKLE ---
        if (isTimeTrackingEnabled) {
            processedStats = finalStats.map { playerStat ->
                val entryTime = playerEntryTimes[playerStat.playerId]
                if (entryTime != null) {
                    val duration = matchTimerSeconds - entryTime
                    playerStat.copy(secondsPlayed = playerStat.secondsPlayed + duration)
                } else {
                    playerStat
                }
            }
        }
        val thisPointDuration = currentPointSeconds
        previousPointsDuration += thisPointDuration
        val allStats = processedStats + subbedOutStats
        pointsArchive = pointsArchive + PointData(
            stats = allStats,
            whoScored = whoScored,
            startMode = currentPointStartMode,
            captureMode = captureModeSettings,
            pullDurationSeconds = currentPointPullDuration, // <-- Pull süresini kaydet
            durationSeconds = thisPointDuration,

            stoppages = currentPointStoppages
        )
        // Sayı bitince Pull süresini sıfırla
        currentPointSeconds = 0
        currentPointPullDuration = 0.0
        currentPointStoppages = emptyList()
        handleResetMatch()
        playerEntryTimes = emptyMap()
    }

    fun handleCatch(receiverId: String) {
        recordTempoForPasser()
        val passerId = activePasserId ?: return
        saveCurrentStateToHistory()
        currentPointStats = currentPointStats.map { p ->
            when (p.playerId) {
                receiverId -> p.copy(catchStat = p.catchStat + 1)
                passerId -> {
                    val dist = p.passDistribution.toMutableMap(); dist[receiverId] = (dist[receiverId] ?: 0) + 1
                    p.copy(successfulPass = p.successfulPass + 1, passDistribution = dist)
                }
                else -> p
            }
        }
        activePasserId = receiverId
        possessionStartTime = matchTimerSeconds
    }
    fun handleDrop(receiverId: String) {
        recordTempoForPasser()
        val passerId = activePasserId ?: return
        saveCurrentStateToHistory()
        currentPointStats = currentPointStats.map { if (it.playerId == receiverId) it.copy(drop = it.drop + 1) else if (it.playerId == passerId) it.copy(successfulPass = it.successfulPass + 1) else it }
        gameMode = GameMode.DEFENSE; activePasserId = null
        possessionStartTime = null
    }
    fun handleThrowaway() {
        recordTempoForPasser()
        val passerId = activePasserId ?: return
        saveCurrentStateToHistory()
        currentPointStats = currentPointStats.map { if (it.playerId == passerId) it.copy(throwaway = it.throwaway + 1) else it }
        gameMode = GameMode.DEFENSE; activePasserId = null
        possessionStartTime = null
    }
    fun handleGoal(receiverId: String) {
        recordTempoForPasser()
        val passerId = activePasserId ?: return
        val finalStats = currentPointStats.map { p ->
            when (p.playerId) {
                receiverId -> p.copy(goal = 1)
                passerId -> {
                    val dist = p.passDistribution.toMutableMap(); dist[receiverId] = (dist[receiverId] ?: 0) + 1
                    p.copy(assist = 1, passDistribution = dist)
                }
                else -> p
            }
        }
        scoreUs++
        endPoint(finalStats, "US")
    }
    fun handleCallahan(playerId: String) {
        saveCurrentStateToHistory()

        // Callahan yapan oyuncuyu bul ve istatistiklerini güncelle
        // Callahan = Blok + Gol + Callahan Sayısı
        val finalStats = currentPointStats.map { p ->
            if (p.playerId == playerId) {
                p.copy(
                    block = p.block + 1,
                    goal = p.goal + 1,
                    callahan = p.callahan + 1,
                    // Opsiyonel: catchStat da eklenebilir ama Callahan teknik olarak bir bloktur.
                    // Genelde blok+gol olarak sayılır.
                )
            } else p
        }

        // Skoru Artır
        scoreUs++

        // Sayıyı Bitir (BİZİM SAYIMIZ olarak kaydet)
        endPoint(finalStats, "US")
    }
    fun handleOpponentGoal() {
        val pointDuration = if (matchTimerSeconds > lastPointTimeSeconds) matchTimerSeconds - lastPointTimeSeconds else 0

        // Rakip gol attı ama bizimkiler sahada kaldı, süreleri yazılmalı
        val finalStats = currentPointStats.map { p ->
            val timeToAdd = if (isTimeTrackingEnabled) pointDuration else 0L
            p.copy(secondsPlayed = p.secondsPlayed + timeToAdd)
        }

        lastPointTimeSeconds = matchTimerSeconds
        scoreThem++
        endPoint(finalStats, "THEM")
    }
    fun handleSimpleGoal(assisterId: String, scorerId: String) {
        // 1. Bu sayının ne kadar sürdüğünü hesapla
        val pointDuration = if (matchTimerSeconds > lastPointTimeSeconds) matchTimerSeconds - lastPointTimeSeconds else 0

        // 2. LİSTE OLUŞTURMA MANTIĞI GÜNCELLENDİ
        // currentPointStats zaten handleLineSelected ile sahadaki 7 kişiyi içeriyor olmalı.
        // Eğer boşsa (Line seçilmediyse), sadece golcü ve asistçiyi ekleriz.

        var finalStats = currentPointStats

        // Eğer liste boşsa veya golcü listede yoksa (Örn: Hızlı modda kadro seçilmediyse)
        // Manuel olarak oluşturuyoruz.
        val rosterMap = fullRoster.associateBy { it.id }
        if (finalStats.none { it.playerId == scorerId }) {
            // Golcüyü ekle
            val scorer = rosterMap[scorerId]
            if (scorer != null) finalStats = finalStats + PlayerStats(scorer.id, scorer.name, pointsPlayed = 1)
        }
        if (assisterId.isNotEmpty() && finalStats.none { it.playerId == assisterId }) {
            // Asistçiyi ekle
            val assister = rosterMap[assisterId]
            if (assister != null) finalStats = finalStats + PlayerStats(assister.id, assister.name, pointsPlayed = 1)
        }

        // 3. İSTATİSTİKLERİ VE SÜREYİ DAĞIT
        finalStats = finalStats.map { p ->
            // Eğer süre takibi açıksa, bu listedeki herkese süre ekle
            val timeToAdd = if (isTimeTrackingEnabled) pointDuration else 0L

            // Eğer bu kişi zaten sahadaysa (playerEntryTimes'da varsa) süresi daha hassas hesaplanabilir
            // Ama basit mod için pointDuration eklemek yeterli.

            when(p.playerId) {
                assisterId -> p.copy(
                    assist = 1,
                    passDistribution = mapOf(scorerId to 1),
                    secondsPlayed = p.secondsPlayed + timeToAdd
                )
                scorerId -> p.copy(
                    goal = 1,
                    secondsPlayed = p.secondsPlayed + timeToAdd
                )
                else -> p.copy(
                    secondsPlayed = p.secondsPlayed + timeToAdd // <-- DİĞERLERİNE DE SÜRE YAZIYORUZ
                )
            }
        }

        // 4. Zamanlayıcı işaretini güncelle
        lastPointTimeSeconds = matchTimerSeconds

        scoreUs++
        endPoint(finalStats, "US")
    }
    fun handleFinishMatch() {
        val finalMatchId = matchToEdit?.id ?: UUID.randomUUID().toString()
        val finishedMatch = Match(
            id = finalMatchId,
            opponentName = opponentName,
            scoreUs = scoreUs,
            scoreThem = scoreThem,
            pointsArchive = pointsArchive,
            matchDurationSeconds = totalMatchTimeSeconds

        )
        onMatchFinished(finishedMatch)
    }


    // --- UI ---
    val matchNavController = rememberNavController()
    NavHost(navController = matchNavController, startDestination = "stat_entry") {
        composable("stat_entry") {
            if (showInjuryDialog) {
                InjurySubDialog(currentLine = currentPointStats, fullRoster = fullRoster, onDismiss = { showInjuryDialog = false }, onSubConfirmed = { outId, inId -> handleInjurySub(outId, inId) })
            }

            StatEntryScreen(
                navController = matchNavController,
                ourTeamName = ourTeamName,
                theirTeamName = opponentName,
                scoreUs = scoreUs,
                scoreThem = scoreThem,
                completedPointsSize = pointsArchive.size,
                pointHistoryStackSize = pointHistoryStack.size,
                activePasserId = activePasserId,
                activePasserName = activePasserName,
                fullRosterWithStats = rosterWithMatchStats,
                currentLine = currentLine,
                currentPointStats = currentPointStats,
                gameMode = gameMode,
                presetLines = presetLines,
                lastPointLine = lastPointLineIds,
                onLineSelected = { handleLineSelected(it) },
                onGameModeSelected = { handleGameModeSelected(it) },
                onStatModeSelected = { handleStatModeSelected(it) },
                onSimpleWeScored = { a, s -> handleSimpleGoal(a, s) },
                onSimpleTheyScored = { handleOpponentGoal() },
                onSimpleUndo = { /* Basit mod undo */ },
                onPullSelected = { handlePull(it) },
                onPullResult = { handlePullResult(it) },
                onBlock = { handleBlock(it) },
                onCallahan = { handleCallahan(it) },
                onOpponentTurnover = { handleOpponentTurnover() },
                onOpponentScore = { handleOpponentGoal() },
                onResetMatch = { handleResetMatch() },
                onFinishMatch = { handleFinishMatch() },
                onUndoLastPoint = { handleUndoLastPoint() },
                onSelectPasser = { selectPasser(it) },
                onCatch = { handleCatch(it) },
                onDrop = { handleDrop(it) },
                onGoal = { handleGoal(it) },
                onThrowaway = { handleThrowaway() },
                onInjuryClick = { showInjuryDialog = true },
                nameFormatter = { name -> mainViewModel.formatPlayerName(name) },
                activeStoppageType = activeStoppageType,
                currentStoppageSeconds = currentStoppageSeconds,
                onStartStoppage = { startStoppage(it) },
                onEndStoppage = { endStoppage() },
                isTimeTrackingEnabled = isTimeTrackingEnabled,
                currentPointSeconds = currentPointSeconds, // Büyük Saat (Sayı)
                totalMatchSeconds = totalMatchTimeSeconds,
                isTimerRunning = isTimerRunning,
                onToggleTimer = { isTimerRunning = !isTimerRunning },

                // DEĞİŞİKLİK 3: Artık doğru ViewModel'den gelen veriyi kullanıyoruz
                isLeftHanded = isLeftHanded,
                isEditMode = false
            )
        }
    }
}
// --- 3. SAYI DÜZENLEME HOST (EDIT POINT) ---
// Screens_MatchEntry.kt dosyasındaki PointEditHost fonksiyonunu bununla değiştirin:

// Yardımcı Enum (Bu fonksiyonun üstüne veya altına ekleyebilirsin)
private enum class EditFlowStep {
    LINE_SELECTION,
    MODE_CHOICE, // Sadece Basit Mod ayarı açıksa görünür
    START_MODE_SELECTION, // Offense / Defense seçimi (Advance için)
    EDITING_SIMPLE,
    EDITING_ADVANCE
}

// Yardımcı Enum (Bu fonksiyonun üstüne veya altına ekleyebilirsin)


// Screens_MatchEntry.kt

@Composable
fun PointEditHost(
    navController: NavController,
    tournament: Tournament,
    match: Match,
    pointIndex: Int,
    pointData: PointData,
    fullRoster: List<Player>,
    onMatchUpdated: (Match) -> Unit
) {
    val mainViewModel: MainViewModel = viewModel()
    val captureModeSetting by mainViewModel.captureMode.collectAsState()

    // KARAR MEKANİZMASI:
    // Eğer sayı "Basit" modda kaydedilmişse VE Ayarlarımız da "Basit" ise -> Basit Editör
    // Diğer tüm durumlarda (Sayı Advance ise VEYA Ayarlar Advance ise) -> Advance Editör
    val openSimpleEditor = pointData.captureMode == CaptureMode.SIMPLE && captureModeSetting == CaptureMode.SIMPLE

    // --- KAYDETME VE ÇIKMA (Overwrite Logic) ---
    fun overwritePointAndExit(newPointData: PointData) {
        // 1. Mevcut arşivin kopyasını al
        val newArchive = match.pointsArchive.toMutableList()

        // 2. İlgili indeksteki ESKİ veriyi sil, YENİSİNİ yaz (Overwrite)
        if (pointIndex in newArchive.indices) {
            newArchive[pointIndex] = newPointData
        }

        // 3. Skorları baştan hesapla (Tutarlılık için)
        val newScoreUs = newArchive.count { it.whoScored == "US" }
        val newScoreThem = newArchive.count { it.whoScored == "THEM" }

        // 4. Maç objesini güncelle
        val updatedMatch = match.copy(
            pointsArchive = newArchive,
            scoreUs = newScoreUs,
            scoreThem = newScoreThem
        )

        // 5. MainActivity'e gönder ve çık
        onMatchUpdated(updatedMatch)
        navController.popBackStack()
    }

    // --- YÖNLENDİRME ---
    if (openSimpleEditor) {
        SimplePointEditor(
            navController = navController,
            pointData = pointData,
            onSave = { newData -> overwritePointAndExit(newData) }
        )
    } else {
        AdvancedPointEditor(
            navController = navController,
            tournament = tournament,
            match = match,
            pointData = pointData, // Düzenlenecek ham veri
            fullRoster = fullRoster,
            mainViewModel = mainViewModel,
            pointIndex = pointIndex,
            onSave = { newData -> overwritePointAndExit(newData) }
        )
    }
}

// Yardımcı Fonksiyon: Maçı Güncelle ve Çık
fun updateMatchAndExit(
    navController: NavController,
    match: Match,
    pointIndex: Int,
    newPointData: PointData,
    onMatchUpdated: (Match) -> Unit
) {
    val newArchive = match.pointsArchive.toMutableList()
    if (pointIndex in newArchive.indices) {
        newArchive[pointIndex] = newPointData
    }

    // Skorları yeniden hesapla
    val newScoreUs = newArchive.count { it.whoScored == "US" }
    val newScoreThem = newArchive.count { it.whoScored == "THEM" }

    val updatedMatch = match.copy(pointsArchive = newArchive, scoreUs = newScoreUs, scoreThem = newScoreThem)
    onMatchUpdated(updatedMatch)
    navController.popBackStack()
}

// Yardımcı Enum (Bu dosyada bir yere ekleyin, sınıf dışında)
enum class EditStage {
    LINE_SELECTION,
    MODE_CHOICE,
    START_MODE,
    EDITING_SIMPLE,
    EDITING_ADVANCE
}
// --- 4. ISTATISTIK GİRİŞ EKRANI (ANA LAYOUT) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatEntryScreen(
    navController: NavController,
    ourTeamName: String,
    theirTeamName: String,
    scoreUs: Int,
    scoreThem: Int,
    completedPointsSize: Int,
    pointHistoryStackSize: Int,
    activePasserId: String?,
    activePasserName: String?,
    fullRosterWithStats: List<Pair<Player, AdvancedPlayerStats>>,
    currentLine: List<Player>,
    currentPointStats: List<PlayerStats>,
    gameMode: GameMode,
    presetLines: List<PresetLine> = emptyList(),
    initialSelectedPlayerIds: Set<String> = emptySet(),
    initialPointStartMode: PointStartMode? = null,
    onLineSelected: (List<Pair<Player, AdvancedPlayerStats>>) -> Unit,
    onGameModeSelected: (GameMode) -> Unit,
    onStatModeSelected: (isDetailed: Boolean) -> Unit,
    onSimpleWeScored: (assisterId: String, scorerId: String) -> Unit,
    onSimpleTheyScored: () -> Unit,
    onSimpleUndo: () -> Unit,
    onPullSelected: (String) -> Unit,
    onPullResult: (isSuccess: Boolean) -> Unit,
    onBlock: (String) -> Unit,
    onCallahan: (String) -> Unit,
    onOpponentTurnover: () -> Unit,
    onOpponentScore: () -> Unit,
    onResetMatch: () -> Unit,
    onFinishMatch: () -> Unit,
    onUndoLastPoint: () -> Unit,
    onSelectPasser: (String) -> Unit,
    onCatch: (String) -> Unit,
    onDrop: (String) -> Unit,
    onGoal: (String) -> Unit,
    onThrowaway: () -> Unit,
    onInjuryClick: () -> Unit = {},
    isEditMode: Boolean = false,
    onDeletePoint: () -> Unit = {},
    nameFormatter: (String) -> String = { it },
    isLeftHanded: Boolean = false,
    lastPointLine: List<String> = emptyList(),
    currentPointSeconds: Long = 0, // Sayı Süresi
    totalMatchSeconds: Long = 0,   // Toplam Süre
    isTimerRunning: Boolean = false,
    onToggleTimer: () -> Unit = {},
    activeStoppageType: StoppageType? = null,
    currentStoppageSeconds: Long = 0,
    onStartStoppage: (StoppageType) -> Unit = {},
    onEndStoppage: () -> Unit = {},
    isTimeTrackingEnabled: Boolean = false
) {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showDeletePointDialog by remember { mutableStateOf(false) }

    // Dialoglar (Aynı) ...
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Sayıyı Sıfırla") },
            text = { Text("Kadro seçimine geri dönülecek. Emin misiniz?") },
            confirmButton = {
                Button(
                    onClick = { onResetMatch(); showResetDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Evet") }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("İptal") } }
        )
    }
    if (activeStoppageType != null) {
        StoppageActiveDialog(
            type = activeStoppageType,
            elapsedSeconds = currentStoppageSeconds,
            onResume = onEndStoppage
        )
    }
    // --- BURADAN AŞAĞISINI KOPYALAYIP YAPIŞTIRIN (EKSİK KISIM) ---

    // 1. MAÇI BİTİR DIALOGU (Bu eksikti, o yüzden buton tepki vermiyordu)
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Maçı Bitir") },
            text = { Text("Maç kaydedilecek ve detay ekranına gidilecek. Emin misiniz?") },
            confirmButton = {
                Button(
                    onClick = {
                        showFinishDialog = false // Önce pencreyi kapat
                        onFinishMatch()          // Sonra kaydetme fonksiyonunu tetikle
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StitchOffense)
                ) { Text("Evet, Kaydet") }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("İptal") }
            }
        )
    }

    // 2. SAYI SİLME DIALOGU (Edit modu için gerekli)
    if (showDeletePointDialog) {
        AlertDialog(
            onDismissRequest = { showDeletePointDialog = false },
            title = { Text("Sayıyı Sil") },
            text = { Text("Bu sayı kalıcı olarak silinecek. Emin misiniz?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeletePointDialog = false
                        onDeletePoint()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.discbase.ui.theme.StitchDefense)
                ) { Text("Evet, Sil") }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePointDialog = false }) { Text("İptal") }
            }
        )
    }
    // --- EKLEME SONU ---

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    // --- SADECE AYAR AÇIKSA GÖSTER ---
                    if (isTimeTrackingEnabled) {
                        // --- YENİ ÇİFTLİ SAAT TASARIMI ---
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isTimerRunning) StitchColor.Surface else Color(0xFFFFEBEE),
                            border = BorderStroke(1.dp, if(isTimerRunning) Color.Gray else Color.Red),
                            onClick = onToggleTimer,
                            modifier = Modifier.height(42.dp) // Yüksekliği sabitledik
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. İKON
                                Icon(
                                    if (isTimerRunning) Icons.Default.AccessTime else Icons.Default.PanTool,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if(isTimerRunning) StitchColor.Primary else Color.Red
                                )
                                Spacer(Modifier.width(8.dp))

                                // 2. BÜYÜK SAAT (Sayı Süresi)
                                Text(
                                    text = formatSecondsToTime(currentPointSeconds),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp, // Daha büyük
                                    color = if(isTimerRunning) StitchColor.TextPrimary else Color.Red
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        // Duraksama (El İkonu) Menüsü
                        var showStoppageMenu by remember { mutableStateOf(false) }
                        Box {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFFEBEE),
                                border = BorderStroke(1.dp, Color.Red),
                                modifier = Modifier.size(36.dp)
                                    .clickable { showStoppageMenu = true }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.PanTool,
                                        null,
                                        tint = Color.Red,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showStoppageMenu,
                            onDismissRequest = { showStoppageMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Mola (Timeout)") },
                                onClick = { onStartStoppage(StoppageType.TIMEOUT); showStoppageMenu = false },
                                leadingIcon = { Icon(Icons.Default.AccessTime, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Faul / Call") },
                                onClick = { onStartStoppage(StoppageType.CALL); showStoppageMenu = false },
                                leadingIcon = { Icon(Icons.Default.PanTool, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Sakatlık") },
                                onClick = { onStartStoppage(StoppageType.INJURY); showStoppageMenu = false },
                                leadingIcon = { Icon(Icons.Default.MedicalServices, null) }
                            )
                        }
                    }
                    // -----------------------------
                },
                navigationIcon = {
                    if (isEditMode) {
                        IconButton(onClick = onResetMatch) { Icon(Icons.Default.ArrowBack, "Geri") }
                    }
                },
                actions = {
                    // HEM EDIT HEM NORMAL MODDA GÖRÜNSÜN
                    ModernIconButton(
                        Icons.Default.Undo,
                        { if (gameMode == GameMode.SIMPLE_ENTRY) onSimpleUndo() else onUndoLastPoint() },
                        StitchTextPrimary,
                        "Geri Al"
                    )
                    Spacer(Modifier.width(8.dp))

                    ModernIconButton(
                        Icons.Default.Refresh,
                        { showResetDialog = true },
                        com.example.discbase.ui.theme.StitchDefense,
                        "Sıfırla"
                    )
                    Spacer(Modifier.width(8.dp))

                    if (isEditMode) {
                        // EDIT MODU: KAYDET BUTONU
                        ModernIconButton(
                            icon = Icons.Default.Check,
                            onClick = onFinishMatch, // Bu fonksiyon kaydetmeyi tetikler
                            color = StitchOffense,
                            contentDescription = "Değişiklikleri Kaydet"
                        )
                        // Silme butonu isterseniz buraya ekleyebilirsiniz, ama sadelik için kaldırdım.
                    } else {
                        // NORMAL MOD: MAÇI BİTİR
                        ModernIconButton(
                            Icons.Default.Check,
                            { showFinishDialog = true },
                            StitchOffense,
                            "Maçı Bitir"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {

            when (gameMode) {
                GameMode.IDLE -> {
                    if (currentLine.isEmpty()) {

                        // --- YENİ SIRALAMA MANTIĞI BURADA ---
                        val sortedRoster = remember(fullRosterWithStats) {
                            fullRosterWithStats.sortedWith(
                                compareBy<Pair<Player, AdvancedPlayerStats>> { it.first.position != "Handler" && it.first.position != "Hybrid" }
                                    .thenByDescending { it.second.basicStats.pointsPlayed }
                            )
                        }
                        LineSelectionScreen(
                            fullRosterWithStats = sortedRoster,
                            ourTeamName = ourTeamName,
                            theirTeamName = theirTeamName,
                            scoreUs = scoreUs,
                            scoreThem = scoreThem,
                            onLineConfirmed = onLineSelected,
                            initialSelectedPlayerIds = initialSelectedPlayerIds,
                            presetLines = presetLines,
                            lastPointLine = lastPointLine
                        )
                    } else {
                        PointStartUI(
                            ourTeamName = ourTeamName,
                            theirTeamName = theirTeamName,
                            scoreUs = scoreUs,
                            scoreThem = scoreThem,
                            onStartOffense = { onGameModeSelected(GameMode.OFFENSE) },
                            onStartDefense = { onGameModeSelected(GameMode.DEFENSE_PULL) },
                            initialMode = initialPointStartMode
                        )
                    }
                }

                GameMode.MODE_SELECTION -> {
                    StatModeSelectionUI(
                        ourTeamName = ourTeamName,
                        theirTeamName = theirTeamName,
                        scoreUs = scoreUs,
                        scoreThem = scoreThem,
                        onModeSelected = onStatModeSelected
                    )
                }

                GameMode.SIMPLE_ENTRY -> {
                    SimpleStatEntryUI(
                        ourTeamName = ourTeamName,
                        theirTeamName = theirTeamName,
                        scoreUs = scoreUs,
                        scoreThem = scoreThem,
                        currentPointStats = currentPointStats,
                        onWeScored = onSimpleWeScored,
                        onTheyScored = onSimpleTheyScored
                    )
                }

                GameMode.DEFENSE_PULL -> {
                    PullSelectionUI(
                        currentLine = currentLine,
                        ourTeamName = ourTeamName,
                        theirTeamName = theirTeamName,
                        scoreUs = scoreUs,
                        scoreThem = scoreThem,
                        onPlayerSelected = onPullSelected
                    )
                }

                GameMode.DEFENSE_PULL_RESULT -> {
                    PullResultUI(
                        ourTeamName = ourTeamName,
                        theirTeamName = theirTeamName,
                        scoreUs = scoreUs,
                        scoreThem = scoreThem,
                        onResultSelected = onPullResult
                    )
                }

                GameMode.OFFENSE, GameMode.DEFENSE -> {
                    StatTrackingUI(
                        navController = navController,
                        ourTeamName = ourTeamName,
                        theirTeamName = theirTeamName,
                        scoreUs = scoreUs,
                        scoreThem = scoreThem,
                        completedPointsSize = completedPointsSize,
                        activePasserId = activePasserId,
                        activePasserName = activePasserName,
                        currentPointStats = currentPointStats,
                        fullRosterWithStats = fullRosterWithStats,
                        gameMode = gameMode,
                        onBlock = onBlock,
                        onCallahan = onCallahan,
                        onOpponentTurnover = onOpponentTurnover,
                        onOpponentScore = onOpponentScore,
                        onSelectPasser = onSelectPasser,
                        onCatch = onCatch,
                        onDrop = onDrop,
                        onGoal = onGoal,
                        onThrowaway = onThrowaway,
                        onInjuryClick = onInjuryClick,
                        showStatsMenuButton = true,
                        onChangeStatMode = { onGameModeSelected(GameMode.MODE_SELECTION) },

                        // Parametreyi geçir:
                        nameFormatter = nameFormatter,
                        isLeftHanded = isLeftHanded,

                        // --- YENİ EKLENEN KISIM: Verileri aşağı geçiriyoruz ---
                        isTimerRunning = isTimerRunning,
                        isTimeTrackingEnabled = isTimeTrackingEnabled,
                        // Butona basıldığında toggle yap (Başlat)
                        onStartTimer = onToggleTimer,
                        currentPointSeconds = currentPointSeconds,
                        totalMatchSeconds = totalMatchSeconds
                    )
                }
            }
        }
    }
}
// --- YARDIMCI EKRANLAR VE COMPOSABLE'LAR ---
@Composable
fun PointStartUI(
    ourTeamName: String,
    theirTeamName: String,
    scoreUs: Int,
    scoreThem: Int,
    onStartOffense: () -> Unit,
    onStartDefense: () -> Unit,
    initialMode: PointStartMode? = null
) {
    Column(
        modifier = Modifier.fillMaxSize().background(StitchBackground),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Skor
        Text("$ourTeamName vs $theirTeamName", fontWeight = FontWeight.Bold, color = Color.Gray)
        Text(
            "$scoreUs - $scoreThem",
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = StitchColor.TextPrimary
        )

        Spacer(Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Sayı Başlangıcı", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                // OFANS Butonu
                Button(
                    onClick = onStartOffense,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StitchOffense) // Yeşil
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sports, null)
                        Spacer(Modifier.width(8.dp))
                        Text("HÜCUM (Offense)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // DEFANS Butonu
                Button(
                    onClick = onStartDefense,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.discbase.ui.theme.StitchDefense) // Kırmızı
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, null)
                        Spacer(Modifier.width(8.dp))
                        Text("DEFANS (Pull)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
@Composable
fun StatModeSelectionUI(
    ourTeamName: String,
    theirTeamName: String,
    scoreUs: Int,
    scoreThem: Int,
    onModeSelected: (isDetailed: Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(StitchBackground),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "$scoreUs - $scoreThem",
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = StitchColor.TextPrimary
        )
        Spacer(Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Kayıt Modunu Seçin",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // HIZLI KAYIT
                Button(
                    onClick = { onModeSelected(false) },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StitchSecondary)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("HIZLI KAYIT", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Sadece Gol ve Asist",
                            fontSize = 12.sp,
                            color = Color.White.copy(0.8f)
                        )
                    }
                }

                // DETAYLI İSTATİSTİK
                Button(
                    onClick = { onModeSelected(true) },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StitchColor.Primary)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DETAYLI İSTATİSTİK", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Pas, Hata, Blok, Drop takibi",
                            fontSize = 12.sp,
                            color = Color.White.copy(0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleStatEntryUI(
    ourTeamName: String,
    theirTeamName: String,
    scoreUs: Int,
    scoreThem: Int,
    currentPointStats: List<PlayerStats>, // Dropdown için gerekli
    onWeScored: (assisterId: String, scorerId: String) -> Unit,
    onTheyScored: () -> Unit,
    // Diğer parametreleri sildik çünkü TopBar'ı üst katman yönetecek
) {
    var assisterId by remember { mutableStateOf<String?>(null) }
    var scorerId by remember { mutableStateOf<String?>(null) }

    // Golü kaydetmek için en azından golcü seçilmeli
    val canSaveGoal = scorerId != null

    // --- SADECE İÇERİK (COLUMN) ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. SKOR KARTI
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
            elevation = CardDefaults.cardElevation(4.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                Modifier.padding(24.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(ourTeamName, fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 16.sp)
                Spacer(Modifier.width(16.dp))
                Text("$scoreUs", fontSize = 48.sp, fontWeight = FontWeight.Black, color = com.example.discbase.ui.theme.StitchPrimary)
                Text(" - ", fontSize = 48.sp, fontWeight = FontWeight.Light)
                Text("$scoreThem", fontSize = 48.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(16.dp))
                Text(theirTeamName, fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 16.sp)
            }
        }

        Spacer(Modifier.height(32.dp))

        // 2. OYUNCU SEÇİMİ
        Text("KİM GOL ATTI?", fontWeight = FontWeight.Bold, color = com.example.discbase.ui.theme.StitchPrimary, fontSize = 18.sp)
        Spacer(Modifier.height(16.dp))

        PlayerSelectionDropdown("Asist Yapan (Pas)", currentPointStats, assisterId, { assisterId = it }, setOfNotNull(scorerId))
        Spacer(Modifier.height(12.dp))
        PlayerSelectionDropdown("Golü Atan (Skor)", currentPointStats, scorerId, { scorerId = it }, setOfNotNull(assisterId))

        Spacer(Modifier.weight(1f))

        // 3. BUTONLAR
        Button(
            onClick = { onWeScored(assisterId ?: "", scorerId!!) },
            enabled = canSaveGoal,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StitchOffense)
        ) {
            Text("GOL BİZİM! (+1)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { onTheyScored() },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = com.example.discbase.ui.theme.StitchDefense)
        ) {
            Text("RAKİP ATTI (+1)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullSelectionUI(
    currentLine: List<Player>,
    ourTeamName: String,
    theirTeamName: String,
    scoreUs: Int,
    scoreThem: Int,
    onPlayerSelected: (String) -> Unit
) {
    var selectedPlayerId by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        Scoreboard(
            ourTeamName = ourTeamName,
            theirTeamName = theirTeamName,
            scoreUs = scoreUs,
            scoreThem = scoreThem
        )
        Divider()
        Text(
            text = "Pull Atan Oyuncuyu Seçin",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(currentLine) { player ->
                ListItem(
                    headlineContent = { Text(player.name) },
                    leadingContent = {
                        RadioButton(
                            selected = (player.id == selectedPlayerId),
                            onClick = {
                                selectedPlayerId = player.id
                                onPlayerSelected(player.id)
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedPlayerId = player.id
                            onPlayerSelected(player.id)
                        }
                )
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}
@Composable
fun PullResultUI(
    ourTeamName: String,
    theirTeamName: String,
    scoreUs: Int,
    scoreThem: Int,
    onResultSelected: (isSuccess: Boolean) -> Unit
) {
    // --- CANLI SAYAÇ İÇİN STATE ---
    var elapsedTimeMs by remember { mutableStateOf(0L) }

    // Ekran açıldığında sayacı başlat
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        while (true) {
            elapsedTimeMs = System.currentTimeMillis() - startTime
            kotlinx.coroutines.delay(16L) // 100ms'de bir güncelle (daha akıcı)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Scoreboard(
            ourTeamName = ourTeamName,
            theirTeamName = theirTeamName,
            scoreUs = scoreUs,
            scoreThem = scoreThem
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = StitchColor.Surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Disk Havada...",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )

                    // --- CANLI SAYAÇ ---
                    Text(
                        text = String.format("%.2f sn", elapsedTimeMs / 1000.0), // Örn: 4.2 sn
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = StitchColor.Primary,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    // -------------------

                    Text(
                        "Disk yakalandığında veya düştüğünde seçin:",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = { onResultSelected(true) },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.discbase.ui.theme.StitchPrimary)
                    ) {
                        Text("BAŞARILI PULL (In bounds)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { onResultSelected(false) },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("BAŞARISIZ PULL (OB / Brick)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
@Composable
fun StatTrackingUI(
    navController: NavController,
    ourTeamName: String,
    theirTeamName: String,
    scoreUs: Int,
    scoreThem: Int,
    completedPointsSize: Int,
    activePasserId: String?,
    activePasserName: String?,
    currentPointStats: List<PlayerStats>,
    fullRosterWithStats: List<Pair<Player, AdvancedPlayerStats>>,
    gameMode: GameMode,
    onBlock: (String) -> Unit,
    onCallahan: (String) -> Unit,
    onOpponentTurnover: () -> Unit,
    onOpponentScore: () -> Unit,
    onSelectPasser: (String) -> Unit,
    onCatch: (String) -> Unit,
    onDrop: (String) -> Unit,
    onGoal: (String) -> Unit,
    onThrowaway: () -> Unit,
    onInjuryClick: () -> Unit = {},
    showStatsMenuButton: Boolean = true,
    onChangeStatMode: () -> Unit = {}, // Artık kullanılmıyor ama parametre yapısı bozulmasın diye duruyor
    isLeftHanded: Boolean = false,
    nameFormatter: (String) -> String = { it },
    isTimerRunning: Boolean = false,
    isTimeTrackingEnabled: Boolean = false,
    onStartTimer: () -> Unit = {},
    // YENİ PARAMETRELERİ EKLEYİN (Eğer yoksa):
    currentPointSeconds: Long = 0,
    totalMatchSeconds: Long = 0
) {
    val sortedPointStats = remember(currentPointStats, fullRosterWithStats) {
        currentPointStats.sortedWith(
            compareByDescending<PlayerStats> { playerStat ->
                val rosterEntry = fullRosterWithStats.find { it.first.id == playerStat.playerId }
                rosterEntry?.first?.position == "Handler" || rosterEntry?.first?.position == "Hybrid"
            }
                .thenByDescending { playerStat ->
                    val rosterEntry =
                        fullRosterWithStats.find { it.first.id == playerStat.playerId }
                    (rosterEntry?.second?.basicStats?.successfulPass
                        ?: 0) + (rosterEntry?.second?.basicStats?.assist ?: 0)
                }
                .thenBy { it.name }
        )
    }

    // --- DURUM ÇUBUĞU MANTIĞI (GÜNCELLENDİ) ---
    val showStartTimerButton = isTimeTrackingEnabled && !isTimerRunning

    // Renk Belirleme
    val statusColor = if (showStartTimerButton) {
        // Süre durduysa dikkat çekici bir renk (Örn: Turuncu veya Koyu Gri)
        Color(0xFFFB8C00) // Canlı Turuncu
    } else if (gameMode == GameMode.DEFENSE) {
        com.example.discbase.ui.theme.StitchDefense
    } else {
        StitchOffense
    }

    // Metin Belirleme
    val statusText = if (showStartTimerButton) {
        "SÜREYİ BAŞLAT ▶"
    } else if (gameMode == GameMode.DEFENSE) {
        "DEFANS"
    } else if (activePasserId == null) {
        "HÜCUM BAŞLADI"
    } else {
        "$activePasserName"
    }
    // ------------------------------------------

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchColor.Background)
    ) {
        // --- GÜNCELLENMİŞ SKOR KARTI ---
        // --- GÜNCELLENMİŞ SKOR KARTI ---
        Surface(
            shadowElevation = 6.dp,
            color = StitchColor.Surface,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            modifier = Modifier.zIndex(1f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // --- YENİ: TOPLAM SÜRE (SKORUN ÜSTÜNE) ---
                if (isTimeTrackingEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5)) // Hafif gri şerit
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // TOPLAM SÜRE
                        Text("Match Time: ", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(
                            text = formatSecondsToTime(totalMatchSeconds),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }
                    Divider(color = Color.LightGray.copy(0.2f))
                }
                // -----------------------------------------
                // 1. SATIR: SKOR VE İSİMLER
                Row(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // SOL: BİZİM TAKIM
                    Text(
                        text = ourTeamName.uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = com.example.discbase.ui.theme.StitchPrimary,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // ORTA: SKOR
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text(
                            "$scoreUs",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                            color = com.example.discbase.ui.theme.StitchPrimary
                        )
                        // DÜZELTME: Tire işareti ve rakip skoru rengi dinamik yapıldı
                        Text(
                            "-",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Light,
                            modifier = Modifier.padding(horizontal = 6.dp),
                            color = StitchColor.TextPrimary
                        )
                        Text(
                            "$scoreThem",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                            color = StitchColor.TextPrimary
                        ) // <-- RAKİP SKOR RENGİ
                    }

                    // SAĞ: RAKİP TAKIM
                    Text(
                        text = theirTeamName.uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = StitchColor.TextSecondary, // <-- DÜZELTME: Gri yerine dinamik ikincil renk
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 2. SATIR: DURUM ÇUBUĞU VE SAKATLIK BUTONU
                // 2. SATIR: DURUM ÇUBUĞU (GÜNCELLENDİ)
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    // Durum Bilgisi / BAŞLAT BUTONU
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(statusColor)
                            // Eğer buton modundaysa tıklanabilir yap
                            .clickable(enabled = showStartTimerButton) {
                                onStartTimer()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = statusText,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }

                    // Sakatlık Butonu (Skorboardun altına, barın sağına alındı)
                    if (showStatsMenuButton) {
                        Box(
                            modifier = Modifier
                                .width(60.dp) // Sabit genişlik
                                .fillMaxHeight()
                                .background(com.example.discbase.ui.theme.StitchDefense.copy(alpha = 0.1f)) // Hafif kırmızı zemin
                                .clickable(onClick = onInjuryClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = "Sakatlık",
                                tint = com.example.discbase.ui.theme.StitchDefense
                            )
                        }
                    }
                }
            }
        }

        // --- OYUNCU LİSTESİ BAŞLIĞI (Catch/Drop/Goal) ---
        if (gameMode == GameMode.OFFENSE && activePasserId != null) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Solak moduna göre başlıkları hizala (gerekirse)
                if (isLeftHanded) {
                    Row(
                        modifier = Modifier.weight(0.65f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "THROW AWAY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.weight(0.35f))
                } else {
                    Spacer(modifier = Modifier.weight(0.35f))
                    Row(
                        modifier = Modifier.weight(0.65f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "CATCH",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "DROP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "GOAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // --- OYUNCU LİSTESİ ---
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(sortedPointStats) { playerStats ->
                val formattedName = nameFormatter(playerStats.name)

                PlayerRow(
                    playerStats = playerStats,
                    displayName = formattedName,
                    activePasserId = activePasserId,
                    gameMode = gameMode,
                    onSelectPasser = { onSelectPasser(playerStats.playerId) },
                    onCatch = { onCatch(playerStats.playerId) },
                    onDrop = { onDrop(playerStats.playerId) },
                    onGoal = { onGoal(playerStats.playerId) },
                    onThrowaway = onThrowaway,
                    onBlock = { onBlock(playerStats.playerId) },
                    onCallahan = { onCallahan(playerStats.playerId) }, // <-- Geçir
                    isLeftHanded = isLeftHanded
                )
            }
        }

        // --- DEFANS AKSİYON BUTONLARI (EN ALTTA) ---
        if (gameMode == GameMode.DEFENSE) {
            Surface(shadowElevation = 16.dp, color = Color.White) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onOpponentScore,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.example.discbase.ui.theme.StitchDefense.copy(0.1f), contentColor = com.example.discbase.ui.theme.StitchDefense
                        ),
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, com.example.discbase.ui.theme.StitchDefense)
                    ) { Text("RAKİP GOL", fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = onOpponentTurnover,
                        colors = ButtonDefaults.buttonColors(containerColor = StitchColor.Primary),
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("RAKİP HATA", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
@Composable
fun PlayerRow(
    playerStats: PlayerStats,
    activePasserId: String?,
    displayName: String,
    gameMode: GameMode,
    onSelectPasser: () -> Unit,
    onCatch: () -> Unit,
    onDrop: () -> Unit,
    onGoal: () -> Unit,
    onThrowaway: () -> Unit,
    onBlock: () -> Unit,
    onCallahan: () -> Unit,
    isLeftHanded: Boolean // <-- YENİ PARAMETRE
) {
    val isPasser = playerStats.playerId == activePasserId

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable(enabled = (gameMode == GameMode.OFFENSE && activePasserId == null)) { onSelectPasser() },
        colors = CardDefaults.cardColors(
            containerColor = if (isPasser) Color(0xFFE0F2F1) else Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        // Solak modu varsa LayoutDirection'ı RTL (Sağdan Sola) yapabiliriz
        // veya manuel olarak bileşenlerin yerini değiştirebiliriz.
        // Manuel değişim metin hizalaması için daha güvenlidir.

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            // --- İÇERİK BLOĞU ---
            if (isLeftHanded) {
                // SOLAK MODU: Önce Butonlar, Sonra İsim

                // 1. Butonlar (Weight 0.6f)
                Row(
                    modifier = Modifier.weight(0.6f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionButtonsContent(
                        gameMode,
                        activePasserId,
                        isPasser,
                        onThrowaway,
                        onCatch,
                        onDrop,
                        onGoal,
                        onBlock,
                        onCallahan
                    )
                }

                Spacer(Modifier.width(8.dp))

                // 2. İsim (Weight 0.4f) - Sağa yaslı
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StitchColor.TextPrimary,
                    modifier = Modifier.weight(0.4f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End // İsim sağa yaslansın ki parmak altına gelmesin
                )

            } else {
                // SAĞLAK (NORMAL) MOD: Önce İsim, Sonra Butonlar

                // 1. İsim (Weight 0.4f)
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StitchColor.TextPrimary,
                    modifier = Modifier.weight(0.4f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 2. Butonlar (Weight 0.6f)
                Row(
                    modifier = Modifier.weight(0.6f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionButtonsContent(
                        gameMode,
                        activePasserId,
                        isPasser,
                        onThrowaway,
                        onCatch,
                        onDrop,
                        onGoal,
                        onBlock,
                        onCallahan
                    )
                }
            }
        }
    }
}
@Composable
fun RowScope.ActionButtonsContent(
    gameMode: GameMode,
    activePasserId: String?,
    isPasser: Boolean,
    onThrowaway: () -> Unit,
    onCatch: () -> Unit,
    onDrop: () -> Unit,
    onGoal: () -> Unit,
    onBlock: () -> Unit,
    onCallahan: () -> Unit
) {
    if (gameMode == GameMode.OFFENSE) {
        val isReceiver = activePasserId != null && !isPasser

        if (isPasser) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Button(
                    onClick = onThrowaway,
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.discbase.ui.theme.StitchDefense),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.width(140.dp).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("THROW AWAY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else if (isReceiver) {
            // ALICI: 3 İKON
            StatIconButton(
                icon = Icons.Default.Check,
                color = StitchOffense.copy(alpha = 0.2f),
                tint = StitchOffense,
                onClick = onCatch,
                modifier = Modifier.weight(1f)
            )
            StatIconButton(
                icon = Icons.Default.Close,
                color = com.example.discbase.ui.theme.StitchDefense.copy(alpha = 0.2f),
                tint = com.example.discbase.ui.theme.StitchDefense,
                onClick = onDrop,
                modifier = Modifier.weight(1f)
            )
            StatIconButton(
                icon = Icons.Default.Adjust,
                color = com.example.discbase.ui.theme.StitchPrimary,
                tint = Color.White,
                onClick = onGoal,
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        // DEFANS MODU - BURAYI GÜNCELLİYORUZ
        Spacer(Modifier.weight(0.1f))

        // Callahan Butonu (Altın Sarısı / Özel Renk)
        Button(
            onClick = onCallahan,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)), // Gold
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.weight(1f).height(36.dp)
        ) {
            Text("CALLAHAN", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black)
        }

        Spacer(Modifier.width(8.dp))

        // Blok Butonu
        Button(
            onClick = onBlock,
            colors = ButtonDefaults.buttonColors(containerColor = StitchColor.Primary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1.5f).height(36.dp)
        ) {
            Text("BLOK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
@Composable
fun StatIconButton(
    icon: ImageVector,
    color: Color,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = color,
        modifier = modifier.fillMaxHeight(0.7f) // Yüksekliği %70 yap (çok büyük durmasın)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InjurySubDialog(
    currentLine: List<PlayerStats>, // Sahadakiler
    fullRoster: List<Player>, // Tüm Kadro
    onDismiss: () -> Unit,
    onSubConfirmed: (playerOutId: String, playerInId: String) -> Unit
) {
    // Sahada olmayan oyuncuları bul (Yedekler)
    val currentIds = currentLine.map { it.playerId }.toSet()
    val benchPlayers = fullRoster.filter { !currentIds.contains(it.id) }.sortedBy { it.name }

    var selectedPlayerOut by remember { mutableStateOf<String?>(null) }
    var selectedPlayerIn by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MedicalServices, null, tint = com.example.discbase.ui.theme.StitchDefense)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Sakatlık / Oyuncu Değişimi",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    "Sakatlanan oyuncu ve yerine girecek oyuncuyu seçiniz.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.height(16.dp))

                // ÇIKAN OYUNCU (Sahadakiler)
                Text(
                    "ÇIKAN OYUNCU",
                    fontWeight = FontWeight.Bold,
                    color = com.example.discbase.ui.theme.StitchDefense,
                    fontSize = 12.sp
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                ) {
                    LazyColumn {
                        items(currentLine) { player ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPlayerOut = player.playerId }
                                    .background(
                                        if (selectedPlayerOut == player.playerId) com.example.discbase.ui.theme.StitchDefense.copy(
                                            0.1f
                                        ) else Color.Transparent
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPlayerOut == player.playerId,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = com.example.discbase.ui.theme.StitchDefense)
                                )
                                Text(player.name, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // GİREN OYUNCU (Yedekler)
                Text(
                    "GİREN OYUNCU",
                    fontWeight = FontWeight.Bold,
                    color = StitchOffense,
                    fontSize = 12.sp
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                ) {
                    LazyColumn {
                        items(benchPlayers) { player ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPlayerIn = player.id }
                                    .background(
                                        if (selectedPlayerIn == player.id) StitchOffense.copy(
                                            0.1f
                                        ) else Color.Transparent
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPlayerIn == player.id,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = StitchOffense)
                                )
                                Text(player.name, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedPlayerOut != null && selectedPlayerIn != null) {
                        onSubConfirmed(selectedPlayerOut!!, selectedPlayerIn!!)
                    }
                },
                enabled = selectedPlayerOut != null && selectedPlayerIn != null,
                colors = ButtonDefaults.buttonColors(containerColor = StitchColor.Primary)
            ) {
                Text("Değişikliği Onayla")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = Color.Gray) }
        },
        containerColor = StitchColor.Surface,
        shape = RoundedCornerShape(16.dp)
    )
}
@Composable
fun LineSelectionScreen(
    fullRosterWithStats: List<Pair<Player, AdvancedPlayerStats>>,
    ourTeamName: String,
    theirTeamName: String,
    scoreUs: Int,
    scoreThem: Int,
    onLineConfirmed: (List<Pair<Player, AdvancedPlayerStats>>) -> Unit,
    initialSelectedPlayerIds: Set<String>,
    presetLines: List<PresetLine>,
    lastPointLine: List<String> = emptyList()
) {
    var selectedPlayerIds by remember { mutableStateOf(initialSelectedPlayerIds) }
    var activeFilterIds by remember { mutableStateOf<Set<String>?>(null) }
    var isQuickSetExpanded by remember { mutableStateOf(false) }

    // Listelenecek oyuncular (Filtre varsa daraltılmış liste)
    val displayedRoster = if (activeFilterIds != null) {
        fullRosterWithStats.filter { activeFilterIds!!.contains(it.first.id) }
    } else {
        fullRosterWithStats
    }

    // Oyuncuları Grupla
    val handlers =
        displayedRoster.filter { it.first.position == "Handler" || it.first.position == "Hybrid" }
    val cutters = displayedRoster.filter { it.first.position == "Cutter" }

    Column(
        Modifier
            .fillMaxSize()
            .background(StitchBackground)
            .padding(16.dp)
    ) {
        // ÜST BİLGİ
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Kadro (${selectedPlayerIds.size}/7)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (selectedPlayerIds.size == 7) StitchOffense else StitchTextPrimary
            )
            // Filtre varsa temizleme butonu
            if (activeFilterIds != null) {
                TextButton(onClick = { activeFilterIds = null }) {
                    Text("Filtreyi Kaldır", color = com.example.discbase.ui.theme.StitchPrimary)
                }
            } else if (selectedPlayerIds.isNotEmpty()) {
                // Filtre yoksa ama seçim varsa "Temizle" butonu
                TextButton(onClick = { selectedPlayerIds = emptySet() }) {
                    Text("Temizle", color = com.example.discbase.ui.theme.StitchDefense)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- 1. HIZLI SET YÜKLE ---
        if (presetLines.isNotEmpty() || lastPointLine.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .clickable { isQuickSetExpanded = !isQuickSetExpanded },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Hızlı Set Yükle",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0),
                            fontSize = 16.sp
                        )
                        Icon(
                            imageVector = if (isQuickSetExpanded) Icons.Default.ExpandLess else Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color(0xFF1565C0)
                        )
                    }

                    if (isQuickSetExpanded) {
                        Spacer(Modifier.height(12.dp))

                        // --- LAST LINE (AYNI KADRO) ---
                        if (lastPointLine.isNotEmpty()) {
                            val lastLineSet = lastPointLine.toSet()
                            // Şu anki seçim, son set ile birebir aynı mı?
                            val isLastLineActive =
                                selectedPlayerIds.containsAll(lastLineSet) && selectedPlayerIds.size == lastLineSet.size

                            Text(
                                "Son Oynanan:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Spacer(Modifier.height(4.dp))

                            Surface(
                                onClick = {
                                    if (isLastLineActive) {
                                        // Zaten seçiliyse -> İPTAL ET (Boşalt)
                                        selectedPlayerIds = emptySet()
                                    } else {
                                        // Seçili değilse -> SEÇ
                                        selectedPlayerIds = lastLineSet
                                        activeFilterIds = null
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                // Aktifse Yeşil, Değilse Mor
                                color = if (isLastLineActive) StitchOffense else com.example.discbase.ui.theme.StitchPrimary,
                                contentColor = Color.White
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 8.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isLastLineActive) Icons.Default.Check else Icons.Default.Restore,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "AYNI KADRO (${lastPointLine.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        val fullLines = presetLines.filter { it.playerIds.size > 7 }
                        val smallLines = presetLines.filter { it.playerIds.size <= 7 }

                        // --- TAM KADROLAR (FİLTRELER) ---
                        if (fullLines.isNotEmpty()) {
                            Text(
                                "Tam Kadrolar (Filtreler):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            FlowRow(
                                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                fullLines.forEach { line ->
                                    val lineSet = line.playerIds.toSet()
                                    val isFilterActive = activeFilterIds == lineSet

                                    QuickSetChip(
                                        line = line,
                                        isFilter = true,
                                        isActive = isFilterActive // Rengi değiştirmek için
                                    ) {
                                        if (isFilterActive) {
                                            // Zaten aktifse -> FİLTREYİ KALDIR
                                            activeFilterIds = null
                                        } else {
                                            // Değilse -> FİLTREYİ UYGULA
                                            activeFilterIds = lineSet
                                            selectedPlayerIds = emptySet()
                                        }
                                    }
                                }
                            }
                        }

                        // --- KÜÇÜK SETLER (EKLE/ÇIKAR) ---
                        if (smallLines.isNotEmpty()) {
                            Text(
                                "Setler (Ekle/Çıkar):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            FlowRow(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                smallLines.forEach { line ->
                                    val lineSet = line.playerIds.toSet()
                                    // Bu setin tamamı şu an seçili mi?
                                    val isSetSelected = selectedPlayerIds.containsAll(lineSet)

                                    QuickSetChip(
                                        line = line,
                                        isFilter = false,
                                        isActive = isSetSelected // Rengi değiştirmek için
                                    ) {
                                        if (isSetSelected) {
                                            // Zaten seçiliyse -> BU OYUNCULARI ÇIKAR (Toggle Off)
                                            selectedPlayerIds = selectedPlayerIds - lineSet
                                        } else {
                                            // Seçili değilse -> LİMİT KONTROLÜ İLE EKLE
                                            val newSet = selectedPlayerIds + lineSet
                                            if (newSet.size <= 7) {
                                                selectedPlayerIds = newSet
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // --- 2. OYUNCU LİSTESİ ---
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // HANDLERS
            if (handlers.isNotEmpty()) {
                item { SectionHeader("HANDLERS") }
                items(handlers) { pair ->
                    RosterSelectionRow(
                        pair = pair,
                        isSelected = selectedPlayerIds.contains(pair.first.id),
                        isSelectionFull = selectedPlayerIds.size >= 7,
                        onToggle = {
                            val id = it.first.id
                            if (selectedPlayerIds.contains(id)) {
                                selectedPlayerIds = selectedPlayerIds - id
                            } else if (selectedPlayerIds.size < 7) {
                                selectedPlayerIds = selectedPlayerIds + id
                            }
                        }
                    )
                }
            }

            // CUTTERS
            if (cutters.isNotEmpty()) {
                item { Spacer(Modifier.height(12.dp)); SectionHeader("CUTTERS") }
                items(cutters) { pair ->
                    RosterSelectionRow(
                        pair = pair,
                        isSelected = selectedPlayerIds.contains(pair.first.id),
                        isSelectionFull = selectedPlayerIds.size >= 7,
                        onToggle = {
                            val id = it.first.id
                            if (selectedPlayerIds.contains(id)) {
                                selectedPlayerIds = selectedPlayerIds - id
                            } else if (selectedPlayerIds.size < 7) {
                                selectedPlayerIds = selectedPlayerIds + id
                            }
                        }
                    )
                }
            }
        }

        // BAŞLAT BUTONU
        Button(
            onClick = {
                val selectedLine =
                    fullRosterWithStats.filter { selectedPlayerIds.contains(it.first.id) }
                onLineConfirmed(selectedLine)
            },
            enabled = selectedPlayerIds.size == 7,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = StitchColor.Primary,
                disabledContainerColor = Color.LightGray
            )
        ) {
            Text(
                if (selectedPlayerIds.size == 7) "MAÇI BAŞLAT"
                else if (selectedPlayerIds.size < 7) "${7 - selectedPlayerIds.size} KİŞİ DAHA SEÇ"
                else "${selectedPlayerIds.size - 7} KİŞİ ÇIKAR",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun QuickSetChip(
    line: PresetLine,
    isFilter: Boolean,
    isActive: Boolean = false, // <-- Yeni Parametre
    onClick: () -> Unit
) {
    // Aktifse renkli, değilse beyaz
    val bgColor = if (isActive) StitchSecondary else Color.White
    val contentColor = if (isActive) Color.White else StitchColor.TextPrimary
    val badgeColor =
        if (isActive) Color.White.copy(0.8f) else if (isFilter) Color.Gray else com.example.discbase.ui.theme.StitchPrimary

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = Modifier.padding(bottom = 8.dp),
        border = if (!isActive) BorderStroke(
            1.dp,
            Color.LightGray
        ) else null // Aktif değilse çerçeve
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Aktifse Tik işareti, değilse isim
            if (isActive) {
                Icon(
                    Icons.Default.Check,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
            }

            Text(
                line.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = contentColor
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (isFilter) "filtre" else "+${line.playerIds.size}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = badgeColor
            )
        }
    }
}

@Composable
fun RosterSelectionRow(
    pair: Pair<Player, AdvancedPlayerStats>,
    isSelected: Boolean,
    isSelectionFull: Boolean, // Yeni parametre: Limit doldu mu?
    onToggle: (Pair<Player, AdvancedPlayerStats>) -> Unit
) {
    val player = pair.first
    val stats = pair.second

    // Arka plan rengi
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.White

    // Kartın şeffaflığı: Seçili DEĞİLSE ve LİMİT DOLUYSA biraz silikleşsin (Görsel ipucu)
    val alpha = if (!isSelected && isSelectionFull) 0.5f else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clickable { onToggle(pair) }, // Tıklama mantığı yukarıda halledildi (Limit kontrolü orada)
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerAvatar(
                name = player.name,
                jerseyNumber = player.jerseyNumber,
                photoUrl = player.photoUrl,
                size = 40.dp,
                fontSize = 14.sp
            )
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(player.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${player.position}", fontSize = 12.sp, color = Color.Gray)
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("Oyn: ${stats.basicStats.pointsPlayed}", fontSize = 12.sp)
                Text(
                    "O: ${stats.oPointsPlayed} | D: ${stats.dPointsPlayed}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = com.example.discbase.ui.theme.StitchPrimary
                )
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(checkedColor = com.example.discbase.ui.theme.StitchPrimary),
                enabled = isSelected || !isSelectionFull // Doluysa ve seçili değilse disable et
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSelectionDropdown(
    title: String,
    players: List<PlayerStats>, // PlayerStats listesi alır
    selectedPlayerId: String?,
    onPlayerSelected: (String) -> Unit,
    filterIds: Set<String> // Diğer dropdown'da seçilen oyuncuyu göstermemek için
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedPlayerName =
        players.find { it.playerId == selectedPlayerId }?.name ?: "Oyuncu Seç..."

    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))

        // --- GÜNCELLENMİŞ KOD BLOĞU ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = { expanded = true }, // Tıklamayı Box'a taşıdık
                    role = Role.Button
                )
        ) {
            OutlinedTextField(
                value = selectedPlayerName,
                onValueChange = { },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(), // Clickable buradan kaldırıldı
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, "Aç/Kapat")
                },
                // Tıklamanın Box'a gitmesi için TextField'ı devre dışı bırakıyoruz
                enabled = false,
                // Devre dışı olmasına rağmen normal görünmesi için renkleri eziyoruz
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                // Filtrelenmiş oyuncu listesi
                val availablePlayers = players.filterNot { filterIds.contains(it.playerId) }

                availablePlayers.forEach { playerStat ->
                    DropdownMenuItem(
                        text = { Text(playerStat.name) },
                        onClick = {
                            onPlayerSelected(playerStat.playerId)
                            expanded = false
                        }
                    )
                }
            }
        }
        // --- GÜNCELLEME BİTTİ ---
    }
}
@Composable
fun StoppageActiveDialog(
    type: StoppageType,
    elapsedSeconds: Long, // Duraksama süresi
    onResume: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {}, // Dışarı tıklayınca kapanmasın (Zorunlu seçim)
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PanTool, null, tint = Color.Red)
                Spacer(Modifier.width(8.dp))
                Text("Oyun Durdu: ${type.label}", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatSecondsToTime(elapsedSeconds), // Utils'deki fonksiyon
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = StitchColor.TextPrimary
                )
                Text("Süre işliyor...", fontSize = 12.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(
                onClick = onResume,
                colors = ButtonDefaults.buttonColors(containerColor = StitchOffense),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("OYUNU BAŞLAT (RESUME)", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = StitchColor.Surface,
        shape = RoundedCornerShape(16.dp)
    )
}
@Composable
fun KeepScreenOn(enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(enabled) {
        val window = (context as? android.app.Activity)?.window
        if (enabled) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            // Ekrandan çıkınca özelliği kapat ki pil tükenmesin
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimplePointEditor(
    navController: NavController,
    pointData: PointData,
    onSave: (PointData) -> Unit
) {
    // STATE: Başlangıç verilerini yükle
    var editedDurationSeconds by remember { mutableStateOf(pointData.durationSeconds) }
    var assisterId by remember { mutableStateOf(pointData.stats.find { it.assist > 0 }?.playerId) }
    var scorerId by remember { mutableStateOf(pointData.stats.find { it.goal > 0 }?.playerId) }

    // Kadroyu koru (Sadece istatistikleri sıfırlayıp yeniden yazacağız)
    val currentLineStats = pointData.stats

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Basit Düzenleme", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Geri") }
                },
                actions = {
                    // KAYDET BUTONU
                    ModernIconButton(
                        icon = Icons.Default.Check,
                        color = StitchOffense,
                        contentDescription = "Kaydet",
                        onClick = {
                            // --- OVERWRITE MANTIĞI ---
                            // Mevcut kadrodaki herkesin istatistiğini sıfırla, sadece yeni seçilenleri işle.
                            val finalStats = currentLineStats.map { p ->
                                p.copy(
                                    goal = if (p.playerId == scorerId) 1 else 0,
                                    assist = if (p.playerId == assisterId) 1 else 0,
                                    secondsPlayed = editedDurationSeconds,
                                    pointsPlayed = 1,
                                    // Diğer detayları temizle (Basit mod olduğu için)
                                    block = 0, throwaway = 0, drop = 0, catchStat = 0, successfulPass = 0
                                )
                            }

                            val newData = pointData.copy(
                                stats = finalStats,
                                durationSeconds = editedDurationSeconds,
                                whoScored = if (scorerId != null) "US" else "THEM",
                                captureMode = CaptureMode.SIMPLE
                            )
                            onSave(newData)
                        }
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SÜRE DÜZENLEME
            Card(colors = CardDefaults.cardColors(containerColor = StitchColor.Surface)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Sayı Süresi (Manuel)", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, null, tint = StitchColor.Primary)
                        Spacer(Modifier.width(8.dp))

                        var minText by remember { mutableStateOf((editedDurationSeconds / 60).toString()) }
                        OutlinedTextField(
                            value = minText,
                            onValueChange = { if(it.all{c->c.isDigit()}) { minText=it; val m=it.toLongOrNull()?:0L; val s=editedDurationSeconds%60; editedDurationSeconds=m*60+s } },
                            modifier = Modifier.width(80.dp), label = {Text("Dk")}
                        )
                        Text(" : ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                        var secText by remember { mutableStateOf((editedDurationSeconds % 60).toString()) }
                        OutlinedTextField(
                            value = secText,
                            onValueChange = { if(it.all{c->c.isDigit()} && it.length<=2) { secText=it; val m=editedDurationSeconds/60; val s=it.toLongOrNull()?:0L; editedDurationSeconds=m*60+s } },
                            modifier = Modifier.width(80.dp), label = {Text("Sn")}
                        )
                    }
                }
            }

            // SKOR SEÇİMİ
            Text("Skor Bilgileri", fontWeight = FontWeight.Bold, color = StitchColor.Primary)
            PlayerSelectionDropdown("Asist Yapan", currentLineStats, assisterId, { assisterId = it }, setOfNotNull(scorerId))
            PlayerSelectionDropdown("Golü Atan", currentLineStats, scorerId, { scorerId = it }, setOfNotNull(assisterId))
        }
    }
}
// Bu Enum'ı dosyanın en altına veya uygun bir yere ekleyin (Sınıfların dışına)
private enum class AdvancedEditStep {
    ROSTER_SELECTION,
    START_MODE_SELECTION,
    EDITING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedPointEditor(
    navController: NavController,
    tournament: Tournament,
    match: Match,
    pointData: PointData,
    fullRoster: List<Player>,
    mainViewModel: MainViewModel,
    pointIndex: Int,
    onSave: (PointData) -> Unit
) {
    val context = LocalContext.current
    val isLeftHanded by mainViewModel.isLeftHanded.collectAsState()

    // --- EKSİK OLAN SATIR BURASI (BUNU EKLEYİN) ---
    val isTimeTrackingEnabled by mainViewModel.timeTrackingEnabled.collectAsState()

    // --- AKIŞ KONTROLÜ ---
    var currentStep by remember { mutableStateOf(AdvancedEditStep.ROSTER_SELECTION) }

    // --- VERİ STATE'LERİ ---

    // 1. Kadro Listesi (Türü List<Player> olarak zorluyoruz)
    // Başlangıçta istatistiklerdeki oyuncuları bulup listeye atıyoruz
    var currentLine by remember {
        mutableStateOf<List<Player>>(
            fullRoster.filter { p -> pointData.stats.any { it.playerId == p.id } }
        )
    }

    // 2. İstatistikler (Başlangıçta boş değil, düzenlenecek veriyi alıyor)
    var currentPointStats by remember { mutableStateOf(pointData.stats) }

    // 3. Başlangıçta Seçili Oyuncular (ID Seti)
    val initialSelectedIds = remember { pointData.stats.map { it.playerId }.toSet() }

    // 4. Tarihçe (Undo için)
    var pointHistoryStack by remember { mutableStateOf<List<PointStateSnapshot>>(emptyList()) }

    // 5. Oyun Modu ve Pasör
    var gameMode by remember { mutableStateOf(GameMode.IDLE) }
    var activePasserId by remember { mutableStateOf<String?>(null) }
    var startMode by remember { mutableStateOf(pointData.startMode ?: PointStartMode.OFFENSE) }

    // 6. Süre ve Tempo
    // Edit modunda süre 0'dan mı başlasın yoksa eski süreyi mi alsın?
    // "Baştan oynatma" dediğin için 0'dan başlatıyoruz.
    var matchTimeSeconds by remember { mutableStateOf(0L) }
    val preceedingDuration = remember(match.pointsArchive, pointIndex) {
        match.pointsArchive.take(pointIndex).sumOf { it.durationSeconds }
    }
    var isTimerRunning by remember { mutableStateOf(false) }
    var possessionStartTime by remember { mutableStateOf<Long?>(null) }

    // Kadro Görünümü (Pair Listesi: Player + Stats)
    // Bu sadece LineSelectionScreen'de görsel amaçlı kullanılıyor
    val rosterWithStats = remember(match.pointsArchive, fullRoster) {
        fullRoster.map { player ->
            val stats = calculateStatsFromPoints(player.id, match.pointsArchive)
            Pair(player, stats)
        }.sortedByDescending { it.second.basicStats.pointsPlayed }
            .sortedBy { it.first.position != "Handler" && it.first.position != "Hybrid" }
    }

    // --- YARDIMCI FONKSİYONLAR ---
    fun saveHistory() {
        pointHistoryStack = pointHistoryStack + PointStateSnapshot(
            pointStats = currentPointStats,
            activePasserId = activePasserId,
            gameMode = gameMode,
            subbedOutStats = emptyList()
        )
    }

    // KRONOMETRE
    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning) {
            kotlinx.coroutines.delay(1000L)
            matchTimeSeconds++
            // ----------------------------------------------
        }
    }

    // TEMPO KAYIT
    fun recordTempo() {
        val passerId = activePasserId ?: return
        val duration = if (isTimerRunning) 1L else 0L
        currentPointStats = currentPointStats.map {
            if (it.playerId == passerId) it.copy(totalTempoSeconds = it.totalTempoSeconds + duration) else it
        }
    }

    // --- BUTON AKSİYONLARI (Canlı Maç Mantığı) ---
    fun handleCatch(receiverId: String) {
        saveHistory()
        recordTempo()
        val passerId = activePasserId ?: return
        currentPointStats = currentPointStats.map { p ->
            when (p.playerId) {
                receiverId -> p.copy(catchStat = p.catchStat + 1)
                passerId -> {
                    val dist = p.passDistribution.toMutableMap(); dist[receiverId] = (dist[receiverId] ?: 0) + 1
                    p.copy(successfulPass = p.successfulPass + 1, passDistribution = dist)
                }
                else -> p
            }
        }
        activePasserId = receiverId
    }

    fun handleGoal(receiverId: String) {
        saveHistory()
        recordTempo()
        val passerId = activePasserId ?: return
        currentPointStats = currentPointStats.map { p ->
            when (p.playerId) {
                receiverId -> p.copy(goal = 1)
                passerId -> {
                    val dist = p.passDistribution.toMutableMap(); dist[receiverId] = (dist[receiverId] ?: 0) + 1
                    p.copy(assist = 1, passDistribution = dist)
                }
                else -> p
            }
        }
        activePasserId = null
        gameMode = GameMode.IDLE
        isTimerRunning = false
        Toast.makeText(context, "Gol! Kaydetmek için yukarıdaki tike basın.", Toast.LENGTH_SHORT).show()
    }

    fun handleDrop(receiverId: String) {
        saveHistory()
        recordTempo()
        val passerId = activePasserId ?: return
        currentPointStats = currentPointStats.map {
            if (it.playerId == receiverId) it.copy(drop = it.drop + 1)
            else if (it.playerId == passerId) it.copy(successfulPass = it.successfulPass + 1)
            else it
        }
        gameMode = GameMode.DEFENSE; activePasserId = null
    }

    fun handleThrowaway() {
        saveHistory()
        recordTempo()
        val passerId = activePasserId ?: return
        currentPointStats = currentPointStats.map { if (it.playerId == passerId) it.copy(throwaway = it.throwaway + 1) else it }
        gameMode = GameMode.DEFENSE; activePasserId = null
    }

    fun handleBlock(playerId: String) {
        saveHistory()
        currentPointStats = currentPointStats.map { if (it.playerId == playerId) it.copy(block = it.block + 1) else it }
        gameMode = GameMode.OFFENSE; activePasserId = null
    }

    fun handleCallahan(playerId: String) {
        saveHistory()
        currentPointStats = currentPointStats.map { if (it.playerId == playerId) it.copy(block = it.block + 1, goal = 1, callahan = 1) else it }
        gameMode = GameMode.IDLE
        isTimerRunning = false
    }

    fun handleUndo() {
        if (pointHistoryStack.isEmpty()) {
            Toast.makeText(context, "Geri alınacak işlem yok.", Toast.LENGTH_SHORT).show()
            return
        }
        val lastState = pointHistoryStack.last()
        currentPointStats = lastState.pointStats
        activePasserId = lastState.activePasserId
        gameMode = lastState.gameMode
        pointHistoryStack = pointHistoryStack.dropLast(1)
    }

    // --- SIFIRLAMA (RESET) ---
    fun resetToCleanSlate() {
        // Kadrodaki oyuncuların istatistiklerini sıfırla (Oynadı: 1 hariç)
        currentPointStats = currentLine.map {
            PlayerStats(playerId = it.id, name = it.name, pointsPlayed = 1)
        }
        gameMode = if (startMode == PointStartMode.OFFENSE) GameMode.OFFENSE else GameMode.DEFENSE
        if (startMode == PointStartMode.OFFENSE) activePasserId = null
        matchTimeSeconds = 0
        isTimerRunning = false
        pointHistoryStack = emptyList()
    }

    // --- EKRAN YAPISI ---

    // Geri butonu mantığı
    fun handleBack() {
        when(currentStep) {
            AdvancedEditStep.ROSTER_SELECTION -> navController.popBackStack()
            AdvancedEditStep.START_MODE_SELECTION -> currentStep = AdvancedEditStep.ROSTER_SELECTION
            AdvancedEditStep.EDITING -> currentStep = AdvancedEditStep.START_MODE_SELECTION
        }
    }

    // ANA YAPIDA SCAFFOLD KULLANMIYORUZ (StatEntryScreen kendi Scaffold'unu getiriyor)
    // Sadece ilk iki adım (Kadro ve Mod seçimi) için Scaffold kullanıyoruz.

    when (currentStep) {
        AdvancedEditStep.ROSTER_SELECTION -> {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Kadro Düzenle", fontWeight = FontWeight.Bold) },
                        navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Geri") } }
                    )
                }
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    LineSelectionScreen(
                        fullRosterWithStats = rosterWithStats,
                        ourTeamName = tournament.ourTeamName,
                        theirTeamName = match.opponentName,
                        scoreUs = match.scoreUs,
                        scoreThem = match.scoreThem,
                        initialSelectedPlayerIds = initialSelectedIds,
                        presetLines = tournament.presetLines,
                        onLineConfirmed = { selectedPairs ->
                            // List<Pair> -> List<Player> dönüşümü
                            val selectedPlayers = selectedPairs.map { it.first }
                            currentLine = selectedPlayers

                            // REPLAY MANTIĞI: Kadro seçilince istatistikleri sıfırdan oluştur
                            // Eski 'pointData.stats' kullanılmaz, temiz sayfa açılır.
                            currentPointStats = selectedPlayers.map {
                                PlayerStats(playerId = it.id, name = it.name, pointsPlayed = 1)
                            }
                            currentStep = AdvancedEditStep.START_MODE_SELECTION
                        }
                    )
                }
            }
        }

        AdvancedEditStep.START_MODE_SELECTION -> {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Başlangıç Durumu", fontWeight = FontWeight.Bold) },
                        navigationIcon = { IconButton(onClick = { handleBack() }) { Icon(Icons.Default.ArrowBack, "Geri") } }
                    )
                }
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    PointStartUI(
                        ourTeamName = tournament.ourTeamName,
                        theirTeamName = match.opponentName,
                        scoreUs = match.scoreUs,
                        scoreThem = match.scoreThem,
                        onStartOffense = {
                            startMode = PointStartMode.OFFENSE
                            gameMode = GameMode.OFFENSE
                            activePasserId = null
                            currentStep = AdvancedEditStep.EDITING
                        },
                        onStartDefense = {
                            startMode = PointStartMode.DEFENSE
                            gameMode = GameMode.DEFENSE
                            currentStep = AdvancedEditStep.EDITING
                        }
                    )
                }
            }
        }

        AdvancedEditStep.EDITING -> {
            // BURADA SCAFFOLD YOK! StatEntryScreen KENDİ SCAFFOLD'UNU KULLANACAK.
            StatEntryScreen(
                navController = navController,
                ourTeamName = tournament.ourTeamName,
                theirTeamName = match.opponentName,
                scoreUs = match.scoreUs,
                scoreThem = match.scoreThem,
                completedPointsSize = match.pointsArchive.size,
                pointHistoryStackSize = pointHistoryStack.size,
                activePasserId = activePasserId,
                activePasserName = currentPointStats.find { it.playerId == activePasserId }?.name,
                fullRosterWithStats = rosterWithStats,
                currentLine = currentLine,  // Türü: List<Player> (Doğru)
                currentPointStats = currentPointStats,
                gameMode = gameMode,

                // --- MAÇ KAYIT EKRANI FONKSİYONLARI ---
                onSelectPasser = {
                    saveHistory()
                    activePasserId = it
                    if (gameMode == GameMode.IDLE || gameMode == GameMode.OFFENSE) gameMode = GameMode.OFFENSE
                },
                onCatch = { handleCatch(it) },
                onDrop = { handleDrop(it) },
                onGoal = { handleGoal(it) },
                onThrowaway = { handleThrowaway() },
                onBlock = { handleBlock(it) },
                onCallahan = { handleCallahan(it) },
                onOpponentTurnover = { saveHistory(); gameMode = GameMode.OFFENSE; activePasserId = null },
                onOpponentScore = {
                    gameMode = GameMode.IDLE
                    isTimerRunning = false
                    Toast.makeText(context, "Rakip Sayı.", Toast.LENGTH_SHORT).show()
                },

                // --- EDİT EKRANI ÖZEL BUTONLARI ---
                // Bu butonlar StatEntryScreen'in üst barında çıkacak
                onResetMatch = { resetToCleanSlate() }, // Sıfırla butonu için
                onUndoLastPoint = { handleUndo() },     // Geri Al butonu için

                // KAYDET (FİNİSH) BUTONU
                onFinishMatch = {
                    val hasGoal = currentPointStats.any { it.goal > 0 }
                    val whoScored = if (hasGoal) "US" else if(gameMode == GameMode.IDLE) "THEM" else pointData.whoScored
                    // Edit modunda sahadaki herkes o sayının tam süresi kadar oynamış sayılır.
                    val statsWithTime = currentPointStats.map { playerStat ->
                        if (isTimeTrackingEnabled) {
                            playerStat.copy(secondsPlayed = matchTimeSeconds)
                        } else {
                            playerStat
                        }
                    }
                    // ---------------------------------------------
                    val newData = pointData.copy(
                        stats = statsWithTime,// Yeni (sıfırdan oluşturulmuş) veriler eskiyi ezecek
                        durationSeconds = matchTimeSeconds,
                        captureMode = CaptureMode.ADVANCED,
                        startMode = startMode,
                        whoScored = whoScored
                    )
                    onSave(newData)
                },

                // SÜRE
                currentPointSeconds = matchTimeSeconds, // Parametre ismini güncelledik
                totalMatchSeconds = preceedingDuration + matchTimeSeconds,
                isTimerRunning = isTimerRunning,
                onToggleTimer = { isTimerRunning = !isTimerRunning },
                isTimeTrackingEnabled = true,

                isEditMode = true, // StatEntryScreen'e edit modunda olduğumuzu bildirir
                nameFormatter = { mainViewModel.formatPlayerName(it) },
                isLeftHanded = isLeftHanded,

                // Kullanılmayanlar
                onLineSelected = {}, onGameModeSelected = {}, onStatModeSelected = {},
                onSimpleWeScored = {_,_ ->}, onSimpleTheyScored = {}, onSimpleUndo = {},
                onPullSelected = {}, onPullResult = {},
            )
        }
    }
}