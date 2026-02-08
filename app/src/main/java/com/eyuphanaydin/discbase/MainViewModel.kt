package com.eyuphanaydin.discbase

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
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import com.android.billingclient.api.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.Source // Offline kontrolü için gerekebilir
enum class AppTheme { LIGHT, DARK, SYSTEM }

sealed class SignInState {
    object Idle : SignInState()
    object Loading : SignInState()
    data class Success(val message: String) : SignInState()
    data class Error(val message: String) : SignInState()

}
class MainViewModel(application: Application) : AndroidViewModel(application) {
    enum class UserProfileState { UNKNOWN, LOADING, NEEDS_CREATION, EXISTS }

    private val repository = TournamentRepository(application.applicationContext)
    private val auth = Firebase.auth
    val currentUserId: String? get() = com.google.firebase.ktx.Firebase.auth.currentUser?.uid
    // UI Mesajları
    private val _userMessage = MutableSharedFlow<String>(replay = 1)
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()
    // MainViewModel sınıfının değişkenlerinin olduğu yere ekle:
    private val _signInState = MutableStateFlow<SignInState>(SignInState.Loading)
    val signInState = _signInState.asStateFlow()
    // Auth State
    private val _isLoadingAuth = MutableSharedFlow<Boolean>(replay = 1).apply { tryEmit(true) }
    val isLoadingAuth: SharedFlow<Boolean> = _isLoadingAuth.asSharedFlow()
    val currentUser: StateFlow<FirebaseUser?> = repository.getAuthState()
        .map { _isLoadingAuth.tryEmit(false); it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _profileState = MutableStateFlow<UserProfileState>(UserProfileState.LOADING)
    val profileState: StateFlow<UserProfileState> = _profileState.asStateFlow()
    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()
    // -------------------
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
    // --- ONBOARDING ---
    private val _onboardingCompleted = MutableStateFlow(false)
    val onboardingCompleted = _onboardingCompleted.asStateFlow()
    private val _isCheckingOnboarding = MutableStateFlow(true)
    val isCheckingOnboarding = _isCheckingOnboarding.asStateFlow()

    // Aktif Takım ID
    val activeTeamId: StateFlow<String?> = repository.getActiveTeamId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Takım Profili
    @OptIn(ExperimentalCoroutinesApi::class)
    val profile: StateFlow<TeamProfile> = activeTeamId.flatMapLatest { teamId ->
        if (teamId != null) repository.getProfile(teamId) else flowOf(TeamProfile())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TeamProfile())
    val efficiencyCriteria: StateFlow<List<EfficiencyCriterion>> = profile
        .map { it.efficiencyCriteria }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
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
    // --- ABONELİK SİSTEMİ (GÜVENLİ VERSİYON) ---
    private val _isPremium = MutableStateFlow(false)
    val isPremium = _isPremium.asStateFlow()
    // --- YENİ: Fiyat Bilgisi ---
    private val _monthlyPrice = MutableStateFlow("") // Örn: "₺29.99"
    val monthlyPrice = _monthlyPrice.asStateFlow()
    // Listener'ı ayrı tanımlamak yerine doğrudan build içinde veriyoruz (En güvenli yöntem)
    private val billingClient = BillingClient.newBuilder(application)
        .setListener { billingResult, purchases ->
            // Listener burada güvenli bir şekilde çalışır
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
        }
        .enablePendingPurchases()
        .build()

    // Bağlantıyı Başlat
    fun startBillingConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    checkPurchases()
                    fetchProductPrices() // <--- BUNU EKLEMELİSİNİZ
                }
            }
            override fun onBillingServiceDisconnected() {
                // Tekrar bağlanmayı dene
            }
        })
    }

    // 2. Satın Almayı Doğrulama ve Onaylama (EN ÖNEMLİ KISIM)
    private fun handlePurchase(purchase: Purchase) {
        // Satın alınan ürün bizim ürün mü?
        if (purchase.products.contains("advanced_mode_monthly") && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {

            // UI'ı güncelle (Premium aktif)
            _isPremium.value = true

            // Eğer ürün henüz onaylanmadıysa (Acknowledge), onayla.
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d("Billing", "Satın alma onaylandı/teslim edildi.")
                    }
                }
            }
        }
    }

    // Mevcut satın alımları kontrol et (Uygulama açılınca)
    private fun checkPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                // Listede aktif abonelik var mı bak
                val hasPremium = purchases.any {
                    it.products.contains("advanced_mode_monthly") && it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                _isPremium.value = hasPremium

                // Varsa ve onaylanmamışsa yine onayla
                purchases.filter { it.products.contains("advanced_mode_monthly") && !it.isAcknowledged }.forEach { handlePurchase(it) }
            }
        }
    }
    // Fiyatı Play Store'dan öğrenip değişkene kaydeder
    private fun fetchProductPrices() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("advanced_mode_monthly")
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { result, productDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                val offer = productDetails.subscriptionOfferDetails?.firstOrNull()
                // Fiyatı alıp değişkene atıyoruz
                val price = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                if (price != null) {
                    _monthlyPrice.value = price
                }
            }
        }
    }

    // Satın Alma Ekranını Başlat
    fun launchPurchaseFlow(activity: Activity) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("advanced_mode_monthly")
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { result, productDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]

                // Abonelik teklif token'ını al
                val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return@queryProductDetailsAsync

                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(offerToken)
                                .build()
                        )
                    )
                    .build()
                billingClient.launchBillingFlow(activity, flowParams)
            } else {
                viewModelScope.launch { _userMessage.emit("Ürün bilgisi alınamadı. Play Console ayarlarını kontrol edin.") }
            }
        }
    }
    // INIT
    // INIT BLOĞU (GÜNCELLENMİŞ)
    // MainViewModel içindeki init bloğu:
    init {
        // 1. Kullanıcı Giriş Durumu (GÜNCELLENDİ: Offline Korumalı)
        checkUserLoggedIn()

        // 2. Diğer Ayarları Dinle
        viewModelScope.launch {
            repository.getTimeTrackingEnabled().collect { _timeTrackingEnabled.value = it }
        }
        viewModelScope.launch {
            repository.getProModeEnabled().collect { _proModeEnabled.value = it }
        }

        // 3. Mod Ayarı
        viewModelScope.launch {
            repository.getCaptureMode().collect { savedMode ->
                if (savedMode != null) {
                    try {
                        _captureMode.value = CaptureMode.valueOf(savedMode)
                    } catch (e: Exception) { /* Enum hatası yutulur */ }
                }
            }
        }

        // 4. ABONELİK BAŞLATMA
        try {
            startBillingConnection()
        } catch (e: Exception) {
            Log.e("BillingError", "Abonelik sistemi başlatılamadı: ${e.message}")
        }
        // Tanıtım ekranı durumunu dinle
        viewModelScope.launch {
            repository.getOnboardingCompleted().collect { completed ->
                _onboardingCompleted.value = completed
                // Veri geldi, artık kontrol bitti. Yüklemeyi durdur.
                _isCheckingOnboarding.value = false
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
            val msg = getApplication<Application>().getString(R.string.msg_team_created)
            _userMessage.emit(msg)
        }
    }

    fun joinExistingTeam(teamId: String) = viewModelScope.launch {
        val context = getApplication<Application>()
        val uid = auth.currentUser?.uid ?: return@launch
        if (repository.checkIfTeamExists(teamId)) {
            val success = repository.joinExistingTeam(uid, teamId)
            if (success) {
                _userMessage.emit(context.getString(R.string.msg_request_sent))
            } else {
                _userMessage.emit(context.getString(R.string.error_generic))
            }
        } else {
            _userMessage.emit(context.getString(R.string.msg_team_not_found))
        }
    }

    // CRUD İşlemleri
    private fun getCurrentTeamId() = activeTeamId.value

    fun savePlayer(player: Player) = viewModelScope.launch {
        val tid = getCurrentTeamId() ?: return@launch
        if (repository.savePlayer(tid, player)) {
            _userMessage.emit(getApplication<Application>().getString(R.string.msg_saved))
        }
    }

    fun deletePlayer(player: Player) = viewModelScope.launch {
        val tid = getCurrentTeamId() ?: return@launch
        if (repository.deletePlayer(tid, player)) {
            _userMessage.emit(getApplication<Application>().getString(R.string.msg_deleted))
        }
    }
    fun updatePlayerLimited(pid: String, photo: String?, num: Int?) = viewModelScope.launch {
        val tid = getCurrentTeamId() ?: return@launch
        repository.updatePlayerLimited(tid, pid, photo, num)
    }

    fun saveTournament(t: Tournament) = viewModelScope.launch {
        val tid = getCurrentTeamId() ?: return@launch
        if (repository.saveTournament(tid, t)) {
            _userMessage.emit(getApplication<Application>().getString(R.string.msg_saved))
        }
    }

    fun deleteTournament(t: Tournament) = viewModelScope.launch {
        val tid = getCurrentTeamId() ?: return@launch
        if (repository.deleteTournament(tid, t)) {
            _userMessage.emit(getApplication<Application>().getString(R.string.msg_deleted))
        }
    }

    // MainViewModel.kt içindeki saveMatch fonksiyonunu bununla değiştir:

    // onSuccess parametresi eklendi
    fun saveMatch(tournamentId: String, match: Match, onSuccess: () -> Unit = {}) = viewModelScope.launch {
        val context = getApplication<Application>()
        val tid = getCurrentTeamId() ?: return@launch

        // Repository işlemi bitene kadar bekle (async değil, sonucunu bekliyoruz)
        val success = repository.saveMatch(tid, tournamentId, match)

        if (success) {
            _userMessage.emit(context.getString(R.string.msg_match_saved))
            // Kayıt başarılıysa sayfayı değiştirmesi için UI'a haber ver
            onSuccess()
        } else {
            _userMessage.emit(context.getString(R.string.msg_match_save_error))
        }
    }

    fun deleteMatch(tournamentId: String, matchId: String) = viewModelScope.launch {
        val tid = getCurrentTeamId() ?: return@launch
        if (repository.deleteMatch(tid, tournamentId, matchId)) {
            _userMessage.emit(getApplication<Application>().getString(R.string.msg_match_deleted))
        }
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
    // MainViewModel.kt içinde

    // MainViewModel.kt içinde deleteTeam fonksiyonunu bununla değiştir:

    fun deleteTeam(teamId: String) = viewModelScope.launch {
        val context = getApplication<Application>()

        if (activeTeamId.value == teamId) {
            clearActiveTeam()
        }

        try {
            val db = FirebaseFirestore.getInstance()
            val batch = db.batch()

            // --- DÜZELTME: Verileri "Root" yerine "Takımın İçinden" siliyoruz ---

            // 1. OYUNCULARI SİL (path: teams/{teamId}/players)
            val playersSnapshot = db.collection("teams").document(teamId)
                .collection("players")
                .get()
                .await()
            for (document in playersSnapshot) {
                batch.delete(document.reference)
            }

            // 2. TURNUVALARI VE MAÇLARI SİL
            // (path: teams/{teamId}/tournaments)
            val tournamentsSnapshot = db.collection("teams").document(teamId)
                .collection("tournaments")
                .get()
                .await()

            for (tourDoc in tournamentsSnapshot) {
                // Önce bu turnuvanın içindeki MAÇLARI bul ve sil (Sub-sub collection)
                val matchesSnapshot = tourDoc.reference.collection("matches").get().await()
                for (matchDoc in matchesSnapshot) {
                    batch.delete(matchDoc.reference)
                }
                // Sonra turnuvanın kendisini sil
                batch.delete(tourDoc.reference)
            }

            // 3. ANTRENMANLARI SİL (path: teams/{teamId}/trainings)
            val trainingsSnapshot = db.collection("teams").document(teamId)
                .collection("trainings")
                .get()
                .await()
            for (document in trainingsSnapshot) {
                batch.delete(document.reference)
            }

            // 4. TAKIMIN KENDİSİNİ SİL
            val teamRef = db.collection("teams").document(teamId)
            batch.delete(teamRef)

            batch.commit().await()

            _userMessage.emit(context.getString(R.string.msg_team_deleted))
            fetchUserProfile()

        } catch (e: Exception) {
            Log.e("DeleteTeam", "Hata: ${e.message}")
            _userMessage.emit(context.getString(R.string.error_generic))
        }
    }
    // --- PROFILE ---
    fun saveProfile(name: String, logo: String?, onSuccess: () -> Unit) = viewModelScope.launch {
        val tid = getCurrentTeamId() ?: return@launch
        val newProfile = profile.value.copy(teamName = name, logoPath = logo ?: profile.value.logoPath)
        if (repository.saveProfile(tid, newProfile)) onSuccess()
    }
    fun updateEfficiencyCriteria(newCriteria: List<EfficiencyCriterion>) = viewModelScope.launch {
        val tid = activeTeamId.value ?: return@launch
        val updatedProfile = profile.value.copy(efficiencyCriteria = newCriteria)
        repository.saveProfile(tid, updatedProfile)
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

            // Formatlı String kullanımı
            _userMessage.emit(context.getString(R.string.msg_backup_filename, filename))
        } catch (e: Exception) {
            _userMessage.emit(context.getString(R.string.msg_backup_error))
        }
    }

    fun saveBackupToUri(uri: Uri, players: List<Player>, tournaments: List<Tournament>) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        val context = getApplication<Application>()
        _userMessage.emit(context.getString(R.string.msg_backup_progress))

        try {
            val contentResolver = context.contentResolver

            // 1. Veriyi Hazırla
            val backupOriginal = BackupData(profile.value, players, tournaments, trainings.value)
            val jsonString = Json.encodeToString(backupOriginal)

            // 2. Seçilen Uri'ye yaz
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            }

            // 3. DOĞRULAMA (VERIFICATION)
            val inputStream = contentResolver.openInputStream(uri)
            val writtenContent = inputStream?.bufferedReader().use { it?.readText() }

            if (writtenContent.isNullOrBlank()) {
                throw Exception(context.getString(R.string.msg_file_empty))
            }

            val backupCheck = Json.decodeFromString<BackupData>(writtenContent)

            if (backupCheck.players.size == backupOriginal.players.size &&
                backupCheck.tournaments.size == backupOriginal.tournaments.size) {

                _userMessage.emit(context.getString(R.string.msg_backup_verified))
            } else {
                throw Exception(context.getString(R.string.msg_data_mismatch))
            }

        } catch (e: Exception) {
            Log.e("Backup", "Yedekleme hatası", e)
            _userMessage.emit(context.getString(R.string.msg_backup_failed, e.localizedMessage))
        }
    }
    // MainViewModel.kt içine ekle

    // Kullanıcının seçtiği konuma (Uri) veriyi yazan fonksiyon
    // MainViewModel.kt

    // Kullanıcının seçtiği konuma (Uri) veriyi yazan ve DOĞRULAYAN fonksiyon

    // --- GÜNCELLENMİŞ IMPORT FONKSİYONU ---
    fun importBackupFromJson(uri: Uri) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        val context = getApplication<Application>()
        _userMessage.emit(context.getString(R.string.msg_reading_backup))

        try {
            // 1. Dosyayı Oku
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader().use { it?.readText() }

            if (jsonString.isNullOrBlank()) {
                _userMessage.emit(context.getString(R.string.msg_file_empty_error))
                return@launch
            }

            // 2. JSON'ı Parse Et
            val jsonParser = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
            val backupData = jsonParser.decodeFromString<BackupData>(jsonString)

            // 3. Aktif Takıma Yükle
            val currentTeamId = activeTeamId.value
            if (currentTeamId != null) {
                _userMessage.emit(context.getString(R.string.msg_writing_db))

                val success = repository.restoreTeamData(currentTeamId, backupData)

                if (success) {
                    _userMessage.emit(context.getString(R.string.msg_restore_success))
                } else {
                    _userMessage.emit(context.getString(R.string.msg_db_write_error))
                }
            } else {
                _userMessage.emit(context.getString(R.string.msg_select_team_first))
            }

        } catch (e: Exception) {
            Log.e("Import", "Json hatası", e)
            _userMessage.emit(context.getString(R.string.msg_invalid_format, e.localizedMessage))
        }
    }
    fun shareMatchReport(context: Context, match: Match, tournamentName: String, stats: List<AdvancedPlayerStats>) = viewModelScope.launch {
        // Context parametresi dışarıdan geliyor, onu kullanıyoruz.
        _userMessage.emit(context.getString(R.string.msg_preparing_pdf))
        try {
            val generator = PdfReportGenerator(context)
            val pdfFile = generator.generateMatchReport(match, tournamentName, stats)

            if (pdfFile != null) {
                shareFile(context, pdfFile)
            } else {
                _userMessage.emit(context.getString(R.string.msg_pdf_error))
            }
        } catch (e: Exception) {
            Log.e("PDF", "Paylaşım hatası", e)
            _userMessage.emit(context.getString(R.string.msg_share_error, e.localizedMessage))
        }
    }
    // ... shareMatchReport vb. fonksiyonların altına ekle ...

    fun shareTrainingReport(context: Context, training: Training) = viewModelScope.launch {
        _userMessage.emit(context.getString(R.string.msg_preparing_pdf))
        try {
            val generator = PdfReportGenerator(context)
            // Mevcut oyuncu listesini state'den alıyoruz
            val pdfFile = generator.generateTrainingReport(training, players.value)

            if (pdfFile != null) {
                shareFile(context, pdfFile)
            } else {
                _userMessage.emit(context.getString(R.string.msg_pdf_creation_error))
            }
        } catch (e: Exception) {
            _userMessage.emit("Error: ${e.localizedMessage}")
        }
    }

    fun shareTournamentReport(context: Context, tournament: Tournament, sourcePlayers: List<Player>) = viewModelScope.launch {
        _userMessage.emit(context.getString(R.string.msg_preparing_tour_report))
        try {
            val teamStats = calculateTeamStatsForFilter(listOf(tournament), tournament.id)
            val tournamentPoints = tournament.matches.flatMap { it.pointsArchive }

            val tournamentPlayerStats = tournament.rosterPlayerIds.map { playerId ->
                val player = sourcePlayers.find { it.id == playerId }
                val stats = calculateStatsFromPoints(playerId, tournamentPoints)

                // "Bilinmeyen Oyuncu" -> Resource kullanımı
                val finalName = player?.name ?: context.getString(R.string.msg_unknown_player)

                val statsWithName = stats.copy(basicStats = stats.basicStats.copy(name = finalName))
                statsWithName
            }

            val generator = PdfReportGenerator(context)
            val pdfFile = generator.generateTournamentReport(tournament, teamStats, tournamentPlayerStats)

            if (pdfFile != null) {
                shareFile(context, pdfFile)
            } else {
                _userMessage.emit(context.getString(R.string.msg_pdf_error))
            }
        } catch (e: Exception) {
            Log.e("PDF", "Turnuva rapor hatası", e)
            _userMessage.emit(context.getString(R.string.msg_report_create_error, e.localizedMessage))
        }
    }

    fun sharePlayerReport(context: Context, player: Player, allTournaments: List<Tournament>, allPlayers: List<Player>) = viewModelScope.launch {
        _userMessage.emit(context.getString(R.string.msg_preparing_profile))
        try {
            val stats = calculateGlobalPlayerStats(player.id, "GENEL", null, allTournaments)
            val generator = PdfReportGenerator(context)
            val pdfFile = generator.generatePlayerReport(player, allTournaments, stats, allPlayers)

            if (pdfFile != null) {
                shareFile(context, pdfFile)
            } else {
                _userMessage.emit(context.getString(R.string.msg_pdf_error))
            }
        } catch (e: Exception) {
            Log.e("PDF", "Oyuncu rapor hatası", e)
            _userMessage.emit(context.getString(R.string.msg_report_create_error, e.localizedMessage))
        }
    }



    // HELPER: Share Intent
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

        // GÜNCELLENDİ: Başlık artık strings.xml'den geliyor
        val title = context.getString(R.string.chooser_share_report)
        val chooser = android.content.Intent.createChooser(shareIntent, title)

        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
    // --- TOPLU YOKLAMA LİSTESİ (EXCEL/CSV) ---
    fun exportAttendanceToCSV(context: Context, players: List<Player>, trainings: List<Training>) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        // Kullanıcıya bilgi ver
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            Toast.makeText(context, context.getString(R.string.msg_preparing_csv), Toast.LENGTH_SHORT).show()
        }

        try {
            // 1. Antrenmanları Tarihe Göre Sırala
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val sortedTrainings = trainings.sortedBy {
                try { sdf.parse(it.date)?.time ?: 0L } catch (e: Exception) { 0L }
            }

            // 2. CSV İçeriğini Oluştur
            val sb = StringBuilder()
            sb.append("\uFEFF") // Excel Türkçe karakter sorunu için BOM

            // -- BAŞLIK SATIRI --
            sb.append("${context.getString(R.string.csv_header_name)};")
            sb.append("${context.getString(R.string.csv_header_rate)};")
            sb.append("${context.getString(R.string.csv_header_total)}")

            sortedTrainings.forEach { training ->
                val shortDate = try {
                    val dateObj = sdf.parse(training.date)
                    val formatShort = java.text.SimpleDateFormat("dd MMM", java.util.Locale("tr"))
                    formatShort.format(dateObj!!)
                } catch (e: Exception) {
                    training.date
                }
                sb.append(";$shortDate")
            }
            sb.append("\n")

            // -- OYUNCU SATIRLARI --
            players.sortedBy { it.name }.forEach { player ->
                var attendedCount = 0
                val attendanceStatuses = StringBuilder()

                sortedTrainings.forEach { training ->
                    val isPresent = training.attendeeIds.contains(player.id)
                    if (isPresent) {
                        attendedCount++
                        attendanceStatuses.append(";${context.getString(R.string.csv_val_present)}")
                    } else {
                        attendanceStatuses.append(";${context.getString(R.string.csv_val_absent)}")
                    }
                }

                val totalTrainings = sortedTrainings.size
                val percentage = if (totalTrainings > 0) (attendedCount.toDouble() / totalTrainings) * 100 else 0.0
                val percentageStr = String.format("%.2f%%", percentage)

                sb.append("${player.name};$percentageStr;$attendedCount$attendanceStatuses\n")
            }

            // 3. Dosyayı Kaydet
            val fileName = "Yoklama_Cizelgesi_${System.currentTimeMillis()}.csv"
            val file = java.io.File(context.cacheDir, fileName)
            java.io.FileOutputStream(file).use { output ->
                output.write(sb.toString().toByteArray(java.nio.charset.StandardCharsets.UTF_8))
            }

            // 4. Paylaş
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                shareFile(context, file, "text/csv") // CSV türü gönderiyoruz
            }

        } catch (e: Exception) {
            e.printStackTrace()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.msg_csv_error), Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- GENEL DOSYA PAYLAŞIM FONKSİYONU (GÜNCELLENMİŞ) ---
    // Bu fonksiyon hem PDF hem CSV paylaşmak için kullanılır.
    // mimeType parametresi eklendi (varsayılan "application/pdf").
    private fun shareFile(context: Context, file: java.io.File, mimeType: String = "application/pdf") {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)

            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, file.name)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val title = context.getString(R.string.chooser_share_report)
            val chooser = android.content.Intent.createChooser(shareIntent, title)
            chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e("ShareFile", "Hata: ${e.message}")
            Toast.makeText(context, "Paylaşım hatası: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    // --- BİREYSEL OYUNCU MODU (CAREER MODE) ---

    // Bu veri sınıfını MainViewModel sınıfının İÇİNE ekleyin
    // MainViewModel içinde (yaklaşık 595. satır civarı olabilir)
    data class TeamCareerData(
        val teamProfile: TeamProfile,
        val player: Player,
        val tournaments: List<Tournament>,
        val allTeamPlayers: List<Player> // <-- YENİ EKLENDİ
    )
    private val _careerData = MutableStateFlow<List<TeamCareerData>>(emptyList())
    val careerData = _careerData.asStateFlow()

    private val _isLoadingCareer = MutableStateFlow(false)
    val isLoadingCareer = _isLoadingCareer.asStateFlow()

    // Kullanıcının e-postasının eşleştiği tüm takımları ve verileri getirir
    fun loadCareerData() = viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val email = user.email ?: return@launch

            _isLoadingCareer.value = true

            val teams = repository.getUserTeams(user.uid).first()
            val collectedData = mutableListOf<TeamCareerData>()

            teams.forEach { team ->
                // Takım oyuncularını çek
                val teamPlayers = repository.getPlayers(team.teamId).first()
                // Kendimizi bul
                val matchedPlayer = teamPlayers.find { it.email == email }

                if (matchedPlayer != null) {
                    val teamTournaments = repository.getTournaments(team.teamId).first()

                    // --- DEĞİŞİKLİK BURADA: teamPlayers listesini de ekliyoruz ---
                    collectedData.add(TeamCareerData(team, matchedPlayer, teamTournaments, teamPlayers))
                }
            }

            _careerData.value = collectedData
            _isLoadingCareer.value = false
        }
    // Filtreleme ve İstatistik Hesaplama
    // MainViewModel.kt içinde güncellenecek fonksiyon:
    // MainViewModel.kt içine ekle/güncelle:

    // MainViewModel içinde calculateCareerStats fonksiyonunu güncelle:
    fun calculateCareerStats(
        teamIdFilter: String?,
        tournamentIdFilter: String?,
        matchIdFilter: String?
    ): AdvancedPlayerStats {
        val dataList = _careerData.value

        val filteredData = if (teamIdFilter == "GENEL" || teamIdFilter == null) {
            dataList
        } else {
            dataList.filter { it.teamProfile.teamId == teamIdFilter }
        }

        // Değişkenler
        var totalGoals = 0
        var totalAssists = 0
        var totalBlocks = 0
        var totalThrowaways = 0
        var totalDrops = 0
        var totalCatches = 0
        var totalPasses = 0
        var totalPointsPlayed = 0
        var totalOPoints = 0
        var totalDPoints = 0
        var totalSecondsPlayed = 0L

        val combinedPassDistribution = mutableMapOf<String, Int>()

        filteredData.forEach { data ->
            val stats = calculateGlobalPlayerStats(
                playerId = data.player.id,
                filterTournamentId = tournamentIdFilter ?: "GENEL",
                filterMatchId = matchIdFilter,
                allTournaments = data.tournaments
            )

            totalGoals += stats.basicStats.goal
            totalAssists += stats.basicStats.assist
            totalBlocks += stats.basicStats.block
            totalThrowaways += stats.basicStats.throwaway
            totalDrops += stats.basicStats.drop
            totalCatches += stats.basicStats.catchStat
            totalPasses += stats.basicStats.successfulPass
            totalPointsPlayed += stats.basicStats.pointsPlayed
            totalOPoints += stats.oPointsPlayed
            totalDPoints += stats.dPointsPlayed
            totalSecondsPlayed += stats.basicStats.secondsPlayed

            // --- DÜZELTME: ID -> İSİM ÇEVİRİMİ ---
            // 'receiverKey' muhtemelen bir ID'dir (Örn: "Ab12...").
            // Takım listesinden (data.allTeamPlayers) bu ID'nin kime ait olduğunu buluyoruz.

            stats.basicStats.passDistribution.forEach { (receiverKey, count) ->
                // ID ile eşleşen oyuncuyu bul, yoksa olduğu gibi bırak
                val playerName = data.allTeamPlayers.find { it.id == receiverKey }?.name ?: receiverKey

                val currentCount = combinedPassDistribution[playerName] ?: 0
                combinedPassDistribution[playerName] = currentCount + count
            }
        }

        val basicStats = PlayerStats(
            playerId = "CAREER",
            name = "Kariyer Toplamı",
            goal = totalGoals,
            assist = totalAssists,
            block = totalBlocks,
            throwaway = totalThrowaways,
            drop = totalDrops,
            catchStat = totalCatches,
            successfulPass = totalPasses,
            pointsPlayed = totalPointsPlayed,
            secondsPlayed = totalSecondsPlayed,
            passDistribution = combinedPassDistribution
        )

        val plusMinus = totalGoals + totalAssists + totalBlocks - totalThrowaways - totalDrops

        return AdvancedPlayerStats(
            basicStats = basicStats,
            plusMinus = plusMinus.toDouble(),
            oPointsPlayed = totalOPoints,
            dPointsPlayed = totalDPoints
        )
    }
    // MainViewModel sınıfının sonuna (calculateCareerStats fonksiyonunun altına) ekle:

    // --- GİRİŞ VE VERİ YÖNETİMİ ---

    fun checkUserLoggedIn() {
        val user = auth.currentUser
        if (user != null) {
            _signInState.value = SignInState.Loading
            viewModelScope.launch {
                try {
                    // İnternet varsa veriyi tazelemeye çalışır
                    fetchUserProfile()
                    _signInState.value = SignInState.Success("Giriş yapıldı")
                } catch (e: Exception) {
                    Log.e("Auth", "Offline mod devreye girdi: ${e.message}")

                    // KRİTİK NOKTA: Hata alsa bile "Success" diyoruz ki ekran açılsın
                    _signInState.value = SignInState.Success("Çevrimdışı Mod")
                }
            }
        } else {
            _signInState.value = SignInState.Error("Giriş yapılmadı")
        }
    }

    // MainViewModel.kt dosyasının en alt kısımlarında

    // MainViewModel.kt dosyasının en altındaki bu fonksiyonu değiştir:

    // MainViewModel.kt dosyasının en altındaki fetchUserProfile fonksiyonunu bununla değiştirin:

    private suspend fun fetchUserProfile() {
        val user = auth.currentUser ?: return

        _profileState.value = UserProfileState.LOADING

        try {
            // YENİ EKLENEN KISIM: withTimeout
            // 5000 milisaniye (5 saniye) bekler.
            // İnternet yoksa Firestore sonsuza kadar beklemek yerine 5 saniye sonra hata fırlatır.
            kotlinx.coroutines.withTimeout(5000L) {

                // Firestore işlemleri
                // (Bu işlemler 5 saniyeyi geçerse iptal edilir ve catch bloğuna düşer)
                repository.updateUserInfo(user)

                val p = repository.getUserProfile(user.uid)

                _currentUserProfile.value = p
                _profileState.value = if (p == null || p.displayName.isNullOrBlank())
                    UserProfileState.NEEDS_CREATION else UserProfileState.EXISTS
            }

        } catch (e: Exception) {
            // Timeout (Zaman aşımı) veya başka bir hata olursa buraya düşer
            Log.e("UserProfile", "Profil yüklenemedi veya zaman aşımı: ${e.message}")

            // Kritik Müdahale: Bekleme ekranını kapatıp "VAR" (EXISTS) diyoruz.
            // Böylece uygulama açılıyor.
            _profileState.value = UserProfileState.EXISTS

            // Üst fonksiyona da haber veriyoruz (Orası da "Çevrimdışı Mod" mesajı verecek)
            throw e
        }
    }
    fun completeOnboarding() = viewModelScope.launch {
        repository.setOnboardingCompleted(true)
    }
}


