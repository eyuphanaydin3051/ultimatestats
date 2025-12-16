package com.example.discbase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.Composable
import java.io.ByteArrayOutputStream

data class StatPercentage(
    val text: String,
    val ratio: String,
    val progress: Float
)
fun calculateSafePercentage(numerator: Int, denominator: Int): StatPercentage {
    val rate = if (denominator > 0) (numerator.toDouble() / denominator) else 0.0
    val percentageText = "${String.format("%.1f", rate * 100)}%"
    val ratioText = "$numerator / $denominator"
    return StatPercentage(
        text = percentageText,
        ratio = ratioText,
        progress = rate.toFloat()
    )
}
fun calculateGlobalPlayerStats(
    playerId: String,
    filterTournamentId: String,
    filterMatchId: String? = null, // Varsayılan değer null
    allTournaments: List<Tournament>
): AdvancedPlayerStats {
    val tournamentsToScan = if (filterTournamentId == "GENEL") {
        allTournaments
    } else {
        allTournaments.filter { it.id == filterTournamentId }
    }

    var relevantMatches = tournamentsToScan.flatMap { it.matches }

    // Eğer bir maç ID'si seçildiyse, sadece o maçı al
    if (filterMatchId != null) {
        relevantMatches = relevantMatches.filter { it.id == filterMatchId }
    }

    val allPointsArchive = relevantMatches.flatMap { it.pointsArchive }
    return calculateStatsFromPoints(playerId, allPointsArchive)
}
fun calculateStatsFromPoints(playerId: String, pointsArchive: List<PointData>): AdvancedPlayerStats {
    var totalStats = PlayerStats(playerId = playerId)
    var oPointsPlayed = 0
    var dPointsPlayed = 0
    val aggregatedPassMap = mutableMapOf<String, Int>()

    for (pointData in pointsArchive) {
        val playerStat = pointData.stats.find { it.playerId == playerId }
        if (playerStat != null) {
            totalStats = totalStats.copy(
                pointsPlayed = totalStats.pointsPlayed + 1,
                successfulPass = totalStats.successfulPass + playerStat.successfulPass,
                assist = totalStats.assist + playerStat.assist,
                throwaway = totalStats.throwaway + playerStat.throwaway,
                catchStat = totalStats.catchStat + playerStat.catchStat,
                drop = totalStats.drop + playerStat.drop,
                goal = totalStats.goal + playerStat.goal,
                pullAttempts = totalStats.pullAttempts + playerStat.pullAttempts,
                successfulPulls = totalStats.successfulPulls + playerStat.successfulPulls,
                block = totalStats.block + playerStat.block,
                callahan = totalStats.callahan + playerStat.callahan, // <-- BU SATIRI EKLEYİN
                secondsPlayed = totalStats.secondsPlayed + playerStat.secondsPlayed,
                totalTempoSeconds = totalStats.totalTempoSeconds + playerStat.totalTempoSeconds,
                totalPullTimeSeconds = totalStats.totalPullTimeSeconds + playerStat.totalPullTimeSeconds
            )
            playerStat.passDistribution.forEach { (receiverId, count) ->
                val currentTotal = aggregatedPassMap[receiverId] ?: 0
                aggregatedPassMap[receiverId] = currentTotal + count
            }
            if (pointData.startMode == PointStartMode.OFFENSE) { oPointsPlayed++ }
            else if (pointData.startMode == PointStartMode.DEFENSE) { dPointsPlayed++ }
        }
    }
    totalStats = totalStats.copy(passDistribution = aggregatedPassMap)

    // --- GÜNCELLENEN FORMÜL (Blok = 1.5 Puan) ---
    // Formül: (Gol + Asist) + (Blok * 1.5) - (Hata + Drop) + (Pas * 0.05)+(callahan * 2)
    val efficiencyScore = ((totalStats.goal - totalStats.callahan) * 1.0) + // Callahan harici Goller
            (totalStats.assist * 1.0) +
            ((totalStats.block - totalStats.callahan) * 1.5) + // Callahan harici Bloklar
            (totalStats.callahan * 3.5) - // Callahan'ın Toplam Değeri
            ((totalStats.throwaway + totalStats.drop) * 1.0) +
            (totalStats.successfulPass * 0.05)

    return AdvancedPlayerStats(
        basicStats = totalStats,
        plusMinus = efficiencyScore,
        oPointsPlayed = oPointsPlayed,
        dPointsPlayed = dPointsPlayed
    )
}
fun calculateTeamStatsForFilter(tournaments: List<Tournament>, filterTournamentId: String): AdvancedTeamStats {
    val tournamentsToScan = if (filterTournamentId == "GENEL") {
        tournaments
    } else {
        tournaments.filter { it.id == filterTournamentId }
    }

    if (tournamentsToScan.isEmpty()) return AdvancedTeamStats()
    // --- YENİ: Toplam Maç Sayısı Hesaplama ---
    val totalMatchesCount = tournamentsToScan.sumOf { it.matches.size }
    // -----------------------------------------
    // --- DÜZELTME BAŞLANGICI ---
    var wins = 0
    var losses = 0

    // Maç sonuçlarını hesapla
    for (tournament in tournamentsToScan) {
        for (match in tournament.matches) {
            if (match.scoreUs > match.scoreThem) {
                wins++
            } else if (match.scoreThem > match.scoreUs) {
                losses++
            }
            // Beraberlik (0-0 veya eşitlik) durumunda hiçbir şey yapmıyoruz,
            // böylece yeni açılan maçlar (0-0) galibiyet/mağlubiyet olarak sayılmaz.
        }
    }

    val allPointsArchive = mutableListOf<PointData>()
    val allPlayerStatsMap = mutableMapOf<String, PlayerStats>()

    for (tournament in tournamentsToScan) {
        for (match in tournament.matches) {
            allPointsArchive.addAll(match.pointsArchive)
            for (pointData in match.pointsArchive) {
                for (playerStat in pointData.stats) {
                    val playerId = playerStat.playerId
                    val currentStat = allPlayerStatsMap[playerId] ?: PlayerStats(playerId = playerId)
                    allPlayerStatsMap[playerId] = currentStat.copy(
                        successfulPass = currentStat.successfulPass + playerStat.successfulPass,
                        assist = currentStat.assist + playerStat.assist,
                        throwaway = currentStat.throwaway + playerStat.throwaway,
                        catchStat = currentStat.catchStat + playerStat.catchStat,
                        drop = currentStat.drop + playerStat.drop,
                        goal = currentStat.goal + playerStat.goal,
                        pullAttempts = currentStat.pullAttempts + playerStat.pullAttempts,
                        successfulPulls = currentStat.successfulPulls + playerStat.successfulPulls,
                        block = currentStat.block + playerStat.block,

                        // EKSİK OLAN KISIMLAR EKLENDİ:
                        totalTempoSeconds = currentStat.totalTempoSeconds + playerStat.totalTempoSeconds,
                        totalPullTimeSeconds = currentStat.totalPullTimeSeconds + playerStat.totalPullTimeSeconds
                    )
                }
            }
        }
    }

    val aggregatedPlayerStats = allPlayerStatsMap.values.toList()

    val totalGoals = aggregatedPlayerStats.sumOf { it.goal }
    val totalAssists = aggregatedPlayerStats.sumOf { it.assist }
    val totalSuccessfulPass = aggregatedPlayerStats.sumOf { it.successfulPass }
    val totalThrowaways = aggregatedPlayerStats.sumOf { it.throwaway }
    val totalDrops = aggregatedPlayerStats.sumOf { it.drop }
    val totalBlocks = aggregatedPlayerStats.sumOf { it.block }
    val totalPulls = aggregatedPlayerStats.sumOf { it.pullAttempts }
    val totalTempoSeconds = aggregatedPlayerStats.sumOf { it.totalTempoSeconds }
    val totalPullTimeSeconds = aggregatedPlayerStats.sumOf { it.totalPullTimeSeconds }
    val totalPointsPlayed = allPointsArchive.size
    val totalOffensePoints = allPointsArchive.count { it.startMode == PointStartMode.OFFENSE }
    val totalDefensePoints = allPointsArchive.count { it.startMode == PointStartMode.DEFENSE }
    val offensiveHolds = allPointsArchive.count { it.startMode == PointStartMode.OFFENSE && it.whoScored == "US" }
    val cleanHolds = allPointsArchive.count {
        it.startMode == PointStartMode.OFFENSE &&
                it.whoScored == "US" &&
                it.stats.sumOf { s -> s.throwaway + s.drop } == 0
    }
    val breakPointsScored = allPointsArchive.count { it.startMode == PointStartMode.DEFENSE && it.whoScored == "US" }
    val totalPassesCompleted = totalSuccessfulPass + totalAssists
    val totalPassesAttempted = totalPassesCompleted + totalThrowaways
    val pointsWithBlocks = allPointsArchive.filter { it.stats.sumOf { s -> s.block } > 0 }
    val totalBlockPoints = pointsWithBlocks.size
    val blocksConvertedToGoals = pointsWithBlocks.count { it.whoScored == "US" }
    val totalPossessions = totalGoals + totalThrowaways + totalDrops


    return AdvancedTeamStats(
        totalMatchesPlayed = totalMatchesCount,
        wins = wins,      // <-- YENİ
        losses = losses,  // <-- YENİ
        totalPointsPlayed = totalPointsPlayed,
        totalGoals = totalGoals,
        totalAssists = totalAssists,
        totalSuccessfulPass = totalSuccessfulPass,
        totalThrowaways = totalThrowaways,
        totalDrops = totalDrops,
        totalBlocks = totalBlocks,
        totalPulls = totalPulls,
        totalOffensePoints = totalOffensePoints,
        offensiveHolds = offensiveHolds,
        cleanHolds = cleanHolds,
        totalDefensePoints = totalDefensePoints,
        breakPointsScored = breakPointsScored,
        totalPassesAttempted = totalPassesAttempted,
        totalPassesCompleted = totalPassesCompleted,
        totalBlockPoints = totalBlockPoints,
        blocksConvertedToGoals = blocksConvertedToGoals,
        totalPossessions = totalPossessions,
        totalTempoSeconds = totalTempoSeconds,
        totalPullTimeSeconds = totalPullTimeSeconds
    )
}
fun calculateTeamStatsForMatch(
    pointsArchive: List<PointData>,
    overallStats: List<AdvancedPlayerStats>
): AdvancedTeamStats {
    val totalGoals = overallStats.sumOf { it.basicStats.goal }
    val totalAssists = overallStats.sumOf { it.basicStats.assist }
    val totalSuccessfulPass = overallStats.sumOf { it.basicStats.successfulPass }
    val totalThrowaways = overallStats.sumOf { it.basicStats.throwaway }
    val totalDrops = overallStats.sumOf { it.basicStats.drop }
    val totalBlocks = overallStats.sumOf { it.basicStats.block }
    val totalPulls = overallStats.sumOf { it.basicStats.pullAttempts }
    val totalPointsPlayed = pointsArchive.size
    val totalOffensePoints = pointsArchive.count { it.startMode == PointStartMode.OFFENSE }
    val totalDefensePoints = pointsArchive.count { it.startMode == PointStartMode.DEFENSE }
    val offensiveHolds = pointsArchive.count {
        it.startMode == PointStartMode.OFFENSE && it.whoScored == "US"
    }
    val cleanHolds = pointsArchive.count {
        it.startMode == PointStartMode.OFFENSE &&
                it.whoScored == "US" &&
                it.stats.sumOf { s -> s.throwaway + s.drop } == 0
    }
    val breakPointsScored = pointsArchive.count {
        it.startMode == PointStartMode.DEFENSE && it.whoScored == "US"
    }
    val totalPassesCompleted = totalSuccessfulPass + totalAssists
    val totalPassesAttempted = totalPassesCompleted + totalThrowaways
    val pointsWithBlocks = pointsArchive.filter { it.stats.sumOf { s -> s.block } > 0 }
    val totalBlockPoints = pointsWithBlocks.size
    val blocksConvertedToGoals = pointsWithBlocks.count { it.whoScored == "US" }
    val totalPossessions = totalGoals + totalThrowaways + totalDrops
    val totalTempoSeconds = overallStats.sumOf { it.basicStats.totalTempoSeconds }
    val totalPullTimeSeconds = overallStats.sumOf { it.basicStats.totalPullTimeSeconds } // <-- Yeni

    return AdvancedTeamStats(
        totalPointsPlayed = totalPointsPlayed,
        totalGoals = totalGoals,
        totalAssists = totalAssists,
        totalSuccessfulPass = totalSuccessfulPass,
        totalThrowaways = totalThrowaways,
        totalDrops = totalDrops,
        totalBlocks = totalBlocks,
        totalPulls = totalPulls,
        totalOffensePoints = totalOffensePoints,
        offensiveHolds = offensiveHolds,
        cleanHolds = cleanHolds,
        totalDefensePoints = totalDefensePoints,
        breakPointsScored = breakPointsScored,
        totalPassesAttempted = totalPassesAttempted,
        totalPassesCompleted = totalPassesCompleted,
        totalBlockPoints = totalBlockPoints,
        blocksConvertedToGoals = blocksConvertedToGoals,
        totalPossessions = totalPossessions,
        totalTempoSeconds = totalTempoSeconds,
        totalPullTimeSeconds = totalPullTimeSeconds
    )
}
fun getLogoModel(path: String?): Any? {
    if (path.isNullOrBlank()) return null

    // Eğer yerel bir dosya veya web URL'i ise Uri olarak döndür
    if (path.startsWith("content://") || path.startsWith("file://") || path.startsWith("http")) {
        return Uri.parse(path)
    }

    // Base64 Çözümleme Mantığı
    return try {
        // "data:image/jpeg;base64," başlığı varsa temizle
        val pureBase64 = if (path.contains(",")) {
            path.substringAfter(",")
        } else {
            path
        }

        val decodedString = Base64.decode(pureBase64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
fun uriToCompressedBase64(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (originalBitmap == null) return null

        // Boyutlandırma (Maksimum 600px - Kalite ve Performans Dengesi)
        val maxDimension = 600
        val ratio = Math.min(
            maxDimension.toDouble() / originalBitmap.width,
            maxDimension.toDouble() / originalBitmap.height
        )

        // Eğer resim zaten küçükse boyutlandırma, değilse küçült
        val finalBitmap = if (ratio < 1.0) {
            val width = (originalBitmap.width * ratio).toInt()
            val height = (originalBitmap.height * ratio).toInt()
            Bitmap.createScaledBitmap(originalBitmap, width, height, true)
        } else {
            originalBitmap
        }

        val outputStream = ByteArrayOutputStream()
        // JPEG formatında, %80 kalite ile sıkıştır
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()

        // NO_WRAP: Satır atlamalarını engeller, tek satır string üretir
        val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)

        // Başlık ekleyerek döndür
        "data:image/jpeg;base64,$base64String"

    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
fun formatSecondsToTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
// ... mevcut kodlar ...

// --- EKRAN YÖNÜ KİLİTLEME YARDIMCISI ---
// Utils.kt dosyasının en altı:

@Composable
fun LockScreenOrientation(orientation: Int) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // DÜZELTME: Unit yerine 'orientation' parametresini dinliyoruz.
    androidx.compose.runtime.DisposableEffect(orientation) {
        val activity = context as? android.app.Activity
        val originalOrientation = activity?.requestedOrientation ?: android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        // Eğer mevcut yön, istenen yön değilse değiştir
        if (activity?.requestedOrientation != orientation) {
            activity?.requestedOrientation = orientation
        }

        onDispose {
            // Ekrandan çıkarken eski haline döndür
            activity?.requestedOrientation = originalOrientation
        }
    }
}
// Utils.kt içine ekle

fun countMatchesPlayedByPlayer(
    playerId: String,
    filterTournamentId: String,
    filterMatchId: String?,
    allTournaments: List<Tournament>
): Int {
    val tournamentsToScan = if (filterTournamentId == "GENEL") {
        allTournaments
    } else {
        allTournaments.filter { it.id == filterTournamentId }
    }

    var relevantMatches = tournamentsToScan.flatMap { it.matches }

    if (filterMatchId != null) {
        relevantMatches = relevantMatches.filter { it.id == filterMatchId }
    }

    // Oyuncunun 'pointsPlayed > 0' olduğu maçları say
    return relevantMatches.count { match ->
        match.pointsArchive.any { point ->
            point.stats.any { stat -> stat.playerId == playerId && stat.pointsPlayed > 0 }
        }
    }
}