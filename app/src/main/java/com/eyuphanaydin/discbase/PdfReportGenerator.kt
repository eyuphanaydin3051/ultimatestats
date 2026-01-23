package com.eyuphanaydin.discbase

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.sin

class PdfReportGenerator(private val context: Context) {

    // --- RENK PALETİ ---
    private val colorPrimary = Color.parseColor("#6200EA") // Mor
    private val colorSecondary = Color.parseColor("#7C4DFF") // Açık Mor
    private val colorOffense = Color.parseColor("#00C49A") // Yeşil
    private val colorDefense = Color.parseColor("#FF6B6B") // Kırmızı
    private val colorAmber = Color.parseColor("#FFA000") // Amber
    private val colorBrown = Color.parseColor("#795548") // Kahverengi
    private val colorTextPrimary = Color.parseColor("#212121")
    private val colorTextSecondary = Color.GRAY
    private val colorDivider = Color.parseColor("#E0E0E0")
    private val colorLightBg = Color.parseColor("#F5F5F5")

    // --- TURNUVA RAPORU (GÜNCELLENDİ) ---
    fun generateTournamentReport(
        tournament: Tournament,
        teamStats: AdvancedTeamStats,
        playerStats: List<AdvancedPlayerStats>
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()

        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var currentY = 0f

        // ---------------------------------------------------------
        // 1. BAŞLIK VE ÖZET
        // ---------------------------------------------------------
        drawHeader(canvas, tournament.tournamentName)
        currentY = 100f

        // Galibiyet/Mağlubiyet Özeti
        val wins = teamStats.wins
        val losses = teamStats.losses
        val totalMatches = teamStats.totalMatchesPlayed

        drawCardBackground(canvas, 20f, currentY, 555f, 80f)

        val titleP = Paint().apply { color = Color.GRAY; textSize = 12f; textAlign = Paint.Align.CENTER }
        val valP = Paint().apply { color = Color.BLACK; textSize = 24f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }

        // Toplam Maç
        canvas.drawText(context.getString(R.string.pdf_total_match), 100f, currentY + 30f, titleP)
        canvas.drawText("$totalMatches", 100f, currentY + 60f, valP)

        // Galibiyet
        canvas.drawText(context.getString(R.string.pdf_wins), 297f, currentY + 30f, titleP)
        valP.color = Color.parseColor("#4CAF50") // Yeşil
        canvas.drawText("$wins", 297f, currentY + 60f, valP)

        // Mağlubiyet
        canvas.drawText(context.getString(R.string.pdf_losses), 495f, currentY + 30f, titleP)
        valP.color = Color.parseColor("#F44336") // Kırmızı
        canvas.drawText("$losses", 495f, currentY + 60f, valP)

        currentY += 100f

        // ---------------------------------------------------------
        // 2. TAKIM PERFORMANSI & VERİMLİLİK
        // ---------------------------------------------------------
        drawSectionTitle(canvas, context.getString(R.string.pdf_team_performance), currentY)
        currentY += 25f

        // Hesaplamalar
        val holdRate = calculateSafePercentage(teamStats.offensiveHolds, teamStats.totalOffensePoints)
        val breakRate = calculateSafePercentage(teamStats.breakPointsScored, teamStats.totalDefensePoints)
        val passRate = calculateSafePercentage(teamStats.totalPassesCompleted, teamStats.totalPassesAttempted)
        val conversionRate = calculateSafePercentage(teamStats.totalGoals, teamStats.totalPossessions)
        val cleanHoldRate = calculateSafePercentage(teamStats.cleanHolds, teamStats.totalOffensePoints)
        val blockConversionRate = calculateSafePercentage(teamStats.blocksConvertedToGoals, teamStats.totalBlockPoints)

        val colWidth = 270f

        // Sol Kart: Performans
        drawCardBackground(canvas, 20f, currentY, colWidth, 160f)
        drawSectionTitleSmall(canvas, context.getString(R.string.pdf_performance), 35f, currentY + 20f)
        var innerY = currentY + 40f

        drawModernStatBar(canvas, 35f, innerY, context.getString(R.string.pdf_label_hold), holdRate.progress, holdRate.text, Color.parseColor("#03DAC5"))
        innerY += 40f
        drawModernStatBar(canvas, 35f, innerY, context.getString(R.string.pdf_label_break), breakRate.progress, breakRate.text, Color.parseColor("#FF9800"))
        innerY += 40f
        drawModernStatBar(canvas, 35f, innerY, context.getString(R.string.pdf_label_pass_completed), passRate.progress, passRate.text, Color.BLUE)

        // Sağ Kart: Verimlilik
        drawCardBackground(canvas, 305f, currentY, colWidth, 160f)
        innerY = currentY + 40f
        drawSectionTitleSmall(canvas, context.getString(R.string.pdf_efficiency), 320f, currentY + 20f)

        drawModernStatBar(canvas, 320f, innerY, context.getString(R.string.pdf_label_conversion), conversionRate.progress, conversionRate.text, if(conversionRate.progress>0.5) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
        innerY += 40f
        drawModernStatBar(canvas, 320f, innerY, context.getString(R.string.pdf_label_clean_holds), cleanHoldRate.progress, cleanHoldRate.text, Color.parseColor("#00B0FF"))
        innerY += 40f
        drawModernStatBar(canvas, 320f, innerY, context.getString(R.string.pdf_label_dline_conv), blockConversionRate.progress, blockConversionRate.text, Color.MAGENTA)

        currentY += 180f

        // ---------------------------------------------------------
        // 3. DETAYLI ANALİZ (GRID)
        // ---------------------------------------------------------
        drawSectionTitle(canvas, "Detaylı Analiz", currentY)
        currentY += 25f

        drawCardBackground(canvas, 20f, currentY, 555f, 130f)
        val boxY = currentY + 15f
        val boxH = 50f
        val boxW = 165f
        val gap = 10f

        val totalTurnovers = teamStats.totalThrowaways + teamStats.totalDrops
        val avgTurnoverPerMatch = if (teamStats.totalMatchesPlayed > 0) String.format("%.1f", totalTurnovers.toDouble() / teamStats.totalMatchesPlayed) else "0.0"
        val teamAvgTempo = if (teamStats.totalPassesAttempted > 0) String.format("%.2f sn", teamStats.totalTempoSeconds.toDouble() / teamStats.totalPassesAttempted) else "0.00 sn"
        val teamAvgPullTime = if (teamStats.totalPulls > 0) String.format("%.2f sn", teamStats.totalPullTimeSeconds.toDouble() / teamStats.totalPulls) else "0.00 sn"

        // Satır 1
        drawStitchStatBox(canvas, 35f, boxY, boxW, boxH, context.getString(R.string.pdf_label_pass_attempts), "${teamStats.totalPassesAttempted}", Color.DKGRAY)
        drawStitchStatBox(canvas, 35f + boxW + gap, boxY, boxW, boxH, context.getString(R.string.pdf_label_pass_completed), "${teamStats.totalPassesCompleted}", Color.parseColor("#4CAF50"))
        drawStitchStatBox(canvas, 35f + (boxW + gap)*2, boxY, boxW, boxH, context.getString(R.string.pdf_label_avg_tempo), teamAvgTempo, Color.parseColor("#FFC107"))

        // Satır 2
        val row2Y = boxY + boxH + gap
        drawStitchStatBox(canvas, 35f, row2Y, boxW, boxH, context.getString(R.string.pdf_label_total_turnover), "$totalTurnovers", Color.parseColor("#F44336"))
        drawStitchStatBox(canvas, 35f + boxW + gap, row2Y, boxW, boxH, context.getString(R.string.pdf_label_turnover_per_match), avgTurnoverPerMatch, Color.parseColor("#F44336"))
        drawStitchStatBox(canvas, 35f + (boxW + gap)*2, row2Y, boxW, boxH, context.getString(R.string.pdf_label_avg_pull), teamAvgPullTime, Color.parseColor("#795548"))

        currentY += 150f

        // ---------------------------------------------------------
        // 4. TURNUVA LİDERLERİ (TOP 3) - GÜNCELLENDİ
        // ---------------------------------------------------------

        // Sayfada yer yoksa yeni sayfaya geç
        if (currentY > 550f) {
            document.finishPage(page)
            page = document.startPage(pageInfo)
            canvas = page.canvas
            drawHeader(canvas, tournament.tournamentName + " (continue)")
            currentY = 100f
        }

        drawSectionTitle(canvas, context.getString(R.string.pdf_leaders), currentY)
        currentY += 30f

        // --- VERİ HAZIRLIĞI ---

        // 1. Oyuncuların oynadığı maç sayılarını (Maç Başı İstatistik için) turnuva verisinden hesapla
        // Logic: Bir oyuncu bir maçın en az 1 sayısında (point) oynadıysa (pointsPlayed > 0), o maça katılmış sayılır.
        val matchesPlayedMap = playerStats.associate { p ->
            val count = tournament.matches.count { m ->
                m.pointsArchive.any { point ->
                    point.stats.any { it.playerId == p.basicStats.playerId && it.pointsPlayed > 0 }
                }
            }
            p.basicStats.playerId to count
        }

        // --- LİSTELERİ OLUŞTUR ---

        // A) Mevcut Listeler
        val topGoals = playerStats.filter { it.basicStats.goal > 0 }.sortedByDescending { it.basicStats.goal }.take(3)
        val topAssists = playerStats.filter { it.basicStats.assist > 0 }.sortedByDescending { it.basicStats.assist }.take(3)
        val topBlocks = playerStats.filter { it.basicStats.block > 0 }.sortedByDescending { it.basicStats.block }.take(3)

        // B) Pas Yüzdesi (>10 Pas Denemesi Olanlar) - YENİ
        val topPassRate = playerStats.filter {
            val attempts = it.basicStats.successfulPass + it.basicStats.throwaway
            attempts > 10
        }.sortedByDescending {
            it.basicStats.successfulPass.toDouble() / (it.basicStats.successfulPass + it.basicStats.throwaway)
        }.take(3)

        // C) Toplam Oynanan Sayı - YENİ
        val topPointsPlayed = playerStats.filter { it.basicStats.pointsPlayed > 0 }
            .sortedByDescending { it.basicStats.pointsPlayed }.take(3)

        // D) Maç Başına Oynanan Sayı - YENİ
        // (Toplam Sayı / Oynadığı Maç Sayısı)
        val topPointsPerMatch = playerStats.filter {
            val mp = matchesPlayedMap[it.basicStats.playerId] ?: 0
            mp > 0 && it.basicStats.pointsPlayed > 0
        }
            .sortedByDescending {
                it.basicStats.pointsPlayed.toDouble() / matchesPlayedMap[it.basicStats.playerId]!!
            }.take(3)

        // E) Hatalar
        val topTurnovers = playerStats.filter { it.basicStats.throwaway > 0 }.sortedByDescending { it.basicStats.throwaway }.take(3)
        val topDrops = playerStats.filter { it.basicStats.drop > 0 }.sortedByDescending { it.basicStats.drop }.take(3)

        // --- ÇİZİM (3 SATIR OLACAK ŞEKİLDE AYARLANDI) ---
        val cardW = 175f
        val cardH = 110f
        val cardGap = 15f

        // SATIR 1: Gol - Asist - Blok
        drawLeaderCard(canvas, 20f, currentY, cardW, cardH, context.getString(R.string.pdf_goals_leader), Color.parseColor("#4CAF50"), topGoals) {
            it.basicStats.goal.toString()
        }
        drawLeaderCard(canvas, 20f + cardW + cardGap, currentY, cardW, cardH, context.getString(R.string.pdf_assists_leader), Color.BLUE, topAssists) {
            it.basicStats.assist.toString()
        }
        drawLeaderCard(canvas, 20f + (cardW + cardGap)*2, currentY, cardW, cardH, context.getString(R.string.pdf_blocks_leader), Color.MAGENTA, topBlocks) {
            it.basicStats.block.toString()
        }

        currentY += cardH + cardGap

        // SATIR 2: Pas % - Toplam Sayı - Maç Başı Sayı (YENİ SATIR)
        drawLeaderCard(canvas, 20f, currentY, cardW, cardH, "Pas % (>10 Pas)", Color.BLUE, topPassRate) {
            val total = it.basicStats.successfulPass + it.basicStats.throwaway
            val rate = if(total > 0) (it.basicStats.successfulPass.toDouble() / total * 100) else 0.0
            "${String.format("%.1f", rate)}%"
        }

        drawLeaderCard(canvas, 20f + cardW + cardGap, currentY, cardW, cardH, context.getString(R.string.pdf_label_total_points_played), Color.DKGRAY, topPointsPlayed) {
            it.basicStats.pointsPlayed.toString()
        }

        drawLeaderCard(canvas, 20f + (cardW + cardGap)*2, currentY, cardW, cardH, context.getString(R.string.pdf_label_points_per_match), Color.parseColor("#FFC107"), topPointsPerMatch) {
            val mp = matchesPlayedMap[it.basicStats.playerId] ?: 1
            String.format("%.1f", it.basicStats.pointsPlayed.toDouble() / mp)
        }

        currentY += cardH + cardGap

        // SATIR 3: Turnover - Drop - Boş
        drawLeaderCard(canvas, 20f, currentY, cardW, cardH, "Turnover", Color.parseColor("#F44336"), topTurnovers) {
            it.basicStats.throwaway.toString()
        }
        drawLeaderCard(canvas, 20f + cardW + cardGap, currentY, cardW, cardH, "Drop", Color.parseColor("#F44336"), topDrops) {
            it.basicStats.drop.toString()
        }
        // 3. kutu şimdilik boş veya "En Çok Pas" olabilir (İsteğe bağlı)
        val topPasses = playerStats.filter { it.basicStats.successfulPass > 0 }.sortedByDescending { it.basicStats.successfulPass }.take(3)
        drawLeaderCard(canvas, 20f + (cardW + cardGap)*2, currentY, cardW, cardH, context.getString(R.string.pdf_label_most_passes), Color.DKGRAY, topPasses) {
            it.basicStats.successfulPass.toString()
        }

        currentY += cardH + 40f

        // ---------------------------------------------------------
        // 5. MAÇ SONUÇLARI LİSTESİ
        // ---------------------------------------------------------
        if (currentY > 750f) { // Sayfa sonu kontrolü
            document.finishPage(page)
            page = document.startPage(pageInfo)
            canvas = page.canvas
            drawHeader(canvas, context.getString(R.string.pdf_match_results))
            currentY = 100f
        } else {
            // Başlık atmadan devam et veya ayır
            drawSectionTitle(canvas, context.getString(R.string.pdf_match_results), currentY)
            currentY += 30f
        }

        val headers = listOf(context.getString(R.string.pdf_header_opponent), context.getString(R.string.pdf_header_result), context.getString(R.string.pdf_header_score), context.getString(R.string.pdf_header_mvp))
        val colX = floatArrayOf(30f, 250f, 350f, 450f)

        val headerBg = Paint().apply { color = Color.LTGRAY; style = Paint.Style.FILL }
        canvas.drawRect(20f, currentY, 575f, currentY + 25f, headerBg)

        val headerText = Paint().apply { textSize = 10f; typeface = Typeface.DEFAULT_BOLD }
        headers.forEachIndexed { i, h -> canvas.drawText(h, colX[i], currentY + 16f, headerText) }

        currentY += 35f
        val rowText = Paint().apply { textSize = 11f; color = Color.BLACK }

        tournament.matches.forEachIndexed { index, match ->
            if (currentY > 780f) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                currentY = 50f
                canvas.drawRect(20f, currentY, 575f, currentY + 25f, headerBg)
                headers.forEachIndexed { i, h -> canvas.drawText(h, colX[i], currentY + 16f, headerText) }
                currentY += 35f
            }

            if (index % 2 == 1) {
                val stripePaint = Paint().apply { color = Color.parseColor("#F5F5F5") }
                canvas.drawRect(20f, currentY - 10f, 575f, currentY + 10f, stripePaint)
            }

            val isWin = match.scoreUs > match.scoreThem
            val resultText = if (isWin) context.getString(R.string.W) else if (match.scoreUs < match.scoreThem) context.getString(R.string.L) else context.getString(R.string.D)
            val resultColor = if (isWin) Color.parseColor("#4CAF50") else if (match.scoreUs < match.scoreThem) Color.parseColor("#F44336") else Color.GRAY

            // 1. Rakip
            canvas.drawText(match.opponentName, colX[0], currentY, rowText)

            // 2. Sonuç Yuvarlağı
            val resP = Paint().apply { color = resultColor; style = Paint.Style.FILL }
            canvas.drawCircle(colX[1] + 10f, currentY - 4f, 10f, resP)
            val resT = Paint().apply { color = Color.WHITE; textSize = 10f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
            canvas.drawText(resultText, colX[1] + 10f, currentY, resT)

            // 3. Skor
            rowText.textAlign = Paint.Align.LEFT
            canvas.drawText("${match.scoreUs} - ${match.scoreThem}", colX[2], currentY, rowText)

            // 4. MVP (Şimdilik boş)
            rowText.textAlign = Paint.Align.CENTER
            canvas.drawText("-", colX[3] + 10f, currentY, rowText)
            rowText.textAlign = Paint.Align.LEFT

            currentY += 25f
        }

        drawFooter(canvas)
        document.finishPage(page)

        return savePdf(document, "Turnuva_${tournament.tournamentName}")
    }


    // --- MEVCUT MATCH REPORT (DEĞİŞMEDİ, AYNI KALACAK) ---
    // (Burayı silmeyin, generateMatchReport ve generatePlayerReport fonksiyonlarınız olduğu gibi kalsın)
    // Sadece generateTournamentReport'u yukarıdaki ile değiştirdik.

    // ... generateMatchReport KODLARI BURADA KALACAK ...
    fun generateMatchReport(
        match: Match,
        tournamentName: String,
        playerStats: List<AdvancedPlayerStats>
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var currentY = 0f

        // 1. BAŞLIK VE TURNUVA ADI (İSTEK 1: Turnuva adı eklendi)
        drawHeader(canvas, "vs ${match.opponentName}", tournamentName)
        currentY = 100f

        // 2. SKOR TABELASI
        drawScoreboard(canvas, currentY, match)
        currentY += 110f

        // 3. TAKIM PERFORMANSI (Modern Grid Yapısı)
        drawSectionTitle(canvas, context.getString(R.string.pdf_team_performance), currentY)
        currentY += 25f

        val teamStats = calculateTeamStatsForMatch(match.pointsArchive, playerStats)
        val holdRate = calculateSafePercentage(teamStats.offensiveHolds, teamStats.totalOffensePoints)
        val breakRate = calculateSafePercentage(teamStats.breakPointsScored, teamStats.totalDefensePoints)
        val passRate = calculateSafePercentage(teamStats.totalPassesCompleted, teamStats.totalPassesAttempted)
        val conversionRate = calculateSafePercentage(teamStats.totalGoals, teamStats.totalPossessions)

        // Sol Kart: Oranlar
        drawCardBackground(canvas, 20f, currentY, 270f, 160f)
        var innerY = currentY + 40f
        drawSectionTitleSmall(canvas, context.getString(R.string.pdf_general_rates), 35f, currentY + 20f)
        drawModernStatBar(canvas, 35f, innerY, context.getString(R.string.pdf_label_hold), holdRate.progress, holdRate.text, Color.parseColor("#03DAC5"))
        innerY += 40f
        drawModernStatBar(canvas, 35f, innerY, context.getString(R.string.pdf_label_break), breakRate.progress, breakRate.text, Color.parseColor("#FF9800"))
        innerY += 40f
        drawModernStatBar(canvas, 35f, innerY, context.getString(R.string.pdf_pass_success), passRate.progress, passRate.text, colorPrimary)

        // Sağ Kart: Verimlilik
        drawCardBackground(canvas, 305f, currentY, 270f, 160f)
        innerY = currentY + 40f
        drawSectionTitleSmall(canvas, context.getString(R.string.pdf_efficiency), 320f, currentY + 20f)
        drawModernStatBar(canvas, 320f, innerY, context.getString(R.string.pdf_label_conversion), conversionRate.progress, conversionRate.text, if(conversionRate.progress>0.5) colorOffense else colorDefense)

        val detailP = Paint().apply { textSize = 12f; color = colorTextPrimary }
        val totalTurnovers = teamStats.totalThrowaways + teamStats.totalDrops
        canvas.drawText("Toplam Hata: $totalTurnovers", 320f, innerY + 60f, detailP)
        canvas.drawText("Blok (D): ${teamStats.totalBlocks}", 320f, innerY + 80f, detailP)
        currentY += 180f

        // 4. DETAYLI KUTULAR
        val boxW = 125f; val gap = 15f
        drawStitchStatBox(canvas, 20f, currentY, boxW, 50f, "Paslar", "${teamStats.totalPassesCompleted}/${teamStats.totalPassesAttempted}", Color.DKGRAY)
        drawStitchStatBox(canvas, 20f+boxW+gap, currentY, boxW, 50f, context.getString(R.string.pdf_total_match), "${teamStats.totalPointsPlayed}", Color.GRAY)
        drawStitchStatBox(canvas, 20f+(boxW+gap)*2, currentY, boxW, 50f, context.getString(R.string.pdf_label_match_duration), "${match.matchDurationSeconds/60} dk", colorAmber)
        drawStitchStatBox(canvas, 20f+(boxW+gap)*3, currentY, boxW, 50f, context.getString(R.string.pdf_label_blocks), "${teamStats.totalBlocks}", colorSecondary)
        currentY += 80f

        // 5. OYUNCU TABLOSU
        if (currentY > 750f) { document.finishPage(page); page = document.startPage(pageInfo); canvas = page.canvas; currentY = 50f }
        drawSectionTitle(canvas, context.getString(R.string.pdf_section_player_stats), currentY)
        currentY += 30f
        drawPlayerTable(canvas, currentY, playerStats)

        drawFooter(canvas)
        document.finishPage(page)
        return savePdf(document, "Mac_${match.opponentName}")
    }

    // ... generatePlayerReport ve diğerleri (AYNI KALSIN) ...
    // ============================================================================================
    // 3. OYUNCU RAPORU (GÜNCELLENDİ)
    // ============================================================================================
    fun generatePlayerReport(
        player: Player,
        allTournaments: List<Tournament>,
        overallStats: AdvancedPlayerStats,
        allPlayers: List<Player> // <--- 1. BU PARAMETREYİ EKLE
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var currentY = 0f

        // --- 1. SAYFA: KİMLİK VE GENEL İSTATİSTİKLER ---

        // 1.1. OYUNCU KARTI (HEADER)
        drawPlayerHeaderCard(canvas, player)
        currentY = 140f

        // 1.2. GENEL KARİYER İSTATİSTİKLERİ
        drawSectionTitle(canvas, context.getString(R.string.pdf_section_career_general), currentY)
        currentY += 30f

        // 2. DEĞİŞİKLİK: createDummyRosterFromStats YERİNE allPlayers KULLANILIYOR
        currentY = drawPlayerDetailedStats(canvas, currentY, overallStats, allPlayers, "Kariyer Toplamı")

        // Eğer sayfa sonuna yaklaştıysak yeni sayfa
        if (currentY > 700f) {
            document.finishPage(page)
            page = document.startPage(pageInfo)
            canvas = page.canvas
            currentY = 50f
        } else {
            currentY += 40f
        }

        // 1.3. TURNUVA BAZLI İSTATİSTİKLER
        drawSectionTitle(canvas, context.getString(R.string.tour_detail_title), currentY)
        currentY += 30f

        allTournaments.forEach { tournament ->
            // Sayfa Kontrolü
            if (currentY > 500f) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                currentY = 50f
                drawSectionTitle(canvas, context.getString(R.string.tour_detail_title), currentY)
                currentY += 30f
            }

            // Turnuva İstatistiklerini Hesapla
            val tourPoints = tournament.matches.flatMap { it.pointsArchive }
            val tourStats = calculateStatsFromPoints(player.id, tourPoints)

            // Eğer oyuncu bu turnuvada oynadıysa
            if (tourStats.basicStats.pointsPlayed > 0 || tourStats.basicStats.secondsPlayed > 0) {
                val subTitlePaint = Paint().apply { color = colorPrimary; textSize = 16f; typeface = Typeface.DEFAULT_BOLD }
                canvas.drawText("Turnuva: ${tournament.tournamentName}", 20f, currentY, subTitlePaint)
                canvas.drawLine(20f, currentY+10f, 575f, currentY+10f, Paint().apply{color=Color.LTGRAY})
                currentY += 30f

                // 3. DEĞİŞİKLİK: BURADA DA allPlayers KULLANIYORUZ
                currentY = drawPlayerDetailedStats(canvas, currentY, tourStats, allPlayers, tournament.date)

                currentY += 40f
            }
        }

        drawFooter(canvas)
        document.finishPage(page)
        return savePdf(document, "Oyuncu_${player.name.replace(" ", "_")}")
    }

