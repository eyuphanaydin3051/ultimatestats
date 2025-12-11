package com.example.ilkuygulamam

import com.google.firebase.firestore.Exclude
import kotlinx.serialization.Serializable
import java.util.UUID
@Serializable
data class Player(
    val id: String = "",
    val name: String = "",
    val gender: String = "Erkek",
    val jerseyNumber: Int? = null,
    val position: String = "Cutter",
    @JvmField val isCaptain: Boolean = false,
    val email: String? = null,    // <-- YENİ: Eşleştirme için
    val photoUrl: String? = null  // <-- YENİ: Profil fotoğrafı için
)

@Serializable
data class Match(
    val id: String = "",
    val opponentName: String = "",
    val ourTeamName: String = "",
    val scoreUs: Int = 0,
    val scoreThem: Int = 0,
    val pointsArchive: List<PointData> = emptyList(),
    val matchDurationSeconds: Long = 0L,
    @field:JvmField // Bu etiket Firestore'un "is" ekini silmesini engeller.
    val isProMode: Boolean = false
)
@Serializable
data class Tournament(
    val id: String = "",
    val tournamentName: String = "",
    val ourTeamName: String = "",
    val date: String = "",
    val rosterPlayerIds: List<String> = emptyList(),
    @get:Exclude val matches: List<Match> = emptyList(),
    val presetLines: List<PresetLine> = emptyList()
)
// --- 1. VERİ YAPILARI (GÜNCELLENDİ) ---
enum class GameMode {
    IDLE,                  // Başlangıç (Line seçimi/O-D seçimi)
    MODE_SELECTION,        // YENİ: Detaylı mı, Hızlı mı? seçim ekranı
    SIMPLE_ENTRY,          // YENİ: Hızlı Kayıt (Asist/Gol) ekranı
    DEFENSE_PULL,          // Detaylı Defans: Pull atanı seç
    DEFENSE_PULL_RESULT,   // Detaylı Defans: Pull sonucu
    OFFENSE,               // Detaylı Ofans: Pas pas takip
    DEFENSE                // Detaylı Defans: Blok bekleme
}
@Serializable
data class PlayerStats(
    val playerId: String = "",
    val name: String = "",
    val successfulPass: Int = 0,
    val assist: Int = 0,
    val throwaway: Int = 0,
    val catchStat: Int = 0,
    val drop: Int = 0,
    val goal: Int = 0,
    val pullAttempts: Int = 0,
    val successfulPulls: Int = 0,
    val block: Int = 0,
    val callahan: Int = 0, // <-- YENİ EKLENEN
    val secondsPlayed: Long = 0,
    val totalTempoSeconds: Long = 0, // <-- YENİ: Toplam Topa Sahip Olma Süresi (Saniye)
    val pointsPlayed: Int = 0,
    val totalPullTimeSeconds: Double = 0.0,
    val passDistribution: Map<String, Int> = emptyMap()
)
@Serializable

// BU SINIFLAR HATALARIN ÇÖZÜMÜ İÇİN EKLENDİ (Önceki kodda eksikti)
data class AdvancedPlayerStats(
    val basicStats: PlayerStats = PlayerStats(),
    val plusMinus: Double = 0.0,
    val oPointsPlayed: Int = 0,
    val dPointsPlayed: Int = 0
)
// --- YENİ ENUM ---
enum class CaptureMode {
    SIMPLE,    // Basit Mod (Line + Skor + Gol/Asist)
    ADVANCED,  // Gelişmiş Mod (Pas, Hata, Blok vb.)
    PRO        // Pro Mod (Gelecek özellik - Atış türleri vb.)
}

enum class StoppageType(val label: String) {
    TIMEOUT("Mola (Timeout)"),
    INJURY("Sakatlık"),
    CALL("Faul / Call"),
    OTHER("Diğer")
}

@Serializable
data class Stoppage(
    val id: String = UUID.randomUUID().toString(),
    val type: StoppageType = StoppageType.CALL,
    val durationSeconds: Long = 0L,
    val startTimeSeconds: Long = 0L // Maçın kaçıncı saniyesinde olduğu
)

// PointData'yı güncelle:
@Serializable
data class PointData(
    val stats: List<PlayerStats> = emptyList(),
    val whoScored: String = "",
    val startMode: PointStartMode? = null,
    val captureMode: CaptureMode = CaptureMode.ADVANCED,
    val pullDurationSeconds: Double = 0.0,
    val durationSeconds: Long = 0L, // <-- YENİ: Sayının toplam süresi
    val stoppages: List<Stoppage> = emptyList(),// <-- YENİ: Duraksama Listesi
    val proEvents: List<ProEventData> = emptyList()
)



