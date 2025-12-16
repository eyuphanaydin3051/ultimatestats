package com.example.discbase

// Bu satırları mevcut importların arasına yapıştır:
// Gerekli importlar
// --- YENİ EKLENMESİ GEREKEN KÜTÜPHANELER ---
// --- BİTTİ ---
import androidx.compose.material3.Surface
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.discbase.ui.theme.IlkuygulamamTheme
import kotlinx.serialization.Serializable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.FitnessCenter
// MainActivity.kt - En üst kısımlara bu importları ekleyin/güncelleyin

// --- İKON IMPORTLARI ---
import androidx.compose.material.icons.filled.Map
import com.example.discbase.ui.theme.StitchColor
import com.example.discbase.ui.theme.StitchPrimary

// ... diğer ikonlar ...



// --- TASARIM RENKLERİ (KESİN ÇÖZÜM) ---
val StitchPrimary = Color(0xFF6200EA) // Koyu Mor
val StitchSecondary = Color(0xFF7C4DFF) // Açık Mor
val StitchBackground = Color(0xFFF9F9F9) // Açık Gri Zemin
val StitchCardBg = Color(0xFFFFFFFF) // Beyaz Kart
val StitchTextPrimary = Color(0xFF212121) // Koyu Gri/Siyah Yazı
val StitchOffense = Color(0xFF00C49A) // Teal/Yeşil
val StitchDefense = Color(0xFFFF6B6B) // Kırmızı/Turuncu
val StitchGradientStart = Color(0xFF3A1078)
val StitchGradientEnd = Color(0xFF2F58CD)


enum class PointStartMode { OFFENSE, DEFENSE }








@Serializable
data class TeamProfile(
    val teamName: String = "Takım Adı Giriniz",
    val logoPath: String? = null,
    val teamId: String = "", // Davet Kodu olarak kullanılacak
    // Key = User ID, Value = "admin" veya "member"
    val members: Map<String, String> = emptyMap()
)


@Serializable

// Sayının o anki durumunun fotoğrafı (Geri alabilmek için)
data class PointStateSnapshot(
    val pointStats: List<PlayerStats> = emptyList(),
    val activePasserId: String? = null, // Pasör kimdi?
    val gameMode: GameMode = GameMode.IDLE, // Oyun ne durumdaydı? (Offense/Defense)
    val subbedOutStats: List<PlayerStats> = emptyList() // Kenarda bekleyen sakat oyuncular kimdi?
)
// YENİ EKLENEN YEDEKLEME VERİ MODELİ



// --- 2. İSTATİSTİK HESAPLAMA FONKSİYONLARI ---
// (Bu fonksiyonlar değişmedi, sadece AdvancedPlayerStats/AdvancedTeamStats tanımlarına ihtiyaçları vardı)


// MainActivity.kt içinde bu fonksiyonu bul ve TAMAMEN bununla değiştir:




// --- 3. ANA UYGULAMA GİRİŞİ ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // ViewModel'i burada al
            val mainViewModel: MainViewModel = viewModel()
            // Tema durumunu dinle
            val currentTheme by mainViewModel.appTheme.collectAsState()

            // Temayı buraya parametre olarak geçiyoruz
            IlkuygulamamTheme(appTheme = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UltimateStatsApp(mainViewModel = mainViewModel)
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Ana Sayfa", Icons.Default.Home)
    object Tournaments : Screen("tournaments", "Turnuvalar", Icons.Default.BarChart)
    object Roster : Screen("roster", "Kadro", Icons.Default.People)
    object Trainings : Screen("trainings", "Antrenman", Icons.Default.FitnessCenter)
    object ProMode : Screen("pro_mode", "Pro Mod", Icons.Default.Map)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Tournaments,
    Screen.Roster,
    Screen.Trainings,
)