    // ============================================================================================
    // YARDIMCI: OYUNCU DETAYLI İSTATİSTİK ÇİZİMİ (İSTEK 3'teki 7 maddeyi çizer)
    // ============================================================================================
    private fun drawPlayerDetailedStats(
        canvas: Canvas,
        startY: Float,
        stats: AdvancedPlayerStats,
        allPlayers: List<Player>, // Pas ağı isimleri için
        contextTitle: String // "Kariyer" veya Tarih
    ): Float {
        var y = startY
        val basic = stats.basicStats

        // --- A) OYUN SÜRESİ KARTI ---
        val timeStr = formatSecondsToTime(basic.secondsPlayed)
        val avgTime = if(basic.pointsPlayed>0) formatSecondsToTime(basic.secondsPlayed/basic.pointsPlayed) else "00:00"

        drawCardBackground(canvas, 20f, y, 555f, 60f)
        val labelP = Paint().apply { color = Color.GRAY; textSize = 10f }
        val valP = Paint().apply { color = colorTextPrimary; textSize = 14f; typeface = Typeface.DEFAULT_BOLD }

        // Grid: Toplam Süre | Sayı | O | D | Ort. Süre
        var x = 35f
        canvas.drawText("Toplam Süre", x, y+20f, labelP); canvas.drawText(timeStr, x, y+40f, valP); x+=110f
        canvas.drawText("Oynanan Sayı", x, y+20f, labelP); canvas.drawText("${basic.pointsPlayed}", x, y+40f, valP); x+=110f
        canvas.drawText("Ofans (O)", x, y+20f, labelP); canvas.drawText("${stats.oPointsPlayed}", x, y+40f, valP.apply{color=colorOffense}); x+=110f
        canvas.drawText("Defans (D)", x, y+20f, labelP); canvas.drawText("${stats.dPointsPlayed}", x, y+40f, valP.apply{color=colorDefense}); x+=110f
        valP.color=colorTextPrimary
        canvas.drawText("Ort. Süre/Sayı", x, y+20f, labelP); canvas.drawText(avgTime, x, y+40f, valP)

        y += 75f

        // --- B, C, D) PERFORMANS KARTLARI (3'lü Yan Yana) ---
        val cardW = 175f
        val cardH = 100f
        val gap = 15f

        // B) Pas Performansı
        val totalPasses = basic.successfulPass + basic.assist + basic.throwaway
        val passRate = if(totalPasses>0) (basic.successfulPass+basic.assist).toFloat()/totalPasses else 0f
        drawMiniStatCard(canvas, 20f, y, cardW, cardH, "Pas Performansı", colorPrimary,
            listOf("Pas: ${basic.successfulPass}", "Asist: ${basic.assist}", "Hata: ${basic.throwaway}"), passRate)

        // C) Yakalama (Receiving)
        val totalCatches = basic.catchStat + basic.goal + basic.drop
        val catchRate = if(totalCatches>0) (basic.catchStat+basic.goal).toFloat()/totalCatches else 0f
        drawMiniStatCard(canvas, 20f+cardW+gap, y, cardW, cardH, "Yakalama", colorOffense,
            listOf("Catch: ${basic.catchStat}", "Gol: ${basic.goal}", "Drop: ${basic.drop}"), catchRate)

        // D) Savunma
        // Savunma için yüzde yok, sadece sayı
        drawMiniStatCard(canvas, 20f+(cardW+gap)*2, y, cardW, cardH, "Savunma", colorDefense,
            listOf("Blok (D): ${basic.block}", "Callahan: ${basic.callahan}", "Pull: ${basic.successfulPulls}/${basic.pullAttempts}"), -1f)

        y += cardH + 20f

        // --- E, F) PAS AĞI ve OYUN KARAKTERİ (Yan Yana) ---
        // Sol: Pas Ağı (Canvas Çizimi), Sağ: Oyun Karakteri (Bar) ve Detaylar

        // E) Pas Bağlantı Ağı (Sol Taraf)
        val netH = 200f
        drawCardBackground(canvas, 20f, y, 270f, netH)
        drawSectionTitleSmall(canvas, "Pas Bağlantı Ağı (Top 5)", 35f, y+20f)
        drawPassNetworkOnCanvas(canvas, 155f, y+110f, 70f, basic.passDistribution, allPlayers) // Merkez (155, y+110), Yarıçap 70

        // F) Oyun Karakteri (Sağ Taraf - Üst)
        drawCardBackground(canvas, 305f, y, 270f, 90f)
        drawSectionTitleSmall(canvas, "Oyun Karakteri (Flow)", 320f, y+20f)
        drawFlowBarOnCanvas(canvas, 320f, y+40f, 240f, basic.passDistribution, allPlayers)

        // G) Detaylı Bağlantılar (Sağ Taraf - Alt - Liste)
        drawCardBackground(canvas, 305f, y+100f, 270f, 100f)
        drawSectionTitleSmall(canvas, "En Çok Paslaşılanlar", 320f, y+120f)

        // Listeyi yazdır
        val sortedDist = basic.passDistribution.toList().sortedByDescending { it.second }.take(3)
        var listY = y + 145f
        val listP = Paint().apply { textSize=11f; color=colorTextPrimary }
        sortedDist.forEach { (pid, count) ->
            val pName = allPlayers.find { it.id == pid }?.name ?: "Bilinmeyen"
            val pRole = allPlayers.find { it.id == pid }?.position ?: "-"
            canvas.drawText("$pName ($pRole)", 320f, listY, listP)
            canvas.drawText("$count pas", 550f, listY, listP.apply { textAlign=Paint.Align.RIGHT })
            listP.textAlign = Paint.Align.LEFT
            listY += 18f
        }

        y += netH + 20f
        return y
    }

