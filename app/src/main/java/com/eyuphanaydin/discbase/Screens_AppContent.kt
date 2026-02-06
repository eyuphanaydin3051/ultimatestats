package com.eyuphanaydin.discbase

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.eyuphanaydin.discbase.ui.theme.*
import kotlinx.coroutines.launch
import java.util.*
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalContext
// --- 1. ANA SAYFA (HOME SCREEN) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    teamProfile: TeamProfile,
    tournaments: List<Tournament>
) {
    val advancedStats = calculateTeamStatsForFilter(tournaments, "GENEL")
    val winRate = "${advancedStats.wins}${stringResource(R.string.W)}- ${advancedStats.losses}${stringResource(R.string.L)}"
    val passRate = calculateSafePercentage(advancedStats.totalPassesCompleted, advancedStats.totalPassesAttempted)
    val totalTurnovers = advancedStats.totalThrowaways + advancedStats.totalDrops

    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.home_title), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                },
                actions = {
                    val mainViewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    val hasNotification by mainViewModel.hasPendingRequests.collectAsState()

                    androidx.compose.material3.BadgedBox(
                        badge = {
                            if (hasNotification) {
                                androidx.compose.material3.Badge(
                                    containerColor = com.eyuphanaydin.discbase.ui.theme.StitchDefense,
                                    contentColor = Color.White
                                )
                            }
                        },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        ModernIconButton(
                            icon = Icons.Default.Person,
                            onClick = { navController.navigate("profile_edit") },
                            color = com.eyuphanaydin.discbase.ui.theme.StitchPrimary,
                            contentDescription = stringResource(R.string.desc_profile_settings)
                        )
                    }
                    ModernIconButton(
                        icon = Icons.Default.Settings,
                        onClick = { navController.navigate("settings") },
                        color = StitchColor.TextPrimary,
                        contentDescription = stringResource(R.string.nav_settings)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (teamProfile.logoPath != null) {
                    AsyncImage(
                        model = getLogoModel(teamProfile.logoPath),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp).clip(CircleShape).border(2.dp,
                            com.eyuphanaydin.discbase.ui.theme.StitchPrimary, CircleShape)
                    )
                } else {
                    Icon(
                        Icons.Default.Groups,
                        null,
                        tint = StitchColor.Primary,
                        modifier = Modifier.size(64.dp).background(Color.White, CircleShape).padding(12.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(stringResource(R.string.home_welcome), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Text(teamProfile.teamName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = StitchColor.TextPrimary)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(StitchGradientStart, StitchGradientEnd)
                        )
                    )
                    .clickable { navController.navigate("season_matches") }
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Text(stringResource(R.string.home_season_report), color = Color.White.copy(0.8f), fontSize = 14.sp)
                    Text(winRate, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                }
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color.White.copy(0.2f),
                    modifier = Modifier.size(100.dp).align(Alignment.TopEnd).offset(x = 20.dp, y = -20.dp)
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(0.6f),
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    modifier = Modifier.weight(1f).height(140.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = StitchCardBg),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(stringResource(R.string.home_pass_success), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(progress = 1f, modifier = Modifier.size(60.dp), color = Color(0xFFF0F0F0), strokeWidth = 6.dp)
                            CircularProgressIndicator(progress = passRate.progress, modifier = Modifier.size(60.dp), color = Color(0xFFFF9800), strokeWidth = 6.dp)
                            Text(passRate.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
                Card(
                    modifier = Modifier.weight(1f).height(140.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = StitchCardBg),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(stringResource(R.string.home_total_turnover), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Text("$totalTurnovers", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = com.eyuphanaydin.discbase.ui.theme.StitchDefense)
                        Icon(Icons.Default.TrendingDown, null, tint = com.eyuphanaydin.discbase.ui.theme.StitchDefense)
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clickable { navController.navigate("team_stats") },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = StitchSecondary.copy(alpha = 0.1f)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(stringResource(R.string.home_stats_center), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = com.eyuphanaydin.discbase.ui.theme.StitchPrimary)
                        Text(stringResource(R.string.home_stats_desc), fontSize = 12.sp, color = Color.Gray)
                    }
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = StitchColor.Primary,
                        modifier = Modifier.size(24.dp).background(Color.White, CircleShape).padding(4.dp)
                    )
                }
            }
        }
    }
}
// --- 2. KADRO EKRANI (ROSTER) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterScreen(
    navController: NavController,
    rosterPlayers: List<Player>,
    onAddPlayerClick: () -> Unit,
    isAdmin: Boolean
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredPlayers = if (searchQuery.isBlank()) {
        rosterPlayers
    } else {
        rosterPlayers.filter { player ->
            player.name.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = StitchColor.Background,
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = onAddPlayerClick,
                    containerColor = StitchColor.Primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.desc_add_player))
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.roster_search_hint), color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null) }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = com.eyuphanaydin.discbase.ui.theme.StitchPrimary,
                        unfocusedBorderColor = Color.LightGray,
                        focusedContainerColor = StitchColor.Surface,
                        unfocusedContainerColor = StitchColor.Surface
                    )
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { navController.navigate("jersey_grid") },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF37474F),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FormatListNumbered, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.roster_btn_numbers), fontSize = 12.sp, maxLines = 1)
                    }

                    Button(
                        onClick = { navController.navigate("player_leaderboard") },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StitchColor.Primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.roster_btn_stats), fontSize = 12.sp, maxLines = 1)
                    }
                }
            }

            if (filteredPlayers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "'$searchQuery' ${stringResource(R.string.roster_not_found)}" else stringResource(R.string.roster_empty),
                        color = Color.Gray
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredPlayers) { player ->
                        PlayerGridCard(navController, player)
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}
@Composable
private fun PlayerGridCard(
    navController: NavController,
    player: Player
) {
    // Pozisyona göre renk
    val posColor = when (player.position) {
        "Handler" -> com.eyuphanaydin.discbase.ui.theme.StitchPrimary // Mor
        "Cutter" -> StitchOffense // Yeşil/Teal
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f) // Dikey kart oranı
            .clickable { navController.navigate("player_detail/${player.id}") },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // --- SAĞ ÜST KÖŞE: SADECE KAPTANLIK SİMGESİ ("C") ---
            // Numara buradan kaldırıldı, artık avatarın içinde yazacak.
            if (player.isCaptain) {
                Surface(
                    color = Color(0xFFFFC107), // Altın Sarısı
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "C",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // --- ORTA: AVATAR ve İSİM ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // AVATAR (Büyük ve Yuvarlak)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .border(2.dp, posColor, CircleShape)
                        .padding(4.dp)
                ) {
                    // DEĞİŞİKLİK BURADA:
                    // jerseyNumber parametresini 'null' yerine 'player.jerseyNumber' olarak gönderiyoruz.
                    // Böylece numara varsa "10", yoksa "EA" yazacak.
                    PlayerAvatar(
                        name = player.name,
                        jerseyNumber = player.jerseyNumber,
                        photoUrl = player.photoUrl, // <-- BU SATIR EKSİKTİ
                        size = 64.dp,
                        fontSize = 22.sp)
                }

                Spacer(Modifier.height(12.dp))

                // İSİM
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = StitchColor.TextPrimary
                )

                Spacer(Modifier.height(8.dp))

                // POZİSYON ETİKETİ
                Surface(
                    color = posColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, posColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = player.position,
                        color = posColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// --- 3. FORMA NUMARALARI EKRANI ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JerseyGridScreen(
    navController: NavController,
    allPlayers: List<Player>
) {
    val jerseyMap = remember(allPlayers) {
        allPlayers.filter { it.jerseyNumber != null }.associateBy { it.jerseyNumber!! }
    }

    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                // GÜNCELLENDİ
                title = { Text(stringResource(R.string.jersey_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    ModernIconButton(
                        icon = Icons.Default.ArrowBack,
                        onClick = { navController.popBackStack() },
                        color = StitchColor.TextPrimary,
                        contentDescription = stringResource(R.string.desc_back)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            // Bilgi Çubuğu (Legend)
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(16.dp).background(MaterialTheme.colorScheme.errorContainer, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.jersey_taken), fontSize = 12.sp) // Dolu
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(16.dp).background(Color(0xFFE8F5E9), CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.jersey_empty), fontSize = 12.sp) // Boş
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 60.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(100) { number ->
                    val owner = jerseyMap[number]
                    val isTaken = owner != null

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isTaken) MaterialTheme.colorScheme.errorContainer else Color(0xFFE8F5E9)
                        ),
                        modifier = Modifier.height(60.dp),
                        onClick = { if (isTaken) navController.navigate("player_detail/${owner!!.id}") }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "$number",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (isTaken) MaterialTheme.colorScheme.onErrorContainer else Color.Black
                            )
                            if (isTaken) {
                                Text(
                                    owner!!.name.split(" ").first().take(5),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 4. OYUNCU EKLEME EKRANI ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerAddScreen(
    navController: NavController,
    onPlayerSave: (Player) -> Unit
) {
    var newPlayerName by remember { mutableStateOf("") }
    var newPlayerGender by remember { mutableStateOf("Erkek") }
    var newPlayerPosition by remember { mutableStateOf("Cutter") }
    val context = LocalContext.current

    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.title_add_player), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    ModernIconButton(
                        Icons.Default.ArrowBack,
                        { navController.popBackStack() },
                        StitchTextPrimary,
                        stringResource(R.string.desc_back)
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp) // Geniş aralıklar
        ) {
            // Avatar Önizleme (Dekoratif)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier.size(100.dp)
                        .background(com.eyuphanaydin.discbase.ui.theme.StitchPrimary.copy(0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        null,
                        modifier = Modifier.size(50.dp),
                        tint = StitchColor.Primary
                    )
                }
            }

            // Form Kartı
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    OutlinedTextField(
                        value = newPlayerName,
                        onValueChange = { newPlayerName = it },
                        label = { Text(stringResource(R.string.label_fullname)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Divider()

                    // Cinsiyet
                    Text(
                        stringResource(R.string.label_gender),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    GenderSelector(
                        selectedGender = newPlayerGender,
                        onGenderSelect = { newPlayerGender = it })

                    // Pozisyon
                    Text(
                        stringResource(R.string.label_position),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    PositionSelector(
                        selectedPosition = newPlayerPosition,
                        onPositionSelect = { newPlayerPosition = it })
                }
            }

            Spacer(Modifier.weight(1f))

            // Kaydet Butonu
            Button(
                onClick = {
                    if (newPlayerName.isNotBlank()) {
                        onPlayerSave(
                            Player(
                                id = UUID.randomUUID().toString(),
                                name = newPlayerName,
                                gender = newPlayerGender,
                                position = newPlayerPosition,
                                isCaptain = false
                            )
                        )
                    } else {
                        Toast.makeText(context, context.getString(R.string.error_name_required), Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StitchColor.Primary)
            ) {
                Text(stringResource(R.string.btn_add_player_caps), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
// --- 5. ANTRENMAN EKRANI ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    navController: NavController,
    trainings: List<Training>,
    allPlayers: List<Player>,
    isAdmin: Boolean,
    onSaveTraining: (Training) -> Unit,
    onDeleteTraining: (Training) -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf(stringResource(R.string.tab_calendar), stringResource(R.string.tab_attendance))
    val context = LocalContext.current
    val viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val calendar = Calendar.getInstance()
    var selectedMonthIndex by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }

    // GÜNCELLENDİ: Aylar resource'dan geliyor
    val months = listOf(
        stringResource(R.string.month_jan), stringResource(R.string.month_feb),
        stringResource(R.string.month_mar), stringResource(R.string.month_apr),
        stringResource(R.string.month_may), stringResource(R.string.month_jun),
        stringResource(R.string.month_jul), stringResource(R.string.month_aug),
        stringResource(R.string.month_sep), stringResource(R.string.month_oct),
        stringResource(R.string.month_nov), stringResource(R.string.month_dec)
    )

    var trainingToEdit by remember { mutableStateOf<Training?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.training_title), fontWeight = FontWeight.Bold) },
                actions = {
                    ModernIconButton(
                        icon = Icons.Default.Person,
                        onClick = { navController.navigate("profile_edit") },
                        color = com.eyuphanaydin.discbase.ui.theme.StitchPrimary,
                        contentDescription = stringResource(R.string.desc_profile)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            if (isAdmin && selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = { isCreatingNew = true },
                    containerColor = StitchColor.Primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.btn_add))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = StitchColor.Surface,
                contentColor = com.eyuphanaydin.discbase.ui.theme.StitchPrimary,
                indicator = { tabPositions ->
                    androidx.compose.material3.TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = com.eyuphanaydin.discbase.ui.theme.StitchPrimary,
                        height = 3.dp
                    )
                }) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) })
                }
            }

            if (selectedTabIndex == 0) {
                ScrollableTabRow(
                    selectedTabIndex = selectedMonthIndex,
                    containerColor = StitchColor.Background,
                    contentColor = Color.Gray,
                    edgePadding = 16.dp,
                    indicator = {},
                    divider = {}) {
                    months.forEachIndexed { index, monthName ->
                        val isSelected = index == selectedMonthIndex;
                        val textColor = if (isSelected) com.eyuphanaydin.discbase.ui.theme.StitchPrimary else Color.Gray;
                        val bgColor = if (isSelected) com.eyuphanaydin.discbase.ui.theme.StitchPrimary.copy(0.1f) else Color.Transparent;
                        Tab(
                            selected = isSelected,
                            onClick = { selectedMonthIndex = index },
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                .clip(RoundedCornerShape(50)).background(bgColor).height(36.dp)
                        ) {
                            Text(
                                text = monthName,
                                color = textColor,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }

                val filteredTrainings = trainings.filter { training ->
                    val parts = training.date.split("/")
                    val isMonthMatch = if (parts.size == 3) parts[1].toIntOrNull() == (selectedMonthIndex + 1) else false
                    if (isAdmin) {
                        isMonthMatch
                    } else {
                        isMonthMatch && training.isVisibleToMembers
                    }
                }.sortedByDescending { it.date }

                if (filteredTrainings.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("${months[selectedMonthIndex]} ${stringResource(R.string.training_empty_month)}", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredTrainings) { training ->
                            ExpandableTrainingCard(
                                training = training,
                                allPlayers = allPlayers,
                                isAdmin = isAdmin,
                                onDelete = { onDeleteTraining(training) },
                                onEdit = { trainingToEdit = training },
                                onShare = { viewModel.shareTrainingReport(context, training) }
                            )
                        }
                    }
                }
            } else {
                AttendanceReportList(allPlayers, trainings)
            }
        }
    }

    if (isCreatingNew || trainingToEdit != null) {
        TrainingAddDialog(
            trainingToEdit = trainingToEdit,
            onDismiss = {
                isCreatingNew = false
                trainingToEdit = null
            },
            onSave = {
                onSaveTraining(it)
                isCreatingNew = false
                trainingToEdit = null
            },
            allPlayers = allPlayers
        )
    }
}

