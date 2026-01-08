package com.eyuphanaydin.discbase

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.eyuphanaydin.discbase.ui.theme.StitchOffense

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

    // --- STATE TANIMLARI ---
    var selectedTeamId by remember { mutableStateOf("GENEL") }
    var selectedTournamentId by remember { mutableStateOf("GENEL") }
    var selectedMatchId by remember { mutableStateOf<String?>(null) }

    // Dropdown Kontrolü
    var showTeamDropdown by remember { mutableStateOf(false) }
    var showTournamentDropdown by remember { mutableStateOf(false) }
    var showMatchDropdown by remember { mutableStateOf(false) }

    // Tab Kontrolü (İstatistikler / Pas Ağı)
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf(
        stringResource(R.string.tab_player_stats),
        stringResource(R.string.tab_pass_network)
    )

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

    // İstatistik Hesaplama
    val stats = viewModel.calculateCareerStats(selectedTeamId, selectedTournamentId, selectedMatchId)

    // UI Metinleri
    val teamFilterText = if (selectedTeamId == "GENEL")
        stringResource(R.string.filter_all_teams)
    else
        selectedTeamData?.teamProfile?.teamName ?: stringResource(R.string.unknown_team)

    val tournamentFilterText = if (selectedTournamentId == "GENEL")
        stringResource(R.string.filter_all_season)
    else
        selectedTournament?.tournamentName ?: ""

    val matchFilterText = if (selectedMatchId == null)
        stringResource(R.string.filter_all_matches)
    else
        selectedTournament?.matches?.find { it.id == selectedMatchId }?.let { "vs ${it.opponentName}" } ?: stringResource(R.string.filter_unknown_match)


    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.title_player_career), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.desc_back))
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
                    stringResource(R.string.msg_no_profile_found),
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
                // --- 1. PROFİL KARTI (PlayerEditScreen İle Aynı Tasarım) ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
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
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color.White.copy(0.2f), CircleShape)
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (userProfile?.photoUrl != null) {
                                    // AsyncImage burada kullanılabilir, şimdilik ikon
                                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(50.dp))
                                } else {
                                    Text(
                                        text = userProfile?.displayName?.take(1)?.uppercase() ?: "?",
                                        color = Color.White,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    userProfile?.displayName ?: stringResource(R.string.unknown),
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

                // --- 2. FİLTRELER (PlayerEditScreen Yapısına Uygun) ---
                // Takım filtresini en üste, diğerlerini yan yana koyuyoruz
                FilterDropdown(
                    label = stringResource(R.string.filter_team_label),
                    text = teamFilterText,
                    onClick = { showTeamDropdown = true }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Turnuva Filtresi
                    Card(
                        modifier = Modifier.weight(1f),
                        onClick = { showTournamentDropdown = true },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(stringResource(R.string.filter_tournament), fontSize = 10.sp, color = Color.Gray)
                                Text(tournamentFilterText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Maç Filtresi (Sadece Turnuva Seçiliyse Aktif)
                    if (selectedTournamentId != "GENEL") {
                        Card(
                            modifier = Modifier.weight(1f),
                            onClick = { showMatchDropdown = true },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(stringResource(R.string.filter_match), fontSize = 10.sp, color = Color.Gray)
                                    Text(matchFilterText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                // --- 3. SEKMELER (TABS) ---
                TabRow(selectedTabIndex = selectedTabIndex, containerColor = StitchColor.Background) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                // --- 4. İÇERİK ---
                if (selectedTabIndex == 0) {
                    // --- İSTATİSTİKLER TABI ---
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        EfficiencyCard(
                            efficiencyScore = stats.plusMinus,
                            onInfoClick = { /* Bilgi diyaloğu eklenebilir */ }
                        )

                        GameTimeCard(
                            totalPoints = stats.basicStats.pointsPlayed,
                            offensePoints = stats.oPointsPlayed,
                            defensePoints = stats.dPointsPlayed
                        )

                        // Oranlar
                        val totalPasses = stats.basicStats.successfulPass + stats.basicStats.assist + stats.basicStats.throwaway
                        val totalPassesCompleted = stats.basicStats.successfulPass + stats.basicStats.assist
                        val passRate = calculateSafePercentage(totalPassesCompleted, totalPasses)

                        val totalCatchesAttempted = stats.basicStats.catchStat + stats.basicStats.goal + stats.basicStats.drop
                        val totalCatchesSuccessful = stats.basicStats.catchStat + stats.basicStats.goal
                        val catchRate = calculateSafePercentage(totalCatchesSuccessful, totalCatchesAttempted)

                        // Dummy Team Avg (Kariyerde kıyaslama zor olduğu için boş geçiyoruz)
                        val dummyTeamAvg = mapOf<String, Double>()

                        // Kartlar
                        ReceivingStatsCard(catchRate, stats.basicStats, dummyTeamAvg)
                        PassingStatsCard(passRate, stats.basicStats, dummyTeamAvg)
                        DefenseStatsCard(stats.basicStats, dummyTeamAvg)
                    }

                } else {
                    // --- PAS AĞI TABI ---
                    if (stats.basicStats.passDistribution.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.no_data), color = Color.Gray)
                        }
                    } else {
                        // DETAYLI BAĞLANTILAR LİSTESİ (PlayerEditScreen'deki gibi)
                        Text(
                            stringResource(R.string.detailed_connections_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = StitchColor.TextPrimary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )

                        val sortedConnections = stats.basicStats.passDistribution.entries
                            .sortedByDescending { it.value }

                        val totalPasses = stats.basicStats.successfulPass + stats.basicStats.assist

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            sortedConnections.forEach { (receiverName, count) ->
                                val percentage = if (totalPasses > 0) (count.toDouble() / totalPasses) else 0.0

                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
                                    elevation = CardDefaults.cardElevation(1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Basit Avatar (İsim baş harfi)
                                        Box(
                                            modifier = Modifier.size(40.dp).background(Color.LightGray, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(receiverName.take(1), fontWeight = FontWeight.Bold, color = Color.White)
                                        }

                                        Spacer(Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(receiverName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            modifier = Modifier.width(120.dp)
                                        ) {
                                            Text(
                                                text = "$count Pas (${String.format("%.0f", percentage * 100)}%)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = StitchColor.TextPrimary
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = percentage.toFloat(),
                                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                                                color = StitchColor.Primary,
                                                trackColor = Color(0xFFF0F0F0)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))
            }

            // --- DROP DOWN MENÜLER ---
            if (showTeamDropdown) {
                AlertDialog(
                    onDismissRequest = { showTeamDropdown = false },
                    title = { Text(stringResource(R.string.filter_team_label)) },
                    text = {
                        LazyColumn {
                            item {
                                Text(
                                    stringResource(R.string.filter_all_teams),
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

            if (showTournamentDropdown) {
                AlertDialog(
                    onDismissRequest = { showTournamentDropdown = false },
                    title = { Text(stringResource(R.string.title_select_tournament)) },
                    text = {
                        LazyColumn {
                            item {
                                Text(
                                    stringResource(R.string.label_all_season),
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

            if (showMatchDropdown && selectedTournament != null) {
                AlertDialog(
                    onDismissRequest = { showMatchDropdown = false },
                    title = { Text(stringResource(R.string.title_select_match)) },
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
                            items(selectedTournament.matches) { m ->
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
    fun calculateSafePercentage(numerator: Int, denominator: Int): Double {
        return if (denominator > 0) {
            numerator.toDouble() / denominator.toDouble()
        } else {
            0.0
        }
    }
}