package fr.harmoniamk.statsmkworld.model.local

import fr.harmoniamk.statsmkworld.extension.pointsToPosition
import fr.harmoniamk.statsmkworld.extension.positionToPoints
import fr.harmoniamk.statsmkworld.extension.safeSubList
import fr.harmoniamk.statsmkworld.extension.sizeOrOne
import fr.harmoniamk.statsmkworld.extension.sum
import fr.harmoniamk.statsmkworld.extension.warScoreToDiff
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

data class Stats(
    val warStats: WarStats,
    val warScores: List<WarScore>,
    val maps: List<TrackStats>,
    val averageForMaps: List<TrackStats>,
    // userId non-null ⇒ vue joueur : active les stats individuelles avancées
    // (contribution à l'équipe, distribution des positions). Défaut null pour ne
    // pas casser les sites d'appel équipe/adversaire ni la preview.
    val userId: String? = null,
) {
    val averagePoints: Int =
        warScores.sumOf { it.score } / warScores.sizeOrOne()
    val averagePointsLabel: String = averagePoints.warScoreToDiff(warStats.is24p)
    val averageMapPoints: Int =
        (averageForMaps.map { it.teamScore }.sum() / averageForMaps.sizeOrOne())
    val averagePlayerPosition: List<Int> =
        (averageForMaps.map { it.playerScore }.sum() / averageForMaps.sizeOrOne())
            .pointsToPosition(warStats.is24p)

    val averagePlayerPosLabel = when (val single = averagePlayerPosition.singleOrNull()) {
        null -> "${averagePlayerPosition.firstOrNull()} - ${averagePlayerPosition.lastOrNull()}"
        else -> single.toString()
    }

    val mapsWon = averageForMaps.takeIf { it.isNotEmpty() }?.let {
        "${(it.filter { (it.teamScore ?: 0) > 41 }.size * 100 / it.size)}%"
    }

    val shockCount = averageForMaps.map { it.shockCount }.sum()

    // Wars triées chronologiquement (war.id = timestamp). Source UNIQUE de tri pour toutes
    // les stats temporelles (séries, forme récente) — ne pas réintroduire de tri parallèle.
    val chronologicalWars: List<WarDetails> =
        warScores.sortedBy { it.war.war.id }.map { it.war }

    // Mode 12p/24p (propagé depuis WarStats) : base des marges/outcomes et étendue de la
    // distribution des positions (P1→P12 ou P1→P24).
    private val is24p: Boolean = warStats.is24p

    /**
     * Résultat d'une war côté hôte : +1 victoire / -1 défaite / 0 égalité.
     * - 12p : signe de l'écart de score ([WarDetails.displayedDiff]).
     * - 24p : signe de `scoreMargin` (hôte − meilleur adverse, même règle que [WarStats]).
     */
    private fun WarDetails.outcome(): Int = when (is24p) {
        false -> when {
            displayedDiff.contains('+') -> 1
            displayedDiff.contains('-') -> -1
            else -> 0
        }
        true -> scoreMargin(is24p = true).let {
            when {
                it > 0 -> 1
                it < 0 -> -1
                else -> 0
            }
        }
    }

    /** Série en cours (la plus récente), signée : >0 victoires, <0 défaites, 0 aucune. */
    val currentStreak: Int = currentStreakOf(chronologicalWars)

    /** Issues V/N/D de toutes les wars (chronologique) — pastilles « Momentum » du dashboard (`takeLast(n)`). */
    val chronologicalOutcomes: List<Int> = chronologicalWars.map { it.outcome() }

    /** Score par war (joueur ou équipe) en ordre chronologique — sparkline « Momentum » (`takeLast(n)`). */
    val scoreTimeline: List<Int> = warScores.sortedBy { it.war.war.id }.map { it.score }

    /** Record de série de victoires (max historique). */
    val bestWinStreak: Int = longestStreak(chronologicalWars) { it > 0 }

    /** Record de série de défaites (max historique). */
    val worstLossStreak: Int = longestStreak(chronologicalWars) { it < 0 }

    private fun longestStreak(wars: List<WarDetails>, predicate: (Int) -> Boolean): Int {
        var best = 0
        var current = 0
        wars.forEach { war ->
            when (predicate(war.outcome())) {
                true -> {
                    current++
                    if (current > best) best = current
                }
                else -> current = 0
            }
        }
        return best
    }

    private fun currentStreakOf(wars: List<WarDetails>): Int {
        var streak = 0
        for (war in wars.reversed()) {
            val outcome = war.outcome()
            when {
                outcome == 0 -> if (streak != 0) break
                streak == 0 -> streak = outcome
                streak > 0 && outcome > 0 -> streak++
                streak < 0 && outcome < 0 -> streak--
                else -> return streak
            }
        }
        return streak
    }

    /**
     * Séries déclinées par adversaire (id d'opposant = rosterId/teamId) : pour
     * chaque adversaire, la série en cours, le record de victoires et le record
     * de défaites, calculés sur ses wars triées chronologiquement.
     */
    val streaksByOpponent: Map<String, StreakStats> = chronologicalWars
        .flatMap { war -> war.war.teamOpponent.map { it to war } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, wars) ->
            StreakStats(
                current = currentStreakOf(wars),
                bestWin = longestStreak(wars) { it > 0 },
                worstLoss = longestStreak(wars) { it < 0 }
            )
        }

    /**
     * Séries déclinées par circuit (index de map) : la série de manches
     * gagnées/perdues sur chaque circuit, triées chronologiquement.
     */
    val streaksByTrack: Map<String, StreakStats> = chronologicalWars
        .flatMap { war -> war.warTracks.map { it to it.trackOutcome() } }
        .groupBy({ it.first.index.joinToString("-") }, { it.second })
        .mapValues { (_, outcomes) ->
            StreakStats(
                current = currentStreakOfOutcomes(outcomes),
                bestWin = longestStreakOfOutcomes(outcomes) { it > 0 },
                worstLoss = longestStreakOfOutcomes(outcomes) { it < 0 }
            )
        }

    private fun currentStreakOfOutcomes(outcomes: List<Int>): Int {
        var streak = 0
        for (outcome in outcomes.reversed()) {
            when {
                outcome == 0 -> if (streak != 0) break
                streak == 0 -> streak = outcome
                streak > 0 && outcome > 0 -> streak++
                streak < 0 && outcome < 0 -> streak--
                else -> return streak
            }
        }
        return streak
    }

    private fun longestStreakOfOutcomes(outcomes: List<Int>, predicate: (Int) -> Boolean): Int {
        var best = 0
        var current = 0
        outcomes.forEach { outcome ->
            when (predicate(outcome)) {
                true -> {
                    current++
                    if (current > best) best = current
                }
                else -> current = 0
            }
        }
        return best
    }

    // ---------------------------------------------------------------------
    // Lot A — Top6/Bot6 global (12p)
    // ---------------------------------------------------------------------
    // Une manche est « Top6 » quand les 6 joueurs de l'équipe occupent les
    // positions 1..6 (teamScore == 61 : 15+12+10+9+8+7, barème 12p), et « Bot6 »
    // quand ils occupent 7..12 (teamScore == 21 : 6+5+4+3+2+1). C'est une égalité
    // EXACTE sur le score d'équipe de la manche — même définition que la table
    // équipe MapStats.topsTable["Top 6"] / bottomsTable["Bot 6"] (les 6 positions
    // toutes <= 6, resp. >= 7).

    /** Nombre de manches Top6 (les 6 joueurs en positions 1..6, teamScore == 61). */
    val top6Count: Int = chronologicalWars
        .flatMap { it.warTracks }
        .count { it.teamScore == 61 }
    /** Nombre de manches Bot6 (les 6 joueurs en positions 7..12, teamScore == 21). */
    val bot6Count: Int = chronologicalWars
        .flatMap { it.warTracks }
        .count { it.teamScore == 21 }

    // ---------------------------------------------------------------------
    // Lot B — Meilleures/pires maps par winrate ET par score moyen
    //
    // Seuil échantillon : une map n'entre dans ces classements qu'à partir de
    // MIN_RANKING_SAMPLE matchs joués (cf. DÉCISION PRODUIT ≥ 3).
    // ---------------------------------------------------------------------
    private val mapsRankable: List<TrackStats> = maps.filter { it.totalPlayed >= MIN_RANKING_SAMPLE }

    val bestMapByWinrate: TrackStats? = mapsRankable.maxByOrNull { it.winRate ?: 0 }
    val worstMapByWinrate: TrackStats? = mapsRankable.minByOrNull { it.winRate ?: 0 }

    // Score d'un circuit pour le classement « par score » : en vue joueur, c'est le
    // score DU JOUEUR sur ce circuit (playerScore), pas le score d'équipe — le
    // MapCell affiche déjà la position moyenne du joueur, le tri doit suivre la même
    // base. En vue équipe, teamScore.
    private val TrackStats.rankingScore: Int
        get() = (if (userId != null) playerScore else teamScore) ?: 0

    val bestMapByScore: TrackStats? = mapsRankable.maxByOrNull { it.rankingScore }
    val worstMapByScore: TrackStats? = mapsRankable.minByOrNull { it.rankingScore }

    /** Top 3 / Flop 3 des maps par winrate (seuil ≥ 3 matchs appliqué). */
    val topMapsByWinrate: List<TrackStats> =
        mapsRankable.sortedByDescending { it.winRate ?: 0 }.take(3)
    val flopMapsByWinrate: List<TrackStats> =
        mapsRankable.sortedBy { it.winRate ?: 0 }.take(3)
    val topMapsByScore: List<TrackStats> =
        mapsRankable.sortedByDescending { it.rankingScore }.take(3)
    val flopMapsByScore: List<TrackStats> =
        mapsRankable.sortedBy { it.rankingScore }.take(3)
    /** Top 3 / Flop 3 des maps par NOMBRE de fois jouées (occurrences). Le seuil
     * MIN_RANKING_SAMPLE n'est PAS appliqué ici : « le moins joué » a du sens même
     * sous le seuil, donc on classe sur toutes les maps rencontrées. */
    val topMapsByCount: List<TrackStats> =
        maps.sortedByDescending { it.totalPlayed }.take(3)
    val flopMapsByCount: List<TrackStats> =
        maps.filter { it.totalPlayed > 0 }.sortedBy { it.totalPlayed }.take(3)

    // =====================================================================
    // Stats supplémentaires (bis) — Vagues 1/2/3
    // =====================================================================

    // warScores triés chronologiquement (même clé de tri que chronologicalWars :
    // war.war.id). Conserve le couple (WarDetails, score) pour la forme récente.
    private val chronologicalScores: List<WarScore> =
        warScores.sortedBy { it.war.war.id }

    // --- Vague 1 : forme récente vs historique -------------------------------
    // « Forme récente » = comparaison de 3 fenêtres (all-time, 5 dernières, 10
    // dernières wars) sur les mêmes indicateurs, calculés ici (rule 13 : calcul
    // prioritaire). Chaque indicateur réutilise la définition all-time historique :
    // - winrate (wars gagnées / jouées) ;
    // - score moyen par war (playerScore en vue joueur, total équipe sinon) ;
    // - position moyenne du joueur (vue joueur) / score moyen par manche (équipe) ;
    // - % de manches gagnées (teamScore de manche > 41, cf. mapsWon historique) ;
    // - shocks par war (total shocks filtrés joueur / nb de wars, cf. section détails).
    // Les deltas des 5/10 dernières sont mesurés vs l'all-time ; leur SENS dépend de
    // l'indicateur (position : plus bas = mieux) et est géré à l'affichage.

    /** Forme all-time (toutes les wars) : base des deltas des fenêtres récentes. */
    val allTimeForm: FormStats? = formStats(chronologicalScores, requestedSize = null)

    /** Forme sur les 5 dernières wars ; null si aucune war. */
    val recentForm5: FormStats? = formStats(chronologicalScores.takeLast(5), requestedSize = 5)

    /** Forme sur les 10 dernières wars ; null si aucune war. */
    val recentForm10: FormStats? = formStats(chronologicalScores.takeLast(10), requestedSize = 10)

    /**
     * Construit une [FormStats] sur une fenêtre de wars (déjà triée chrono).
     * [requestedSize] = taille demandée (5/10) pour signaler un petit échantillon ;
     * null pour l'all-time. Les deltas sont mesurés vs [allTimeForm] (null pour
     * l'all-time lui-même, ou si l'un des termes manque).
     */
    private fun formStats(scores: List<WarScore>, requestedSize: Int?): FormStats? {
        if (scores.isEmpty()) return null
        val wars = scores.map { it.war }
        val tracks = wars.flatMap { it.warTracks }

        val winrate = winrateOf(wars)
        val avgScore = averageScoreOf(scores)
        // Position moyenne du joueur (raw position, comme halfAveragePosition) — vue joueur.
        val avgPosition = when (userId) {
            null -> null
            else -> tracks
                .mapNotNull { it.track.positions.firstOrNull { pos -> pos.playerId == userId }?.position }
                .takeIf { it.isNotEmpty() }
                ?.let { it.sum() / it.size }
        }
        // Score moyen par manche (équipe) — même base que averageMapPoints historique.
        val avgMapScore = tracks
            .takeIf { it.isNotEmpty() && userId == null }
            ?.let { list -> list.sumOf { it.teamScore } / list.size }
        // % de manches gagnées — teamScore de manche > 41 (moitié haute), cf. mapsWon.
        val mapsWonPct = tracks
            .takeIf { it.isNotEmpty() }
            ?.let { list -> (list.count { it.teamScore > 41 } * 100) / list.size }
        // Shocks par war — shocks filtrés selon la vue / nb de wars.
        val shocksPerWar = wars
            .takeIf { it.isNotEmpty() }
            ?.let { list ->
                val total = list.sumOf { war ->
                    war.war.tracks.sumOf { track ->
                        track.shocks?.filter { userId == null || it.playerId == userId }?.sumOf { it.count } ?: 0
                    }
                }
                total.toFloat() / list.size
            }

        // Régularité (écart-type) & amplitude min/max des scores sur la fenêtre.
        val windowScores = scores.map { it.score }
        val stdDev = windowScores
            .takeIf { it.size >= 2 }
            ?.let { values ->
                val mean = values.average()
                Math.round(Math.sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size)).toInt()
            }
        val scoreMin = windowScores.minOrNull()
        val scoreMax = windowScores.maxOrNull()
        // Marges moyennes de victoire / défaite sur la fenêtre (écart de score signé).
        val margins = wars.map { it.scoreMargin(is24p = is24p) }
        val winMargin = margins.filter { it > 0 }.takeIf { it.isNotEmpty() }?.let { it.sum() / it.size }
        val lossMargin = margins.filter { it < 0 }.takeIf { it.isNotEmpty() }?.let { m -> m.sumOf { kotlin.math.abs(it) } / m.size }
        // Records de série (max) & comptes Top6/Bot6 sur la FENÊTRE (mêmes définitions
        // que les champs all-time : longestStreak sur les wars fenêtrées, Top6 =
        // teamScore de manche == 61, Bot6 == 21). `scores` est déjà trié chrono.
        val windowWins = longestStreak(wars) { it > 0 }
        val windowLosses = longestStreak(wars) { it < 0 }
        val windowTracks = wars.flatMap { it.warTracks }
        val windowTop6 = windowTracks.count { it.teamScore == 61 }
        val windowBot6 = windowTracks.count { it.teamScore == 21 }
        // Points perdus en pénalités par l'équipe hôte, sur la FENÊTRE (même définition
        // que penaltyPointsLost all-time mais restreinte aux wars fenêtrées).
        val windowPenalty = wars.sumOf { war ->
            war.war.penalties.filter { it.teamId == war.war.teamHost }.sumOf { it.amount }
        }

        val base = allTimeForm
        return FormStats(
            sampleSize = scores.size,
            requestedSize = requestedSize,
            winrate = winrate,
            averageScore = avgScore,
            averagePosition = avgPosition,
            averageMapScore = avgMapScore,
            mapsWonPercent = mapsWonPct,
            shocksPerWar = shocksPerWar,
            scoreStdDev = stdDev,
            scoreMin = scoreMin,
            scoreMax = scoreMax,
            winMargin = winMargin,
            lossMargin = lossMargin,
            bestWinStreak = windowWins,
            worstLossStreak = windowLosses,
            top6Count = windowTop6,
            bot6Count = windowBot6,
            penaltyPointsLost = windowPenalty,
            // Deltas vs all-time : null pour l'all-time (base == null) et si un terme manque.
            winrateDelta = delta(winrate, base?.winrate),
            scoreDelta = delta(avgScore, base?.averageScore),
            positionDelta = delta(avgPosition, base?.averagePosition),
            mapScoreDelta = delta(avgMapScore, base?.averageMapScore),
            mapsWonDelta = delta(mapsWonPct, base?.mapsWonPercent)
        )
    }

    private fun delta(value: Int?, base: Int?): Int? =
        if (value != null && base != null) value - base else null

    private fun winrateOf(wars: List<WarDetails>): Int? = wars
        .takeIf { it.isNotEmpty() }
        ?.let { list -> (list.count { it.outcome() > 0 } * 100) / list.size }

    private fun averageScoreOf(scores: List<WarScore>): Int? = scores
        .takeIf { it.isNotEmpty() }
        ?.let { list -> list.sumOf { it.score } / list.size }

    // --- Vague 1 : contribution du joueur à l'équipe (vue joueur only) -------
    /**
     * % moyen des points de l'équipe apportés par le joueur : moyenne, war par
     * war, du ratio playerScore/teamScore (12p). Null hors vue joueur ou si
     * aucune war exploitable. playerScore = warScores.score (vue joueur) ;
     * teamScore = points totaux de l'équipe hôte sur la war ([WarDetails.scoreHost]).
     */
    val playerContribution: Int? = when (userId) {
        null -> null
        else -> chronologicalScores
            .mapNotNull { warScore ->
                warScore.war.scoreHost
                    .takeIf { it > 0 }
                    ?.let { team -> (warScore.score * 100f) / team }
            }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.let { Math.round(it).toInt() }
    }

    // --- Vague 1 : régularité (écart-type ET amplitude min/max des scores) ----
    private val scoreValues: List<Int> = chronologicalScores.map { it.score }

    /** Écart-type (population) des scores par war ; null si < 2 wars. */
    val scoreStdDev: Int? = scoreValues
        .takeIf { it.size >= 2 }
        ?.let { values ->
            val mean = values.average()
            val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
            Math.round(Math.sqrt(variance)).toInt()
        }

    /** Amplitude min/max des scores par war (null si aucune war). */
    val scoreMin: Int? = scoreValues.minOrNull()
    val scoreMax: Int? = scoreValues.maxOrNull()

    // --- Vague 2 : distribution complète des positions P1→P12 (vue joueur) ---
    /**
     * Nombre de manches où le joueur a fini à chaque position (1..12 en 12p,
     * 1..24 en 24p). Vide hors vue joueur. Étend le principe des tables
     * individuelles de MapStats à l'ensemble des positions. L'étendue suit le
     * mode ([is24p]) pour ne pas tronquer les positions 13..24 en 24p.
     *
     * NB : le RENDU de cette distribution (histogramme P1→P24, couleurs 24p)
     * relève du ticket UI dédié — ici on garantit seulement la justesse des
     * données produites.
     */
    val positionDistribution: List<Pair<Int, Int>> = positionDistributionFor(lastN = null)

    /**
     * Distribution des positions du joueur sur une FENÊTRE : [lastN] = null (all-time),
     * 5 ou 10 dernières wars (triées chrono). Vide hors vue joueur. Alimente le
     * sélecteur de fenêtre de la section « Répartition des positions » (ticket #36).
     */
    fun positionDistributionFor(lastN: Int?): List<Pair<Int, Int>> = when (userId) {
        null -> listOf()
        else -> {
            val windowWars = lastN?.let { chronologicalWars.takeLast(it) } ?: chronologicalWars
            val positions = windowWars
                .flatMap { it.war.tracks }
                .mapNotNull { track -> track.positions.firstOrNull { it.playerId == userId }?.position }
            val range = if (is24p) 1..24 else 1..12
            range.map { pos -> pos to positions.count { it == pos } }
        }
    }

    // --- Vague 2 : marge moyenne de victoire / défaite (gains/défaites séparés)
    /** Marge moyenne (écart de score) lors des victoires ; null si aucune. */
    val averageWinMargin: Int? = warMargins { it > 0 }
    /** Marge moyenne (écart de score) lors des défaites ; null si aucune. */
    val averageLossMargin: Int? = warMargins { it < 0 }

    private fun warMargins(predicate: (Int) -> Boolean): Int? = chronologicalWars
        .map { it.scoreMargin(is24p = is24p) }
        .filter { predicate(it) }
        .takeIf { it.isNotEmpty() }
        ?.let { margins -> margins.sumOf { kotlin.math.abs(it) } / margins.size }

    // --- Vague 3 : perf 1ʳᵉ vs 2ᵉ moitié de war (positions moyennes) ---------
    // N'a de sens qu'en vue joueur (position du joueur). Index de track ordonné.
    val firstHalfAvgPosition: Int? = halfAveragePosition(firstHalf = true)
    val secondHalfAvgPosition: Int? = halfAveragePosition(firstHalf = false)

    private fun halfAveragePosition(firstHalf: Boolean): Int? {
        if (userId == null) return null
        val positions = chronologicalWars.flatMap { war ->
            val tracks = war.war.tracks
            val mid = tracks.size / 2
            val half = if (firstHalf) tracks.take(mid) else tracks.drop(mid)
            half.mapNotNull { track -> track.positions.firstOrNull { it.playerId == userId }?.position }
        }
        return positions.takeIf { it.isNotEmpty() }?.let { it.sum() / it.size }
    }

    // --- Vague 3 : invaincu depuis (série W+T en cours) ----------------------
    /**
     * Nombre de wars consécutives sans défaite (victoires + nuls) les plus
     * récentes. Variante de [currentStreak] : compte outcome >= 0.
     */
    val unbeatenStreak: Int = run {
        var streak = 0
        for (war in chronologicalWars.reversed()) {
            if (war.outcome() >= 0) streak++ else break
        }
        streak
    }

    // --- Vague 3 : points perdus en pénalités --------------------------------
    /** Total des points de pénalité subis par l'équipe hôte sur l'historique. */
    val penaltyPointsLost: Int = chronologicalWars.sumOf { war ->
        war.war.penalties.filter { it.teamId == war.war.teamHost }.sumOf { it.amount }
    }


    companion object {
        // Seuil d'échantillon minimal pour figurer dans les classements
        // winrate/score (maps ET adversaires). Cf. DÉCISION PRODUIT du ticket.
        const val MIN_RANKING_SAMPLE = 3
    }
}