@Composable
fun ExpandableTrainingCard(
    training: Training,
    allPlayers: List<Player>,
    isAdmin: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val attendeeCount = training.attendeeIds.size
    val attendees = allPlayers.filter { training.attendeeIds.contains(it.id) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = com.eyuphanaydin.discbase.ui.theme.StitchPrimary.copy(0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(50.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val day = training.date.split("/").getOrNull(0) ?: "?"
                            Text(day, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = com.eyuphanaydin.discbase.ui.theme.StitchPrimary)
                        }
                    }
                    Spacer(Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(training.date, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (!training.isVisibleToMembers) {
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = stringResource(R.string.training_hidden),
                                    tint = Color.Red.copy(0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(training.location.ifBlank { "Konum Yok" }, fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Surface(color = StitchOffense.copy(0.1f), shape = RoundedCornerShape(50)) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Groups, null, tint = StitchOffense, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("$attendeeCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StitchOffense)
                    }
                }
            }

            if (expanded) {
                Divider(Modifier.padding(vertical = 12.dp))
                if (!training.isVisibleToMembers) {
                    Text(
                        stringResource(R.string.training_hidden_warning),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                if (training.time.isNotBlank()) {
                    Text("${stringResource(R.string.training_time_prefix)} ${training.time}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                }
                if (training.description.isNotBlank()) {
                    Text(stringResource(R.string.training_notes_label), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text(training.description, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                }

                Text(stringResource(R.string.training_attendees_label), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                if (attendees.isEmpty()) Text("-", fontSize = 14.sp)
                else Text(attendees.joinToString(", ") { it.name }, fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // RAPOR BUTONU (Herkes görebilsin diye buraya koyduk)
                    Button(
                        onClick = onShare,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF607D8B)), // Gri-Mavi tonu (Rapor rengi)
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_report))
                    }

                    // Eğer Admin değilse yan taraf boş kalmasın diye Spacer atabiliriz veya buton tüm genişliği kaplasın diyorsan weight'i ayarlayabilirsin.
                    // Şimdilik Admin değilse tek buton tüm genişliği kaplasın diye weight kullanıyoruz.
                }
                if (isAdmin) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onEdit,
                            colors = ButtonDefaults.buttonColors(containerColor = StitchColor.Primary),
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_edit))
                        }
                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(containerColor = com.eyuphanaydin.discbase.ui.theme.StitchDefense.copy(0.1f), contentColor = com.eyuphanaydin.discbase.ui.theme.StitchDefense),
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, com.eyuphanaydin.discbase.ui.theme.StitchDefense)
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_delete))
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun AttendanceReportList(
    allPlayers: List<Player>,
    trainings: List<Training>
) {
    // Seçili oyuncuyu tutmak için state
    val context = LocalContext.current
    val viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    // Seçili oyuncuyu tutmak için state
    var selectedPlayerForDetail by remember { mutableStateOf<Player?>(null) }

    val attendanceMap = remember(allPlayers, trainings) {
        allPlayers.associateWith { player ->
            trainings.count { it.attendeeIds.contains(player.id) }
        }.toList().sortedByDescending { it.second }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
        Button(
            onClick = {
                viewModel.exportAttendanceToCSV(context, allPlayers, trainings)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)), // Excel Yeşili
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.btn_export_excel), color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
        item {
            Text(
                "${stringResource(R.string.sum_training)}: ${trainings.size}",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp),
                color = StitchColor.TextPrimary
            )
        }

        items(attendanceMap) { (player, count) ->
            val percentage =
                if (trainings.isNotEmpty()) (count.toFloat() / trainings.size) else 0f

            Card(
                colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                // TIKLAMA ÖZELLİĞİ EKLENDİ
                onClick = { selectedPlayerForDetail = player }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerAvatar(
                        name = player.name,
                        jerseyNumber = player.jerseyNumber,
                        size = 48.dp
                    )
                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                player.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = StitchColor.TextPrimary
                            )
                            Text(
                                "$count / ${trainings.size}",
                                fontWeight = FontWeight.Bold,
                                color = if (percentage > 0.7) StitchOffense else if (percentage > 0.4) com.eyuphanaydin.discbase.ui.theme.StitchPrimary else com.eyuphanaydin.discbase.ui.theme.StitchDefense
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = percentage,
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = if (percentage > 0.7f) StitchOffense else if (percentage > 0.4f) com.eyuphanaydin.discbase.ui.theme.StitchPrimary else com.eyuphanaydin.discbase.ui.theme.StitchDefense,
                            trackColor = Color(0xFFF0F0F0)
                        )
                    }

                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
                }
            }
        }
    }

    // DETAY PENCERESİ (DIALOG)
    if (selectedPlayerForDetail != null) {
        PlayerAttendanceDetailDialog(
            player = selectedPlayerForDetail!!,
            allTrainings = trainings,
            onDismiss = { selectedPlayerForDetail = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingAddDialog(
    trainingToEdit: Training?,
    onDismiss: () -> Unit,
    onSave: (Training) -> Unit,
    allPlayers: List<Player>
) {
    var date by remember { mutableStateOf(trainingToEdit?.date ?: "") }
    var time by remember { mutableStateOf(trainingToEdit?.time ?: "") }
    var location by remember { mutableStateOf(trainingToEdit?.location ?: "") }
    var description by remember { mutableStateOf(trainingToEdit?.description ?: "") }
    var selectedAttendees by remember { mutableStateOf(trainingToEdit?.attendeeIds?.toSet() ?: emptySet()) }

    var isVisibleToMembers by remember(trainingToEdit) {
        mutableStateOf(trainingToEdit?.isVisibleToMembers ?: true)
    }
    var isDetailsExpanded by remember { mutableStateOf(trainingToEdit == null) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val isEditing = trainingToEdit != null

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day -> date = "$day/${month + 1}/$year" },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute -> time = String.format("%02d:%02d", hour, minute) },
        calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // BAŞLIK
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isEditing) Icons.Default.Edit else Icons.Default.AddCircle, null, tint = StitchColor.Primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    // GÜNCELLENDİ
                    Text(
                        if (isEditing) stringResource(R.string.title_edit_training) else stringResource(R.string.title_new_training),
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold
                    )
                }

                Divider()

                // --- 1. GENİŞLETİLEBİLİR DETAYLAR KARTI ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().animateContentSize()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isDetailsExpanded = !isDetailsExpanded }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.training_details_header), fontWeight = FontWeight.Bold, color = StitchColor.TextPrimary)
                            Icon(
                                if (isDetailsExpanded) Icons.Default.ExpandLess else Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }

                        if (isDetailsExpanded) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)).padding(8.dp)
                                ) {
                                    Icon(if(isVisibleToMembers) Icons.Default.Groups else Icons.Default.VisibilityOff, null, tint = if(isVisibleToMembers) StitchColor.Primary else Color.Gray)
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(stringResource(R.string.training_visibility_label), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(if(isVisibleToMembers) stringResource(R.string.training_visibility_public) else stringResource(R.string.training_visibility_private), fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Switch(
                                        checked = isVisibleToMembers,
                                        onCheckedChange = { isVisibleToMembers = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = StitchColor.Primary)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = date, onValueChange = {}, readOnly = true, label = { Text(stringResource(R.string.label_date)) },
                                        modifier = Modifier.weight(1f).clickable { datePickerDialog.show() }, enabled = false,
                                        trailingIcon = { Icon(Icons.Default.Event, null) },
                                        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = Color.Gray, disabledLabelColor = Color.Black)
                                    )
                                    OutlinedTextField(
                                        value = time, onValueChange = {}, readOnly = true, label = { Text(stringResource(R.string.label_time)) },
                                        modifier = Modifier.weight(1f).clickable { timePickerDialog.show() }, enabled = false,
                                        trailingIcon = { Icon(Icons.Default.AccessTime, null) },
                                        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = Color.Gray, disabledLabelColor = Color.Black)
                                    )
                                }

                                OutlinedTextField(
                                    value = location, onValueChange = { location = it }, label = { Text(stringResource(R.string.label_location)) },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true
                                )

                                OutlinedTextField(
                                    value = description, onValueChange = { description = it }, label = { Text(stringResource(R.string.label_notes)) },
                                    modifier = Modifier.fillMaxWidth().height(300.dp)
                                )
                            }
                        }
                    }
                }

                // --- 2. KATILIMCI LİSTESİ ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = StitchColor.Background),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.training_attendance_list), fontWeight = FontWeight.Bold)
                            // GÜNCELLENDİ: Suffix
                            Text("${selectedAttendees.size} ${stringResource(R.string.suffix_person)}", color = StitchColor.Primary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))

                        LazyColumn {
                            items(allPlayers.sortedBy { it.name }) { player ->
                                val isSelected = selectedAttendees.contains(player.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedAttendees = if (isSelected) selectedAttendees - player.id else selectedAttendees + player.id
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = StitchColor.Primary)
                                    )
                                    Text(player.name, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (date.isNotBlank()) {
                        val updatedTraining = Training(
                            id = trainingToEdit?.id ?: UUID.randomUUID().toString(),
                            date = date,
                            time = time,
                            location = location,
                            description = description,
                            attendeeIds = selectedAttendees.toList(),
                            isVisibleToMembers = isVisibleToMembers
                        )
                        onSave(updatedTraining)
                        onDismiss()
                    } else {
                        Toast.makeText(context, context.getString(R.string.error_date_required), Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StitchColor.Primary),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isEditing) stringResource(R.string.btn_save) else stringResource(R.string.btn_create), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.btn_cancel), color = Color.Gray) }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = StitchColor.Surface
    )
}
// --- 6. İSTATİSTİK EKRANLARI (LEADERBOARD & TEAM STATS) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamStatisticsScreen(
    navController: NavController,
    teamProfile: TeamProfile,
    tournaments: List<Tournament>
) {
    var showDropdown by remember { mutableStateOf(false) }
    var selectedTournamentId by remember { mutableStateOf("GENEL") }

    // Filtreleme Mantığı
    val selectedTournament = tournaments.find { it.id == selectedTournamentId }
    val selectedTournamentName =
        if (selectedTournamentId == "GENEL") stringResource(R.string.label_general_stats) else selectedTournament?.tournamentName
            ?: stringResource(R.string.label_general_stats)
    val advancedStats = calculateTeamStatsForFilter(tournaments, selectedTournamentId)

    // İstatistik Hesaplamaları
    val holdRate =
        calculateSafePercentage(advancedStats.offensiveHolds, advancedStats.totalOffensePoints)
    val breakRate =
        calculateSafePercentage(
            advancedStats.breakPointsScored,
            advancedStats.totalDefensePoints
        )
    val passRate = calculateSafePercentage(
        advancedStats.totalPassesCompleted,
        advancedStats.totalPassesAttempted
    )
    val conversionRate =
        calculateSafePercentage(advancedStats.totalGoals, advancedStats.totalPossessions)
    val blockConversionRate = calculateSafePercentage(
        advancedStats.blocksConvertedToGoals,
        advancedStats.totalBlockPoints
    )
    val cleanHoldRate =
        calculateSafePercentage(advancedStats.cleanHolds, advancedStats.totalOffensePoints)
    val totalTurnovers = advancedStats.totalThrowaways + advancedStats.totalDrops

    val avgTurnoverPerPoint = if (advancedStats.totalPointsPlayed > 0)
        String.format("%.2f", totalTurnovers.toDouble() / advancedStats.totalPointsPlayed) else "0.0"
    val avgTurnoverPerMatch = if (advancedStats.totalMatchesPlayed > 0) String.format("%.1f", totalTurnovers.toDouble() / advancedStats.totalMatchesPlayed) else "0.0"

    // Filtre Dialogu
    if (showDropdown) {
        AlertDialog(
            onDismissRequest = { showDropdown = false },
            title = { Text(stringResource(R.string.label_select_filter)) },
            text = {
                LazyColumn {
                    item {
                        Text(
                            stringResource(R.string.label_general_stats),
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    selectedTournamentId = "GENEL"; showDropdown = false
                                }
                                .padding(12.dp)
                        )
                    }
                    items(tournaments) { t ->
                        Text(
                            t.tournamentName,
                            modifier = Modifier.fillMaxWidth()
                                .clickable { selectedTournamentId = t.id; showDropdown = false }
                                .padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
    // Önce sayısal değerleri hesaplayın
    val avgTempoValue = if (advancedStats.totalPassesAttempted > 0)
        advancedStats.totalTempoSeconds.toDouble() / advancedStats.totalPassesAttempted
    else 0.0

    val avgPullTimeValue = if (advancedStats.totalPulls > 0)
        advancedStats.totalPullTimeSeconds.toDouble() / advancedStats.totalPulls
    else 0.0

// Compose içinde dile göre formatlanmış stringleri alın
    val teamAvgTempo = stringResource(
        id = R.string.label_sec_yanına_değer,
        String.format("%.2f", avgTempoValue)
    )

    val teamAvgPullTime = stringResource(
        id = R.string.label_sec_yanına_değer,
        String.format("%.2f", avgPullTimeValue)
    )

    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_stats_center), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    ModernIconButton(
                        icon = Icons.Default.ArrowBack,
                        onClick = { navController.popBackStack() },
                        color = StitchColor.TextPrimary,
                        contentDescription = stringResource(R.string.desc_back)
                    )
                },
                actions = {
                    // Filtre Butonu (Header'daki gibi)
                    Surface(
                        onClick = { showDropdown = true },
                        shape = RoundedCornerShape(50),
                        color = com.eyuphanaydin.discbase.ui.theme.StitchPrimary.copy(alpha = 0.1f),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                selectedTournamentName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = com.eyuphanaydin.discbase.ui.theme.StitchPrimary
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                null,
                                tint = StitchColor.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Performans Kartı (Hold/Break)
            PerformanceCard(holdRate, breakRate, passRate)

            // 2. Verimlilik Kartı (Possession)
            PossessionCard(conversionRate, blockConversionRate, cleanHoldRate)

            // 3. Detaylı Analiz Kartı
            DetailedStatsCard(
                totalPasses = advancedStats.totalPassesAttempted.toString(),
                totalCompleted = advancedStats.totalPassesCompleted.toString(),
                totalTurnovers = totalTurnovers.toString(),
                avgTurnoverPoint = avgTurnoverPerPoint,
                avgTurnoverMatch = avgTurnoverPerMatch,
                totalPointsPlayed = advancedStats.totalPointsPlayed.toString(),
                matchesPlayed = advancedStats.totalMatchesPlayed.toString(),
                teamAvgTempo = teamAvgTempo,
                teamAvgPullTime = teamAvgPullTime
            )
        }
    }
}
// Yardımcı Kartlar (Performance, Possession, Detailed)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceCard(
    holdRate: StatPercentage,
    breakRate: StatPercentage,
    passRate: StatPercentage
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.header_team_performance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StitchColor.TextPrimary
            )
            Spacer(Modifier.height(16.dp))

            // Hold Percentage
            PerformanceStatRow(
                title = stringResource(R.string.stat_hold_percentage),
                percentage = holdRate.text,
                ratio = holdRate.ratio,
                progress = holdRate.progress,
                progressColor = Color(0xFF03DAC5) // Teal
            )

            // Break Percentage
            PerformanceStatRow(
                title = stringResource(R.string.stat_break_percentage),
                percentage = breakRate.text,
                ratio = breakRate.ratio,
                progress = breakRate.progress,
                progressColor = Color(0xFFFF9800) // Turuncu
            )

            // Completion Rate
            PerformanceStatRow(
                title = stringResource(R.string.stat_pass_success),
                percentage = passRate.text,
                ratio = passRate.ratio,
                progress = passRate.progress,
                progressColor = com.eyuphanaydin.discbase.ui.theme.StitchPrimary // Mor
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PossessionCard(
    conversionRate: StatPercentage,
    blockConversionRate: StatPercentage,
    cleanHoldRate: StatPercentage // <-- YENİ PARAMETRE EKLENDİ
) {
    // Renk Mantığı: %70 üzeri yeşil, %50 üzeri turuncu, altı kırmızı
    val conversionColor = if (conversionRate.progress >= 0.7f) Color(0xFF4CAF50)
    else if (conversionRate.progress >= 0.5f) Color(0xFFFF9800)
    else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.header_efficiency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StitchColor.TextPrimary
            )
            Spacer(Modifier.height(16.dp))

            // Hata oranı kalktı, Conversion Rate renklendi
            PerformanceStatRow(
                title = "Conversion Rate",
                percentage = conversionRate.text,
                ratio = conversionRate.ratio + stringResource(R.string.suffix_goal_converted),
                progress = conversionRate.progress,
                progressColor = conversionColor // <-- DİNAMİK RENK
            )
            PerformanceStatRow(
                title = stringResource(R.string.stat_clean_holds),
                percentage = cleanHoldRate.text,
                ratio = cleanHoldRate.ratio + stringResource(R.string.suffix_clean_hold),
                progress = cleanHoldRate.progress,
                progressColor = Color(0xFF00B0FF) // Açık Mavi
            )

            // Blok sonrası gol şansı
            PerformanceStatRow(
                title = stringResource(R.string.stat_d_line_conversion),
                percentage = blockConversionRate.text,
                ratio = blockConversionRate.ratio + stringResource(R.string.suffix_block_converted),
                progress = blockConversionRate.progress
            )
        }
    }
}

