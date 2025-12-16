package com.example.discbase

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.FileOutputStream
import android.app.Activity
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

enum class AppTheme { LIGHT, DARK, SYSTEM }

class MainViewModel(application: Application) : AndroidViewModel(application) {
    enum class UserProfileState { UNKNOWN, LOADING, NEEDS_CREATION, EXISTS }

    private val repository = TournamentRepository(application.applicationContext)
    private val auth = Firebase.auth

    // UI Mesajları
    private val _userMessage = MutableSharedFlow<String>(replay = 1)
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Auth State
    private val _isLoadingAuth = MutableSharedFlow<Boolean>(replay = 1).apply { tryEmit(true) }
    val isLoadingAuth: SharedFlow<Boolean> = _isLoadingAuth.asSharedFlow()
    val currentUser: StateFlow<FirebaseUser?> = repository.getAuthState()
        .map { _isLoadingAuth.tryEmit(false); it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _profileState = MutableStateFlow<UserProfileState>(UserProfileState.LOADING)
    val profileState: StateFlow<UserProfileState> = _profileState.asStateFlow()

    // --- AYARLAR ---
    private val _nameFormat = MutableStateFlow(NameFormat.FIRST_NAME_LAST_INITIAL)
    val nameFormat = _nameFormat.asStateFlow()
    fun updateNameFormat(format: NameFormat) { _nameFormat.value = format }

    private val _keepScreenOn = MutableStateFlow(false)
    val keepScreenOn = _keepScreenOn.asStateFlow()
    fun setKeepScreenOn(enabled: Boolean) { _keepScreenOn.value = enabled }

    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled = _vibrationEnabled.asStateFlow()
    fun setVibrationEnabled(enabled: Boolean) { _vibrationEnabled.value = enabled }

    private val _isLeftHanded = MutableStateFlow(false)
    val isLeftHanded = _isLeftHanded.asStateFlow()
    fun setLeftHanded(enabled: Boolean) { _isLeftHanded.value = enabled }

    // --- TEMA ---
    // Hata veren _appTheme burada tanımlanıyor
    private val _appTheme = MutableStateFlow(AppTheme.LIGHT)
    val appTheme = _appTheme.asStateFlow()
    fun updateTheme(theme: AppTheme) {
        _appTheme.value = theme
        viewModelScope.launch { repository.setAppTheme(theme.name) }
    }

    // Aktif Takım ID
    val activeTeamId: StateFlow<String?> = repository.getActiveTeamId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Takım Profili
    @OptIn(ExperimentalCoroutinesApi::class)
    val profile: StateFlow<TeamProfile> = activeTeamId.flatMapLatest { teamId ->
        if (teamId != null) repository.getProfile(teamId) else flowOf(TeamProfile())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TeamProfile())

    // Roller ve İstekler
    val currentUserRole: StateFlow<String?> = combine(currentUser, profile) { user, teamProfile ->
        user?.uid?.let { teamProfile.members[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val hasPendingRequests: StateFlow<Boolean> = combine(profile, currentUser) { teamProfile, user ->
        val myRole = user?.uid?.let { teamProfile.members[it] }
        val isAdmin = myRole == "admin"
        val hasPending = teamProfile.members.containsValue("pending")
        isAdmin && hasPending
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Kullanıcı Profilleri
    @OptIn(ExperimentalCoroutinesApi::class)
    val allUserProfiles: StateFlow<Map<String, UserProfile>> = combine(currentUser, profile) { user, teamProfile ->
        if (user != null && teamProfile.members.isNotEmpty()) {
            repository.getTeamMemberProfiles(teamProfile.members.keys.toList())
        } else flowOf(emptyMap())
    }.flatMapLatest { it }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Veri Akışları
    @OptIn(ExperimentalCoroutinesApi::class)
    val players: StateFlow<List<Player>> = activeTeamId.flatMapLatest { id ->
        if (id != null) repository.getPlayers(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val tournaments: StateFlow<List<Tournament>> = activeTeamId.flatMapLatest { id ->
        if (id != null) repository.getTournaments(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val trainings: StateFlow<List<Training>> = activeTeamId.flatMapLatest { id ->
        if (id != null) repository.getTrainings(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val userTeamsList: StateFlow<List<TeamProfile>> = currentUser.flatMapLatest { user ->
        if (user != null) repository.getUserTeams(user.uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    // --- YENİ: VERİ KAYIT MODU ---
    // Varsayılanı ADVANCED yaptık (Mevcut kullanıcılar alışkanlıklarını kaybetmesin)
    private val _captureMode = MutableStateFlow(CaptureMode.ADVANCED)
    val captureMode = _captureMode.asStateFlow()

    fun setCaptureMode(mode: CaptureMode) {
        _captureMode.value = mode
        viewModelScope.launch {
            repository.setCaptureMode(mode.name)
        }
    }
    // --- SÜRE YÖNETİMİ ---
    private val _timeTrackingEnabled = MutableStateFlow(false)
    val timeTrackingEnabled = _timeTrackingEnabled.asStateFlow()

    fun setTimeTrackingEnabled(enabled: Boolean) {
        _timeTrackingEnabled.value = enabled
        viewModelScope.launch {
            repository.setTimeTrackingEnabled(enabled)
        }
    }
    private val _proModeEnabled = MutableStateFlow(false)
    val proModeEnabled = _proModeEnabled.asStateFlow()

    fun setProModeEnabled(enabled: Boolean) {
        _proModeEnabled.value = enabled
        viewModelScope.launch {
            repository.setProModeEnabled(enabled)
        }
    }
    // INIT
    init {
        // 1. Kullanıcı Giriş Durumu ve Profil Kontrolü (Bunu silmiş olabilirsin, geri ekliyoruz)
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    repository.updateUserInfo(user)
                    _profileState.value = UserProfileState.LOADING
                    val p = repository.getUserProfile(user.uid)
                    _profileState.value = if (p == null || p.displayName.isNullOrBlank())
                        UserProfileState.NEEDS_CREATION
                    else
                        UserProfileState.EXISTS
                } else {
                    _profileState.value = UserProfileState.UNKNOWN
                }
            }
        }

        // 2. Süre Yönetimi Ayarını Dinle (Eklediğimiz özellik)
        viewModelScope.launch {
            repository.getTimeTrackingEnabled().collect {
                _timeTrackingEnabled.value = it
            }
        }

        // 3. Veri Kayıt Modunu (Simple/Advanced) Dinle (Üzerine yazdığın kısım)
        viewModelScope.launch {
            repository.getCaptureMode().collect { savedMode ->
                if (savedMode != null) {
                    try {
                        _captureMode.value = CaptureMode.valueOf(savedMode)
                    } catch (e: Exception) {
                        // Enum hatası olursa varsayılanda (ADVANCED) kalır, uygulama çökmez.
                    }
                }
            }
        }
        // Pro Mod Ayarını Dinle
        viewModelScope.launch {
            repository.getProModeEnabled().collect {
                _proModeEnabled.value = it
            }
        }
    }

    // --- FONKSİYONLAR ---
    fun formatPlayerName(fullName: String): String {
        val format = _nameFormat.value
        val parts = fullName.trim().split(" ")
        if (parts.size < 2) return fullName
        return when (format) {
            NameFormat.FULL_NAME -> fullName
            NameFormat.FIRST_NAME_LAST_INITIAL -> "${parts.dropLast(1).joinToString(" ")} ${parts.last().take(1)}."
            NameFormat.INITIAL_LAST_NAME -> "${parts.first().take(1)}. ${parts.last()}"
        }
    }

    fun saveManualProfile(name: String) = viewModelScope.launch {
        val uid = auth.currentUser?.uid ?: return@launch
        val success = repository.createManualUserProfile(uid, name, auth.currentUser?.email)
        if (success) _profileState.value = UserProfileState.EXISTS
    }

    fun selectActiveTeam(teamId: String) = viewModelScope.launch { repository.setActiveTeamId(teamId) }
    fun clearActiveTeam() = viewModelScope.launch { repository.clearActiveTeamId() }

    fun createNewTeam(name: String) = viewModelScope.launch {
        val uid = auth.currentUser?.uid ?: return@launch
        val id = repository.createNewTeam(uid, name)
        if (id != null) {
            repository.setActiveTeamId(id)
            _userMessage.emit("Takım oluşturuldu!")
        }
    }

    fun joinExistingTeam(teamId: String) = viewModelScope.launch {
        val uid = auth.currentUser?.uid ?: return@launch
        if (repository.checkIfTeamExists(teamId)) {
            val success = repository.joinExistingTeam(uid, teamId)
            if (success) _userMessage.emit("İstek gönderildi.") else _userMessage.emit("Hata.")
        } else _userMessage.emit("Takım bulunamadı.")
    }

    // CRUD İşlemleri
    private fun getCurrentTeamId() = activeTeamId.value

    fun savePlayer(player: Player) = viewModelScope.launch {
        val tid = getCurrentTeamId() ?: return@launch
        if (repository.savePlayer(tid, player)) _userMessage.emit("Kaydedildi.")
    }
    fun deletePlayer(player: Player) = viewModelScope.launch {
        val tid = getCurrentTeamId() ?: return@launch
        if (repository.deletePlayer(tid, player)) _userMessage.emit("Silindi.")
    }
    fun updatePlayerLimited(pid: String, photo: String?, num: Int?) = viewModelScope.launch {
        val tid = getCurrentTeamId() ?: return@launch
        repository.updatePlayerLimited(tid, pid, photo, num)
    }

    fun saveTournament(t: Tournament) = viewModelScope.launch {
        val tid = getCurrentTeamId() ?: return@launch
        if (repository.saveTournament(tid, t)) _userMessage.emit("Kaydedildi.")
    }
    fun deleteTournament(t: Tournament) = viewModelScope.launch {
        val tid = getCurrentTeamId() ?: return@launch
        if (repository.deleteTournament(tid, t)) _userMessage.emit("Silindi.")
    }

    // --- MATCH SAVING (DÜZELTİLDİ) ---
    fun saveMatch(tournamentId: String, match: Match) = viewModelScope.launch {
        val tid = getCurrentTeamId() ?: return@launch
        // Alt koleksiyona kaydet
        if (repository.saveMatch(tid, tournamentId, match)) _userMessage.emit("Maç kaydedildi.")
        else _userMessage.emit("Maç kaydedilemedi.")
    }

    fun deleteMatch(tournamentId: String, matchId: String) = viewModelScope.launch {
        val tid = getCurrentTeamId() ?: return@launch
        if (repository.deleteMatch(tid, tournamentId, matchId)) _userMessage.emit("Maç silindi.")
    }

    fun deleteLastPoint(tournamentId: String, matchId: String) = viewModelScope.launch {
        val tid = getCurrentTeamId() ?: return@launch
        // Mevcut turnuvalardan maçı bul
        val tournament = tournaments.value.find { it.id == tournamentId }
        val match = tournament?.matches?.find { it.id == matchId } ?: return@launch

        if (match.pointsArchive.isEmpty()) return@launch

        val newArchive = match.pointsArchive.dropLast(1)
        val newScoreUs = newArchive.count { it.whoScored == "US" }
        val newScoreThem = newArchive.count { it.whoScored == "THEM" }

        val updatedMatch = match.copy(
            pointsArchive = newArchive,
            scoreUs = newScoreUs,
            scoreThem = newScoreThem
        )
        // Sadece maçı güncelle
        repository.saveMatch(tid, tournamentId, updatedMatch)
    }

    fun saveTraining(t: Training) = viewModelScope.launch {
        val tid = getCurrentTeamId() ?: return@launch
        repository.saveTraining(tid, t)
    }
    fun deleteTraining(id: String) = viewModelScope.launch {
        val tid = getCurrentTeamId() ?: return@launch
        repository.deleteTraining(tid, id)
    }

    // --- PROFILE ---
    fun saveProfile(name: String, logo: String?, onSuccess: () -> Unit) = viewModelScope.launch {
        val tid = getCurrentTeamId() ?: return@launch
        val newProfile = profile.value.copy(teamName = name, logoPath = logo ?: profile.value.logoPath)
        if (repository.saveProfile(tid, newProfile)) onSuccess()
    }
    fun updateMemberRole(uid: String, role: String) = viewModelScope.launch {
        val tid = getCurrentTeamId() ?: return@launch
        repository.updateMemberRole(tid, uid, role)
    }
    fun removeMember(uid: String) = viewModelScope.launch {
        val tid = getCurrentTeamId() ?: return@launch
        repository.removeMember(tid, uid)
    }

    // --- BACKUP ---
    fun exportBackupAsJson(context: Context, players: List<Player>, tournaments: List<Tournament>) = viewModelScope.launch {
        try {
            val backup = BackupData(profile.value, players, tournaments, trainings.value)
            val jsonString = Json.encodeToString(backup)
            val filename = "backup_${System.currentTimeMillis()}.json"
            val fileOutputStream: FileOutputStream = context.openFileOutput(filename, Context.MODE_PRIVATE)
            fileOutputStream.write(jsonString.toByteArray())
            fileOutputStream.close()
            _userMessage.emit("Yedeklendi: $filename")
        } catch (e: Exception) { _userMessage.emit("Yedekleme hatası.") }
    }
    // MainViewModel.kt içine ekle

    // Kullanıcının seçtiği konuma (Uri) veriyi yazan fonksiyon
    // MainViewModel.kt

    // Kullanıcının seçtiği konuma (Uri) veriyi yazan ve DOĞRULAYAN fonksiyon
    fun saveBackupToUri(uri: Uri, players: List<Player>, tournaments: List<Tournament>) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        _userMessage.emit("Yedekleme yapılıyor, lütfen bekleyin...")

        try {
            val contentResolver = getApplication<Application>().contentResolver

            // 1. Veriyi Hazırla (Nesneyi oluştur)
            val backupOriginal = BackupData(profile.value, players, tournaments, trainings.value)
            val jsonString = Json.encodeToString(backupOriginal)

            // 2. Seçilen Uri'ye yaz
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            }

            // --- 3. DOĞRULAMA ADIMI (VERIFICATION) ---
            // Dosyayı hemen geri okuyup parse etmeyi deniyoruz.
            // Eğer dosya yarım yazıldıysa veya bozuksa, burada hata fırlatır.
            val inputStream = contentResolver.openInputStream(uri)
            val writtenContent = inputStream?.bufferedReader().use { it?.readText() }

            if (writtenContent.isNullOrBlank()) {
                throw Exception("Dosya oluşturuldu ama içi boş.")
            }

            // JSON'ı tekrar nesneye çevirmeyi dene (Parse Testi)
            val backupCheck = Json.decodeFromString<BackupData>(writtenContent)

            // İçerik sayılarını karşılaştır (Double Check)
            if (backupCheck.players.size == backupOriginal.players.size &&
                backupCheck.tournaments.size == backupOriginal.tournaments.size) {

                _userMessage.emit("✅ Yedekleme başarılı ve doğrulandı!")
            } else {
                throw Exception("Veri uyuşmazlığı tespit edildi.")
            }

        } catch (e: Exception) {
            Log.e("Backup", "Yedekleme hatası", e)
            _userMessage.emit("❌ Yedekleme başarısız: ${e.localizedMessage}")
        }
    }

    // --- GÜNCELLENMİŞ IMPORT FONKSİYONU ---
    fun importBackupFromJson(uri: Uri) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        _userMessage.emit("Yedek dosyası okunuyor...")

        try {
            // 1. Dosyayı Oku
            val contentResolver = getApplication<Application>().contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader().use { it?.readText() }

            if (jsonString.isNullOrBlank()) {
                _userMessage.emit("Hata: Dosya boş veya okunamadı.")
                return@launch
            }

            // 2. JSON'ı Parse Et
            // JSON yapısındaki olası eksik alanları tolere etmek için leniment modunu açıyoruz
            val jsonParser = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
            val backupData = jsonParser.decodeFromString<BackupData>(jsonString)

            // 3. Aktif Takıma Yükle
            val currentTeamId = activeTeamId.value
            if (currentTeamId != null) {
                _userMessage.emit("Veriler veritabanına yazılıyor...")

                val success = repository.restoreTeamData(currentTeamId, backupData)

                if (success) {
                    _userMessage.emit("Yedek başarıyla yüklendi! Lütfen sayfayı yenileyin.")
                } else {
                    _userMessage.emit("Hata: Veritabanına yazarken sorun oluştu.")
                }
            } else {
                _userMessage.emit("Hata: Önce bir takım seçmelisiniz.")
            }

        } catch (e: Exception) {
            Log.e("Import", "Json hatası", e)
            _userMessage.emit("Hata: Dosya formatı geçersiz. (${e.localizedMessage})")
        }
    }
    fun shareMatchReport(context: Context, match: Match, tournamentName: String, stats: List<AdvancedPlayerStats>) = viewModelScope.launch {
        _userMessage.emit("PDF Raporu hazırlanıyor...")
        try {
            val generator = PdfReportGenerator(context)
            val pdfFile = generator.generateMatchReport(match, tournamentName, stats)

            if (pdfFile != null) {
                shareFile(context, pdfFile)
            } else {
                _userMessage.emit("Hata: PDF oluşturulamadı.")
            }
        } catch (e: Exception) {
            Log.e("PDF", "Paylaşım hatası", e)
            _userMessage.emit("Paylaşım hatası: ${e.localizedMessage}")
        }
    }
    // MainViewModel.kt içinde...

    // Fonksiyona 'sourcePlayers' parametresini ekliyoruz
    fun shareTournamentReport(context: Context, tournament: Tournament, sourcePlayers: List<Player>) = viewModelScope.launch {
        _userMessage.emit("Turnuva raporu hazırlanıyor...")
        try {
            // 1. Takım İstatistikleri
            val teamStats = calculateTeamStatsForFilter(listOf(tournament), tournament.id)

            // 2. Oyuncu İstatistikleri
            val tournamentPoints = tournament.matches.flatMap { it.pointsArchive }

            // ARTIK BURADA 'players.value' YERİNE PARAMETRE OLARAK GELEN LİSTEYİ KULLANIYORUZ
            val tournamentPlayerStats = tournament.rosterPlayerIds.map { playerId ->
                // Parametre olarak gelen listeden oyuncuyu bul
                val player = sourcePlayers.find { it.id == playerId }

                // İstatistikleri hesapla
                val stats = calculateStatsFromPoints(playerId, tournamentPoints)

                // Oyuncu bulunduysa ismini, bulunamazsa varsayılanı kullan
                val finalName = player?.name ?: "Bilinmeyen Oyuncu"

                // İsim bilgisini stats objesine kopyala
                val statsWithName = stats.copy(basicStats = stats.basicStats.copy(name = finalName))

                statsWithName
            }

            val generator = PdfReportGenerator(context)
            val pdfFile = generator.generateTournamentReport(tournament, teamStats, tournamentPlayerStats)

            if (pdfFile != null) {
                shareFile(context, pdfFile)
            } else {
                _userMessage.emit("Hata: PDF oluşturulamadı.")
            }
        } catch (e: Exception) {
            Log.e("PDF", "Turnuva rapor hatası", e)
            _userMessage.emit("Rapor oluşturulamadı: ${e.localizedMessage}")
        }
    }

    // --- OYUNCU PROFİLİ PAYLAŞ ---
    // --- OYUNCU PROFİLİ PAYLAŞ ---
    // Parametreye 'allPlayers' eklendi
    fun sharePlayerReport(context: Context, player: Player, allTournaments: List<Tournament>, allPlayers: List<Player>) = viewModelScope.launch {
        _userMessage.emit("Oyuncu profili hazırlanıyor...")
        try {
            // Calculate global stats for this player
            val stats = calculateGlobalPlayerStats(player.id, "GENEL", null, allTournaments)

            val generator = PdfReportGenerator(context)

            // Parametreyi buraya geçiriyoruz
            val pdfFile = generator.generatePlayerReport(player, allTournaments, stats, allPlayers)

            if (pdfFile != null) {
                shareFile(context, pdfFile)
            } else {
                _userMessage.emit("Hata: PDF oluşturulamadı.")
            }
        } catch (e: Exception) {
            Log.e("PDF", "Oyuncu rapor hatası", e)
            _userMessage.emit("Rapor oluşturulamadı.")
        }
    }

    // HELPER: Share Intent
    private fun shareFile(context: Context, file: java.io.File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = android.content.Intent.createChooser(shareIntent, "Raporu Paylaş")
        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}


// Giriş işleminin durumunu tutmak için
sealed class SignInState {
    object Idle : SignInState()
    object Loading : SignInState()
    data class Success(val message: String) : SignInState()
    data class Error(val message: String) : SignInState()
}

class SignInViewModel : ViewModel() {

    private val auth = Firebase.auth

    private val _signInState = MutableStateFlow<SignInState>(SignInState.Idle)
    val signInState = _signInState.asStateFlow()

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        // --- ÖNEMLİ DÜZELTME BURADA ---
        // R.string.default_web_client_id yerine WEB_CLIENT_ID'nizi manuel olarak yapıştırın
        // Bu kimlik ...apps.googleusercontent.com ile biten kimliktir.
        val webClientId = "860197339001-4m2pci2vido0olf43dqp4a8p6eaipjmi.apps.googleusercontent.com" // <-- BURAYI DOLDURUN

        // YENİ: Google Drive'a dosya ekleme izni

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun handleGoogleSignInResult(result: ActivityResult, context: Context) {
        if (result.resultCode != Activity.RESULT_OK) {
            _signInState.value = SignInState.Error("Google girişi iptal edildi veya başarısız oldu.")
            return
        }

        viewModelScope.launch {
            _signInState.value = SignInState.Loading
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)!!
                val idToken = account.idToken!!

                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()

                _signInState.value = SignInState.Success("Giriş başarılı!")
                // --- GÜNCELLENMİŞ VE YENİ KOD BLOĞU ---

                // 1. Firebase'e giriş yap
                val authResult = auth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user

                // 2. Kullanıcı bilgisini ALIR ALMAZ 'users' koleksiyonuna yaz
                if (firebaseUser != null) {
                    // Repository'yi (Context ile) burada anlık oluştur
                    val repository = TournamentRepository(context.applicationContext)

                    // Giriş yapan kullanıcının adını 'users' koleksiyonuna hemen yaz/güncelle
                    repository.updateUserInfo(firebaseUser)
                }
                // --- KOD BLOĞU BİTTİ ---

            } catch (e: ApiException) {
                _signInState.value = SignInState.Error("Giriş hatası (ApiException): ${e.message}")
            } catch (e: Exception) {
                _signInState.value = SignInState.Error("Giriş hatası: ${e.message}")
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            auth.signOut()
            getGoogleSignInClient(context).signOut().await()
        }
    }
}
// MainViewModel.kt içine ekleyin

// --- TURNUVA RAPORU PAYLAŞ ---