/**
 * Forme d'une fenêtre de wars (all-time, 5 ou 10 dernières), avec deltas vs
 * l'all-time. [sampleSize] = nb de wars réellement disponibles ; [requestedSize] =
 * taille demandée (5/10) ou null pour l'all-time (sert à signaler un petit
 * échantillon). Les deltas sont null pour l'all-time et si un terme manque.
 *
 * Indicateurs (12p) : winrate, score moyen par war, position moyenne du joueur
 * (vue joueur ⇒ [averagePosition]) OU score moyen par manche (vue équipe ⇒
 * [averageMapScore]), % de manches gagnées, shocks/war. Sens des deltas géré à
 * l'affichage (position : plus bas = mieux ; shocks : neutre).
 */
data class FormStats(
    val sampleSize: Int,
    val requestedSize: Int?,
    val winrate: Int?,
    val averageScore: Int?,
    val averagePosition: Int?,
    val averageMapScore: Int?,
    val mapsWonPercent: Int?,
    val shocksPerWar: Float?,
    // Ticket #36 — régularité/amplitude/marges + records de série et Top6/Bot6
    // déclinés par fenêtre (all-time/5/10) pour les sélecteurs de fenêtre des
    // sections Indicateurs ET Records & séries de StatsFullScreen.
    val scoreStdDev: Int? = null,
    val scoreMin: Int? = null,
    val scoreMax: Int? = null,
    val winMargin: Int? = null,
    val lossMargin: Int? = null,
    val bestWinStreak: Int = 0,
    val worstLossStreak: Int = 0,
    val top6Count: Int = 0,
    val bot6Count: Int = 0,
    val penaltyPointsLost: Int = 0,
    val winrateDelta: Int?,
    val scoreDelta: Int?,
    val positionDelta: Int?,
    val mapScoreDelta: Int?,
    val mapsWonDelta: Int?
)