    // --- ORTAK YARDIMCILAR ---

    // Basit bir Color Extension int to int (Zaten Android Color int döner ama isim karışıklığı olmasın diye)



    private fun drawHeader(canvas: Canvas, title: String) {
        val bgPaint = Paint().apply { color = colorPrimary; style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, 595f, 80f, bgPaint)

        val textPaint = Paint().apply { color = Color.WHITE; textSize = 24f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
        canvas.drawText(title, 297f, 40f, textPaint)

        textPaint.textSize = 14f
        textPaint.color = Color.parseColor("#B3FFFFFF")
        canvas.drawText("Maç Raporu", 297f, 65f, textPaint)
    }







    // YARDIMCI FONKSİYON: Top 3 Listesi Çizimi (GÜNCELLENMİŞ VERSİYON)
    private fun drawTopCategory(
        canvas: Canvas,
        title: String,
        players: List<AdvancedPlayerStats>,
        x: Float,
        y: Float,
        width: Float,
        valueSelector: (AdvancedPlayerStats) -> String// <-- YENİ EKLENEN PARAMETRE
    ) {
        // Kutu Arka Planı
        val bgPaint = Paint().apply {
            color = android.graphics.Color.LTGRAY
            style = Paint.Style.FILL
            alpha = 50
        }
        canvas.drawRect(x, y, x + width, y + 100f, bgPaint)

        // Başlık
        val headerPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(title, x + (width / 2), y + 20f, headerPaint)

        // Çizgi
        val linePaint = Paint().apply {
            color = android.graphics.Color.GRAY
            strokeWidth = 1f
        }
        canvas.drawLine(x + 10, y + 25f, x + width - 10, y + 25f, linePaint)

        // Oyuncular ve Değerleri
        val namePaint = Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 12f
            textAlign = Paint.Align.LEFT
        }

        val valuePaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.RIGHT
        }