@Composable
fun DetailedStatsCard(
    totalPasses: String,
    totalCompleted: String,
    totalTurnovers: String,
    avgTurnoverPoint: String,
    avgTurnoverMatch: String,
    totalPointsPlayed: String,
    matchesPlayed: String,
    teamAvgTempo: String,
    teamAvgPullTime: String
) {
    val myFontSize = 18.sp
    // Şık bir başlık ve 2 satırlı Grid yapısı
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface), // <-- DİNAMİK RENK
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                stringResource(R.string.stats_detailed),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StitchColor.TextPrimary // <-- BURADA HATA ALMAMALISINIZ
            )
            Spacer(Modifier.height(16.dp))

            // 1. Satır: Paslar
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StitchStatBox(stringResource(R.string.stats_pass_attempt), totalPasses, StitchSecondary, Modifier.weight(1f),
                    valueFontSize = myFontSize)
                StitchStatBox(
                    stringResource(R.string.stats_pass_completed),
                    totalCompleted,
                    StitchOffense,
                    Modifier.weight(1f),
                    valueFontSize = myFontSize
                )
                StitchStatBox(
                    stringResource(R.string.stats_avg_tempo),
                    teamAvgTempo,
                    Color(0xFFFFA000), // Amber rengi
                    Modifier.weight(1f),
                    valueFontSize = 14.sp
                )
            }
            Spacer(Modifier.height(12.dp))

            // 2. Satır: Hatalar
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StitchStatBox(stringResource(R.string.stats_turnover), totalTurnovers,
                    com.eyuphanaydin.discbase.ui.theme.StitchDefense, Modifier.weight(1f))
                StitchStatBox(
                    stringResource(R.string.stats_turnover_per_match),
                    avgTurnoverMatch,
                    com.eyuphanaydin.discbase.ui.theme.StitchDefense.copy(alpha = 0.8f),
                    Modifier.weight(1f),
                    valueFontSize = myFontSize
                )
                StitchStatBox(
                    stringResource(R.string.stats_avg_pull),
                    teamAvgPullTime,
                    Color(0xFF795548), // Kahverengi tonu
                    Modifier.weight(1f),
                    valueFontSize = 14.sp
                )
            }
            Spacer(Modifier.height(12.dp))

            // 3. Satır: Oyun Süresi
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StitchStatBox(
                    stringResource(R.string.stats_points_played),
                    totalPointsPlayed,
                    Color.Gray,
                    Modifier.weight(1f),
                    valueFontSize = myFontSize
                )
                StitchStatBox(stringResource(R.string.nav_match), matchesPlayed, Color.Gray, Modifier.weight(1f))
            }
        }
    }
}
// PlayerLeaderboardScreen ve SeasonMatchesScreen de buraya eklenebilir.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonMatchesScreen(
    navController: NavController,
    tournaments: List<Tournament>
) {
    val allMatches = remember(tournaments) {
        tournaments.flatMap { tournament ->
            tournament.matches.map { match ->
                MatchDisplayData(
                    match = match,
                    tournamentName = tournament.tournamentName,
                    tournamentId = tournament.id,
                    ourTeamName = tournament.ourTeamName
                )
            }
        }.reversed()
    }

    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.title_season_fixtures), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    ModernIconButton(
                        Icons.Default.ArrowBack,
                        { navController.popBackStack() },
                        StitchTextPrimary,
                        stringResource(R.string.desc_back)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        if (allMatches.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.msg_no_matches_played), color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(allMatches) { item ->
                    // Maç Kartı
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(16.dp))
                            .clickable { navController.navigate("match_detail/${item.tournamentId}/${item.match.id}") },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Turnuva Adı (Etiket)
                            Text(
                                text = item.tournamentName,
                                fontSize = 11.sp,
                                color = com.eyuphanaydin.discbase.ui.theme.StitchPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Skor Alanı
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    item.ourTeamName.ifBlank { stringResource(R.string.label_us) }.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.match.scoreUs > item.match.scoreThem) StitchOffense else Color.Gray,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                // Skor
                                Surface(
                                    color = StitchBackground,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        "${item.match.scoreUs} - ${item.match.scoreThem}",
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        ),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp
                                    )
                                }

                                // Rakip
                                Text(
                                    item.match.opponentName,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.match.scoreThem > item.match.scoreUs) com.eyuphanaydin.discbase.ui.theme.StitchDefense else Color.Gray,
                                    textAlign = TextAlign.End,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Screens_AppContent.kt içindeki PlayerLeaderboardScreen fonksiyonunu bununla değiştir:

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerLeaderboardScreen(
    navController: NavController,
    allPlayers: List<Player>,
    tournaments: List<Tournament>,
    viewModel: MainViewModel
) {
    var showEfficiencyInfo by remember { mutableStateOf(false) }
    var showTempoInfo by remember { mutableStateOf(false) }

    // 1. KRİTERLERİ DİNLEME (Canlı Güncelleme İçin)
    val efficiencyCriteria by viewModel.efficiencyCriteria.collectAsState()

    // 2. DİALOGA KRİTERLERİ GÖNDERME (Bilgi ekranında doğru görünmesi için)
    if (showEfficiencyInfo) {
        EfficiencyDescriptionDialog(
            onDismiss = { showEfficiencyInfo = false },
            criteria = efficiencyCriteria // <--- EKLENDİ
        )
    }
    if (showTempoInfo) TempoDescriptionDialog(onDismiss = { showTempoInfo = false })

    // --- STATE ---
    var selectedStatType by remember { mutableStateOf(StatType.GOAL) }
    var calculationMode by remember { mutableStateOf(CalculationMode.TOTAL) }

    var showTournamentDropdown by remember { mutableStateOf(false) }
    var showMatchDropdown by remember { mutableStateOf(false) }

    // Filtreler
    var selectedTournamentId by remember { mutableStateOf("GENEL") }
    var selectedMatchId by remember { mutableStateOf<String?>(null) }

    val selectedTournament = tournaments.find { it.id == selectedTournamentId }
    val selectedTournamentName = selectedTournament?.tournamentName ?: stringResource(R.string.label_all_season)
    val selectedMatchName = remember(selectedTournament, selectedMatchId) {
        if (selectedMatchId == null) "All Matches"
        else selectedTournament?.matches?.find { it.id == selectedMatchId }?.let { "vs ${it.opponentName}" } ?: "Bilinmeyen Maç"
    }
    val selectedMatchNameDisplay = if (selectedMatchId == null) stringResource(R.string.label_all_matches) else selectedMatchName

    // --- HESAPLAMA (GÜNCELLENEN KISIM) ---
    val rankedPlayers = remember(
        allPlayers, selectedStatType, calculationMode,
        selectedTournamentId, selectedMatchId, tournaments,
        efficiencyCriteria // <--- REMEMBER KEY EKLENDİ (Kriter değişirse yeniden hesapla)
    ) {
        allPlayers.map { player ->
            // 3. HESAPLAMA FONKSİYONUNA KRİTERLERİ GÖNDERME
            val stats = calculateGlobalPlayerStats(
                playerId = player.id,
                filterTournamentId = selectedTournamentId,
                filterMatchId = selectedMatchId,
                allTournaments = tournaments,
                criteria = efficiencyCriteria // <--- KRİTİK: Utils.kt'ye iletiyoruz
            )

            // 2. Ham Değeri Al (Toplam Değer)
            val rawValue = when (selectedStatType) {
                StatType.GOAL -> stats.basicStats.goal.toDouble()
                StatType.ASSIST -> stats.basicStats.assist.toDouble()
                StatType.BLOCK -> stats.basicStats.block.toDouble()
                StatType.CALLAHAN -> stats.basicStats.callahan.toDouble()
                StatType.THROWAWAY -> stats.basicStats.throwaway.toDouble()
                StatType.DROP -> stats.basicStats.drop.toDouble()
                StatType.PLUS_MINUS -> stats.plusMinus
                StatType.PASS_COUNT -> (stats.basicStats.successfulPass + stats.basicStats.assist+ stats.basicStats.throwaway).toDouble()
                StatType.POINTS_PLAYED -> stats.basicStats.pointsPlayed.toDouble()
                StatType.PLAYTIME -> stats.basicStats.secondsPlayed.toDouble()
                StatType.CATCH_RATE -> {
                    val catches = stats.basicStats.catchStat + stats.basicStats.goal
                    val attempts = catches + stats.basicStats.drop
                    if (attempts > 0) (catches.toDouble() / attempts) * 100 else 0.0
                }
                StatType.PASS_RATE -> {
                    val completed = stats.basicStats.successfulPass + stats.basicStats.assist
                    val attempts = completed + stats.basicStats.throwaway
                    if (attempts > 0) (completed.toDouble() / attempts) * 100 else 0.0
                }
                StatType.TEMPO -> {
                    val totalThrows = stats.basicStats.successfulPass + stats.basicStats.assist + stats.basicStats.throwaway
                    if (totalThrows > 0) stats.basicStats.totalTempoSeconds.toDouble() / totalThrows else 0.0
                }
                StatType.AVG_PULL_TIME -> {
                    if (stats.basicStats.pullAttempts > 0) stats.basicStats.totalPullTimeSeconds / stats.basicStats.pullAttempts else 0.0
                }
                StatType.AVG_PLAYTIME -> {
                    if (stats.basicStats.pointsPlayed > 0) stats.basicStats.secondsPlayed.toDouble() / stats.basicStats.pointsPlayed else 0.0
                }
            }

            val isAlreadyAverage = listOf(
                StatType.CATCH_RATE, StatType.PASS_RATE, StatType.TEMPO,
                StatType.AVG_PULL_TIME, StatType.AVG_PLAYTIME
            ).contains(selectedStatType)

            val finalValue = if (isAlreadyAverage) {
                rawValue
            } else {
                when (calculationMode) {
                    CalculationMode.TOTAL -> rawValue
                    CalculationMode.PER_MATCH -> {
                        val matches = countMatchesPlayedByPlayer(player.id, selectedTournamentId, selectedMatchId, tournaments).toDouble()
                        if (matches > 0) rawValue / matches else 0.0
                    }
                    CalculationMode.PER_POINT -> {
                        val points = stats.basicStats.pointsPlayed.toDouble()
                        if (points > 0) {
                            if (selectedStatType == StatType.PLUS_MINUS) (rawValue / points) * 10.0 else rawValue / points
                        } else 0.0
                    }
                }
            }
            Triple(player, finalValue, stats)
        }.filter { it.second > 0.0 || (selectedStatType == StatType.PLUS_MINUS && it.second != 0.0) }
            .sortedByDescending { it.second }
    }

    // --- FİLTRE DİALOGLARI ---
    if (showTournamentDropdown) {
        AlertDialog(
            onDismissRequest = { showTournamentDropdown = false },
            title = { Text(stringResource(R.string.filter_tournament)) },
            text = {
                LazyColumn {
                    item {
                        Text(stringResource(R.string.label_all_season), modifier = Modifier.fillMaxWidth().clickable { selectedTournamentId = "GENEL"; selectedMatchId = null; showTournamentDropdown = false }.padding(12.dp))
                    }
                    items(tournaments) { t ->
                        Text(t.tournamentName, modifier = Modifier.fillMaxWidth().clickable { selectedTournamentId = t.id; selectedMatchId = null; showTournamentDropdown = false }.padding(12.dp))
                    }
                }
            }, confirmButton = {}
        )
    }

    if (showMatchDropdown && selectedTournament != null) {
        AlertDialog(
            onDismissRequest = { showMatchDropdown = false },
            title = { Text(stringResource(R.string.filter_match)) },
            text = {
                LazyColumn {
                    item {
                        Text(stringResource(R.string.label_all_matches), fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().clickable { selectedMatchId = null; showMatchDropdown = false }.padding(12.dp))
                    }
                    items(selectedTournament.matches) { match ->
                        Text("vs ${match.opponentName}", modifier = Modifier.fillMaxWidth().clickable { selectedMatchId = match.id; showMatchDropdown = false }.padding(12.dp))
                    }
                }
            }, confirmButton = {}
        )
    }

    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.title_stat_leaders), fontWeight = FontWeight.Bold) },
                navigationIcon = { ModernIconButton(Icons.Default.ArrowBack, { navController.popBackStack() }, StitchTextPrimary, stringResource(R.string.desc_back)) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            // 1. FİLTRE BUTONLARI (TURNUVA/MAÇ)
            Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterButton(text = selectedTournamentName, onClick = { showTournamentDropdown = true }, modifier = Modifier.weight(1f))
                if (selectedTournamentId != "GENEL") {
                    FilterButton(text = selectedMatchNameDisplay, onClick = { showMatchDropdown = true }, modifier = Modifier.weight(0.6f))
                }
            }

            Spacer(Modifier.height(12.dp))

            // 2. HESAPLAMA MODU SEÇİCİSİ
            val isSummable = !listOf(StatType.CATCH_RATE, StatType.PASS_RATE, StatType.TEMPO, StatType.AVG_PULL_TIME, StatType.AVG_PLAYTIME).contains(selectedStatType)

            if (isSummable) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(Color(0xFFE0E0E0), RoundedCornerShape(50)),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CalculationMode.values().forEach { mode ->
                        val isSelected = calculationMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) StitchColor.Primary else Color.Transparent)
                                .clickable { calculationMode = mode }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(mode.labelResId),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color.Gray
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // 3. İSTATİSTİK TİPİ SEÇİCİ
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(StatType.values()) { type ->
                    val isSelected = selectedStatType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedStatType = type },
                        label = { Text(stringResource(type.titleResId)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = StitchColor.Primary, selectedLabelColor = Color.White),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected, borderColor = Color.LightGray, selectedBorderColor = StitchColor.Primary)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // BAŞLIK VE BİLGİ
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                val modeText = if (isSummable) stringResource(calculationMode.labelResId) else stringResource(R.string.mode_average)

                Text(
                    text = "${stringResource(selectedStatType.titleResId)} ($modeText)",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray
                )
                if (selectedStatType == StatType.PLUS_MINUS) IconButton(onClick = { showEfficiencyInfo = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Info, null, tint = StitchColor.Primary) }
                if (selectedStatType == StatType.TEMPO) IconButton(onClick = { showTempoInfo = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Info, null, tint = StitchColor.Primary) }
            }

            Spacer(Modifier.height(8.dp))

            // 4. SIRALAMA LİSTESİ
            if (rankedPlayers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_data), color = Color.Gray) }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(rankedPlayers) { index, (player, value, stats) ->
                        LeaderboardItem(
                            rank = index + 1,
                            player = player,
                            value = value,
                            statType = selectedStatType,
                            navController = navController,
                            advancedStats = stats,
                            calculationMode = calculationMode
                        )
                    }
                }
            }
        }
    }
}

