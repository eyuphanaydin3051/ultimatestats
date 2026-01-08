package com.eyuphanaydin.discbase

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.eyuphanaydin.discbase.ui.theme.StitchColor
import com.eyuphanaydin.discbase.ui.theme.StitchGradientEnd
import com.eyuphanaydin.discbase.ui.theme.StitchGradientStart
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerCareerScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    // Verileri yükle
    LaunchedEffect(Unit) {
        viewModel.loadCareerData()
    }

    val careerData by viewModel.careerData.collectAsState()
    val isLoading by viewModel.isLoadingCareer.collectAsState()
    val userProfile by viewModel.currentUserProfile.collectAsState()

    // Filtre State'leri
    var selectedTeamId by remember { mutableStateOf("GENEL") }
    var selectedTournamentId by remember { mutableStateOf("GENEL") }
    var selectedMatchId by remember { mutableStateOf<String?>(null) }

    // Dropdown Görünürlükleri
    var showTeamDropdown by remember { mutableStateOf(false) }
    var showTournamentDropdown by remember { mutableStateOf(false) }
    var showMatchDropdown by remember { mutableStateOf(false) }

    // Seçili Veriler
    val selectedTeamData = careerData.find { it.teamProfile.teamId == selectedTeamId }

    // Turnuvaları Listele
    val availableTournaments = remember(careerData, selectedTeamId) {
        if (selectedTeamId == "GENEL") {
            careerData.flatMap { it.tournaments }.distinctBy { it.id }
        } else {
            selectedTeamData?.tournaments ?: emptyList()
        }
    }

    val selectedTournament = availableTournaments.find { it.id == selectedTournamentId }

    // Maç listesi: Eğer bir turnuva seçiliyse onun maçları
    val availableMatches = selectedTournament?.matches ?: emptyList()

    // İstatistik Hesaplama
    val stats = viewModel.calculateCareerStats(selectedTeamId, selectedTournamentId, selectedMatchId)

    // UI Metinleri
    val teamFilterText = if (selectedTeamId == "GENEL") "Tüm Takımlar" else selectedTeamData?.teamProfile?.teamName ?: "Bilinmiyor"
    val tournamentFilterText = if (selectedTournamentId == "GENEL") stringResource(R.string.profile_career_stats) else selectedTournament?.tournamentName ?: ""
    val matchFilterText = if (selectedMatchId == null) stringResource(R.string.filter_all_matches) else "Seçili Maç"

    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Oyuncu Kariyeri", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StitchColor.Primary)
            }
        } else if (careerData.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(
                    "Henüz mail adresinizle eşleşen bir oyuncu profili bulunamadı.\nLütfen takım yöneticinizin sizi doğru mail adresiyle eklediğinden emin olun.",
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. PROFİL KARTI (User Profile'dan gelen bilgiler)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(StitchGradientStart, StitchGradientEnd)
                                )
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Profil fotoğrafı yoksa ikon göster
                            Icon(
                                Icons.Default.Person, null,
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    userProfile?.displayName ?: "Oyuncu",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    userProfile?.email ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(0.8f)
                                )
                            }
                        }
                    }
                }

                // 2. FİLTRELER (Takım -> Turnuva -> Maç)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Takım Filtresi
                    FilterDropdown(
                        label = "Takım Filtresi",
                        text = teamFilterText,
                        onClick = { showTeamDropdown = true }
                    )

                    // Turnuva Filtresi
                    FilterDropdown(
                        label = "Turnuva Filtresi",
                        text = tournamentFilterText,
                        onClick = { showTournamentDropdown = true }
                    )

                    // Maç Filtresi (Sadece turnuva seçiliyse aktif)
                    if (selectedTournamentId != "GENEL") {
                        FilterDropdown(
                            label = "Maç Filtresi",
                            text = matchFilterText,
                            onClick = { showMatchDropdown = true }
                        )
                    }
                }

                // 3. İSTATİSTİK KARTLARI
                EfficiencyCard(
                    efficiencyScore = stats.plusMinus,
                    onInfoClick = { /* Bilgi diyaloğu açılabilir */ }
                )

                GameTimeCard(
                    totalPoints = stats.basicStats.pointsPlayed,
                    offensePoints = stats.oPointsPlayed,
                    defensePoints = stats.dPointsPlayed
                )

                // Pas ve Yakalama Oranları (StatPercentage sınıfını kullanmak için)
                val totalPasses = stats.basicStats.successfulPass + stats.basicStats.assist + stats.basicStats.throwaway
                val totalPassesCompleted = stats.basicStats.successfulPass + stats.basicStats.assist
                val passRate = calculateSafePercentage(totalPassesCompleted, totalPasses)

                val totalCatchesAttempted = stats.basicStats.catchStat + stats.basicStats.goal + stats.basicStats.drop
                val totalCatchesSuccessful = stats.basicStats.catchStat + stats.basicStats.goal
                val catchRate = calculateSafePercentage(totalCatchesSuccessful, totalCatchesAttempted)

                // Takım ortalamaları olmadığı için mapOf boş gönderiliyor
                val dummyTeamAvg = mapOf<String, Double>()

                ReceivingStatsCard(catchRate, stats.basicStats, dummyTeamAvg)
                PassingStatsCard(passRate, stats.basicStats, dummyTeamAvg)
                DefenseStatsCard(stats.basicStats, dummyTeamAvg)

                Spacer(Modifier.height(32.dp))
            }

            // --- DROP DOWN MENÜLER ---

            // Takım Seçimi
            if (showTeamDropdown) {
                AlertDialog(
                    onDismissRequest = { showTeamDropdown = false },
                    title = { Text("Takım Seç") },
                    text = {
                        LazyColumn {
                            item {
                                Text(
                                    "Tüm Takımlar (Kariyer)",
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedTeamId = "GENEL"
                                        selectedTournamentId = "GENEL"
                                        selectedMatchId = null
                                        showTeamDropdown = false
                                    }.padding(12.dp)
                                )
                            }
                            items(careerData) { data ->
                                Text(
                                    data.teamProfile.teamName,
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedTeamId = data.teamProfile.teamId
                                        selectedTournamentId = "GENEL"
                                        selectedMatchId = null
                                        showTeamDropdown = false
                                    }.padding(12.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {}
                )
            }

            // Turnuva Seçimi
            if (showTournamentDropdown) {
                AlertDialog(
                    onDismissRequest = { showTournamentDropdown = false },
                    title = { Text("Turnuva Seç") },
                    text = {
                        LazyColumn {
                            item {
                                Text(
                                    stringResource(R.string.profile_career_stats),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedTournamentId = "GENEL"
                                        selectedMatchId = null
                                        showTournamentDropdown = false
                                    }.padding(12.dp)
                                )
                            }
                            items(availableTournaments) { t ->
                                Text(
                                    t.tournamentName,
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedTournamentId = t.id
                                        selectedMatchId = null
                                        showTournamentDropdown = false
                                    }.padding(12.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {}
                )
            }

            // Maç Seçimi
            if (showMatchDropdown) {
                AlertDialog(
                    onDismissRequest = { showMatchDropdown = false },
                    title = { Text("Maç Seç") },
                    text = {
                        LazyColumn {
                            item {
                                Text(
                                    stringResource(R.string.filter_all_matches),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedMatchId = null
                                        showMatchDropdown = false
                                    }.padding(12.dp)
                                )
                            }
                            items(availableMatches) { m ->
                                Text(
                                    "vs ${m.opponentName}",
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedMatchId = m.id
                                        showMatchDropdown = false
                                    }.padding(12.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {}
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(
    label: String,
    text: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(label, fontSize = 10.sp, color = Color.Gray)
                Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
    }
}