class WarScore(
    val war: WarDetails,
    val score: Int
)

/**
 * Séries associées à une entité (globale, adversaire ou circuit) :
 * - [current] série en cours signée (>0 victoires, <0 défaites, 0 aucune) ;
 * - [bestWin] record de série de victoires ;
 * - [worstLoss] record de série de défaites.
 */
data class StreakStats(
    val current: Int,
    val bestWin: Int,
    val worstLoss: Int
)

data class TrackStats(
    val stats: Stats? = null,
    val map: List<Maps>? = null,
    val trackIndex: List<Int>? = null,
    val teamScore: Int? = null,
    val playerScore: Int? = null,
    val totalPlayed: Int = 0,
    val winRate: Int? = null,
    val shockCount: Int? = null
)

data class WarStats(val list: List<WarDetails>, val is24p: Boolean = false) {
    val warsPlayed = list.count()
    val warsWon = when (is24p) {
        true -> list.count { it.war.scores.sortedByDescending { it.score }.safeSubList(0, 2).map { it.teamId }.contains(it.war.teamHost) }
        else -> list.count { war -> war.displayedDiff.contains('+') }
    }

    val warsTied = list.count { war -> war.displayedDiff == "0" }
    val warsLoss = when (is24p) {
        true -> list.count { it.war.scores.sortedBy { it.score }.safeSubList(0, 2).map { it.teamId }.contains(it.war.teamHost) }
        else -> list.count { war -> war.displayedDiff.contains('-') }
    }
}