// Screens_AppContent.kt içindeki LeaderboardItem fonksiyonunu da güncelle:

@Composable
fun LeaderboardItem(
    rank: Int,
    player: Player,
    value: Double,
    statType: StatType,
    navController: NavController,
    advancedStats: AdvancedPlayerStats,
    calculationMode: CalculationMode
) {
    val rankColor = when (rank) { 1 -> Color(0xFFFFD700); 2 -> Color(0xFFC0C0C0); 3 -> Color(0xFFCD7F32); else -> Color.Gray }
    val rankBg = if (rank <= 3) rankColor.copy(0.2f) else Color.Transparent

    Card(
        modifier = Modifier.fillMaxWidth().clickable { navController.navigate("player_detail/${player.id}") },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(rankBg), contentAlignment = Alignment.Center) {
                Text("$rank", fontWeight = FontWeight.Bold, color = if (rank <= 3) Color.Black else Color.Gray)
            }
            Spacer(Modifier.width(12.dp))
            PlayerAvatar(name = player.name, jerseyNumber = player.jerseyNumber, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(player.name, fontWeight = FontWeight.Bold, color = StitchColor.TextPrimary)
                Text(player.position, fontSize = 12.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                // FORMATLAMA MANTIĞI
                val formattedValue = when {
                    // Süreler
                    statType == StatType.PLAYTIME && calculationMode == CalculationMode.TOTAL -> formatSecondsToTime(value.toLong())
                    statType == StatType.PLAYTIME -> formatSecondsToTime(value.toLong())
                    statType == StatType.AVG_PLAYTIME -> formatSecondsToTime(value.toLong())
                    statType == StatType.TEMPO || statType == StatType.AVG_PULL_TIME -> "${String.format("%.2f", value)} sn"
                    statType == StatType.CATCH_RATE || statType == StatType.PASS_RATE -> "${value.toInt()}%"
                    calculationMode != CalculationMode.TOTAL || statType == StatType.PLUS_MINUS -> String.format("%.2f", value)
                    else -> "${value.toInt()}"
                }

                Text(formattedValue, fontSize = 20.sp, fontWeight = FontWeight.Black, color = StitchColor.Primary)

                // DÜZELTME: statType.unit -> stringResource(statType.unitResId)
                val unitString = stringResource(statType.unitResId)

                val unitText = when(calculationMode) {
                    CalculationMode.PER_MATCH -> "$unitString${stringResource(R.string.unit_per_match)}"
                    CalculationMode.PER_POINT -> {
                        if (statType == StatType.PLUS_MINUS) stringResource(R.string.unit_efficiency_per_point)
                        else "$unitString${stringResource(R.string.unit_per_point)}"
                    }
                    else -> unitString
                }

                if (statType == StatType.POINTS_PLAYED) {
                    Text("O:${advancedStats.oPointsPlayed} D:${advancedStats.dPointsPlayed}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                } else {
                    Text(unitText, fontSize = 10.sp, color = Color.Gray)
                }
            }
        }
    }
}
@Composable
fun EfficiencyDescriptionDialog(
    onDismiss: () -> Unit,
    criteria: List<EfficiencyCriterion> = emptyList() // Varsayılan boş liste
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.efficiency_dialog_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.efficiency_dialog_desc),
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(16.dp))

                if (criteria.isEmpty()) {
                    // --- VARSAYILAN (STANDART) GÖRÜNÜM ---
                    // Sizin orijinal kodunuz burasıdır:

                    // CALLAHAN
                    EfficiencyRow(
                        icon = Icons.Default.Stars,
                        color = Color(0xFFFFC107),
                        title = stringResource(R.string.stat_callahan_points),
                        desc = stringResource(R.string.stat_callahan_desc)
                    )

                    // BLOK
                    EfficiencyRow(
                        icon = Icons.Default.Shield,
                        color = com.eyuphanaydin.discbase.ui.theme.StitchPrimary,
                        title = stringResource(R.string.stat_block_points),
                        desc = stringResource(R.string.stat_block_desc)
                    )

                    // GOL/ASİST
                    EfficiencyRow(
                        icon = Icons.Default.AddCircle,
                        color = StitchOffense,
                        title = stringResource(R.string.stat_goal_points),
                        desc = stringResource(R.string.stat_goal_assist_desc)
                    )

                    // PAS
                    EfficiencyRow(
                        icon = Icons.Default.TrendingUp,
                        color = Color.Gray,
                        title = stringResource(R.string.stat_pass_points),
                        desc = stringResource(R.string.stat_pass_desc)
                    )

                    // HATA
                    EfficiencyRow(
                        icon = Icons.Default.TrendingDown,
                        color = com.eyuphanaydin.discbase.ui.theme.StitchDefense,
                        title = stringResource(R.string.stat_turnover_points),
                        desc = stringResource(R.string.stat_turnover_desc)
                    )

                } else {
                    // --- DİNAMİK (KAPTAN AYARLI) GÖRÜNÜM ---
                    Text(
                        text = "Takım Özel Puanlaması", // İsterseniz strings.xml'e ekleyin
                        fontWeight = FontWeight.Bold,
                        color = StitchColor.TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Divider(modifier = Modifier.padding(bottom = 8.dp))

                    criteria.forEach { item ->
                        // İstatistik tipine göre ikon ve renk belirleme
                        val (icon, color) = when (item.statType) {
                            "CALLAHAN" -> Icons.Default.Stars to Color(0xFFFFC107)
                            "BLOCK" -> Icons.Default.Shield to com.eyuphanaydin.discbase.ui.theme.StitchPrimary
                            "GOAL", "ASSIST" -> Icons.Default.AddCircle to StitchOffense
                            "THROWAWAY", "DROP" -> Icons.Default.TrendingDown to com.eyuphanaydin.discbase.ui.theme.StitchDefense
                            "PASS_COUNT" -> Icons.Default.TrendingUp to Color.Gray
                            else -> Icons.Default.Circle to Color.Gray
                        }

                        // Puan formatı (Örn: +1.5 veya -1.0)
                        val sign = if (item.points > 0) "+" else ""
                        val pointsText = "$sign${item.points} Puan" // "Puan" kelimesini stringResource yapabilirsiniz

                        EfficiencyRow(
                            icon = icon,
                            color = color,
                            title = "${item.name} ($pointsText)",
                            desc = "" // Dinamik kısımda açıklama boş geçilebilir veya statType yazılabilir
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = StitchColor.Primary)) {
                Text(stringResource(R.string.btn_understood))
            }
        },
        containerColor = StitchColor.Surface
    )
}

