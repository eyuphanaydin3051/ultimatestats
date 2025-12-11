package com.example.ilkuygulamam

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
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
import com.example.ilkuygulamam.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// --- 1. ANA SAYFA (HOME SCREEN) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController, // <-- NavController EKLENDİ (Yönlendirme için)
    teamProfile: TeamProfile,
    tournaments: List<Tournament>
) {
    // Genel verileri hesapla (Sadece özet için)
    val advancedStats = calculateTeamStatsForFilter(tournaments, "GENEL")
    val winRate = "${advancedStats.wins}G - ${advancedStats.losses}M"
    val passRate = calculateSafePercentage(advancedStats.totalPassesCompleted, advancedStats.totalPassesAttempted)
    val totalTurnovers = advancedStats.totalThrowaways + advancedStats.totalDrops

    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                // DÜZELTME 1: Başlık artık "Ana Sayfa" (Takım ismi aşağıda logoyla duruyor)
                title = {
                    Text("Ana Sayfa", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                },
                // DÜZELTME 2: Profil butonu burada
                actions = {// --- BİLDİRİM SİSTEMİ DÜZELTMESİ BURADA ---
                    // ViewModel'i hiyerarşiden çekmek için
                    val mainViewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    val hasNotification by mainViewModel.hasPendingRequests.collectAsState()

                    // Profil İkonu (Kırmızı Noktalı)
                    androidx.compose.material3.BadgedBox(
                        badge = {
                            if (hasNotification) {
                                androidx.compose.material3.Badge(
                                    containerColor = com.example.ilkuygulamam.ui.theme.StitchDefense,
                                    contentColor = Color.White
                                )
                            }
                        },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        ModernIconButton(
                            icon = Icons.Default.Person,
                            onClick = { navController.navigate("profile_edit") },
                            color = com.example.ilkuygulamam.ui.theme.StitchPrimary,
                            contentDescription = "Profil Ayarları"
                        )
                    }
                    // -------------------------------------------
                    ModernIconButton(
                        icon = Icons.Default.Settings, // Çark İkonu
                        onClick = { navController.navigate("settings") },
                        color = StitchColor.TextPrimary,
                        contentDescription = "Ayarlar"
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
            // 1. HEADER (Logo + İsim)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (teamProfile.logoPath != null) {
                    AsyncImage(
                        model = getLogoModel(teamProfile.logoPath),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp).clip(CircleShape).border(2.dp,
                            com.example.ilkuygulamam.ui.theme.StitchPrimary, CircleShape)
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
                    Text("Hoş Geldin,", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Text(teamProfile.teamName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = StitchColor.TextPrimary)
                }
            }

            // 2. HERO CARD (Sezon Karnesi)
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
                    Text("Sezon Karnesi", color = Color.White.copy(0.8f), fontSize = 14.sp)
                    Text(winRate, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                }
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color.White.copy(0.2f),
                    modifier = Modifier.size(100.dp).align(Alignment.TopEnd).offset(x = 20.dp, y = -20.dp)
                )
                // İpucu ikonu ekleyelim (Tıklanabilir olduğunu belli etmek için)
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(0.6f),
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            // 3. ÖZET GRID
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Pas Başarısı
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
                        Text("Pas Başarısı", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(progress = 1f, modifier = Modifier.size(60.dp), color = Color(0xFFF0F0F0), strokeWidth = 6.dp)
                            CircularProgressIndicator(progress = passRate.progress, modifier = Modifier.size(60.dp), color = Color(0xFFFF9800), strokeWidth = 6.dp)
                            Text(passRate.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
                // Top Kaybı
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
                        Text("Toplam Hata", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Text("$totalTurnovers", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = com.example.ilkuygulamam.ui.theme.StitchDefense)
                        Icon(Icons.Default.TrendingDown, null, tint = com.example.ilkuygulamam.ui.theme.StitchDefense)
                    }
                }
            }

            // 4. İSTATİSTİK MERKEZİ BUTONU (YENİ)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clickable { navController.navigate("team_stats") }, // Yönlendirme burada
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = StitchSecondary.copy(alpha = 0.1f)), // Hafif mor zemin
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("İstatistik Merkezi", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = com.example.ilkuygulamam.ui.theme.StitchPrimary)
                        Text("Detaylı takım analizi ve veriler", fontSize = 12.sp, color = Color.Gray)
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

    // Filtreleme
    val filteredPlayers = if (searchQuery.isBlank()) {
        rosterPlayers
    } else {
        rosterPlayers.filter { player ->
            player.name.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = StitchColor.Background, // Stitch teması
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = onAddPlayerClick,
                    containerColor = StitchColor.Primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Oyuncu Ekle")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // 1. ARAMA VE NUMARA BUTONU
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Oyuncu Ara...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null) }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = com.example.ilkuygulamam.ui.theme.StitchPrimary,
                        unfocusedBorderColor = Color.LightGray,
                        focusedContainerColor = StitchColor.Surface,   // DOĞRU: Büyük C
                        unfocusedContainerColor = StitchColor.Surface
                    )
                )

                Spacer(Modifier.height(12.dp))

                // 2. BUTON GRUBU (Yan Yana)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Mevcut Numara Butonu (Weight ekleyerek daralttık)
                    Button(
                        onClick = { navController.navigate("jersey_grid") },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF37474F),
                            contentColor = Color.White // <-- DÜZELTME: Yazı rengi beyaz olsun
                        ),                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FormatListNumbered, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Numaralar", fontSize = 12.sp, maxLines = 1)
                    }

                    // --- YENİ İSTATİSTİK BUTONU ---
                    Button(
                        onClick = { navController.navigate("player_leaderboard") },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StitchColor.Primary, // Mor Renk
                            contentColor = Color.White // <-- DÜZELTME: Yazı rengi beyaz olsun
                        ),                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("İstatistikler", fontSize = 12.sp, maxLines = 1)
                    }
                }
            }

            // 2. OYUNCU GRID (IZGARA)
            if (filteredPlayers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "'$searchQuery' bulunamadı." else "Henüz oyuncu yok.",
                        color = Color.Gray
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    // --- EKLENEN KISIM: modifier = Modifier.weight(1f) ---
                    // Bu sayede liste, üstteki arama çubuğundan kalan tüm boşluğu kaplar ve düzgün kayar.
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredPlayers) { player ->
                        PlayerGridCard(navController, player)
                    }
                    // FAB için alt boşluk
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
        "Handler" -> com.example.ilkuygulamam.ui.theme.StitchPrimary // Mor
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
                title = { Text("Forma Numaraları", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    ModernIconButton(
                        icon = Icons.Default.ArrowBack,
                        onClick = { navController.popBackStack() },
                        color = StitchColor.TextPrimary,
                        contentDescription = "Geri"
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            // Bilgi Çubuğu
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(16.dp).background(MaterialTheme.colorScheme.errorContainer, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text("Dolu", fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(16.dp).background(Color(0xFFE8F5E9), CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text("Boş", fontSize = 12.sp)
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

                    // HATAYI ÇÖZEN KISIM: isSelected burada kullanılmıyor, sadece isTaken var.
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
                                    owner!!.name.split(" ").first().take(5), // İsim çok uzunsa kısalt
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
                title = { Text("Yeni Oyuncu", fontWeight = FontWeight.Bold) },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp) // Geniş aralıklar
        ) {
            // Avatar Önizleme (Dekoratif)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier.size(100.dp)
                        .background(com.example.ilkuygulamam.ui.theme.StitchPrimary.copy(0.1f), CircleShape),
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
                        label = { Text("Ad Soyad") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Divider()

                    // Cinsiyet
                    Text(
                        "Cinsiyet",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    GenderSelector(
                        selectedGender = newPlayerGender,
                        onGenderSelect = { newPlayerGender = it })

                    // Pozisyon
                    Text(
                        "Pozisyon",
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
                        Toast.makeText(context, "İsim gerekli", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StitchColor.Primary)
            ) {
                Text("OYUNCUYU EKLE", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
    val tabs = listOf("Takvim", "Katılım Raporu")

    val calendar = Calendar.getInstance()
    var selectedMonthIndex by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    val months = listOf("Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık")

    var trainingToEdit by remember { mutableStateOf<Training?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Antrenman", fontWeight = FontWeight.Bold) },
                actions = {
                    ModernIconButton(
                        icon = Icons.Default.Person,
                        onClick = { navController.navigate("profile_edit") },
                        color = com.example.ilkuygulamam.ui.theme.StitchPrimary,
                        contentDescription = "Profil"
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
                    Icon(Icons.Default.Add, contentDescription = "Ekle")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = StitchColor.Surface,
                contentColor = com.example.ilkuygulamam.ui.theme.StitchPrimary,
                indicator = { tabPositions ->
                    androidx.compose.material3.TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = com.example.ilkuygulamam.ui.theme.StitchPrimary,
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
                // AYLIK FİLTRE BAR
                ScrollableTabRow(
                    selectedTabIndex = selectedMonthIndex,
                    containerColor = StitchColor.Background,
                    contentColor = Color.Gray,
                    edgePadding = 16.dp,
                    indicator = {},
                    divider = {}) {
                    months.forEachIndexed { index, monthName ->
                        val isSelected = index == selectedMonthIndex;
                        val textColor = if (isSelected) com.example.ilkuygulamam.ui.theme.StitchPrimary else Color.Gray;
                        val bgColor = if (isSelected) com.example.ilkuygulamam.ui.theme.StitchPrimary.copy(0.1f) else Color.Transparent;
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

                // --- FİLTRELEME MANTIĞI (DÜZELTİLDİ) ---
                val filteredTrainings = trainings.filter { training ->
                    val parts = training.date.split("/")
                    // 1. Ay eşleşiyor mu?
                    val isMonthMatch = if (parts.size == 3) parts[1].toIntOrNull() == (selectedMonthIndex + 1) else false

                    // 2. Görünürlük kontrolü
                    if (isAdmin) {
                        isMonthMatch // Admin ise sadece aya bak
                    } else {
                        isMonthMatch && training.isVisibleToMembers // Üye ise hem ay hem görünürlük
                    }
                }.sortedByDescending { it.date }

                if (filteredTrainings.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("${months[selectedMonthIndex]} ayında antrenman yok.", color = Color.Gray)
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
                                onEdit = { trainingToEdit = training }
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
    onEdit: () -> Unit
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
                    // TARIH KUTUSU
                    Surface(
                        color = com.example.ilkuygulamam.ui.theme.StitchPrimary.copy(0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(50.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val day = training.date.split("/").getOrNull(0) ?: "?"
                            Text(day, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = com.example.ilkuygulamam.ui.theme.StitchPrimary)
                        }
                    }
                    Spacer(Modifier.width(12.dp))

                    // DETAYLAR
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(training.date, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                            // --- GİZLİLİK İKONU ---
                            if (!training.isVisibleToMembers) {
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff, // Göz kapalı ikonu
                                    contentDescription = "Gizli",
                                    tint = Color.Red.copy(0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            // ----------------------
                        }
                        Text(training.location.ifBlank { "Konum Yok" }, fontSize = 12.sp, color = Color.Gray)
                    }
                }

                // KATILIMCI SAYISI BADGE
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

                // Admin Uyarısı
                if (!training.isVisibleToMembers) {
                    Text(
                        "⚠️ Bu antrenman şu an üyelere GİZLİ.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (training.time.isNotBlank()) {
                    Text("Saat: ${training.time}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                }
                if (training.description.isNotBlank()) {
                    Text("Notlar:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text(training.description, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                }

                Text("Katılanlar:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                if (attendees.isEmpty()) Text("-", fontSize = 14.sp)
                else Text(attendees.joinToString(", ") { it.name }, fontSize = 14.sp, lineHeight = 20.sp)

                if (isAdmin) {
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onEdit,
                            colors = ButtonDefaults.buttonColors(containerColor = StitchColor.Primary),
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Düzenle")
                        }
                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(containerColor = com.example.ilkuygulamam.ui.theme.StitchDefense.copy(0.1f), contentColor = com.example.ilkuygulamam.ui.theme.StitchDefense),
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, com.example.ilkuygulamam.ui.theme.StitchDefense)
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Sil")
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
            Text(
                "Toplam Antrenman: ${trainings.size}",
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
                                color = if (percentage > 0.7) StitchOffense else if (percentage > 0.4) com.example.ilkuygulamam.ui.theme.StitchPrimary else com.example.ilkuygulamam.ui.theme.StitchDefense
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = percentage,
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = if (percentage > 0.7f) StitchOffense else if (percentage > 0.4f) com.example.ilkuygulamam.ui.theme.StitchPrimary else com.example.ilkuygulamam.ui.theme.StitchDefense,
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

    // YENİ: Görünürlük ayarı
    var isVisibleToMembers by remember(trainingToEdit) {
        mutableStateOf(trainingToEdit?.isVisibleToMembers ?: true)
    }
    // YENİ: Detayları gizle/göster (Düzenlerken kapalı, yeni eklerken açık olsun)
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
                    .heightIn(max = 600.dp), // Çok uzarsa kaydırma için sınır
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // BAŞLIK
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isEditing) Icons.Default.Edit else Icons.Default.AddCircle, null, tint = StitchColor.Primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(if (isEditing) "Antrenmanı Düzenle" else "Yeni Antrenman", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }

                Divider()

                // --- 1. GENİŞLETİLEBİLİR DETAYLAR KARTI ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().animateContentSize() // Animasyonlu açılma
                ) {
                    Column {
                        // Başlık (Tıklanabilir)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isDetailsExpanded = !isDetailsExpanded }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🗓️ Tarih, Saat ve Detaylar", fontWeight = FontWeight.Bold, color = StitchColor.TextPrimary)
                            Icon(
                                if (isDetailsExpanded) Icons.Default.ExpandLess else Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }

                        // İçerik (Sadece genişletilmişse görünür)
                        if (isDetailsExpanded) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                                // GÖRÜNÜRLÜK AYARI (SWITCH)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)).padding(8.dp)
                                ) {
                                    Icon(if(isVisibleToMembers) Icons.Default.Groups else Icons.Default.VisibilityOff, null, tint = if(isVisibleToMembers) StitchColor.Primary else Color.Gray)
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Üyelere Göster", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(if(isVisibleToMembers) "Herkes görebilir" else "Sadece kaptanlar görebilir", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Switch(
                                        checked = isVisibleToMembers,
                                        onCheckedChange = { isVisibleToMembers = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = StitchColor.Primary)
                                    )
                                }

                                // Tarih ve Saat
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = date, onValueChange = {}, readOnly = true, label = { Text("Tarih") },
                                        modifier = Modifier.weight(1f).clickable { datePickerDialog.show() }, enabled = false,
                                        trailingIcon = { Icon(Icons.Default.Event, null) },
                                        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = Color.Gray, disabledLabelColor = Color.Black)
                                    )
                                    OutlinedTextField(
                                        value = time, onValueChange = {}, readOnly = true, label = { Text("Saat") },
                                        modifier = Modifier.weight(1f).clickable { timePickerDialog.show() }, enabled = false,
                                        trailingIcon = { Icon(Icons.Default.AccessTime, null) },
                                        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = Color.Gray, disabledLabelColor = Color.Black)
                                    )
                                }

                                OutlinedTextField(
                                    value = location, onValueChange = { location = it }, label = { Text("Konum") },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true
                                )

                                OutlinedTextField(
                                    value = description, onValueChange = { description = it }, label = { Text("Notlar") },
                                    modifier = Modifier.fillMaxWidth().height(80.dp)
                                )
                            }
                        }
                    }
                }

                // --- 2. KATILIMCI LİSTESİ (HER ZAMAN GÖRÜNÜR) ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = StitchColor.Background),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f) // Kalan alanı kaplasın (Kaydırılabilir)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Yoklama Listesi", fontWeight = FontWeight.Bold)
                            Text("${selectedAttendees.size} Kişi", color = StitchColor.Primary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))

                        // Kaydırılabilir Liste
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
                                        onCheckedChange = null, // Tıklamayı Row yönetiyor
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
                            isVisibleToMembers = isVisibleToMembers // <-- Yeni alan
                        )
                        onSave(updatedTraining)
                        onDismiss()
                    } else {
                        Toast.makeText(context, "Tarih seçmelisiniz.", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StitchColor.Primary),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isEditing) "Kaydet" else "Oluştur", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("İptal", color = Color.Gray) }
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
        if (selectedTournamentId == "GENEL") "Genel İstatistikler" else selectedTournament?.tournamentName
            ?: "Genel İstatistikler"
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
            title = { Text("Filtre Seç") },
            text = {
                LazyColumn {
                    item {
                        Text(
                            "Genel İstatistikler",
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
    val teamAvgTempo = if (advancedStats.totalPassesAttempted > 0)
        String.format("%.2f sn", advancedStats.totalTempoSeconds.toDouble() / advancedStats.totalPassesAttempted)
    else "0.00 sn"
    // --- YENİ: PULL SÜRESİ HESAPLAMA ---
    val teamAvgPullTime = if (advancedStats.totalPulls > 0)
        String.format("%.2f sn", advancedStats.totalPullTimeSeconds.toDouble() / advancedStats.totalPulls)
    else "0.00 sn"
    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            TopAppBar(
                title = { Text("İstatistik Merkezi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    ModernIconButton(
                        icon = Icons.Default.ArrowBack,
                        onClick = { navController.popBackStack() },
                        color = StitchColor.TextPrimary,
                        contentDescription = "Geri"
                    )
                },
                actions = {
                    // Filtre Butonu (Header'daki gibi)
                    Surface(
                        onClick = { showDropdown = true },
                        shape = RoundedCornerShape(50),
                        color = com.example.ilkuygulamam.ui.theme.StitchPrimary.copy(alpha = 0.1f),
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
                                color = com.example.ilkuygulamam.ui.theme.StitchPrimary
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
            // Card içine sarmalamaya gerek yok, DetailedStatsCard zaten kendi tasarımına sahip
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
                "Takım Performansı",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StitchColor.TextPrimary
            )
            Spacer(Modifier.height(16.dp))

            // Hold Percentage
            PerformanceStatRow(
                title = "Hold % (Offense)",
                percentage = holdRate.text,
                ratio = holdRate.ratio,
                progress = holdRate.progress,
                progressColor = Color(0xFF03DAC5) // Teal
            )

            // Break Percentage
            PerformanceStatRow(
                title = "Break % (Defense)",
                percentage = breakRate.text,
                ratio = breakRate.ratio,
                progress = breakRate.progress,
                progressColor = Color(0xFFFF9800) // Turuncu
            )

            // Completion Rate
            PerformanceStatRow(
                title = "Pas Başarısı",
                percentage = passRate.text,
                ratio = passRate.ratio,
                progress = passRate.progress,
                progressColor = com.example.ilkuygulamam.ui.theme.StitchPrimary // Mor
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
                "Verimlilik (Efficiency)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StitchColor.TextPrimary
            )
            Spacer(Modifier.height(16.dp))

            // Hata oranı kalktı, Conversion Rate renklendi
            PerformanceStatRow(
                title = "Conversion Rate",
                percentage = conversionRate.text,
                ratio = conversionRate.ratio + " pozisyon gol oldu",
                progress = conversionRate.progress,
                progressColor = conversionColor // <-- DİNAMİK RENK
            )
            PerformanceStatRow(
                title = "Hatasız Ofans (Clean Holds)",
                percentage = cleanHoldRate.text,
                ratio = cleanHoldRate.ratio + " sayı hatasız atıldı",
                progress = cleanHoldRate.progress,
                progressColor = Color(0xFF00B0FF) // Açık Mavi
            )

            // Blok sonrası gol şansı
            PerformanceStatRow(
                title = "D-Line Conversion",
                percentage = blockConversionRate.text,
                ratio = blockConversionRate.ratio + " blok değerlendirildi",
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
                "Detaylı Analiz",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StitchColor.TextPrimary // <-- BURADA HATA ALMAMALISINIZ
            )
            Spacer(Modifier.height(16.dp))

            // 1. Satır: Paslar
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StitchStatBox("Pas den.", totalPasses, StitchSecondary, Modifier.weight(1f),
                    valueFontSize = myFontSize)
                StitchStatBox(
                    "Baş. Pas",
                    totalCompleted,
                    StitchOffense,
                    Modifier.weight(1f),
                    valueFontSize = myFontSize
                )
                StitchStatBox(
                    "Ort. Tempo",
                    teamAvgTempo,
                    Color(0xFFFFA000), // Amber rengi
                    Modifier.weight(1f),
                    valueFontSize = myFontSize
                )
            }
            Spacer(Modifier.height(12.dp))

            // 2. Satır: Hatalar
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StitchStatBox("Turnover", totalTurnovers,
                    com.example.ilkuygulamam.ui.theme.StitchDefense, Modifier.weight(1f))
                StitchStatBox(
                    "O. turn/Maç",
                    avgTurnoverMatch,
                    com.example.ilkuygulamam.ui.theme.StitchDefense.copy(alpha = 0.8f),
                    Modifier.weight(1f),
                    valueFontSize = myFontSize
                )
                StitchStatBox(
                    "Ort. Pull",
                    teamAvgPullTime,
                    Color(0xFF795548), // Kahverengi tonu
                    Modifier.weight(1f),
                    valueFontSize = myFontSize
                )
            }
            Spacer(Modifier.height(12.dp))

            // 3. Satır: Oyun Süresi
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StitchStatBox(
                    "Oynanan Sayı",
                    totalPointsPlayed,
                    Color.Gray,
                    Modifier.weight(1f),
                    valueFontSize = myFontSize
                )
                StitchStatBox("Maç", matchesPlayed, Color.Gray, Modifier.weight(1f))
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
    // 1. ADIM: Triple yerine Quadruple (Dörtlü) yapı veya veri sınıfı kullanarak takım ismini de taşıyalım.
    // (match, tournamentName, tournamentId, ourTeamName)
    val allMatches = remember(tournaments) {
        tournaments.flatMap { tournament ->
            tournament.matches.map { match ->
                // Burada tournament.ourTeamName değerini de listeye ekliyoruz
                MatchDisplayData(
                    match = match,
                    tournamentName = tournament.tournamentName,
                    tournamentId = tournament.id,
                    ourTeamName = tournament.ourTeamName // <-- Veriyi buradan alıyoruz
                )
            }
        }.reversed() // En son oynanan en üstte
    }

    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Sezon Fikstürü", fontWeight = FontWeight.Bold) },
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
        if (allMatches.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Henüz hiç maç oynanmadı.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // items içindeki değişkenleri güncelliyoruz
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
                                color = com.example.ilkuygulamam.ui.theme.StitchPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Skor Alanı
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // 2. ADIM: "BİZ" YERİNE TAKIM İSMİNİ KULLAN
                                // item.ourTeamName boş gelirse diye kontrol ekledik
                                Text(
                                    item.ourTeamName.ifBlank { "BİZ" }.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.match.scoreUs > item.match.scoreThem) StitchOffense else Color.Gray,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f) // İsim uzunsa sığması için
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
                                    color = if (item.match.scoreThem > item.match.scoreUs) com.example.ilkuygulamam.ui.theme.StitchDefense else Color.Gray,
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
    tournaments: List<Tournament>
) {
    var showEfficiencyInfo by remember { mutableStateOf(false) }
    var showTempoInfo by remember { mutableStateOf(false) }

    if (showEfficiencyInfo) EfficiencyDescriptionDialog(onDismiss = { showEfficiencyInfo = false })
    if (showTempoInfo) TempoDescriptionDialog(onDismiss = { showTempoInfo = false })

    // --- STATE ---
    var selectedStatType by remember { mutableStateOf(StatType.GOAL) }
    var calculationMode by remember { mutableStateOf(CalculationMode.TOTAL) } // <-- YENİ MOD SEÇİMİ

    var showTournamentDropdown by remember { mutableStateOf(false) }
    var showMatchDropdown by remember { mutableStateOf(false) }

    // Filtreler
    var selectedTournamentId by remember { mutableStateOf("GENEL") }
    var selectedMatchId by remember { mutableStateOf<String?>(null) }

    val selectedTournament = tournaments.find { it.id == selectedTournamentId }
    val selectedTournamentName = selectedTournament?.tournamentName ?: "Tüm Sezon (Genel)"
    val selectedMatchName = remember(selectedTournament, selectedMatchId) {
        if (selectedMatchId == null) "Tüm Maçlar"
        else selectedTournament?.matches?.find { it.id == selectedMatchId }?.let { "vs ${it.opponentName}" } ?: "Bilinmeyen Maç"
    }

    // --- HESAPLAMA (Sıralama Mantığı) ---
    val rankedPlayers = remember(
        allPlayers, selectedStatType, calculationMode, // Mod değişince yeniden hesapla
        selectedTournamentId, selectedMatchId, tournaments
    ) {
        allPlayers.map { player ->
            // 1. Temel İstatistikleri Çek
            val stats = calculateGlobalPlayerStats(
                playerId = player.id,
                filterTournamentId = selectedTournamentId,
                filterMatchId = selectedMatchId,
                allTournaments = tournaments
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
                StatType.PASS_COUNT -> (stats.basicStats.successfulPass + stats.basicStats.assist).toDouble()
                StatType.POINTS_PLAYED -> stats.basicStats.pointsPlayed.toDouble()
                StatType.PLAYTIME -> stats.basicStats.secondsPlayed.toDouble()

                // Yüzde ve Tempo gibi zaten ortalama olanlar için mod değişikliği yapılmaz
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

            // 3. Bölen (Denominator) Belirle
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
                            if (selectedStatType == StatType.PLUS_MINUS) {
                                // --- ÖZEL AYAR: VERİMLİLİK İÇİN SCALING ---
                                // Sayı başına verimlilik çok küçük çıktığı için (örn 0.1),
                                // bunu 10 ile çarpıp "10 Sayıdaki Verimlilik Endeksi"ne çeviriyoruz.
                                // Böylece 0.15 yerine 1.5 gibi daha okunabilir bir değer görüyoruz.
                                (rawValue / points) * 10.0
                            } else {
                                rawValue / points
                            }
                        } else 0.0
                    }
                }
            }

            Triple(player, finalValue, stats)
        }.filter { it.second > 0.0 || (selectedStatType == StatType.PLUS_MINUS && it.second != 0.0) } // 0 olanları gizle
            .sortedByDescending { it.second }
    }

    // --- FİLTRE DİALOGLARI ---
    if (showTournamentDropdown) {
        AlertDialog(
            onDismissRequest = { showTournamentDropdown = false },
            title = { Text("Turnuva Filtrele") },
            text = {
                LazyColumn {
                    item {
                        Text("Tüm Sezon (Genel)", modifier = Modifier.fillMaxWidth().clickable { selectedTournamentId = "GENEL"; selectedMatchId = null; showTournamentDropdown = false }.padding(12.dp))
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
            title = { Text("Maç Filtrele") },
            text = {
                LazyColumn {
                    item {
                        Text("Tüm Turnuva Maçları", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().clickable { selectedMatchId = null; showMatchDropdown = false }.padding(12.dp))
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
                title = { Text("İstatistik Liderleri", fontWeight = FontWeight.Bold) },
                navigationIcon = { ModernIconButton(Icons.Default.ArrowBack, { navController.popBackStack() }, StitchTextPrimary, "Geri") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            // 1. FİLTRE BUTONLARI (TURNUVA/MAÇ)
            Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterButton(text = selectedTournamentName, onClick = { showTournamentDropdown = true }, modifier = Modifier.weight(1f))
                if (selectedTournamentId != "GENEL") {
                    FilterButton(text = selectedMatchName, onClick = { showMatchDropdown = true }, modifier = Modifier.weight(0.6f))
                }
            }

            Spacer(Modifier.height(12.dp))

            // 2. HESAPLAMA MODU SEÇİCİSİ (YENİ)
            // Sadece toplanabilir veriler için bu barı göster
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
                                text = mode.label,
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
                        label = { Text(type.title) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = StitchColor.Primary, selectedLabelColor = Color.White),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected, borderColor = Color.LightGray, selectedBorderColor = StitchColor.Primary)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // BAŞLIK VE BİLGİ
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${selectedStatType.title} (${if(isSummable) calculationMode.label else "Ortalama"})", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                if (selectedStatType == StatType.PLUS_MINUS) IconButton(onClick = { showEfficiencyInfo = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Info, null, tint = StitchColor.Primary) }
                if (selectedStatType == StatType.TEMPO) IconButton(onClick = { showTempoInfo = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Info, null, tint = StitchColor.Primary) }
            }

            Spacer(Modifier.height(8.dp))

            // 4. SIRALAMA LİSTESİ
            if (rankedPlayers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Veri bulunamadı.", color = Color.Gray) }
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
                            calculationMode = calculationMode // Mod bilgisini de gönderiyoruz
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
                    statType == StatType.PLAYTIME -> formatSecondsToTime(value.toLong()) // Ortalama süre de aynı format
                    statType == StatType.AVG_PLAYTIME -> formatSecondsToTime(value.toLong())

                    // Yüzdeler ve Tempo
                    statType == StatType.CATCH_RATE || statType == StatType.PASS_RATE -> "${value.toInt()}%"
                    statType == StatType.TEMPO || statType == StatType.AVG_PULL_TIME -> "${String.format("%.2f", value)} sn"

                    // Ondalıklı Sayılar (Ortalamalar ve Verimlilik)
                    calculationMode != CalculationMode.TOTAL || statType == StatType.PLUS_MINUS -> String.format("%.2f", value)

                    // Tam Sayılar (Toplam Gol, Asist vb.)
                    else -> "${value.toInt()}"
                }

                Text(formattedValue, fontSize = 20.sp, fontWeight = FontWeight.Black, color = StitchColor.Primary)

                // Alt Metin (Birim)
                val unitText = when(calculationMode) {
                    CalculationMode.PER_MATCH -> "${statType.unit}/maç"
                    CalculationMode.PER_POINT -> {
                        if (statType == StatType.PLUS_MINUS) {
                            // Verimlilik için özel birim
                            "(+/-)*10/sayı"
                        } else {
                            "${statType.unit}/sayı"
                        }
                    }
                    else -> statType.unit
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
fun EfficiencyDescriptionDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Verimlilik Puanı Nedir?") },
        text = {
            Column {
                Text(
                    "Oyuncunun oyuna genel katkısını ölçen özel bir formüldür:",
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(16.dp))
                // --- YENİ EKLENEN: CALLAHAN ---
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Stars, null, tint = Color(0xFFFFC107), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("+3.5 Puan: ", fontWeight = FontWeight.Bold, color = Color(0xFFFFC107))
                    Text("Callahan")
                }
                Spacer(Modifier.height(8.dp))
                // ------------------------------

                // 1.5 Puanlık BLOK (En değerli)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Shield,
                        null,
                        tint = com.example.ilkuygulamam.ui.theme.StitchPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("+1.5 Puan: ", fontWeight = FontWeight.Bold, color = com.example.ilkuygulamam.ui.theme.StitchPrimary)
                    Text("Blok (Defense)")
                }

                Spacer(Modifier.height(8.dp))

                // 1.0 Puanlıklar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AddCircle,
                        null,
                        tint = StitchOffense,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("+1.0 Puan: ", fontWeight = FontWeight.Bold, color = StitchOffense)
                    Text("Gol, Asist")
                }

                Spacer(Modifier.height(8.dp))

                // 0.1 Puanlıklar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TrendingUp,
                        null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("+0.05 Puan: ", fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("Her Başarılı Pas")
                }

                Spacer(Modifier.height(8.dp))

                // Eksiler
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TrendingDown,
                        null,
                        tint = com.example.ilkuygulamam.ui.theme.StitchDefense,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("-1.0 Puan: ", fontWeight = FontWeight.Bold, color = com.example.ilkuygulamam.ui.theme.StitchDefense)
                    Text("Hata (Turnover), Drop")
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Anlaşıldı") } },
        containerColor = StitchColor.Surface
    )
}
@Composable
fun TempoDescriptionDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tempo (Ort. Pas Süresi) Nedir?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Bir oyuncunun diski eline aldığı andan elinden çıkarana kadar geçen ortalama süredir.",
                    fontSize = 14.sp
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = StitchColor.Background),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Formül:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            "Toplam Topa Sahip Olma Süresi / Toplam Pas Sayısı",
                            fontStyle = FontStyle.Italic, fontSize = 14.sp, color = StitchColor.Primary
                        )
                    }
                }

                Text("Nasıl Yorumlanır?", fontWeight = FontWeight.Bold)

                Row(verticalAlignment = Alignment.Top) {
                    Text("🚀", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Hızlı Tempo (< 3.0 sn)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Oyuncu diski bekletmiyor, oyun akışını hızlandırıyor.", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Row(verticalAlignment = Alignment.Top) {
                    Text("🐢", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Yavaş Tempo (> 7.0 sn)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Oyuncu oyunu yavaşlatıyor veya pas kanalı bulmakta zorlanıyor.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Anlaşıldı") } },
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
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) } // Yeni seçilen ama henüz yüklenmeyen
    var showNumberPickerDialog by remember { mutableStateOf(false) }
    var tempLogoUri by remember { mutableStateOf<Uri?>(null) }
    // YENİ: Düzenleme modu açık mı kapalı mı?
    var isEditModeExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val currentUser = mainViewModel.currentUser.collectAsState().value
    val isOwner = remember(currentUser, player.email) {
        val currentMail = currentUser?.email?.trim()
        val playerMail = player.email?.trim()
        currentMail != null && playerMail != null && currentMail.equals(playerMail, ignoreCase = true)
    }
    val canEditEverything = isAdmin // Sadece admin her şeyi düzenler
    val canEditPhotoAndNumber = isAdmin || isOwner
    val hasPhoto = currentPhotoUrl != null || tempPhotoUri != null
    val cropImageLauncher = rememberLauncherForActivityResult(contract = CropImageContract()) { result ->
        if (result.isSuccessful) {
            tempPhotoUri = result.uriContent
        } else {
            val exception = result.error
            Toast.makeText(context, "Kırpma hatası: ${exception?.message}", Toast.LENGTH_SHORT).show()
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

    // Takımdaki üyelerin (UID) profillerini bul ve listele
    // Triple yapısı: (UID, İsim, E-posta)
    val teamMemberEmails = remember(currentProfile.members, allUserProfiles) {
        currentProfile.members.keys.mapNotNull { uid ->
            val userProfile = allUserProfiles[uid]
            val email = userProfile?.email
            if (!email.isNullOrBlank()) {
                // İsim varsa ismini, yoksa "İsimsiz" yaz
                val name = userProfile.displayName ?: "İsimsiz Üye"
                Triple(uid, name, email)
            } else null
        }
    }
    // --- FİLTRE STATE'LERİ ---
    var showTournamentDropdown by remember { mutableStateOf(false) }
    var showMatchDropdown by remember { mutableStateOf(false) }
    var selectedTournamentId by remember { mutableStateOf("GENEL") }
    var selectedMatchId by remember { mutableStateOf<String?>(null) }

    // Seçili turnuvayı bul
    val selectedTournament = allTournaments.find { it.id == selectedTournamentId }

    // --- İSTATİSTİK HESAPLAMALARI ---
    val advancedStats = calculateGlobalPlayerStats(
        playerId = player.id,
        filterTournamentId = selectedTournamentId,
        filterMatchId = selectedMatchId,
        allTournaments = allTournaments
    )
    val stats = advancedStats.basicStats

    val totalPassesCompleted = stats.successfulPass + stats.assist
    val totalPassesAttempted = totalPassesCompleted + stats.throwaway
    val passSuccessRate = calculateSafePercentage(totalPassesCompleted, totalPassesAttempted)

    val totalSuccesfulCatches = stats.catchStat + stats.goal
    val totalCatchesAttempted = totalSuccesfulCatches + stats.drop
    val catchRate = calculateSafePercentage(totalSuccesfulCatches, totalCatchesAttempted)

    // --- TAKIM ORTALAMALARI ---
    val teamAverages = remember(allPlayers, selectedTournamentId, selectedMatchId, allTournaments) {
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
                allTournaments = allTournaments
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

    // --- NUMARA SEÇİM DİALOGU ---
    val takenNumbers = remember(allPlayers, player) {
        allPlayers.filter { it.id != player.id && it.jerseyNumber != null }
            .associateBy { it.jerseyNumber!! }
    }

    if (showNumberPickerDialog) {
        AlertDialog(
            onDismissRequest = { showNumberPickerDialog = false },
            title = { Text("Forma Numarası Seç") },
            text = {
                // Numara seçimi için basit grid (JerseyGridScreen mantığıyla aynı)
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
                                Text("Yok", fontWeight = FontWeight.Bold)
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
                Button(onClick = {
                    showNumberPickerDialog = false
                }) { Text("Tamam") }
            }
        )
    }

    // --- TURNUVA FİLTRE DİALOGU ---
    if (showTournamentDropdown) {
        AlertDialog(
            onDismissRequest = { showTournamentDropdown = false },
            title = { Text("Turnuva Seç") },
            text = {
                LazyColumn {
                    item {
                        Text(
                            "Genel Kariyer İstatistikleri",
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

    // --- MAÇ FİLTRE DİALOGU ---
    if (showMatchDropdown && selectedTournament != null) {
        AlertDialog(
            onDismissRequest = { showMatchDropdown = false },
            title = { Text("Maç Seç") },
            text = {
                LazyColumn {
                    item {
                        Text(
                            "Tüm Turnuva Maçları",
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
                    // --- YENİ EKLENEN BUTON ---
                    val context = LocalContext.current
                    // We reuse the viewModel passed to the screen or get a new instance if needed
                    val vm: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

                    IconButton(onClick = {
                        // Calling the function we just added to MainViewModel
                        vm.sharePlayerReport(context, player, allTournaments, allPlayers)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Oyuncu Karnesi", tint = StitchColor.TextPrimary)
                    }
                },
                title = { Text("Oyuncu Profili") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { innerPadding ->
        var selectedTabIndex by remember { mutableStateOf(0) }
        val tabTitles = listOf("Oyuncu İstatistikleri", "Pas Ağı (Bağlantılar)")
        var showEfficiencyInfo by remember { mutableStateOf(false) }

        if (showEfficiencyInfo) {
            // MainActivity'nin en altındaki global fonksiyonu çağır
            EfficiencyDescriptionDialog(onDismiss = { showEfficiencyInfo = false })
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 1. YENİ PROFİL KARTI (GRADYANLI) ---
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
                                listOf(
                                    StitchGradientStart,
                                    StitchGradientEnd
                                )
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
                            Text(
                                "${player.position} ${if (player.isCaptain) "• Kaptan" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(0.8f)
                            )
                        }

                        if (isAdmin || isOwner) {
                            IconButton(onClick = { isEditModeExpanded = !isEditModeExpanded }) {
                                Icon(
                                    imageVector = if (isEditModeExpanded) Icons.Default.ExpandLess else Icons.Default.Edit,
                                    contentDescription = "Düzenle",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // --- 2. DÜZENLEME ALANI (AYRI BİR KART OLARAK EKLENDİ) ---
            if (isEditModeExpanded && (isAdmin || isOwner)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, com.example.ilkuygulamam.ui.theme.StitchPrimary.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Profili Düzenle", fontWeight = FontWeight.Bold, color = com.example.ilkuygulamam.ui.theme.StitchPrimary)

                        if (isAdmin) {
                            // --- YENİ E-POSTA SEÇİCİ ---
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = updatedEmail,
                                    onValueChange = { updatedEmail = it }, // Hala elle yazmaya izin veriyoruz
                                    label = { Text("E-posta (Eşleştirme)") },
                                    placeholder = { Text("Listeden seçin veya yazın") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    trailingIcon = {
                                        // Aşağı ok ikonu ile listeyi açma
                                        IconButton(onClick = { showEmailDropdown = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Listeyi Aç")
                                        }
                                    }
                                )

                                // Açılır Menü
                                DropdownMenu(
                                    expanded = showEmailDropdown,
                                    onDismissRequest = { showEmailDropdown = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f) // Genişlik ayarı
                                        .heightIn(max = 250.dp) // Çok uzun olursa kaydırma çubuğu çıksın
                                ) {
                                    if (teamMemberEmails.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Kayıtlı e-postası olan üye yok", color = Color.Gray) },
                                            onClick = { showEmailDropdown = false }
                                        )
                                    } else {
                                        // Üyeleri Listele
                                        teamMemberEmails.forEach { (_, name, email) ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                        Text(email, fontSize = 12.sp, color = Color.Gray)
                                                    }
                                                },
                                                onClick = {
                                                    updatedEmail = email // Seçileni kutuya yaz
                                                    showEmailDropdown = false
                                                }
                                            )
                                            Divider(modifier = Modifier.padding(horizontal = 16.dp))
                                        }
                                    }
                                }
                            }
                            // -----------------------------
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // İSİM (Sadece Admin Değiştirebilir)
                            OutlinedTextField(
                                value = updatedName,
                                onValueChange = { updatedName = it },
                                label = { Text("Ad Soyad") },
                                modifier = Modifier.weight(0.7f),
                                enabled = canEditEverything // <--- KİLİT
                            )
                            OutlinedTextField(
                                value = selectedJerseyNumber?.toString() ?: "Yok",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("No") },
                                // Tıklanabilirlik kontrolü:
                                modifier = Modifier.weight(0.3f)
                                    .clickable(enabled = canEditPhotoAndNumber) {
                                        showNumberPickerDialog = true
                                    },
                                enabled = false, // Görsel olarak enabled, tıklama modifier ile yönetiliyor
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color.Black,
                                    disabledBorderColor = if (canEditPhotoAndNumber) com.example.ilkuygulamam.ui.theme.StitchPrimary else Color.Gray,
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
                            Text("Takım Kaptanı", style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = updatedIsCaptain,
                                onCheckedChange = { updatedIsCaptain = it },
                                enabled = canEditEverything
                            )
                        }

                        Divider()

                        // --- BUTONLAR BURADA ---
                        if (canEditPhotoAndNumber) {
                            Text("Profil Fotoğrafı", style = MaterialTheme.typography.labelMedium, color = Color.Gray)

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Fotoğraf Yükle Butonu
                                OutlinedButton(
                                    onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (tempPhotoUri != null) "Değiştir" else "Yükle", fontSize = 12.sp)
                                }

                                // Fotoğraf Kaldır Butonu (Varsa göster)
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
                                        Text("Kaldır", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // KAYDET BUTONU
                        Button(
                            onClick = {
                                scope.launch {
                                    isSaving = true
                                    var finalPhotoUrl = currentPhotoUrl // Mevcut veya null (silindiyse)

                                    // Eğer yeni bir foto seçildiyse işle
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
                                        photoUrl = finalPhotoUrl // Null giderse silinir, dolu giderse güncellenir
                                    )
                                    onPlayerUpdate(updatedPlayer)
                                    isSaving = false
                                    isEditModeExpanded = false
                                    Toast.makeText(context, "Profil güncellendi", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = com.example.ilkuygulamam.ui.theme.StitchPrimary),
                            enabled = !isSaving
                        ) {
                            if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            else Text("DEĞİŞİKLİKLERİ KAYDET", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }


            // --- FİLTRELEME ALANI ---
            val selectedTournamentName = selectedTournament?.tournamentName ?: "Genel Kariyer"
            val selectedMatchName = if (selectedMatchId != null) {
                selectedTournament?.matches?.find { it.id == selectedMatchId }
                    ?.let { "vs ${it.opponentName}" } ?: "Bilinmeyen Maç"
            } else {
                "Tüm Maçlar"
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

            // --- TAB YAPISI ---
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) })
                }
            }

            // --- TAB İÇERİĞİ ---
            if (selectedTabIndex == 0) {
                // İSTATİSTİK TABI
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp) // Boşlukları artırdık
                ) {
                    // ÖZET KARTI (Eskisi yerine yenisini koyuyoruz)
                    // Artık GameTimeCard fonksiyonunu kullanacağız.
                    EfficiencyCard(
                        efficiencyScore = advancedStats.plusMinus,
                        onInfoClick = { showEfficiencyInfo = true }
                    )
                    GameTimeCard(
                        totalPoints = stats.pointsPlayed,
                        offensePoints = advancedStats.oPointsPlayed,
                        defensePoints = advancedStats.dPointsPlayed
                    )

                    // HANDLER / CUTTER KARTLARI (Sıralama aynı)
                    val isHandler = player.position == "Handler" || player.position == "Hybrid"
                    if (isHandler) {
                        PassingStatsCard(passSuccessRate, stats, teamAverages)
                        ReceivingStatsCard(catchRate, stats, teamAverages)
                    } else {
                        ReceivingStatsCard(catchRate, stats, teamAverages)
                        PassingStatsCard(passSuccessRate, stats, teamAverages)
                    }

                    // DEFANS KARTI (Yenisini kullanacağız)
                    DefenseStatsCard(stats, teamAverages)

                    // SİLME BUTONU
                    if (isAdmin) {
                        Button(
                            onClick = { onPlayerDelete(player) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Oyuncuyu Sil")
                        }
                    }

                    Spacer(Modifier.height(50.dp))
                }
            } else {
                // --- PAS AĞI TABI (GÜNCELLENDİ: KORD DİYAGRAMI + YÜZDELER) ---
                val totalPasses = stats.successfulPass + stats.assist

                // --- 1. VERİ ANALİZİ ---
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

                val handlerRatio =
                    if (totalPasses > 0) passesToHandlers.toFloat() / totalPasses else 0f
                val cutterRatio =
                    if (totalPasses > 0) passesToCutters.toFloat() / totalPasses else 0f

                Column(
                    modifier = Modifier.fillMaxWidth(), // Sadece genişliği doldur
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // --- YENİ: KORD DİYAGRAMI ---
                    if (stats.passDistribution.isNotEmpty()) {
                        PassNetworkChordDiagram(
                            mainPlayerName = player.name,
                            passDistribution = stats.passDistribution,
                            allPlayers = allPlayers
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    // ----------------------------

                    // --- 2. AKIŞ ANALİZİ KARTI (FLOW) ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                "Oyun Karakteri (Flow)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = StitchColor.TextPrimary
                            )
                            Spacer(Modifier.height(16.dp))

                            // Görsel Bar
                            Row(
                                modifier = Modifier.fillMaxWidth().height(24.dp)
                                    .clip(RoundedCornerShape(50)).background(Color(0xFFF0F0F0))
                            ) {
                                if (handlerRatio > 0) {
                                    Box(
                                        modifier = Modifier.fillMaxHeight().weight(handlerRatio)
                                            .background(com.example.ilkuygulamam.ui.theme.StitchPrimary)
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
                                        modifier = Modifier.size(
                                            10.dp
                                        ).background(com.example.ilkuygulamam.ui.theme.StitchPrimary, CircleShape)
                                    ); Spacer(Modifier.width(6.dp)); Text(
                                    "Handler'a Pas",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(
                                            10.dp
                                        ).background(StitchOffense, CircleShape)
                                    ); Spacer(Modifier.width(6.dp)); Text(
                                    "Cutter'a Pas",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // --- 3. BAĞLANTI LİSTESİ ---
                    Text(
                        "Detaylı Bağlantılar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = StitchColor.TextPrimary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                    )

                    if (stats.passDistribution.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("Veri yok.", color = Color.Gray) }
                    } else {
                        val sortedConnections =
                            stats.passDistribution.toList().sortedByDescending { it.second }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            sortedConnections.forEach { (receiverId, count) ->
                                val receiver = allPlayers.find { it.id == receiverId }
                                val receiverName = receiver?.name ?: "Bilinmeyen"
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
                                                color = if (isHandlerConnection) com.example.ilkuygulamam.ui.theme.StitchPrimary else StitchOffense,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        // YÜZDELİ GÖSTERİM (GÜNCELLENDİ)
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            modifier = Modifier.width(110.dp)
                                        ) {
                                            // DÜZELTME BURADA:
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
                                                color = if (isHandlerConnection) com.example.ilkuygulamam.ui.theme.StitchPrimary else StitchOffense,
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