        var currentY = y + 45f
        players.forEachIndexed { index, player ->
            // İsim (Sola yaslı)
            val displayName = if (player.basicStats.name.length > 15) player.basicStats.name.take(15) + "." else player.basicStats.name
            canvas.drawText("${index + 1}. $displayName", x + 10, currentY, namePaint)

            // Değer (Sağa yaslı) - BURASI ARTIK DİNAMİK
            val value = valueSelector(player)
            canvas.drawText(value, x + width - 15, currentY, valuePaint)

            currentY += 20f
        }
    }
    // Bu fonksiyon, sayıları yazdırmak için "valueSelector" parametresini kullanır.
    private fun drawLeaderCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        title: String,
        accentColor: Int,
        players: List<AdvancedPlayerStats>,
        valueSelector: (AdvancedPlayerStats) -> String // <-- BU PARAMETRE SAYILARI YAZDIRIR
    ) {
        // Kart Arka Planı
        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            setShadowLayer(4f, 2f, 2f, Color.LTGRAY)
        }
        canvas.drawRect(x, y, x + w, y + h, bgPaint)

        // Üst Başlık Şeridi
        val titleBgPaint = Paint().apply { color = accentColor; style = Paint.Style.FILL }
        canvas.drawRect(x, y, x + w, y + 25f, titleBgPaint)

        // Başlık Yazısı
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(title, x + (w / 2), y + 17f, titlePaint)

        // Oyuncu Listesi
        val namePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
            textAlign = Paint.Align.LEFT
        }
        val valuePaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.RIGHT
        }

        var currentY = y + 45f
        if (players.isEmpty()) {
            val emptyPaint = Paint().apply { color = Color.GRAY; textSize = 10f; textAlign = Paint.Align.CENTER }
            canvas.drawText("- Veri Yok -", x + (w/2), currentY + 10f, emptyPaint)
        } else {
            players.forEachIndexed { index, player ->
                // İsim
                val name = if (player.basicStats.name.length > 14) player.basicStats.name.take(13) + "." else player.basicStats.name
                canvas.drawText("${index + 1}. $name", x + 10f, currentY, namePaint)

                // Değer (ValueSelector burada devreye giriyor)
                val value = valueSelector(player)
                canvas.drawText(value, x + w - 10f, currentY, valuePaint)

                currentY += 20f
            }
        }
    }
    // BU FONKSİYONU EKLEYİN
    //
    private fun drawMiniStatCard(c: Canvas, x: Float, y: Float, w: Float, h: Float, title: String, color: Int, lines: List<String>, progress: Float) {
        drawCardBackground(c, x, y, w, h)
        // Başlık
        val titleP = Paint().apply { color; textSize=12f; typeface=Typeface.DEFAULT_BOLD }
        c.drawText(title, x+10f, y+20f, titleP)

        // Progress Bar (Varsa)
        if(progress >= 0) {
            val barBg = Paint().apply {Color.LTGRAY; alpha=50 }
            c.drawRoundRect(RectF(x+10f, y+30f, x+w-10f, y+35f), 5f, 5f, barBg)
            val barFg = Paint().apply {color}
            c.drawRoundRect(RectF(x+10f, y+30f, x+10f+((w-20f)*progress), y+35f), 5f, 5f, barFg)
            val percentP = Paint().apply { textSize=10f;Color.GRAY; textAlign=Paint.Align.RIGHT }
            c.drawText("${(progress*100).toInt()}%", x+w-10f, y+20f, percentP)
        }

        // Satırlar
        val textP = Paint().apply { textSize=11f;colorTextPrimary }
        var ly = y + if(progress>=0) 55f else 40f
        lines.forEach { line ->
            c.drawText(line, x+10f, ly, textP)
            ly += 15f
        }
    }

    private fun drawPassNetworkOnCanvas(c: Canvas, cx: Float, cy: Float, radius: Float, dist: Map<String, Int>, allPlayers: List<Player>) {
        if(dist.isEmpty()) return
        val total = dist.values.sum().toFloat()
        val top5 = dist.toList().sortedByDescending { it.second }.take(5)

        // Merkez Daire (Oyuncu)
        val centerP = Paint().apply { color=colorPrimary; style=Paint.Style.FILL }
        c.drawCircle(cx, cy, 8f, centerP)

        // Bağlantılar
        val lineP = Paint().apply { color=colorPrimary; strokeCap=Paint.Cap.ROUND }
        val textP = Paint().apply { textSize=9f; color=Color.BLACK; textAlign=Paint.Align.CENTER }
        val countP = Paint().apply { textSize=8f; color=Color.GRAY; textAlign=Paint.Align.CENTER }

        val angleStep = (2 * Math.PI) / top5.size
        top5.forEachIndexed { i, (pid, count) ->
            val angle = i * angleStep - (Math.PI / 2) // Yukarıdan başla
            val px = cx + (radius * cos(angle)).toFloat()
            val py = cy + (radius * sin(angle)).toFloat()

            // Çizgi Kalınlığı
            lineP.strokeWidth = 2f + (8f * (count/total))
            lineP.alpha = 150
            c.drawLine(cx, cy, px, py, lineP)

            // Hedef Nokta
            val targetP = Paint().apply { color=colorSecondary; style=Paint.Style.FILL }
            c.drawCircle(px, py, 5f, targetP)

            // İsim
            val pName = allPlayers.find { it.id == pid }?.name?.split(" ")?.firstOrNull() ?: "?"

            // İsmi dairenin biraz dışına yaz
            val lx = cx + ((radius+15f) * cos(angle)).toFloat()
            val ly = cy + ((radius+15f) * sin(angle)).toFloat()
            c.drawText(pName, lx, ly, textP)
            c.drawText("($count)", lx, ly+10f, countP)
        }
    }

    private fun drawFlowBarOnCanvas(c: Canvas, x: Float, y: Float, w: Float, dist: Map<String, Int>, allPlayers: List<Player>) {
        var toHandlers = 0
        var toCutters = 0
        dist.forEach { (pid, count) ->
            val pos = allPlayers.find { it.id == pid }?.position
            if (pos == "Handler" || pos == "Hybrid") toHandlers += count else toCutters += count
        }
        val total = toHandlers + toCutters
        if (total == 0) return

        val hRatio = toHandlers.toFloat() / total
        val barH = 15f

        // Handler Bar (Sol - Mor)
        val hW = w * hRatio
        val hP = Paint().apply { color = colorPrimary }
        c.drawRect(x, y, x+hW, y+barH, hP)

        // Cutter Bar (Sağ - Yeşil)
        val cP = Paint().apply { color = colorOffense }
        c.drawRect(x+hW, y, x+w, y+barH, cP)

        // Etiketler
        val textP = Paint().apply { textSize=10f; color=Color.BLACK }
        c.drawText("Handler: ${(hRatio*100).toInt()}%", x, y-5f, textP)
        c.drawText("Cutter: ${100 - (hRatio*100).toInt()}%", x+w-60f, y-5f, textP)
    }

    private fun drawPlayerHeaderCard(canvas: Canvas, player: Player) {
        val bgPaint = Paint().apply { color = colorPrimary; style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, 595f, 120f, bgPaint)
        val nameP = Paint().apply { color = Color.WHITE; textSize = 28f; typeface = Typeface.DEFAULT_BOLD }
        canvas.drawText(player.name, 40f, 60f, nameP)
        val subP = Paint().apply { color = Color.WHITE; textSize = 16f; alpha = 200 }
        canvas.drawText("#${player.jerseyNumber ?: "-"}  |  ${player.position}  |  ${player.gender}", 40f, 90f, subP)
        if (player.isCaptain) {
            val capP = Paint().apply { color = colorAmber; style = Paint.Style.FILL }
            canvas.drawCircle(520f, 60f, 20f, capP)
            val cText = Paint().apply { color = Color.BLACK; textSize = 20f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
            canvas.drawText("C", 520f, 68f, cText)
        }
    }

    // --- TEMEL YARDIMCILAR ---
    private fun createDummyRosterFromStats(stats: AdvancedPlayerStats): List<Player> {
        // İstatistiklerdeki ID'leri kullanarak sahte oyuncu listesi oluşturur (Sadece isimleri taşımak için)
        // Gerçek uygulamada tüm oyuncu listesini parametre olarak geçmek daha doğrudur,
        // ancak burada kod karmaşıklığını azaltmak için map içindeki isimleri kullanabiliriz (eğer varsa)
        // Fakat DataModels yapısında passDistribution sadece ID -> Int tutuyor.
        // Bu yüzden generatePlayerReport fonksiyonuna allTournaments'den tüm oyuncuları çekmek daha mantıklı.
        // Şimdilik burayı boş dönüyoruz, generatePlayerReport içinde allPlayers'ı kullanacağız.
        return emptyList()
    }

    private fun calculateSafePercentage(n: Int, d: Int): StatPercentage {
        val rate = if (d > 0) n.toDouble() / d else 0.0
        return StatPercentage("${String.format("%.1f", rate * 100)}%", "$n/$d", rate.toFloat())
    }

    private fun drawHeader(canvas: Canvas, title: String, subTitle: String) {
        val bgPaint = Paint().apply { color = colorPrimary; style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, 595f, 80f, bgPaint)
        val textPaint = Paint().apply { color = Color.WHITE; textSize = 24f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
        canvas.drawText(title, 297f, 40f, textPaint)
        textPaint.textSize = 14f; textPaint.color = Color.parseColor("#B3FFFFFF")
        canvas.drawText(subTitle, 297f, 65f, textPaint)
    }

    private fun drawScoreboard(canvas: Canvas, y: Float, match: Match) {
        val bgPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL; setShadowLayer(4f, 0f, 2f, Color.LTGRAY) }
        val rect = RectF(20f, y, 575f, y + 90f)
        canvas.drawRoundRect(rect, 16f, 16f, bgPaint)
        val textPaint = Paint().apply { color = colorTextPrimary; textSize = 20f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.RIGHT }
        canvas.drawText(match.ourTeamName.uppercase(), 240f, y + 50f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(match.opponentName.uppercase(), 355f, y + 50f, textPaint)
        textPaint.textSize = 42f; textPaint.textAlign = Paint.Align.CENTER; textPaint.color = colorPrimary
        canvas.drawText("${match.scoreUs}", 270f, y + 55f, textPaint)
        textPaint.color = colorTextPrimary
        canvas.drawText("-", 297f, y + 55f, textPaint)
        textPaint.color = if (match.scoreThem > match.scoreUs) colorDefense else colorTextPrimary
        canvas.drawText("${match.scoreThem}", 325f, y + 55f, textPaint)
    }

    private fun drawCardBackground(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val p = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL; setShadowLayer(4f, 0f, 2f, Color.LTGRAY) }
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 12f, 12f, p)
    }

    private fun drawSectionTitle(canvas: Canvas, text: String, y: Float) {
        val p = Paint().apply { color = colorTextPrimary; textSize = 14f; typeface = Typeface.DEFAULT_BOLD }
        canvas.drawText(text, 30f, y, p)
    }

    private fun drawSectionTitleSmall(canvas: Canvas, text: String, x: Float, y: Float) {
        val p = Paint().apply { color = colorTextPrimary; textSize = 12f; typeface = Typeface.DEFAULT_BOLD }
        canvas.drawText(text, x, y, p)
    }

    private fun drawModernStatBar(canvas: Canvas, x: Float, y: Float, label: String, progress: Float, valueText: String, barColor: Int) {
        val labelP = Paint().apply { color = Color.GRAY; textSize = 10f }
        val valP = Paint().apply { color = colorTextPrimary; textSize = 10f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.RIGHT }
        canvas.drawText(label, x, y, labelP); canvas.drawText(valueText, x + 230f, y, valP)
        val barY = y + 6f
        val bgBar = Paint().apply { color = Color.parseColor("#F0F0F0") }
        canvas.drawRoundRect(RectF(x, barY, x + 230f, barY + 6f), 50f, 50f, bgBar)
        val fgBar = Paint().apply { color = barColor }
        val w = 230f * progress.coerceIn(0f, 1f)
        if (w > 0) canvas.drawRoundRect(RectF(x, barY, x + w, barY + 6f), 50f, 50f, fgBar)
    }

    private fun drawStitchStatBox(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, title: String, value: String, boxColor: Int) {
        val bgPaint = Paint().apply { color = boxColor.copy(0.1f); style = Paint.Style.FILL }
        val rect = RectF(x, y, x + w, y + h)
        canvas.drawRoundRect(rect, 12f, 12f, bgPaint)
        val titlePaint = Paint().apply { color = Color.GRAY; textSize = 10f; textAlign = Paint.Align.LEFT }
        canvas.drawText(title, x + 10f, y + 20f, titlePaint)
        val valuePaint = Paint().apply { color = boxColor; textSize = 18f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.LEFT }
        canvas.drawText(value, x + 10f, y + 40f, valuePaint)
    }

    private fun drawPlayerTable(canvas: Canvas, startY: Float, playerStats: List<AdvancedPlayerStats>) {
        val headers = listOf("OYUNCU", "GOL", "ASİST", "BLOK", "HATA", "+/-")
        val colX = floatArrayOf(30f, 250f, 310f, 370f, 430f, 490f)
        val headerBg = Paint().apply { color = colorLightBg; style = Paint.Style.FILL }
        canvas.drawRect(20f, startY, 575f, startY + 25f, headerBg)
        val headerPaint = Paint().apply { textSize = 10f; typeface = Typeface.DEFAULT_BOLD; color = Color.BLACK }
        headers.forEachIndexed { i, h -> canvas.drawText(h, colX[i], startY + 16f, headerPaint) }
        var currentY = startY + 35f
        val textPaint = Paint().apply { textSize = 11f; color = colorTextPrimary }
        val centerPaint = Paint(textPaint).apply { textAlign = Paint.Align.CENTER }
        val boldPaint = Paint(centerPaint).apply { typeface = Typeface.DEFAULT_BOLD }
        playerStats.sortedByDescending { it.plusMinus }.forEachIndexed { index, player ->
            if (index % 2 == 1) { canvas.drawRect(20f, currentY - 10f, 575f, currentY + 10f, Paint().apply { color = Color.parseColor("#F9F9F9") }) }
            val dName = if (player.basicStats.name.length > 20) player.basicStats.name.take(18) + ".." else player.basicStats.name
            canvas.drawText(dName, colX[0], currentY, textPaint)
            canvas.drawText("${player.basicStats.goal}", colX[1] + 10f, currentY, centerPaint)
            canvas.drawText("${player.basicStats.assist}", colX[2] + 10f, currentY, centerPaint)
            canvas.drawText("${player.basicStats.block}", colX[3] + 10f, currentY, centerPaint)
            val err = player.basicStats.throwaway + player.basicStats.drop
            canvas.drawText("$err", colX[4] + 10f, currentY, centerPaint)
            val pm = player.plusMinus
            boldPaint.color = if (pm > 0) colorOffense else if (pm < 0) colorDefense else Color.GRAY
            canvas.drawText(String.format("%.1f", pm), colX[5] + 10f, currentY, boldPaint)
            currentY += 20f
        }
    }

    private fun drawFooter(canvas: Canvas) {
        val footerPaint = Paint().apply { color = Color.GRAY; textSize = 10f; textAlign = Paint.Align.CENTER }
        canvas.drawText("DiscBase Mobile App ile oluşturulmuştur.", 297f, 820f, footerPaint)
    }

    private fun savePdf(document: PdfDocument, fileNamePrefix: String): File? {
        return try {
            val directory = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
            val file = File(directory, "${fileNamePrefix}_Rapor.pdf")
            val outputStream = FileOutputStream(file)
            document.writeTo(outputStream)
            document.close()
            outputStream.close()
            file
        } catch (e: Exception) { e.printStackTrace(); document.close(); null }
    }

    // Int.copy extension (Color alpha için)
    private fun Int.copy(alpha: Float): Int {
        val a = (alpha * 255).toInt()
        return android.graphics.Color.argb(a, android.graphics.Color.red(this), android.graphics.Color.green(this), android.graphics.Color.blue(this))
    }

    // Helper: formatSecondsToTime
    private fun formatSecondsToTime(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%02d:%02d", m, s)
    }

    // Helper: calculateTeamStatsForMatch (Kendi içinde hesaplasın)
    private fun calculateTeamStatsForMatch(
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
        val offensiveHolds = pointsArchive.count { it.startMode == PointStartMode.OFFENSE && it.whoScored == "US" }
        val cleanHolds = pointsArchive.count { it.startMode == PointStartMode.OFFENSE && it.whoScored == "US" && it.stats.sumOf { s -> s.throwaway + s.drop } == 0 }
        val breakPointsScored = pointsArchive.count { it.startMode == PointStartMode.DEFENSE && it.whoScored == "US" }
        val totalPassesCompleted = totalSuccessfulPass + totalAssists
        val totalPassesAttempted = totalPassesCompleted + totalThrowaways
        val pointsWithBlocks = pointsArchive.filter { it.stats.sumOf { s -> s.block } > 0 }
        val totalBlockPoints = pointsWithBlocks.size
        val blocksConvertedToGoals = pointsWithBlocks.count { it.whoScored == "US" }
        val totalPossessions = totalGoals + totalThrowaways + totalDrops

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
            totalPossessions = totalPossessions
        )
    }
    // ... Sınıfın içine, diğer generate fonksiyonlarının altına ekle ...

    // --- ANTRENMAN RAPORU (YENİ) ---
    // app/src/main/java/com/eyuphanaydin/discbase/PdfReportGenerator.kt

    // --- TRAINING REPORT (NEW) ---
    fun generateTrainingReport(
        training: Training,
        allPlayers: List<Player>
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var currentY = 0f

        // 1. Header
        drawHeader(canvas, context.getString(R.string.report_training_title))
        currentY = 100f

        // Participation Calculation
        val totalMembers = allPlayers.size
        val attendedCount = training.attendeeIds.size
        val participationRate = if (totalMembers > 0) (attendedCount.toFloat() / totalMembers) else 0f
        val participationText = "$attendedCount / $totalMembers"
        val rateText = "%${(participationRate * 100).toInt()}"

        // 2. Overview Section
        drawSectionTitle(canvas, context.getString(R.string.report_section_overview), currentY)
        currentY += 25f

        drawCardBackground(canvas, 20f, currentY, 555f, 100f)

        val labelP = Paint().apply { color = Color.GRAY; textSize = 10f }
        val valP = Paint().apply { color = colorTextPrimary; textSize = 14f; typeface = Typeface.DEFAULT_BOLD }

        // Date & Time
        canvas.drawText(context.getString(R.string.label_date), 40f, currentY + 30f, labelP)
        canvas.drawText(training.date, 40f, currentY + 50f, valP)

        canvas.drawText(context.getString(R.string.label_time), 180f, currentY + 30f, labelP)
        canvas.drawText(training.time, 180f, currentY + 50f, valP)

        canvas.drawText(context.getString(R.string.label_location), 320f, currentY + 30f, labelP)
        val loc = if(training.location.length > 15) training.location.take(12)+"..." else training.location
        canvas.drawText(loc, 320f, currentY + 50f, valP)

        // Participation Stat
        val statX = 460f
        canvas.drawText(context.getString(R.string.label_participation_rate), statX, currentY + 30f, labelP)

        val bigStatP = Paint().apply { color = if(participationRate >= 0.5f) Color.parseColor("#4CAF50") else Color.parseColor("#FF9800"); textSize = 24f; typeface = Typeface.DEFAULT_BOLD }
        canvas.drawText(rateText, statX, currentY + 60f, bigStatP)

        val smallStatP = Paint().apply { color = Color.DKGRAY; textSize = 12f }
        canvas.drawText("($participationText)", statX + 60f, currentY + 60f, smallStatP)

        currentY += 130f

        // 3. Notes Section
        if (training.description.isNotEmpty() || training.note.isNotEmpty()) {
            drawSectionTitle(canvas, context.getString(R.string.report_section_notes), currentY)
            currentY += 25f
            drawCardBackground(canvas, 20f, currentY, 555f, 60f)
            val desc = if(training.description.isNotEmpty()) training.description else training.note
            val cleanDesc = if(desc.length > 90) desc.take(87) + "..." else desc
            val textP = Paint().apply { color = colorTextPrimary; textSize = 12f }
            canvas.drawText(cleanDesc, 40f, currentY + 35f, textP)
            currentY += 90f
        }

        // 4. Attendees List
        drawSectionTitle(canvas, context.getString(R.string.report_section_attendees), currentY)
        currentY += 30f

        val headerBg = Paint().apply { color = Color.LTGRAY; style = Paint.Style.FILL }
        canvas.drawRect(20f, currentY, 575f, currentY + 25f, headerBg)
        val hText = Paint().apply { textSize = 10f; typeface = Typeface.DEFAULT_BOLD; color = Color.BLACK }

        canvas.drawText(context.getString(R.string.report_col_no), 30f, currentY + 16f, hText)
        canvas.drawText(context.getString(R.string.report_col_player), 80f, currentY + 16f, hText)
        canvas.drawText(context.getString(R.string.report_col_status), 450f, currentY + 16f, hText)

        currentY += 35f
        val rowText = Paint().apply { textSize = 11f; color = colorTextPrimary }

        val attendees = training.attendeeIds.mapNotNull { id ->
            allPlayers.find { it.id == id }
        }.sortedBy { it.name }

        attendees.forEachIndexed { index, player ->
            if (currentY > 780f) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                drawHeader(canvas, context.getString(R.string.report_training_title_cont))
                currentY = 100f
                canvas.drawRect(20f, currentY, 575f, currentY + 25f, headerBg)
                canvas.drawText(context.getString(R.string.report_col_no), 30f, currentY + 16f, hText)
                canvas.drawText(context.getString(R.string.report_col_player), 80f, currentY + 16f, hText)
                canvas.drawText(context.getString(R.string.report_col_status), 450f, currentY + 16f, hText)
                currentY += 35f
            }

            if (index % 2 == 1) {
                val stripePaint = Paint().apply { color = Color.parseColor("#F5F5F5") }
                canvas.drawRect(20f, currentY - 10f, 575f, currentY + 10f, stripePaint)
            }

            canvas.drawText("${index + 1}", 35f, currentY, rowText)
            canvas.drawText(player.name, 80f, currentY, rowText)

            val greenP = Paint().apply { color = Color.parseColor("#4CAF50"); textSize=11f; typeface = Typeface.DEFAULT_BOLD }
            canvas.drawText(context.getString(R.string.status_attended), 450f, currentY, greenP)

            currentY += 20f
        }

        drawFooter(canvas)
        document.finishPage(page)
        return savePdf(document, "Training_${training.date}")
    }
}