// Kod tekrarını önlemek için yardımcı bir Composable
@Composable
private fun EfficiencyRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    title: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
            if (desc.isNotEmpty()) {
                Text(desc, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}
@Composable
fun TempoDescriptionDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_tempo_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.dialog_tempo_desc),
                    fontSize = 14.sp
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = StitchColor.Background),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.label_formula), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            stringResource(R.string.dialog_tempo_formula),
                            fontStyle = FontStyle.Italic, fontSize = 14.sp, color = StitchColor.Primary
                        )
                    }
                }

                Text(stringResource(R.string.label_interpretation), fontWeight = FontWeight.Bold)

                Row(verticalAlignment = Alignment.Top) {
                    Text("🚀", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(stringResource(R.string.tempo_fast_title), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(stringResource(R.string.tempo_fast_desc), fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Row(verticalAlignment = Alignment.Top) {
                    Text("🐢", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(stringResource(R.string.tempo_slow_title), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(stringResource(R.string.tempo_slow_desc), fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.btn_understood)) } },
        containerColor = StitchColor.Surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerEditScreen(
    navController: NavController,
    player: Player,
    allPlayers: List<Player>,
    allTournaments: List<Tournament>,
    onPlayerUpdate: (Player) -> Unit,
    onPlayerDelete: (Player) -> Unit,
    isAdmin: Boolean
) {
    // --- STATE TANIMLARI ---
    val scope = rememberCoroutineScope()
    val mainViewModel: MainViewModel = viewModel()

    // 1. KRİTERLERİ ÇEKİYORUZ
    val efficiencyCriteria by mainViewModel.efficiencyCriteria.collectAsState()

    val currentProfile by mainViewModel.profile.collectAsState()
    val allUserProfiles by mainViewModel.allUserProfiles.collectAsState()
    var isSaving by remember { mutableStateOf(false) }
    var updatedName by remember { mutableStateOf(player.name) }
    var updatedGender by remember { mutableStateOf(player.gender) }
    var updatedIsCaptain by remember { mutableStateOf(player.isCaptain) }
    var updatedPosition by remember { mutableStateOf(player.position) }
    var selectedJerseyNumber by remember { mutableStateOf(player.jerseyNumber) }
    var updatedEmail by remember { mutableStateOf(player.email ?: "") }
    var currentPhotoUrl by remember { mutableStateOf(player.photoUrl) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showNumberPickerDialog by remember { mutableStateOf(false) }
    var isEditModeExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val currentUser = mainViewModel.currentUser.collectAsState().value
    val isOwner = remember(currentUser, player.email) {
        val currentMail = currentUser?.email?.trim()
        val playerMail = player.email?.trim()
        currentMail != null && playerMail != null && currentMail.equals(playerMail, ignoreCase = true)
    }
    val canEditEverything = isAdmin
    val canEditPhotoAndNumber = isAdmin || isOwner
    val hasPhoto = currentPhotoUrl != null || tempPhotoUri != null

    val cropErrorMsg = stringResource(R.string.msg_crop_error)
    val cropImageLauncher = rememberLauncherForActivityResult(contract = CropImageContract()) { result ->
        if (result.isSuccessful) {
            tempPhotoUri = result.uriContent
        } else {
            val exception = result.error
            Toast.makeText(context, String.format(cropErrorMsg, exception?.message), Toast.LENGTH_SHORT).show()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val cropOptions = CropImageContractOptions(
                    uri,
                    CropImageOptions(
                        cropShape = CropImageView.CropShape.OVAL,
                        fixAspectRatio = true,
                        aspectRatioX = 1,
                        aspectRatioY = 1,
                        outputCompressFormat = Bitmap.CompressFormat.JPEG,
                        outputCompressQuality = 80
                    )
                )
                cropImageLauncher.launch(cropOptions)
            }
        }
    )
    var showEmailDropdown by remember { mutableStateOf(false) }

    val teamMemberEmails = remember(currentProfile.members, allUserProfiles) {
        currentProfile.members.keys.mapNotNull { uid ->
            val userProfile = allUserProfiles[uid]
            val email = userProfile?.email
            if (!email.isNullOrBlank()) {
                val name = userProfile.displayName ?: "İsimsiz Üye"
                Triple(uid, name, email)
            } else null
        }
    }

    var showTournamentDropdown by remember { mutableStateOf(false) }
    var showMatchDropdown by remember { mutableStateOf(false) }
    var selectedTournamentId by remember { mutableStateOf("GENEL") }
    var selectedMatchId by remember { mutableStateOf<String?>(null) }
    val selectedTournament = allTournaments.find { it.id == selectedTournamentId }

    // --- İSTATİSTİK HESAPLAMALARI (GÜNCELLENDİ) ---
    // Criteria parametresi eklendi
    val advancedStats = calculateGlobalPlayerStats(
        playerId = player.id,
        filterTournamentId = selectedTournamentId,
        filterMatchId = selectedMatchId,
        allTournaments = allTournaments,
        criteria = efficiencyCriteria // <--- KRİTERLER BURADA
    )
    val stats = advancedStats.basicStats

    val totalPassesCompleted = stats.successfulPass + stats.assist
    val totalPassesAttempted = totalPassesCompleted + stats.throwaway
    val passSuccessRate = calculateSafePercentage(totalPassesCompleted, totalPassesAttempted)

    val totalSuccesfulCatches = stats.catchStat + stats.goal
    val totalCatchesAttempted = totalSuccesfulCatches + stats.drop
    val catchRate = calculateSafePercentage(totalSuccesfulCatches, totalCatchesAttempted)

    // --- TAKIM ORTALAMALARI (GÜNCELLENDİ) ---
    // Criteria buradaki döngüye de eklendi
    val teamAverages = remember(allPlayers, selectedTournamentId, selectedMatchId, allTournaments, efficiencyCriteria) {
        var totalGoals = 0
        var totalAssists = 0
        var totalBlocks = 0
        var totalThrowaways = 0
        var totalDrops = 0
        var totalPulls = 0
        var totalCatches = 0
        var totalSuccessfulPasses = 0
        var activePlayerCount = 0

        allPlayers.forEach { p ->
            val pStats = calculateGlobalPlayerStats(
                playerId = p.id,
                filterTournamentId = selectedTournamentId,
                filterMatchId = selectedMatchId,
                allTournaments = allTournaments,
                criteria = efficiencyCriteria // <--- BURAYA DA EKLENDİ
            ).basicStats

            if (pStats.pointsPlayed > 0) {
                activePlayerCount++
                totalGoals += pStats.goal
                totalAssists += pStats.assist
                totalBlocks += pStats.block
                totalThrowaways += pStats.throwaway
                totalDrops += pStats.drop
                totalPulls += pStats.successfulPulls
                totalCatches += pStats.catchStat
                totalSuccessfulPasses += pStats.successfulPass
            }
        }

        val count = activePlayerCount.coerceAtLeast(1).toDouble()
        val teamTotalPassesCompleted = totalSuccessfulPasses + totalAssists
        val teamTotalPassesAttempted = teamTotalPassesCompleted + totalThrowaways
        val teamPassRate =
            if (teamTotalPassesAttempted > 0) teamTotalPassesCompleted.toDouble() / teamTotalPassesAttempted else 0.0
        val teamTotalSuccessfulCatches = totalCatches + totalGoals
        val teamTotalCatchAttempts = teamTotalSuccessfulCatches + totalDrops
        val teamCatchRate =
            if (teamTotalCatchAttempts > 0) teamTotalSuccessfulCatches.toDouble() / teamTotalCatchAttempts else 0.0

        mapOf(
            "goal" to (totalGoals / count),
            "assist" to (totalAssists / count),
            "block" to (totalBlocks / count),
            "throwaway" to (totalThrowaways / count),
            "drop" to (totalDrops / count),
            "pull" to (totalPulls / count),
            "passRate" to teamPassRate,
            "catchRate" to teamCatchRate
        )
    }

    val takenNumbers = remember(allPlayers, player) {
        allPlayers.filter { it.id != player.id && it.jerseyNumber != null }
            .associateBy { it.jerseyNumber!! }
    }

    // ... (Dialoglar ve UI Kodlarının geri kalanı aynı) ...
    // Sadece EfficiencyDescriptionDialog kısmında güncelleme var

    if (showNumberPickerDialog) {
        AlertDialog(
            onDismissRequest = { showNumberPickerDialog = false },
            title = { Text(stringResource(R.string.jersey_title)) },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 60.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedJerseyNumber == null) MaterialTheme.colorScheme.primaryContainer else Color.LightGray
                            ),
                            onClick = { selectedJerseyNumber = null }
                        ) {
                            Box(
                                modifier = Modifier.height(60.dp).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(stringResource(R.string.jersey_empty), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    items(100) { number ->
                        val owner = takenNumbers[number]
                        val isTaken = owner != null
                        val isSelected = number == selectedJerseyNumber
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isTaken -> MaterialTheme.colorScheme.errorContainer
                                    else -> Color(0xFFE8F5E9)
                                }
                            ),
                            enabled = !isTaken || isSelected,
                            onClick = { selectedJerseyNumber = number }
                        ) {
                            Box(
                                modifier = Modifier.height(60.dp).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$number",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showNumberPickerDialog = false }) { Text(stringResource(R.string.btn_ok)) }
            }
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
                            stringResource(R.string.profile_career_stats),
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedTournamentId = "GENEL"
                                selectedMatchId = null
                                showTournamentDropdown = false
                            }.padding(vertical = 12.dp)
                        )
                    }
                    items(allTournaments) { tournament ->
                        Text(
                            tournament.tournamentName,
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedTournamentId = tournament.id
                                selectedMatchId = null
                                showTournamentDropdown = false
                            }.padding(vertical = 12.dp)
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
                            stringResource(R.string.filter_all_tournament_matches),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedMatchId = null
                                showMatchDropdown = false
                            }.padding(vertical = 12.dp)
                        )
                    }
                    items(selectedTournament.matches) { match ->
                        Text(
                            "vs ${match.opponentName} (${match.scoreUs}-${match.scoreThem})",
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedMatchId = match.id
                                showMatchDropdown = false
                            }.padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                actions = {
                    val vm: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    IconButton(onClick = {
                        vm.sharePlayerReport(context, player, allTournaments, allPlayers)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.menu_player_report), tint = StitchColor.TextPrimary)
                    }
                },
                title = { Text(stringResource(R.string.desc_profile)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.desc_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        var selectedTabIndex by remember { mutableStateOf(0) }
        val tabTitles = listOf(stringResource(R.string.tab_player_stats), stringResource(R.string.tab_pass_network))
        var showEfficiencyInfo by remember { mutableStateOf(false) }

        if (showEfficiencyInfo) {
            // KRİTERLER DİYALOGA GÖNDERİLDİ
            EfficiencyDescriptionDialog(
                onDismiss = { showEfficiencyInfo = false },
                criteria = efficiencyCriteria
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ... (Profil Kartı ve Düzenleme Alanı aynı kalacak) ...

            // --- 1. YENİ PROFİL KARTI ---
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
                        val modelToShow = tempPhotoUri ?: currentPhotoUrl
                        PlayerAvatar(
                            name = player.name,
                            jerseyNumber = player.jerseyNumber,
                            photoUrl = modelToShow?.toString(),
                            size = 80.dp,
                            fontSize = 28.sp
                        )

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                player.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            val captainLabel = if (player.isCaptain) "• ${stringResource(R.string.role_captain)}" else ""
                            Text(
                                "${player.position} $captainLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(0.8f)
                            )
                        }

                        if (isAdmin || isOwner) {
                            IconButton(onClick = { isEditModeExpanded = !isEditModeExpanded }) {
                                Icon(
                                    imageVector = if (isEditModeExpanded) Icons.Default.ExpandLess else Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.btn_edit),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // --- 2. DÜZENLEME ALANI ---
            if (isEditModeExpanded && (isAdmin || isOwner)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, com.eyuphanaydin.discbase.ui.theme.StitchPrimary.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(stringResource(R.string.profile_edit_title), fontWeight = FontWeight.Bold, color = com.eyuphanaydin.discbase.ui.theme.StitchPrimary)

                        if (isAdmin) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = updatedEmail,
                                    onValueChange = { updatedEmail = it },
                                    label = { Text(stringResource(R.string.label_email_match)) },
                                    placeholder = { Text(stringResource(R.string.hint_email_select)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = { showEmailDropdown = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = stringResource(R.string.desc_open_list))
                                        }
                                    }
                                )

                                DropdownMenu(
                                    expanded = showEmailDropdown,
                                    onDismissRequest = { showEmailDropdown = false },
                                    modifier = Modifier.fillMaxWidth(0.85f).heightIn(max = 250.dp)
                                ) {
                                    if (teamMemberEmails.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.email_no_member), color = Color.Gray) },
                                            onClick = { showEmailDropdown = false }
                                        )
                                    } else {
                                        teamMemberEmails.forEach { (_, name, email) ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                        Text(email, fontSize = 12.sp, color = Color.Gray)
                                                    }
                                                },
                                                onClick = {
                                                    updatedEmail = email
                                                    showEmailDropdown = false
                                                }
                                            )
                                            Divider(modifier = Modifier.padding(horizontal = 16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = updatedName,
                                onValueChange = { updatedName = it },
                                label = { Text(stringResource(R.string.player_name_label)) },
                                modifier = Modifier.weight(0.7f),
                                enabled = canEditEverything
                            )
                            OutlinedTextField(
                                value = selectedJerseyNumber?.toString() ?: stringResource(R.string.jersey_empty),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.label_number)) },
                                modifier = Modifier.weight(0.3f)
                                    .clickable(enabled = canEditPhotoAndNumber) {
                                        showNumberPickerDialog = true
                                    },
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color.Black,
                                    disabledBorderColor = if (canEditPhotoAndNumber) com.eyuphanaydin.discbase.ui.theme.StitchPrimary else Color.Gray,
                                    disabledLabelColor = Color.Gray
                                )
                            )
                        }

                        PositionSelector(
                            selectedPosition = updatedPosition,
                            onPositionSelect = { updatedPosition = it },
                            enabled = canEditEverything
                        )
                        GenderSelector(
                            selectedGender = updatedGender,
                            onGenderSelect = { updatedGender = it },
                            enabled = canEditEverything
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.label_captain_switch), style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = updatedIsCaptain,
                                onCheckedChange = { updatedIsCaptain = it },
                                enabled = canEditEverything
                            )
                        }

                        Divider()

                        if (canEditPhotoAndNumber) {
                            Text(stringResource(R.string.profile_photo_label), style = MaterialTheme.typography.labelMedium, color = Color.Gray)

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (tempPhotoUri != null) stringResource(R.string.btn_change_photo) else stringResource(R.string.btn_upload_photo), fontSize = 12.sp)
                                }

                                if (hasPhoto) {
                                    OutlinedButton(
                                        onClick = {
                                            tempPhotoUri = null
                                            currentPhotoUrl = null
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.btn_remove_photo), fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // KAYDET
                        val msgUpdated = stringResource(R.string.msg_profile_updated)
                        Button(
                            onClick = {
                                scope.launch {
                                    isSaving = true
                                    var finalPhotoUrl = currentPhotoUrl

                                    if (tempPhotoUri != null) {
                                        finalPhotoUrl = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            uriToCompressedBase64(context, tempPhotoUri!!)
                                        }
                                    }

                                    val updatedPlayer = player.copy(
                                        name = updatedName,
                                        gender = updatedGender,
                                        position = updatedPosition,
                                        isCaptain = updatedIsCaptain,
                                        jerseyNumber = selectedJerseyNumber,
                                        email = updatedEmail.trim(),
                                        photoUrl = finalPhotoUrl
                                    )
                                    onPlayerUpdate(updatedPlayer)
                                    isSaving = false
                                    isEditModeExpanded = false
                                    Toast.makeText(context, msgUpdated, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = com.eyuphanaydin.discbase.ui.theme.StitchPrimary),
                            enabled = !isSaving
                        ) {
                            if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            else Text(stringResource(R.string.btn_save_changes), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- FİLTRELEME ALANI ---
            val selectedTournamentName = selectedTournament?.tournamentName ?: stringResource(R.string.profile_career_stats)
            val selectedMatchName = if (selectedMatchId != null) {
                selectedTournament?.matches?.find { it.id == selectedMatchId }
                    ?.let { "vs ${it.opponentName}" } ?: stringResource(R.string.filter_unknown_match)
            } else {
                stringResource(R.string.filter_all_matches)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                        Text(
                            selectedTournamentName,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
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
                            Text(
                                selectedMatchName,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                }
            }

            TabRow(selectedTabIndex = selectedTabIndex) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) })
                }
            }

            if (selectedTabIndex == 0) {
                // İSTATİSTİK TABI
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    EfficiencyCard(
                        efficiencyScore = advancedStats.plusMinus,
                        onInfoClick = { showEfficiencyInfo = true }
                    )
                    GameTimeCard(
                        totalPoints = stats.pointsPlayed,
                        offensePoints = advancedStats.oPointsPlayed,
                        defensePoints = advancedStats.dPointsPlayed
                    )

                    val isHandler = player.position == "Handler" || player.position == "Hybrid"
                    if (isHandler) {
                        PassingStatsCard(passSuccessRate, stats, teamAverages)
                        ReceivingStatsCard(catchRate, stats, teamAverages)
                    } else {
                        ReceivingStatsCard(catchRate, stats, teamAverages)
                        PassingStatsCard(passSuccessRate, stats, teamAverages)
                    }

                    DefenseStatsCard(stats, teamAverages)

                    if (isAdmin) {
                        Button(
                            onClick = { onPlayerDelete(player) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.delete_player_confirm))
                        }
                    }

                    Spacer(Modifier.height(50.dp))
                }
            } else {
                // PAS AĞI TABI
                val totalPasses = stats.successfulPass + stats.assist
                var passesToHandlers = 0
                var passesToCutters = 0

                stats.passDistribution.forEach { (receiverId, count) ->
                    val receiver = allPlayers.find { it.id == receiverId }
                    if (receiver != null) {
                        if (receiver.position == "Handler" || receiver.position == "Hybrid") {
                            passesToHandlers += count
                        } else {
                            passesToCutters += count
                        }
                    }
                }

                val handlerRatio = if (totalPasses > 0) passesToHandlers.toFloat() / totalPasses else 0f
                val cutterRatio = if (totalPasses > 0) passesToCutters.toFloat() / totalPasses else 0f

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (stats.passDistribution.isNotEmpty()) {
                        PassNetworkChordDiagram(
                            mainPlayerName = player.name,
                            passDistribution = stats.passDistribution,
                            allPlayers = allPlayers
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // ... (Akış Oranları Kartı - Değişmedi) ...
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                stringResource(R.string.flow_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = StitchColor.TextPrimary
                            )
                            Spacer(Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth().height(24.dp)
                                    .clip(RoundedCornerShape(50)).background(Color(0xFFF0F0F0))
                            ) {
                                if (handlerRatio > 0) {
                                    Box(
                                        modifier = Modifier.fillMaxHeight().weight(handlerRatio)
                                            .background(com.eyuphanaydin.discbase.ui.theme.StitchPrimary)
                                    ) {
                                        if (handlerRatio > 0.15) Text(
                                            "${(handlerRatio * 100).toInt()}%",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                }
                                if (cutterRatio > 0) {
                                    Box(
                                        modifier = Modifier.fillMaxHeight().weight(cutterRatio)
                                            .background(StitchOffense)
                                    ) {
                                        if (cutterRatio > 0.15) Text(
                                            "${(cutterRatio * 100).toInt()}%",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(10.dp).background(com.eyuphanaydin.discbase.ui.theme.StitchPrimary, CircleShape)
                                    ); Spacer(Modifier.width(6.dp)); Text(
                                    stringResource(R.string.flow_to_handler),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(10.dp).background(StitchOffense, CircleShape)
                                    ); Spacer(Modifier.width(6.dp)); Text(
                                    stringResource(R.string.flow_to_cutter),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        stringResource(R.string.detailed_connections_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = StitchColor.TextPrimary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                    )

                    if (stats.passDistribution.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) { Text(stringResource(R.string.no_data), color = Color.Gray) }
                    } else {
                        val sortedConnections =
                            stats.passDistribution.toList().sortedByDescending { it.second }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            sortedConnections.forEach { (receiverId, count) ->
                                val receiver = allPlayers.find { it.id == receiverId }
                                val receiverName = receiver?.name ?: stringResource(R.string.unknown)
                                val receiverPos = receiver?.position ?: "-"
                                val percentage =
                                    if (totalPasses > 0) (count.toDouble() / totalPasses) else 0.0
                                val isHandlerConnection =
                                    receiverPos == "Handler" || receiverPos == "Hybrid"

                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
                                    elevation = CardDefaults.cardElevation(1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PlayerAvatar(
                                            name = receiverName,
                                            size = 40.dp,
                                            fontSize = 14.sp
                                        )
                                        Spacer(Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                receiverName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = receiverPos,
                                                color = if (isHandlerConnection) com.eyuphanaydin.discbase.ui.theme.StitchPrimary else StitchOffense,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            modifier = Modifier.width(110.dp)
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
                                                modifier = Modifier.fillMaxWidth().height(6.dp)
                                                    .clip(RoundedCornerShape(50)),
                                                color = if (isHandlerConnection) com.eyuphanaydin.discbase.ui.theme.StitchPrimary else StitchOffense,
                                                trackColor = Color(0xFFF0F0F0)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(50.dp))
                }
            }
        }
    }
}