// --- 4. NAVİGASYON YÖNETİCİSİ (GÜNCELLENDİ) ---
@Composable
fun UltimateStatsApp(
    signInViewModel: SignInViewModel = viewModel(),
    mainViewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // --- STATE TANIMLAMALARI ---
    val isLoadingAuth by mainViewModel.isLoadingAuth.collectAsState(initial = true)
    val currentUser by mainViewModel.currentUser.collectAsState()

    // Aktif Takım Verileri
    val activeTeamId by mainViewModel.activeTeamId.collectAsState()
    val teamProfile by mainViewModel.profile.collectAsState()
    val tournaments by mainViewModel.tournaments.collectAsState()
    val allPlayers by mainViewModel.players.collectAsState()

    // --- DÜZELTME BURADA: Tüm kullanıcı profillerini ViewModel'den çekiyoruz ---
    val allUserProfiles by mainViewModel.allUserProfiles.collectAsState()

    // Takım Seçim Ekranı İçin Liste
    val userTeamsList by mainViewModel.userTeamsList.collectAsState()

    // Yetki Kontrolü
    val currentUserRole by mainViewModel.currentUserRole.collectAsState()
    val isAdmin = currentUserRole == "admin"

    // Toast Mesajları
    LaunchedEffect(Unit) {
        mainViewModel.userMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    // --- NAVİGASYON BAŞLANGIÇ MANTIĞI ---
    val startDestination = when {
        isLoadingAuth -> "loading"
        currentUser == null -> "sign_in"
        // Kullanıcı var ama aktif takım seçilmemişse veya profil eksikse 'app_root'a git
        else -> "app_root"
    }

    NavHost(navController = navController, startDestination = startDestination) {

        // 1. YÜKLENİYOR EKRANI
        composable(route = "loading") {
            LoadingScreen("Giriş durumu kontrol ediliyor...")
        }

        // 2. GİRİŞ EKRANI
        composable(route = "sign_in") {
            // Eğer kullanıcı giriş yapmışsa ana ekrana yönlendir
            LaunchedEffect(currentUser) {
                if (currentUser != null) {
                    navController.navigate("app_root") {
                        popUpTo("sign_in") { inclusive = true }
                    }
                }
            }
            SignInScreen()
        }
        composable("settings") {
            // DEĞİŞİKLİK: Verileri SettingsScreen'e gönderiyoruz
            SettingsScreen(
                navController = navController,
                viewModel = mainViewModel,
                allPlayers = allPlayers,
                tournaments = tournaments
            )
        }
        // 3. UYGULAMA KÖKÜ (Profil/Takım Seçimi)
        composable(route = "app_root") {
            val profileState by mainViewModel.profileState.collectAsState()
            val currentActiveTeamId by mainViewModel.activeTeamId.collectAsState()

            when (profileState) {
                MainViewModel.UserProfileState.LOADING -> {
                    LoadingScreen("Profil kontrol ediliyor...")
                }
                MainViewModel.UserProfileState.NEEDS_CREATION -> {
                    CreateProfileScreen(
                        mainViewModel = mainViewModel,
                        onProfileCreated = {
                            // Profil oluşunca sayfayı yenilemek için kendine git
                            navController.navigate("app_root") {
                                popUpTo("app_root") { inclusive = true }
                            }
                        }
                    )
                }
                MainViewModel.UserProfileState.EXISTS -> {
                    if (currentActiveTeamId == null) {
                        // Takım seçimi
                        TeamSelectionScreen(
                            userTeamsList = userTeamsList,
                            onTeamSelected = { teamId ->
                                mainViewModel.selectActiveTeam(teamId)
                            },
                            // DÜZELTME BURADA: Çıkış yapma mantığını buraya ekledik
                            onSignOut = {
                                mainViewModel.clearActiveTeam()
                                signInViewModel.signOut(context)
                            },
                            viewModel = mainViewModel
                        )
                    } else {
                        // Takım seçiliyse ana iskelete git
                        LaunchedEffect(Unit) {
                            navController.navigate("main_scaffold") {
                                popUpTo("app_root") { inclusive = true }
                            }
                        }
                    }
                }
                MainViewModel.UserProfileState.UNKNOWN -> {
                    LoadingScreen("Çıkış yapılıyor...")
                }
            }

            // Kullanıcı çıkış yaparsa sign_in'e at
            LaunchedEffect(currentUser) {
                if (currentUser == null) {
                    navController.navigate("sign_in") { popUpTo(0) }
                }
            }
        }

        // 4. ANA UYGULAMA İSKELETİ (BottomNavigation'lı Ekran)
        composable(route = "main_scaffold") {
            // Güvenlik kontrolleri
            LaunchedEffect(currentUser) {
                if (currentUser == null) {
                    mainViewModel.clearActiveTeam()
                    navController.navigate("sign_in") { popUpTo(0) }
                }
            }
            LaunchedEffect(activeTeamId) {
                if (activeTeamId == null && currentUser != null) {
                    navController.navigate("app_root") { popUpTo(0) }
                }
            }

            MainAppScaffold(
                topLevelNavController = navController,
                tournaments = tournaments,
                teamProfile = teamProfile,
                allPlayers = allPlayers,
                isAdmin = isAdmin,
                activeTeamId = activeTeamId,
                mainViewModel = mainViewModel
            )
        }

        // --- DETAY SAYFALARI (Alt Menüde Olmayanlar) ---

        // Profil Düzenleme
        composable(route = "profile_edit") {
            ProfileEditScreen(
                navController = navController,
                onSignOut = {
                    mainViewModel.clearActiveTeam()
                    signInViewModel.signOut(context)
                },
                onChangeTeam = {
                    mainViewModel.clearActiveTeam()
                    navController.navigate("app_root") { popUpTo(0) }
                },
                allUserProfiles = allUserProfiles,
                allPlayers = allPlayers,
                allTournaments = tournaments
            )
        }
        // --- YENİ EKLENEN ROTA ---
        composable("team_stats") {
            TeamStatisticsScreen(
                navController = navController,
                teamProfile = teamProfile,
                tournaments = tournaments
            )
        }

        // Oyuncu Detay/Düzenleme
        // Oyuncu Detay/Düzenleme
        composable(
            route = "player_detail/{playerId}",
            arguments = listOf(navArgument("playerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val playerId = backStackEntry.arguments?.getString("playerId")
            val player = allPlayers.find { it.id == playerId }

            if (player != null) {
                PlayerEditScreen(
                    navController = navController,
                    player = player,
                    allPlayers = allPlayers,
                    allTournaments = tournaments,
                    // --- GÜNCELLEME BURADA ---
                    onPlayerUpdate = { updatedPlayer ->
                        if (isAdmin) {
                            // Admin ise her şeyi kaydet (Eski yöntem)
                            mainViewModel.savePlayer(updatedPlayer)
                        } else {
                            // Üye ise sadece fotoğraf ve numarayı güncelle (Yeni yöntem)
                            mainViewModel.updatePlayerLimited(
                                updatedPlayer.id,
                                updatedPlayer.photoUrl,
                                updatedPlayer.jerseyNumber
                            )
                        }
                        navController.popBackStack()
                    },
                    // -------------------------
                    onPlayerDelete = { mainViewModel.deletePlayer(it); navController.popBackStack() },
                    isAdmin = isAdmin
                )
            }
        }

        // Oyuncu Ekleme
        composable(route = "player_add") {
            PlayerAddScreen(
                navController = navController,
                onPlayerSave = { mainViewModel.savePlayer(it); navController.popBackStack() }
            )
        }

        // --- YENİ: FORMA NUMARALARI (Grid) ---
        composable("jersey_grid") {
            JerseyGridScreen(
                navController = navController,
                allPlayers = allPlayers
            )
        }

        // Turnuva Ekleme/Düzenleme
        composable(
            route = "tournament_setup?tournamentId={tournamentId}",
            arguments = listOf(navArgument("tournamentId") { type = NavType.StringType; nullable = true })
        ) { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getString("tournamentId")
            val tournamentToEdit = tournaments.find { it.id == tournamentId }

            TournamentSetupScreen(
                onTournamentSave = {
                    mainViewModel.saveTournament(it)
                    navController.popBackStack()
                },
                navController = navController,
                allPlayers = allPlayers,
                currentTeamName = teamProfile.teamName,
                tournamentToEdit = tournamentToEdit
            )
        }

        // Turnuva Detayı
        composable(
            route = "tournament_detail/{tournamentId}",
            arguments = listOf(navArgument("tournamentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getString("tournamentId")
            val tournament = tournaments.find { it.id == tournamentId }

            if (tournament != null) {
                TournamentDetailScreen(
                    navController = navController,
                    tournament = tournament,
                    allPlayers = allPlayers,
                    onStartNewMatch = { navController.navigate("match_setup/${tournament.id}") },
                    onDeleteTournament = {
                        mainViewModel.deleteTournament(tournament)
                        navController.popBackStack()
                    },
                    isAdmin = isAdmin
                )
            }
        }

        // Line Kurulumu (Listeleme)
        composable(
            route = "line_setup/{tournamentId}",
            arguments = listOf(navArgument("tournamentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getString("tournamentId")
            val tournament = tournaments.find { it.id == tournamentId }

            if (tournament != null) {
                LineSetupScreen(
                    navController = navController,
                    tournament = tournament,
                    onNavigateToEdit = { lineId ->
                        val route = "line_edit/${tournament.id}?lineId=$lineId"
                        navController.navigate(route)
                    }
                )
            } else {
                navController.popBackStack()
            }
        }

        // Line Ekleme/Düzenleme
        composable(
            route = "line_edit/{tournamentId}?lineId={lineId}",
            arguments = listOf(
                navArgument("tournamentId") { type = NavType.StringType },
                navArgument("lineId") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getString("tournamentId")
            val lineId = backStackEntry.arguments?.getString("lineId")
            val tournament = tournaments.find { it.id == tournamentId }

            if (tournament != null) {
                // allPlayers'a artık gerek yok, PresetLineAddScreen viewModel'den çekiyor
                val lineToEdit = if (lineId != "null" && lineId != null) {
                    tournament.presetLines.find { it.id == lineId }
                } else { null }

                PresetLineAddScreen( // <-- BİLEŞEN DEĞİŞTİ
                    navController = navController,
                    tournament = tournament,
                    lineToEdit = lineToEdit,
                    // viewModel otomatik olarak alınıyor
                )
            } else {
                navController.popBackStack()
            }
        }

        // Maç Kurulumu (İsim Girme)
        composable(
            route = "match_setup/{tournamentId}",
            arguments = listOf(navArgument("tournamentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getString("tournamentId")
            val tournament = tournaments.find { it.id == tournamentId }

            if (tournament != null) {
                MatchSetupScreen(
                    navController = navController,
                    ourTeamName = tournament.ourTeamName,
                    onMatchStarted = { opponentName ->
                        navController.navigate("match_playback/${tournament.id}/$opponentName?matchId=null")
                    }
                )
            } else {
                navController.popBackStack()
            }
        }

        // Maç Oynatma (Canlı Kayıt)
        // Maç Oynatma (Canlı Kayıt)
        // Maç Oynatma (Canlı Kayıt)
        composable(
            route = "match_playback/{tournamentId}/{opponentName}?matchId={matchId}",
            arguments = listOf(
                navArgument("tournamentId") { type = NavType.StringType },
                navArgument("opponentName") { type = NavType.StringType },
                navArgument("matchId") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getString("tournamentId")
            val opponentName = backStackEntry.arguments?.getString("opponentName")
            val matchId = backStackEntry.arguments?.getString("matchId")
            val tournament = tournaments.find { it.id == tournamentId }

            val matchToEdit = if (matchId != "null" && matchId != null) {
                tournament?.matches?.find { it.id == matchId }
            } else { null }

            if (tournament != null && opponentName != null) {
                val tournamentRoster = allPlayers.filter { tournament.rosterPlayerIds.contains(it.id) }

                MatchPlaybackHost(
                    mainViewModel = mainViewModel,
                    ourTeamName = tournament.ourTeamName,
                    opponentName = opponentName,
                    fullRoster = tournamentRoster,
                    matchToEdit = matchToEdit,
                    presetLines = tournament.presetLines,
                    onMatchFinished = { finishedMatch ->
                        // 1. Kullanıcıya bilgi ver
                        Toast.makeText(context, "Maç Kaydediliyor...", Toast.LENGTH_SHORT).show()

                        // 2. Veritabanına Kaydet
                        mainViewModel.saveMatch(tournament.id, finishedMatch)

                        // 3. Detay Sayfasına Yönlendir
                        navController.navigate("match_detail/${tournament.id}/${finishedMatch.id}") {
                            // Geri tuşuna basınca tekrar maç kaydına dönmemesi için:
                            popUpTo("tournament_detail/${tournament.id}") {
                                inclusive = false
                            }
                        }
                    },
                    onMatchReset = { /* Bellek sıfırlama, UI halleder */ },
                    onMatchUpdated = { /* Undo vs için */ }
                )
            } else {
                navController.popBackStack()
            }
        }

        // Maç Detayı (Sonuç Görüntüleme)
        // Maç Detayı (Sonuç Görüntüleme) - GÜNCELLENDİ
        // Maç Detayı (Sonuç Görüntüleme)
        composable(
            route = "match_detail/{tournamentId}/{matchId}",
            arguments = listOf(
                navArgument("tournamentId") { type = NavType.StringType },
                navArgument("matchId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getString("tournamentId")
            val matchId = backStackEntry.arguments?.getString("matchId")
            val tournament = tournaments.find { it.id == tournamentId }
            val match = tournament?.matches?.find { it.id == matchId }

            if (tournament != null && match != null) {
                val rosterPlayers = allPlayers.filter { tournament.rosterPlayerIds.contains(it.id) }
                MatchDetailScreen(
                    navController = navController,
                    viewModel = mainViewModel, // <-- EKSİK OLAN SATIR BU (EKLENDİ)
                    ourTeamName = tournament.ourTeamName,
                    opponentName = match.opponentName,
                    pointsArchive = match.pointsArchive,
                    rosterAsStats = rosterPlayers,
                    onBack = { navController.popBackStack() },
                    tournamentId = tournament.id,
                    match = match,
                    onDeleteMatch = {
                        mainViewModel.deleteMatch(tournament.id, matchId!!)
                        navController.popBackStack()
                    },
                    isAdmin = isAdmin,
                    onDeleteLastPoint = {
                        mainViewModel.deleteLastPoint(tournament.id, match.id)
                    }
                )
            } else if (tournament != null && match == null) {
                // Maç kaydedildi ama liste henüz güncellenmediyse bekleme ekranı göster
                LoadingScreen("Maç verisi senkronize ediliyor...")
            } else {
                // Turnuva bile yoksa çık
                navController.popBackStack()
            }
        }

        // Sayı Detayı (Salt Okunur)
        composable(
            route = "point_detail_summary/{tournamentId}/{matchId}/{pointIndex}",
            arguments = listOf(
                navArgument("tournamentId") { type = NavType.StringType },
                navArgument("matchId") { type = NavType.StringType },
                navArgument("pointIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getString("tournamentId")
            val matchId = backStackEntry.arguments?.getString("matchId")
            val pointIndex = backStackEntry.arguments?.getInt("pointIndex")
            val tournament = tournaments.find { it.id == tournamentId }
            val match = tournament?.matches?.find { it.id == matchId }
            val pointData = match?.pointsArchive?.getOrNull(pointIndex ?: -1)
            val statsForPoint = pointData?.stats
            val durationSeconds = pointData?.durationSeconds ?: 0L // Süreyi çekiyoruz

            if (statsForPoint != null) {
                val detailedStats = statsForPoint.map { stat ->
                    val playerName = allPlayers.find { it.id == stat.playerId }?.name ?: "Bilinmeyen"
                    stat.copy(name = playerName)
                }

                PointDetailScreen(
                    navController = navController,
                    pointIndex = pointIndex,
                    statsForPoint = detailedStats,
                    pointDuration = durationSeconds
                )
            } else {
                navController.popBackStack()
            }
        }

        // Sayı Düzenleme (Sonradan Değişiklik)
        composable(
            route = "edit_point/{tournamentId}/{matchId}/{pointIndex}",
            arguments = listOf(
                navArgument("tournamentId") { type = NavType.StringType },
                navArgument("matchId") { type = NavType.StringType },
                navArgument("pointIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getString("tournamentId")
            val matchId = backStackEntry.arguments?.getString("matchId")
            val pointIndex = backStackEntry.arguments?.getInt("pointIndex")

            val tournament = tournaments.find { it.id == tournamentId }
            val match = tournament?.matches?.find { it.id == matchId }
            val pointData = pointIndex?.let { match?.pointsArchive?.getOrNull(it) }

            val rosterPlayers = tournament?.rosterPlayerIds
                ?.mapNotNull { pId -> allPlayers.find { it.id == pId } }

            if (tournament != null && match != null && pointIndex != null && pointData != null && rosterPlayers != null) {
                PointEditHost(
                    navController = navController,
                    tournament = tournament,
                    match = match,
                    pointIndex = pointIndex,
                    pointData = pointData,
                    fullRoster = rosterPlayers,
                    // --- BURASI DEĞİŞTİ ---
                    onMatchUpdated = { updatedMatch ->
                        // Artık saveTournament DEĞİL, saveMatch kullanıyoruz.
                        // Bu fonksiyon alt koleksiyonu günceller.
                        mainViewModel.saveMatch(tournament.id, updatedMatch)

                        // Ekran zaten PointEditHost içinde kapanıyor (navController.popBackStack ile)
                        // O yüzden burada tekrar çağırmaya gerek yok, ama garanti olsun diye saveMatch asenkron olduğu için UI'da bir şey yapmıyoruz.
                    }
                    // ----------------------
                )
            } else {
                Toast.makeText(context, "Sayı detayı yüklenemedi.", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
        }
        // 1. YENİ ROTA: Sezon Maçları (Tüm Fikstür)
        composable("season_matches") {
            SeasonMatchesScreen(
                navController = navController,
                tournaments = tournaments
            )
        }

// 2. YENİ ROTA: Oyuncu İstatistik Sıralaması (Leaderboard)
        composable("player_leaderboard") {
            PlayerLeaderboardScreen(
                navController = navController,
                allPlayers = allPlayers,
                tournaments = tournaments
            )
        }
    }
}
@Composable
fun ProModeScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(StitchBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(64.dp), tint = StitchPrimary)
            Spacer(Modifier.height(16.dp))
            Text("Pro Mod Sahası", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = StitchColor.TextPrimary)
            Text("Gelişmiş saha analizleri yakında...", color = Color.Gray)
        }
    }
}