data class MapDetails(
    val war: WarDetails,
    val warTrack: WarTrackDetails,
    val position: Int?
)

@FlowPreview
@ExperimentalCoroutinesApi
class MapStats(
    val list: List<MapDetails>,
    val userId: String? = null,
    val is24p: Boolean
) {

    private val isIndiv = userId != null
    private val playerScoreList = list
        .filter { pair -> pair.war.warTracks.any { it.track.hasPlayer(userId) } }
        .map { it.warTrack.track.positions }
        .map { it.singleOrNull { it.playerId == userId } }
        .mapNotNull { it?.position.positionToPoints(is24p) }
    val trackPlayed =
        list.filter { (isIndiv && it.war.warTracks.any { it.track.hasPlayer(userId) }) || !isIndiv }.size
    val trackWon = list
        .filter { pair -> pair.warTrack.displayedDiff.contains('+') }
        .filter { (isIndiv && it.war.warTracks.any { it.track.hasPlayer(userId) }) || !isIndiv }
        .size
    val trackTie = list
        .filter { pair -> pair.warTrack.displayedDiff == "0" }.count {
            (isIndiv && it.war.warTracks.any {
                it.track.hasPlayer(userId)
            }) || !isIndiv
        }
    val trackLoss = list
        .filter { pair -> pair.warTrack.displayedDiff.contains('-') }.count {
            (isIndiv && it.war.warTracks.any {
                it.track.hasPlayer(userId)
            }) || !isIndiv
        }
    val teamScore = list.map { it.warTrack.teamScore }.sum() / list.sizeOrOne()
    val playerPosition: List<Int> =
        (playerScoreList.sum() / playerScoreList.sizeOrOne()).pointsToPosition(is24p)

    val averagePlayerPosLabel = when (val single = playerPosition.singleOrNull()) {
        null -> "${playerPosition.firstOrNull()} - ${playerPosition.lastOrNull()}"
        else -> single.toString()
    }

    /**
     * Position moyenne de l'ÉQUIPE hôte sur ces manches : moyenne arithmétique de TOUTES
     * les positions saisies (6 par manche), arrondie. À utiliser en vue équipe (userId
     * null), où [averagePlayerPosLabel] n'a pas de sens (aucune position filtrée). `null`
     * si aucune position. Position réelle (1..12), pas un score.
     */
    val teamAveragePosition: Int? = list
        .flatMap { it.warTrack.track.positions }
        .map { it.position }
        .takeIf { it.isNotEmpty() }
        ?.let { Math.round(it.average()).toInt() }

    // Tables d'équipe : pour chaque top/bottom N, on compte les manches où les N
    // meilleures/pires positions sont toutes dans le seuil. Une seule passe sur la liste.
    val topsTable = when {
        isIndiv -> (6 downTo 2).map { "Top $it" to 0 }
        else -> (6 downTo 2).map { n ->
            "Top $n" to list.count { it.warTrack.track.positions.count { pos -> pos.position <= n } == n }
        }
    }
    val bottomsTable = when {
        isIndiv -> (6 downTo 2).map { "Bot $it" to 0 }
        else -> (6 downTo 2).map { n ->
            // seuil bas : Bot 6 -> >=7, Bot 5 -> >=8, … Bot 2 -> >=11
            val threshold = 13 - n
            "Bot $n" to list.count { it.warTrack.track.positions.count { pos -> pos.position >= threshold } == n }
        }
    }

    // Tables ADVERSAIRE (12p uniquement, vue équipe) : les 6 positions adverses d'une
    // manche sont le COMPLÉMENT des 6 positions de l'équipe hôte sur 1..12. On applique
    // exactement la même logique de comptage que les tables d'équipe sur ces positions
    // complémentaires. Neutralisées à 0 en vue individuelle (pas de notion d'adversaire)
    // ET en 24p (le complément 1..12 n'a pas de sens hors 12p).
    val opponentTopsTable = when {
        isIndiv || is24p -> (6 downTo 2).map { "Top $it" to 0 }
        else -> (6 downTo 2).map { n ->
            "Top $n" to list.count { detail ->
                val oppPositions = (1..12) - detail.warTrack.track.positions.map { it.position }.toSet()
                oppPositions.count { pos -> pos <= n } == n
            }
        }
    }
    val opponentBottomsTable = when {
        isIndiv || is24p -> (6 downTo 2).map { "Bot $it" to 0 }
        else -> (6 downTo 2).map { n ->
            // seuil bas : Bot 6 -> >=7, Bot 5 -> >=8, … Bot 2 -> >=11
            val threshold = 13 - n
            "Bot $n" to list.count { detail ->
                val oppPositions = (1..12) - detail.warTrack.track.positions.map { it.position }.toSet()
                oppPositions.count { pos -> pos >= threshold } == n
            }
        }
    }

    // Tables individuelles : nombre de manches où le joueur a fini à la position N.
    val indivTopsTable = (1..6).map { n ->
        n.toString() to when {
            isIndiv -> list.count { it.warTrack.track.positions.singleOrNull { pos -> pos.position == n }?.playerId == userId }
            else -> 0
        }
    }
    val indivBottomsTable = (7..12).map { n ->
        n.toString() to when {
            isIndiv -> list.count { it.warTrack.track.positions.singleOrNull { pos -> pos.position == n }?.playerId == userId }
            else -> 0
        }
    }
    val shockCount = list.map {
        it.warTrack.track.shocks?.filter { (isIndiv && it.playerId == userId) || !isIndiv }
            ?.sumOf { it.count }
    }.sum()

    /**
     * Répartition des positions (position → nombre d'occurrences) sur l'ensemble des
     * manches de cette sélection. En vue **individuelle** (userId non-null) : positions
     * DU JOUEUR ; sinon : toutes les positions de l'ÉQUIPE hôte (6 par manche). Étendue
     * P1→P12 (12p) / P1→P24 (24p). Alimente l'histogramme mutualisé
     * (`ui/stats/MKDistributionCard.kt`) des fiches détail Adversaire/Circuit (#27).
     */
    val positionDistribution: List<Pair<Int, Int>> = run {
        val positions = list
            .flatMap { it.warTrack.track.positions }
            .filter { (isIndiv && it.playerId == userId) || !isIndiv }
            .map { it.position }
        val range = if (is24p) 1..24 else 1..12
        range.map { pos -> pos to positions.count { it == pos } }
    }

}