// Giriş işleminin durumunu tutmak için
// Giriş işleminin durumunu tutmak için


class SignInViewModel : ViewModel() {

    private val auth = Firebase.auth

    private val _signInState = MutableStateFlow<SignInState>(SignInState.Idle)
    val signInState = _signInState.asStateFlow()

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        // Web Client ID (Google Cloud Console'dan alınan ID)
        val webClientId = "860197339001-4m2pci2vido0olf43dqp4a8p6eaipjmi.apps.googleusercontent.com"

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun handleGoogleSignInResult(result: ActivityResult, context: Context) {
        if (result.resultCode != Activity.RESULT_OK) {
            _signInState.value = SignInState.Error(context.getString(R.string.msg_google_cancel))
            return
        }

        viewModelScope.launch {
            _signInState.value = SignInState.Loading
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)!!
                val idToken = account.idToken!!

                val credential = GoogleAuthProvider.getCredential(idToken, null)

                // 1. Firebase Giriş
                val authResult = auth.signInWithCredential(credential).await()

                _signInState.value = SignInState.Success(context.getString(R.string.msg_login_success))

                // 2. Kullanıcı Kaydı (Veritabanı güncellemesi)
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    val repository = TournamentRepository(context.applicationContext)
                    repository.updateUserInfo(firebaseUser)
                }

            } catch (e: ApiException) {
                _signInState.value = SignInState.Error(context.getString(R.string.msg_login_error_api, e.message))
            } catch (e: Exception) {
                _signInState.value = SignInState.Error(context.getString(R.string.msg_login_error, e.message))
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