enum class NameFormat {
    FULL_NAME,             // Eyüphan Aydın
    FIRST_NAME_LAST_INITIAL, // Eyüphan A. (Varsayılan)
    INITIAL_LAST_NAME      // E. Aydın
}
// YENİ KULLANICI PROFİLİ VERİ SINIFI (Dosyanın üst kısımlarına, sınıfın dışına ekle)
data class UserProfile(
    val displayName: String? = null,
    val email: String? = null
)
@Serializable
data class Training(
    val id: String = UUID.randomUUID().toString(),
    val date: String = "",       // Örn: "18/11/2025"
    val time: String = "",       // Örn: "19:30"
    val location: String = "",   // Opsiyonel: Yer
    val note: String = "",
    val description: String = "",   // <-- YENİ: Detaylı Açıklama (Madde 4)
    val attendeeIds: List<String> = emptyList(),// Katılan oyuncuların ID'leri
    @field:JvmField
    val isVisibleToMembers: Boolean = true // Varsayılan: Herkes görür
)
@Serializable
data class PresetLine(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val playerIds: List<String> = emptyList(),
    val type: LineType = LineType.FULL // <-- YENİ EKLENDİ
)
@Serializable
enum class LineType {
    // 7 kişilik, standart bir kadro (Eski sistem)
    FULL,
    // 2-4 kişilik Oyun Kurucu grubu
    HANDLER_SET,
    // 3-5 kişilik Koşucu grubu
    CUTTER_SET
}
@Serializable

data class AdvancedTeamStats(
    val totalMatchesPlayed: Int = 0,
    // --- YENİ EKLENENLER ---
    val wins: Int = 0,
    val losses: Int = 0,
    val totalPointsPlayed: Int = 0,
    val totalGoals: Int = 0,
    val totalAssists: Int = 0,
    val totalSuccessfulPass: Int = 0,
    val totalThrowaways: Int = 0,
    val totalDrops: Int = 0,
    val totalBlocks: Int = 0,
    val totalPulls: Int = 0,
    val totalOffensePoints: Int = 0,
    val offensiveHolds: Int = 0,
    val cleanHolds: Int = 0,
    val totalDefensePoints: Int = 0,
    val breakPointsScored: Int = 0,
    val totalPassesAttempted: Int = 0,
    val totalPassesCompleted: Int = 0,
    val totalBlockPoints: Int = 0,
    val blocksConvertedToGoals: Int = 0,
    val totalPossessions: Int = 0,
    val totalTempoSeconds: Long = 0,
    val totalPullTimeSeconds: Double = 0.0
)
@Serializable // kotlinx.serialization kullandığımız için bu önemli
data class BackupData(
    val profile: TeamProfile,
    val players: List<Player>,
    val tournaments: List<Tournament>,
    val trainings: List<Training> = emptyList() // <-- YENİ EKLENDİ (Varsayılanı boş liste)
)
// İstatistik Tipleri Enum'ı
enum class StatType(val title: String, val unit: String) {
    GOAL("Gol Krallığı", "Gol"),
    ASSIST("Asist Krallığı", "Asist"),
    BLOCK("Blok (D)", "Blok"),
    THROWAWAY("Turnover (Hata)", "Turn"), // İsteğine özel Throwaway
    DROP("Drop (Düşürme)", "Drop"),
    PLUS_MINUS("Verimlilik (+/-)", "+/-"),
    PASS_COUNT("Pas Sayısı", "Pas"),
    CATCH_RATE("Tutuş Yüzdesi", "%"),
    PASS_RATE("Pas Yüzdesi", "%"), // <-- YENİ EKLENEN SATIR
    POINTS_PLAYED("Oynanan Sayı", "Sayı"),
    PLAYTIME("Toplam Süre", "Dk"),// <-- YENİ,
    AVG_PLAYTIME("Ort. Sayı Süresi", "Dk"),
    TEMPO("Ort. Pas Süresi", "Sn"), // <-- YENİ: Birimi Saniye (Sn)
    AVG_PULL_TIME("Ort. Pull Süresi", "Sn"),
    CALLAHAN("Callahan", "Callahan")
}
// pro event
@Serializable
data class ProEventData(
    val fromX: Float? = null,
    val fromY: Float? = null,
    val toX: Float = 0f,
    val toY: Float = 0f,
    val type: String = "PASS", // "PASS", "GOAL", "DROP", "THROWAWAY", "PULL", "DEFENSE"
    val distanceYards: Double = 0.0,
    val throwerName: String? = null, // Pası Atan
    val receiverName: String? = null
)
// DataModels.kt içine ekle

enum class CalculationMode(val label: String) {
    TOTAL("Toplam"),
    PER_MATCH("Maç Başı"),
    PER_POINT("Sayı Başı")
}