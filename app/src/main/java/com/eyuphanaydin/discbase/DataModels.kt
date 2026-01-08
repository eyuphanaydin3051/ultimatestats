package com.eyuphanaydin.discbase

import com.google.firebase.firestore.Exclude
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Player(
    val id: String = "",
    val name: String = "",
    val gender: String = "Erkek", // Veritabanı değeri olduğu için çevrilmedi
    val jerseyNumber: Int? = null,
    val position: String = "Cutter", // Veritabanı değeri olduğu için çevrilmedi
    @JvmField val isCaptain: Boolean = false,
    val email: String? = null,
    val photoUrl: String? = null
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
    @field:JvmField
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

// --- 1. VERİ YAPILARI ---
enum class GameMode {
    IDLE,
    MODE_SELECTION,
    SIMPLE_ENTRY,
    DEFENSE_PULL,
    DEFENSE_PULL_RESULT,
    OFFENSE,
    DEFENSE
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
    val callahan: Int = 0,
    val secondsPlayed: Long = 0,
    val totalTempoSeconds: Long = 0,
    val pointsPlayed: Int = 0,
    val totalPullTimeSeconds: Double = 0.0,
    val passDistribution: Map<String, Int> = emptyMap()
)

@Serializable
data class AdvancedPlayerStats(
    val basicStats: PlayerStats = PlayerStats(),
    val plusMinus: Double = 0.0,
    val oPointsPlayed: Int = 0,
    val dPointsPlayed: Int = 0
)

enum class CaptureMode {
    SIMPLE,
    ADVANCED,
    PRO
}

// GÜNCELLENDİ: String yerine Resource ID
enum class StoppageType(val labelResId: Int) {
    TIMEOUT(R.string.stoppage_label_timeout),
    INJURY(R.string.stoppage_label_injury),
    CALL(R.string.stoppage_label_call),
    OTHER(R.string.stoppage_label_other)
}

@Serializable
data class Stoppage(
    val id: String = UUID.randomUUID().toString(),
    val type: StoppageType = StoppageType.CALL,
    val durationSeconds: Long = 0L,
    val startTimeSeconds: Long = 0L
)

@Serializable
data class PointData(
    val stats: List<PlayerStats> = emptyList(),
    val whoScored: String = "",
    val startMode: PointStartMode? = null,
    val captureMode: CaptureMode = CaptureMode.ADVANCED,
    val pullDurationSeconds: Double = 0.0,
    val durationSeconds: Long = 0L,
    val stoppages: List<Stoppage> = emptyList(),
    val proEvents: List<ProEventData> = emptyList()
)

enum class PointStartMode {
    OFFENSE, DEFENSE
}

enum class NameFormat {
    FULL_NAME,
    FIRST_NAME_LAST_INITIAL,
    INITIAL_LAST_NAME
}

data class UserProfile(
    val displayName: String? = null,
    val email: String? = null,
    val photoUrl: String? = null // <-- BU SATIR EKLENDİ
)

@Serializable
data class Training(
    val id: String = UUID.randomUUID().toString(),
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val note: String = "",
    val description: String = "",
    val attendeeIds: List<String> = emptyList(),
    @field:JvmField
    val isVisibleToMembers: Boolean = true
)

@Serializable
data class PresetLine(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val playerIds: List<String> = emptyList(),
    val type: LineType = LineType.FULL
)

@Serializable
enum class LineType {
    FULL,
    HANDLER_SET,
    CUTTER_SET
}

@Serializable
data class AdvancedTeamStats(
    val totalMatchesPlayed: Int = 0,
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

@Serializable
data class BackupData(
    val profile: TeamProfile,
    val players: List<Player>,
    val tournaments: List<Tournament>,
    val trainings: List<Training> = emptyList()
)

// GÜNCELLENDİ: String yerine Resource ID
enum class StatType(val titleResId: Int, val unitResId: Int) {
    GOAL(R.string.stat_title_goal, R.string.stat_unit_goal),
    ASSIST(R.string.stat_title_assist, R.string.stat_unit_assist),
    BLOCK(R.string.stat_title_block, R.string.stat_unit_block),
    THROWAWAY(R.string.stat_title_throwaway, R.string.stat_unit_turn),
    DROP(R.string.stat_title_drop, R.string.stat_unit_drop),
    PLUS_MINUS(R.string.stat_title_plus_minus, R.string.stat_unit_plus_minus),
    PASS_COUNT(R.string.stat_title_pass_count, R.string.stat_unit_pass),
    CATCH_RATE(R.string.stat_title_catch_rate, R.string.stat_unit_percent),
    PASS_RATE(R.string.stat_title_pass_rate, R.string.stat_unit_percent),
    POINTS_PLAYED(R.string.stat_title_points_played, R.string.stat_unit_point),
    PLAYTIME(R.string.stat_title_playtime, R.string.stat_unit_min),
    AVG_PLAYTIME(R.string.stat_title_avg_playtime, R.string.stat_unit_min),
    TEMPO(R.string.stat_title_tempo, R.string.stat_unit_sec),
    AVG_PULL_TIME(R.string.stat_title_avg_pull, R.string.stat_unit_sec),
    CALLAHAN(R.string.stat_title_callahan, R.string.stat_unit_callahan)
}

@Serializable
data class ProEventData(
    val fromX: Float? = null,
    val fromY: Float? = null,
    val toX: Float = 0f,
    val toY: Float = 0f,
    val type: String = "PASS",
    val distanceYards: Double = 0.0,
    val throwerName: String? = null,
    val receiverName: String? = null
)

// GÜNCELLENDİ: String yerine Resource ID
enum class CalculationMode(val labelResId: Int) {
    TOTAL(R.string.calc_mode_total),
    PER_MATCH(R.string.calc_mode_per_match),
    PER_POINT(R.string.calc_mode_per_point)
}
@Serializable
data class TeamProfile(
    val teamId: String = "",
    val teamName: String = "",
    val members: Map<String, String> = emptyMap(), // uid -> role
    val logoPath: String? = null
)
// DataModels.kt dosyasının en altına ekleyin

@Serializable
data class PointStateSnapshot(
    val pointStats: List<PlayerStats> = emptyList(),
    val activePasserId: String? = null,
    val gameMode: GameMode = GameMode.IDLE,
    val subbedOutStats: List<PlayerStats> = emptyList()
)