package com.eyuphanaydin.discbase

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

// DataStore ve Sabitler
private val ACTIVE_TEAM_ID_KEY = stringPreferencesKey("active_team_id")
private val Context.dataStore by preferencesDataStore(name = "settings")
private val APP_THEME_KEY = stringPreferencesKey("app_theme")
private val TIME_TRACKING_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("time_tracking_enabled")
private val PRO_MODE_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("pro_mode_enabled")
private val CAPTURE_MODE_KEY = stringPreferencesKey("capture_mode")

private val ONBOARDING_COMPLETED_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("onboarding_completed")
private const val TEAMS_COLLECTION = "teams"
private const val TOURNAMENTS_COLLECTION = "tournaments"
private const val PLAYERS_COLLECTION = "players"
private const val USERS_COLLECTION = "users"
private const val TRAININGS_COLLECTION = "trainings"

class TournamentRepository(private val context: Context) {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    // --- AUTH ---
    fun getAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(authStateListener)
        awaitClose { auth.removeAuthStateListener(authStateListener) }
    }

    // --- USER PROFILE ---
    suspend fun updateUserInfo(user: FirebaseUser) {
        val userDoc = db.collection(USERS_COLLECTION).document(user.uid)
        val userInfo = UserProfile(displayName = user.displayName, email = user.email)
        try {
            userDoc.set(userInfo, SetOptions.merge()).await()
        } catch (e: Exception) { Log.e("Firestore", "updateUserInfo error", e) }
    }

    fun getTeamMemberProfiles(memberIds: List<String>): Flow<Map<String, UserProfile>> = callbackFlow {
        if (memberIds.isEmpty()) { trySend(emptyMap()); close(); return@callbackFlow }
        val chunkedIds = memberIds.take(10)
        val listener = db.collection(USERS_COLLECTION)
            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunkedIds)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) { trySend(emptyMap()); return@addSnapshotListener }
                val userMap = snapshot.documents.associate { doc ->
                    doc.id to (doc.toObject<UserProfile>() ?: UserProfile())
                }
                trySend(userMap)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            db.collection(USERS_COLLECTION).document(uid).get().await().toObject<UserProfile>()
        } catch (e: Exception) { null }
    }

    suspend fun createManualUserProfile(uid: String, displayName: String, email: String?): Boolean {
        return try {
            db.collection(USERS_COLLECTION).document(uid)
                .set(UserProfile(displayName, email), SetOptions.merge()).await()
            true
        } catch (e: Exception) { false }
    }

    // --- TEAM OPERATIONS ---
    private fun getTeamDocument(teamId: String) = db.collection(TEAMS_COLLECTION).document(teamId)

    suspend fun checkIfTeamExists(teamId: String): Boolean {
        return try { getTeamDocument(teamId).get().await().exists() } catch (e: Exception) { false }
    }

    fun getUserTeams(uid: String): Flow<List<TeamProfile>> = callbackFlow {
        val listener = db.collection(TEAMS_COLLECTION)
            .whereIn("members.$uid", listOf("member", "admin"))
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snapshot.toObjects(TeamProfile::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun joinExistingTeam(uid: String, teamId: String): Boolean {
        return try {
            val teamRef = getTeamDocument(teamId)
            if (teamRef.get().await().exists()) {
                teamRef.update("members.$uid", "pending").await()
                true
            } else false
        } catch (e: Exception) { false }
    }

    suspend fun createNewTeam(uid: String, teamName: String): String? {
        return try {
            val newTeamRef = db.collection(TEAMS_COLLECTION).document()
            val newProfile = TeamProfile(
                teamName = teamName,
                teamId = newTeamRef.id,
                members = mapOf(uid to "admin")
            )
            newTeamRef.set(newProfile).await()
            newTeamRef.id
        } catch (e: Exception) { null }
    }

    suspend fun deleteTeam(teamId: String): Boolean {
        return try { getTeamDocument(teamId).delete().await(); true } catch (e: Exception) { false }
    }

    // --- DATASTORE ---
    fun getActiveTeamId(): Flow<String?> = context.dataStore.data.map { it[ACTIVE_TEAM_ID_KEY] }
    suspend fun setActiveTeamId(teamId: String) { context.dataStore.edit { it[ACTIVE_TEAM_ID_KEY] = teamId } }
    suspend fun clearActiveTeamId() { context.dataStore.edit { it.remove(ACTIVE_TEAM_ID_KEY) } }

    // --- PROFILE ---
    fun getProfile(teamId: String): Flow<TeamProfile> = callbackFlow {
        val listener = getTeamDocument(teamId).addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null || !snapshot.exists()) {
                trySend(TeamProfile(teamId = teamId))
                return@addSnapshotListener
            }
            trySend(snapshot.toObject<TeamProfile>() ?: TeamProfile(teamId = teamId))
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveProfile(teamId: String, profile: TeamProfile): Boolean {
        return try { getTeamDocument(teamId).set(profile, SetOptions.merge()).await(); true } catch (e: Exception) { false }
    }

    // --- PLAYERS ---
    fun getPlayers(teamId: String): Flow<List<Player>> = callbackFlow {
        val listener = getTeamDocument(teamId).collection(PLAYERS_COLLECTION).addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) { trySend(emptyList()); return@addSnapshotListener }
            trySend(snapshot.documents.mapNotNull { it.toObject<Player>() }.sortedBy { it.name })
        }
        awaitClose { listener.remove() }
    }

    suspend fun savePlayer(teamId: String, player: Player): Boolean {
        return try {
            getTeamDocument(teamId).collection(PLAYERS_COLLECTION).document(player.id).set(player).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun deletePlayer(teamId: String, player: Player): Boolean {
        return try {
            getTeamDocument(teamId).collection(PLAYERS_COLLECTION).document(player.id).delete().await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun updatePlayerLimited(teamId: String, playerId: String, photoUrl: String?, jerseyNumber: Int?): Boolean {
        return try {
            getTeamDocument(teamId).collection(PLAYERS_COLLECTION).document(playerId)
                .update(mapOf("photoUrl" to photoUrl, "jerseyNumber" to jerseyNumber)).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun deleteAllPlayers(teamId: String): Boolean {
        return try {
            val playersRef = getTeamDocument(teamId).collection(PLAYERS_COLLECTION)
            val snapshot = playersRef.get().await()
            val batch = db.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            true
        } catch (e: Exception) { false }
    }

    // --- TOURNAMENTS & MATCHES (FIXED FOR OFFLINE REFRESH) ---
    fun getTournaments(teamId: String): Flow<List<Tournament>> = callbackFlow {
        val tournamentsListener = getTeamDocument(teamId).collection(TOURNAMENTS_COLLECTION)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) { trySend(emptyList()); return@addSnapshotListener }
                val tournaments = snapshot.documents.mapNotNull { it.toObject<Tournament>() }

                if (tournaments.isEmpty()) {
                    trySend(emptyList())
                } else {
                    // --- OFFLINE/ONLINE KONTROLÜ ---
                    // Eğer snapshot Cache'den geliyorsa (isFromCache=true) VEYA yerel yazma varsa (hasPendingWrites=true),
                    // bu bir offline/yerel durumdur. Bu durumda alt koleksiyonları da Cache'den çekmeye zorlamalıyız.
                    val useCache = snapshot.metadata.isFromCache || snapshot.metadata.hasPendingWrites()

                    fetchMatchesForTournaments(teamId, tournaments, useCache) { fullyLoaded ->
                        trySend(fullyLoaded.sortedByDescending { it.date })
                    }
                }
            }
        awaitClose { tournamentsListener.remove() }
    }

    // MAÇLARI ÇEKME MANTIĞI (GÜÇLENDİRİLMİŞ)
    private fun fetchMatchesForTournaments(
        teamId: String,
        tournaments: List<Tournament>,
        forceCache: Boolean,
        onResult: (List<Tournament>) -> Unit
    ) {
        val resultList = tournaments.toMutableList()
        var processedCount = 0

        if (tournaments.isEmpty()) { onResult(emptyList()); return }

        // Ortak İşleme Fonksiyonu
        fun processMatchSnapshot(index: Int, tournament: Tournament, matchSnapshot: QuerySnapshot) {
            val matchesColRef = getTeamDocument(teamId)
                .collection(TOURNAMENTS_COLLECTION)
                .document(tournament.id)
                .collection("matches")

            val subCollectionMatches = matchSnapshot.documents.mapNotNull { it.toObject<Match>() }

            if (subCollectionMatches.isEmpty() && tournament.matches.isNotEmpty()) {
                // Eski verileri taşıma (Migration)
                val batch = db.batch()
                tournament.matches.forEach { oldMatch ->
                    batch.set(matchesColRef.document(oldMatch.id), oldMatch)
                }
                batch.commit().addOnCompleteListener {
                    resultList[index] = tournament
                    processedCount++
                    if (processedCount == tournaments.size) onResult(resultList)
                }
            } else {
                if (subCollectionMatches.isNotEmpty()) {
                    resultList[index] = tournament.copy(matches = subCollectionMatches)
                }
                processedCount++
                if (processedCount == tournaments.size) onResult(resultList)
            }
        }

        tournaments.forEachIndexed { index, tournament ->
            val tournamentRef = getTeamDocument(teamId).collection(TOURNAMENTS_COLLECTION).document(tournament.id)
            val matchesColRef = tournamentRef.collection("matches")

            if (forceCache) {
                // --- DURUM 1: OFFLINE / YEREL GÜNCELLEME ---
                // Sadece Cache'e bak. Sunucuyu deneme (çünkü takılır).
                matchesColRef.get(Source.CACHE)
                    .addOnSuccessListener { snapshot -> processMatchSnapshot(index, tournament, snapshot) }
                    .addOnFailureListener {
                        // Cache hatası olursa boş dön ama takılma
                        processedCount++
                        if (processedCount == tournaments.size) onResult(resultList)
                    }
            } else {
                // --- DURUM 2: ONLINE ---
                // Önce sunucuyu dene (Default).
                matchesColRef.get().addOnSuccessListener { snapshot ->
                    processMatchSnapshot(index, tournament, snapshot)
                }.addOnFailureListener { e ->
                    // Sunucu hatası olursa (internet anlık giderse) Cache'e dön
                    Log.w("Repo", "Server fetch failed, falling back to cache: ${e.message}")
                    matchesColRef.get(Source.CACHE)
                        .addOnSuccessListener { snapshot -> processMatchSnapshot(index, tournament, snapshot) }
                        .addOnFailureListener {
                            processedCount++
                            if (processedCount == tournaments.size) onResult(resultList)
                        }
                }
            }
        }
    }

    // MAÇI KAYDET
    suspend fun saveMatch(teamId: String, tournamentId: String, match: Match): Boolean {
        return try {
            val tournamentRef = getTeamDocument(teamId)
                .collection(TOURNAMENTS_COLLECTION)
                .document(tournamentId)

            // 1. Maçı alt koleksiyona kaydet (Yerel Cache güncellenir)
            tournamentRef.collection("matches").document(match.id).set(match).await()

            // 2. Ana belgeyi güncelle ki 'getTournaments' tetiklensin
            tournamentRef.update("lastUpdated", System.currentTimeMillis()).await()

            true
        } catch (e: Exception) {
            // Eğer update başarısız olursa (örn: belge yoksa), set ile oluştur
            try {
                getTeamDocument(teamId).collection(TOURNAMENTS_COLLECTION).document(tournamentId)
                    .set(mapOf("lastUpdated" to System.currentTimeMillis()), SetOptions.merge()).await()
                true
            } catch (e2: Exception) {
                Log.e("Repo", "SaveMatch error", e2)
                false
            }
        }
    }

    // MAÇI SİL
    suspend fun deleteMatch(teamId: String, tournamentId: String, matchId: String): Boolean {
        return try {
            val tournamentRef = getTeamDocument(teamId)
                .collection(TOURNAMENTS_COLLECTION)
                .document(tournamentId)

            tournamentRef.collection("matches").document(matchId).delete().await()
            tournamentRef.update("lastUpdated", System.currentTimeMillis()).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // --- DİĞER FONKSİYONLAR (AYNEN KORUNDU) ---
    suspend fun saveTournament(teamId: String, tournament: Tournament): Boolean {
        return try {
            getTeamDocument(teamId).collection(TOURNAMENTS_COLLECTION).document(tournament.id).set(tournament).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun deleteTournament(teamId: String, tournament: Tournament): Boolean {
        return try {
            getTeamDocument(teamId).collection(TOURNAMENTS_COLLECTION).document(tournament.id).delete().await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun updateMemberRole(teamId: String, memberUid: String, newRole: String): Boolean {
        return try { getTeamDocument(teamId).update("members.$memberUid", newRole).await(); true } catch (e: Exception) { false }
    }

    suspend fun removeMember(teamId: String, memberUid: String): Boolean {
        return try { getTeamDocument(teamId).update(mapOf("members.$memberUid" to FieldValue.delete())).await(); true } catch (e: Exception) { false }
    }

    // ... (overwriteTeamData, getTrainings vb. diğer fonksiyonlar aynı kalacak) ...
    // Dosya bütünlüğü için eski fonksiyonlarınızı buraya eklemeyi unutmayın veya
    // yukarıdaki değişiklikleri mevcut dosyanıza uygulayın.
    // Ancak en temizi bu kodu komple yapıştırmaktır. Aşağıda eksik kalan kısımları tamamlıyorum:

    fun getTrainings(teamId: String): Flow<List<Training>> = callbackFlow {
        val listener = getTeamDocument(teamId).collection(TRAININGS_COLLECTION).addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) { trySend(emptyList()); return@addSnapshotListener }
            trySend(snapshot.documents.mapNotNull { it.toObject<Training>() })
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveTraining(teamId: String, training: Training): Boolean {
        return try { getTeamDocument(teamId).collection(TRAININGS_COLLECTION).document(training.id).set(training).await(); true } catch (e: Exception) { false }
    }

    suspend fun deleteTraining(teamId: String, trainingId: String): Boolean {
        return try { getTeamDocument(teamId).collection(TRAININGS_COLLECTION).document(trainingId).delete().await(); true } catch (e: Exception) { false }
    }

    fun getAppTheme(): Flow<String?> = context.dataStore.data.map { it[APP_THEME_KEY] }
    suspend fun setAppTheme(themeName: String) { context.dataStore.edit { it[APP_THEME_KEY] = themeName } }

    suspend fun restoreTeamData(teamId: String, backup: BackupData): Boolean {
        return try {
            val teamRef = getTeamDocument(teamId)
            val batch = db.batch()
            val updatedProfile = backup.profile.copy(teamId = teamId, members = backup.profile.members)
            batch.set(teamRef, updatedProfile, SetOptions.merge())
            backup.players.forEach { batch.set(teamRef.collection(PLAYERS_COLLECTION).document(it.id), it) }
            backup.trainings.forEach { batch.set(teamRef.collection(TRAININGS_COLLECTION).document(it.id), it) }
            backup.tournaments.forEach { batch.set(teamRef.collection(TOURNAMENTS_COLLECTION).document(it.id), it) }
            batch.commit().await()
            backup.tournaments.forEach { tournament ->
                if (tournament.matches.isNotEmpty()) {
                    val matchesColRef = teamRef.collection(TOURNAMENTS_COLLECTION).document(tournament.id).collection("matches")
                    tournament.matches.forEach { match -> matchesColRef.document(match.id).set(match).await() }
                }
            }
            true
        } catch (e: Exception) { false }
    }

    fun getCaptureMode(): Flow<String?> = context.dataStore.data.map { it[CAPTURE_MODE_KEY] }
    suspend fun setCaptureMode(mode: String) { context.dataStore.edit { it[CAPTURE_MODE_KEY] = mode } }
    fun getTimeTrackingEnabled(): Flow<Boolean> = context.dataStore.data.map { it[TIME_TRACKING_KEY] ?: false }
    suspend fun setTimeTrackingEnabled(enabled: Boolean) { context.dataStore.edit { it[TIME_TRACKING_KEY] = enabled } }
    fun getProModeEnabled(): Flow<Boolean> = context.dataStore.data.map { it[PRO_MODE_KEY] ?: false }
    suspend fun setProModeEnabled(enabled: Boolean) { context.dataStore.edit { it[PRO_MODE_KEY] = enabled } }
    // --- ONBOARDING KONTROLÜ ---
    fun getOnboardingCompleted(): Flow<Boolean> = context.dataStore.data.map { it[ONBOARDING_COMPLETED_KEY] ?: false }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[ONBOARDING_COMPLETED_KEY] = completed }
    }
}