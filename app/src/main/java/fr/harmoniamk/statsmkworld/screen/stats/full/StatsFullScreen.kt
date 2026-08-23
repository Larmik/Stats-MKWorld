package fr.harmoniamk.statsmkworld.screen.stats.full

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.extension.pointsToPosition
import fr.harmoniamk.statsmkworld.extension.positionColor
import fr.harmoniamk.statsmkworld.extension.trackScoreToDiff
import fr.harmoniamk.statsmkworld.extension.warScoreToDiff
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.screen.stats.ranking.RankingItem
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.stats.DistributionChart
import fr.harmoniamk.statsmkworld.ui.stats.DistributionFooter
import fr.harmoniamk.statsmkworld.ui.stats.Eyebrow
import fr.harmoniamk.statsmkworld.ui.stats.PodiumEntry
import fr.harmoniamk.statsmkworld.ui.stats.PodiumRow
import fr.harmoniamk.statsmkworld.ui.stats.StatCard
import fr.harmoniamk.statsmkworld.ui.stats.StatCardRadius
import fr.harmoniamk.statsmkworld.ui.stats.StatHeaderCard
import fr.harmoniamk.statsmkworld.ui.stats.WinTieLossBar
import fr.harmoniamk.statsmkworld.ui.cells.PlayerMedallion
import fr.harmoniamk.statsmkworld.ui.cells.playerAvatarColor
import fr.harmoniamk.statsmkworld.ui.stats.initialsOf

private val CardRadius = StatCardRadius

/** États hissés des sélecteurs de section (fenêtres par section, tri podiums). */
private class SectionSelectors(
    val windowIndex: Int,
    val onWindowChange: (Int) -> Unit,
    val recordsWindowIndex: Int,
    val onRecordsWindowChange: (Int) -> Unit,
    val distributionWindowIndex: Int,
    val onDistributionWindowChange: (Int) -> Unit,
    val contributorsWindowIndex: Int,
    val onContributorsWindowChange: (Int) -> Unit,
    // Tri des podiums : 0 = occurrences (défaut), 1 = winrate, 2 = score.
    val trackSortIndex: Int,
    val onTrackSortChange: (Int) -> Unit,
    val opponentSortIndex: Int,
    val onOpponentSortChange: (Int) -> Unit
)

/** FormStats de la fenêtre sélectionnée (0 = all-time, 1 = 5, 2 = 10). */
private fun Stats.windowForm(index: Int) = when (index) {
    1 -> recentForm5
    2 -> recentForm10
    else -> allTimeForm
}

/** N dernières wars d'une fenêtre (0 = all-time → null ; 1 → 5 ; 2 → 10). */
private fun windowLastN(index: Int): Int? = when (index) {
    1 -> 5
    2 -> 10
    else -> null
}

/** Sélecteur de fenêtre all-time / 5 dernières / 10 dernières (pill, sur carte sombre). */
@Composable
private fun ColumnScope.WindowSelector(index: Int, onChange: (Int) -> Unit) {
    MKSegmentedSelector(
        items = listOf(
            stringResource(R.string.all_time),
            stringResource(R.string.last_n_short, 5),
            stringResource(R.string.last_n_short, 10)
        ),
        page = index,
        onDark = true,
        onClick = onChange
    )
    Spacer(Modifier.height(11.dp))
}

/**
 * Écran Statistiques du pôle Stats (ticket #25). Deux portées :
 * - onglets **Individuelles / Équipe** ([StatsFullViewModel.showTabs] = true, « mes stats ») ;
 * - vue **pour un joueur donné** (`statsfull`, [showTabs] = false) : rendu Individuelles
 *   seul, avec barre de retour et sous-titre = nom du joueur.
 *
 * Toggle 12 j / 24 j réactif (rule 11). Rendu pixel-perfect maquette (rule 13/15),
 * réutilisant le vocabulaire visuel de l'Accueil (cartes translucides, eyebrows,
 * tuiles, barre V/N/D). Données réelles ; libellés de saison masqués (#30 non livré).
 */
@Composable
fun StatsFullScreen(
    viewModel: StatsFullViewModel,
    onBack: (() -> Unit)? = null,
    onResults: (() -> Unit)? = null
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    // 0 = Individuelles, 1 = Équipe. Sur statsfull (pas d'onglets) → toujours 0.
    var scopeIndex by rememberSaveable { mutableIntStateOf(0) }
    // États UI locaux des sélecteurs de section (rule 11), hissés ici car les
    // sections sont des extensions LazyListScope. Survivent à la rotation.
    // Fenêtres (0 = all-time, 1 = 5 dernières, 2 = 10 dernières) : une pour les
    // Indicateurs, une indépendante pour Records & séries.
    var windowIndex by rememberSaveable { mutableIntStateOf(0) }
    var recordsWindowIndex by rememberSaveable { mutableIntStateOf(0) }
    var distributionWindowIndex by rememberSaveable { mutableIntStateOf(0) }
    var contributorsWindowIndex by rememberSaveable { mutableIntStateOf(0) }
    // Critère des podiums circuits / adversaires : 0 = occurrences (défaut), 1 = winrate, 2 = score.
    var trackSortIndex by rememberSaveable { mutableIntStateOf(0) }
    var opponentSortIndex by rememberSaveable { mutableIntStateOf(0) }

    val subtitle = when (viewModel.showTabs) {
        true -> null
        else -> state.value.playerName
    }
    val selectors = SectionSelectors(
        windowIndex = windowIndex,
        onWindowChange = { windowIndex = it },
        recordsWindowIndex = recordsWindowIndex,
        onRecordsWindowChange = { recordsWindowIndex = it },
        distributionWindowIndex = distributionWindowIndex,
        onDistributionWindowChange = { distributionWindowIndex = it },
        contributorsWindowIndex = contributorsWindowIndex,
        onContributorsWindowChange = { contributorsWindowIndex = it },
        trackSortIndex = trackSortIndex,
        onTrackSortChange = { trackSortIndex = it },
        opponentSortIndex = opponentSortIndex,
        onOpponentSortChange = { opponentSortIndex = it }
    )
    BaseScreen(
        title = stringResource(R.string.statistiques),
        subtitle = subtitle,
        // Bouton retour uniquement en fiche poussée (showTabs=false) ; en onglet du pôle
        // Stats (showTabs=true), pas de retour d'appbar (rule 14, comportement onglet).
        onBack = onBack?.takeIf { !viewModel.showTabs },
        modifier = Modifier.padding(bottom = if (viewModel.showTabs) 90.dp else 0.dp)
    ) {
        when {
            state.value.loading -> CircularProgressIndicator()
            else -> {
                // Le sélecteur 12 j / 24 j est retiré temporairement (ticket #37) :
                // l'écran ne présente que le 12p (is24p figé à false côté VM).
                if (viewModel.showTabs) {
                    MKSegmentedSelector(
                        items = listOf(
                            stringResource(R.string.stats_scope_individual),
                            stringResource(R.string.stats_scope_team)
                        ),
                        page = scopeIndex,
                        onClick = { scopeIndex = it }
                    )
                    Spacer(Modifier.height(11.dp))
                }
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    when (scopeIndex) {
                        1 -> teamSections(state.value, selectors)
                        else -> individualSections(state.value, viewModel.showTabs, onResults, selectors)
                    }
                }
            }
        }
    }
}

// =====================================================================
// Onglet Individuelles (et statsfull)
// =====================================================================

private fun androidx.compose.foundation.lazy.LazyListScope.individualSections(
    state: StatsFullViewModel.State,
    showTabs: Boolean,
    onResults: (() -> Unit)?,
    selectors: SectionSelectors
) {
    val stats = state.playerStats ?: return
    // 1. En-tête (seulement dans l'onglet, pas sur statsfull qui a déjà le sous-titre).
    if (showTabs) item {
        HeaderCard(
            name = state.playerName.orEmpty(),
            subtitle = stringResource(R.string.stats_player_subtitle, stats.warStats.warsPlayed),
            color = Colors.blue,
            logo = state.playerLogo,
            isTeam = false
        )
    }
    // 2. Bilan.
    item { BalanceCard(stats, showResultsLink = !showTabs, onResults = onResults) }
    // 3. Indicateurs (grille régulière) — vue JOUEUR : points/war, position, régularité,
    //    marges V/D, pénalités, maps gagnées, shocks… (fenêtre all/5/10 + deltas).
    item { IndicatorsCard(stats = stats, isPlayer = true, selectors = selectors) }
    // 4. Contribution (rang lu sur le classement all-time des contributeurs).
    stats.playerContribution?.let { contribution ->
        item {
            StatCard(title = stringResource(R.string.stats_contribution_title)) {
                IconLine(
                    icon = R.drawable.stats,
                    accent = Colors.yellow,
                    title = stringResource(R.string.stats_contribution_value, contribution),
                    subtitle = state.contributorsByWindow[0].orEmpty()
                        .indexOfFirst { it.isMe }
                        .takeIf { it >= 0 }
                        ?.let { stringResource(R.string.stats_contribution_rank, it + 1) }
                        ?: ""
                )
            }
        }
    }
    // 5. Forme & séries + Records (grille 3×2 + sélecteur de fenêtre).
    item { FormStreakCard(stats, stringResource(R.string.stats_player_form_title)) }
    item { RecordsTilesCard(stats, selectors) }
    // 6. Distribution des positions (sélecteur de fenêtre + barres ancrées en bas).
    item { DistributionCard(stats, selectors) }
    // 7. Podium circuits (Top3 / Flop3 sur une ligne + sélecteur occ./winrate/score).
    item { MapsPodiumCard(stats = stats, selectors = selectors, userId = state.targetUserId) }
    // 8. Podium adversaires (perspective joueur).
    item { OpponentsPodiumCard(selectors = selectors, podiums = state.playerOpponents, userId = state.targetUserId) }
}

// =====================================================================
// Onglet Équipe
// =====================================================================

private fun androidx.compose.foundation.lazy.LazyListScope.teamSections(
    state: StatsFullViewModel.State,
    selectors: SectionSelectors
) {
    val stats = state.teamStats ?: return
    // 1. En-tête.
    item {
        HeaderCard(
            name = state.teamName.orEmpty(),
            subtitle = stringResource(R.string.stats_team_subtitle, stats.warStats.warsPlayed),
            color = Colors.purple,
            logo = state.teamLogo,
            isTeam = true
        )
    }
    // 2. Bilan équipe.
    item { BalanceCard(stats, showResultsLink = false, onResults = null) }
    // 3. Détails équipe (grille régulière) — vue ÉQUIPE : score = ÉCART de points
    //    (warScoreToDiff), score moyen/manche, maps gagnées, marges d'équipe, pénalités…
    item { IndicatorsCard(stats = stats, isPlayer = false, selectors = selectors) }
    // 4. Forme & séries équipe + Records (grille 3×2 + sélecteur de fenêtre).
    item { FormStreakCard(stats, stringResource(R.string.stats_team_form_title)) }
    item { RecordsTilesCard(stats, selectors) }
    // 5. Contributeurs (sélecteur de fenêtre : classement recalculé par fenêtre).
    item { ContributorsCard(state.contributorsByWindow, selectors) }
    // 6. Podium circuits équipe (Top3 / Flop3 sur une ligne + sélecteur occ./winrate/score).
    item { MapsPodiumCard(stats = stats, selectors = selectors, userId = null) }
    // 7. Podium adversaires équipe.
    item { OpponentsPodiumCard(selectors = selectors, podiums = state.teamOpponents, userId = null) }
}

// =====================================================================
// Section Indicateurs (fenêtre all-time / 5 / 10 + deltas) — ticket #36
// =====================================================================

/**
 * Section « Indicateurs » avec sélecteur de fenêtre (all-time / 5 dernières / 10
 * dernières). Chaque tuile affiche la valeur de la fenêtre choisie + une **progression
 * en %** (delta vs all-time, flèche ↗/↘ colorée) quand pertinent — comme le Momentum
 * de l'Accueil. Distingue strictement vue **joueur** (points/war, position) et
 * **équipe** (points d'équipe/war, score moyen/manche).
 */
@Composable
private fun IndicatorsCard(stats: Stats, isPlayer: Boolean, selectors: SectionSelectors) {
    val window = stats.windowForm(selectors.windowIndex)
    // Deltas seulement hors all-time (index 0 = pas de comparaison).
    val showDelta = selectors.windowIndex != 0
    val title = if (isPlayer) stringResource(R.string.stats_player_indicators) else stringResource(R.string.stats_team_details)
    StatCard(title = title) {
        WindowSelector(selectors.windowIndex, selectors.onWindowChange)
        val tiles = buildList {
            add(MetricTile(stringResource(R.string.form_winrate), window?.winrate?.let { "$it%" } ?: "-", if (showDelta) window?.winrateDelta else null, "%", DeltaPolarity.HIGHER))
            when (isPlayer) {
                true -> {
                    // Vue JOUEUR : score = points/war (brut) + position moyenne.
                    add(MetricTile(stringResource(R.string.stats_points_per_war), window?.averageScore?.toString() ?: "-", if (showDelta) window?.scoreDelta else null, "", DeltaPolarity.HIGHER))
                    add(MetricTile(stringResource(R.string.average_position_short), window?.averagePosition?.toString() ?: "-", if (showDelta) window?.positionDelta else null, "", DeltaPolarity.LOWER))
                }
                else -> {
                    // Vue ÉQUIPE : « Score moyen » = ÉCART de points (warScoreToDiff), pas le total.
                    add(MetricTile(stringResource(R.string.form_score), window?.averageScore?.warScoreToDiff(false) ?: "-", if (showDelta) window?.scoreDelta else null, "", DeltaPolarity.HIGHER))
                    add(MetricTile(stringResource(R.string.average_map_score_short), window?.averageMapScore?.toString() ?: "-", if (showDelta) window?.mapScoreDelta else null, "", DeltaPolarity.HIGHER))
                }
            }
            add(MetricTile(stringResource(R.string.maps_gagn_es), window?.mapsWonPercent?.let { "$it%" } ?: "-", if (showDelta) window?.mapsWonDelta else null, "%", DeltaPolarity.HIGHER))
            add(MetricTile(stringResource(R.string.stats_regularity), window?.scoreStdDev?.let { "±$it" } ?: "-", null, "", DeltaPolarity.NONE))
            add(MetricTile(stringResource(R.string.avg_win_margin), window?.winMargin?.let { "+$it" } ?: "-", null, "", DeltaPolarity.NONE))
            add(MetricTile(stringResource(R.string.avg_loss_margin), window?.lossMargin?.let { "-$it" } ?: "-", null, "", DeltaPolarity.NONE))
            // Pénalités (points perdus par l'équipe hôte) sur la FENÊTRE choisie.
            add(MetricTile(stringResource(R.string.penalty_points_lost), (window?.penaltyPointsLost ?: 0).takeIf { it > 0 }?.let { "-$it" } ?: "-", null, "", DeltaPolarity.NONE))
            add(MetricTile(stringResource(R.string.shocks_per_war_short), window?.shocksPerWar?.let { String.format(java.util.Locale.getDefault(), "%.1f", it) } ?: "-", null, "", DeltaPolarity.NONE))
        }
        MetricTiles(tiles)
    }
}

private enum class DeltaPolarity { HIGHER, LOWER, NONE }

/** Donnée d'une tuile d'indicateur : valeur + delta signé optionnel. */
private data class MetricTile(
    val label: String,
    val value: String,
    val delta: Int?,
    val deltaSuffix: String,
    val polarity: DeltaPolarity
)

// Hauteurs RÉSERVÉES pour figer la taille des tuiles quel que soit le contenu :
// - [DeltaSlotHeight] : la ligne de progression (delta %), occupée même absente ;
// - [LabelSlotHeight] : le libellé, calé sur DEUX lignes (occupé même si le libellé
//   tient sur une ligne) → toutes les tuiles ont exactement la même hauteur, qu'un
//   libellé passe sur 1 ou 2 lignes et qu'un delta soit présent ou non (ticket #36).
private val DeltaSlotHeight = 15.dp
private val LabelSlotHeight = 26.dp

/**
 * Grille RÉGULIÈRE de tuiles : [columns] colonnes à poids égal, toutes les tuiles de
 * même taille. Chaque tuile réserve la place du delta ([DeltaSlotHeight]) qu'il soit
 * présent ou non → aucun redimensionnement au changement de fenêtre.
 */
@Composable
private fun ColumnScope.MetricTiles(tiles: List<MetricTile>, columns: Int = 3) {
    tiles.chunked(columns).forEach { rowTiles ->
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rowTiles.forEach { tile -> MetricTileCell(tile) }
            repeat(columns - rowTiles.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun RowScope.MetricTileCell(tile: MetricTile) {
    Column(Modifier.weight(1f).background(Colors.white30, CardRadius).padding(10.dp)) {
        // Valeur : toujours BLANCHE (aucune couleur).
        MKText(text = tile.value, font = Fonts.Urbanist, textColor = Colors.white, fontSize = 18, textAlign = TextAlign.Start, maxLines = 1)
        // Ligne de progression à hauteur RÉSERVÉE (même vide) : la place du delta est
        // toujours occupée → hauteur de tuile figée sur la plus grande (avec delta).
        Box(Modifier.height(DeltaSlotHeight).padding(top = 3.dp)) {
            tile.delta?.takeIf { it != 0 && tile.polarity != DeltaPolarity.NONE }?.let { delta ->
                val improved = if (tile.polarity == DeltaPolarity.LOWER) delta < 0 else delta > 0
                val arrow = if (delta > 0) "↗" else "↘"
                MKText(
                    text = "${if (delta > 0) "+" else ""}$delta${tile.deltaSuffix} $arrow",
                    font = Fonts.NunitoBD,
                    textColor = if (improved) Colors.green else Colors.red,
                    fontSize = 10,
                    textAlign = TextAlign.Start
                )
            }
        }
        // Libellé à hauteur RÉSERVÉE (2 lignes) : occupé même sur une seule ligne →
        // aucune tuile n'est plus haute qu'une autre selon la longueur du libellé.
        Box(Modifier.padding(top = 6.dp).height(LabelSlotHeight)) {
            MKText(text = tile.label, textColor = Colors.white70, fontSize = 10, textAlign = TextAlign.Start, maxLines = 2)
        }
    }
}

// =====================================================================
// Section Records & séries — grille 3×2 + sélecteur de fenêtre (ticket #36)
// =====================================================================

/**
 * Records & séries : sélecteur de fenêtre (all-time / 5 / 10) + grille **3 lignes ×
 * 2 colonnes**, calculée sur la fenêtre choisie (`FormStats`). La « série en cours »
 * n'est PAS ici (elle est dans « Forme & séries »).
 * - Ligne 1 : Amplitude → deux cellules (min | max).
 * - Ligne 2 : record de victoires | record de défaites.
 * - Ligne 3 : Top 6 | Bot 6 (compte).
 */
@Composable
private fun RecordsTilesCard(stats: Stats, selectors: SectionSelectors) {
    val window = stats.windowForm(selectors.recordsWindowIndex)
    StatCard(title = stringResource(R.string.records_series)) {
        WindowSelector(selectors.recordsWindowIndex, selectors.onRecordsWindowChange)
        val tiles = buildList {
            // Ligne 1 — Amplitude scindée en min | max (par fenêtre).
            add(MetricTile(stringResource(R.string.stats_amplitude_min), window?.scoreMin?.toString() ?: "-", null, "", DeltaPolarity.NONE))
            add(MetricTile(stringResource(R.string.stats_amplitude_max), window?.scoreMax?.toString() ?: "-", null, "", DeltaPolarity.NONE))
            // Ligne 2 — records de série (par fenêtre).
            add(MetricTile(stringResource(R.string.best_win_streak), (window?.bestWinStreak ?: 0).toString(), null, "", DeltaPolarity.NONE))
            add(MetricTile(stringResource(R.string.worst_loss_streak), (window?.worstLossStreak ?: 0).toString(), null, "", DeltaPolarity.NONE))
            // Ligne 3 — Top6 / Bot6 (compte, par fenêtre).
            add(MetricTile(stringResource(R.string.top6_count), (window?.top6Count ?: 0).toString(), null, "", DeltaPolarity.NONE))
            add(MetricTile(stringResource(R.string.bot6_count), (window?.bot6Count ?: 0).toString(), null, "", DeltaPolarity.NONE))
        }
        MetricTiles(tiles, columns = 2)
    }
}

// =====================================================================
// Podiums circuits & adversaires (Top3/Flop3 + sélecteur) — ticket #36
// =====================================================================

/**
 * Podium circuits : sélecteur **occurrences / winrate / score**, Top 3 / Flop 3 chacun
 * sur **une ligne** de 3 `PodiumCell`. Chaque cellule reprend les infos de `MapCell`
 * (nb de fois joué, winrate, score équipe / position joueur). [userId] non-null ⇒
 * score/position du joueur ; null ⇒ score d'équipe.
 */
@Composable
private fun MapsPodiumCard(stats: Stats, selectors: SectionSelectors, userId: String?) {
    val (top, flop) = when (selectors.trackSortIndex) {
        1 -> stats.topMapsByWinrate to stats.flopMapsByWinrate
        2 -> stats.topMapsByScore to stats.flopMapsByScore
        else -> stats.topMapsByCount to stats.flopMapsByCount
    }
    if (top.isEmpty() && flop.isEmpty()) return
    val scoreLabel = if (userId != null) R.string.average_position_short else R.string.form_score
    val toEntry: (fr.harmoniamk.statsmkworld.model.local.TrackStats) -> PodiumEntry = { track ->
        val map = track.map?.firstOrNull()
        val scoreValue = when {
            userId != null -> track.playerScore.pointsToPosition(false).firstOrNull()?.toString() ?: "-"
            else -> track.teamScore?.trackScoreToDiff(false) ?: "-"
        }
        PodiumEntry(
            labelRes = map?.label,
            pictureRes = map?.picture,
            stats = listOf(
                R.string.times_played_short to track.totalPlayed.toString(),
                R.string.form_winrate to "${track.winRate ?: 0}%",
                scoreLabel to scoreValue
            )
        )
    }
    StatCard(title = stringResource(R.string.best_maps_section)) {
        SortSelector(selectors.trackSortIndex, selectors.onTrackSortChange)
        Spacer(Modifier.height(11.dp))
        PodiumLabel(stringResource(R.string.stats_podium_top))
        PodiumRow(top.map(toEntry))
        Spacer(Modifier.height(8.dp))
        PodiumLabel(stringResource(R.string.stats_podium_flop))
        PodiumRow(flop.map(toEntry))
    }
}

/**
 * Podium adversaires : sélecteur **occurrences / winrate / score**, Top 3 / Flop 3
 * chacun sur **une ligne** de 3 `PodiumCell`. Chaque cellule reprend les infos de
 * `TeamCell` (nb de confrontations, winrate, score). [userId] non-null ⇒ score du
 * joueur (Individuelles) ; null ⇒ écart d'équipe (Équipe).
 */
@Composable
private fun OpponentsPodiumCard(
    selectors: SectionSelectors,
    podiums: StatsFullViewModel.OpponentPodiums,
    userId: String?
) {
    val (top, flop) = when (selectors.opponentSortIndex) {
        1 -> podiums.topByWinrate to podiums.flopByWinrate
        2 -> podiums.topByScore to podiums.flopByScore
        else -> podiums.topByCount to podiums.flopByCount
    }
    if (top.isEmpty() && flop.isEmpty()) return
    val toEntry: (RankingItem.OpponentRanking) -> PodiumEntry = { opponent ->
        val scoreValue = when (userId) {
            null -> opponent.stats.averagePointsLabel      // écart d'équipe
            else -> opponent.stats.averagePoints.toString() // score du joueur
        }
        PodiumEntry(
            name = opponent.team.name,
            logo = opponent.team.logo,
            stats = listOf(
                R.string.times_played_short to opponent.warsPlayedLabel,
                R.string.form_winrate to opponent.winrateLabel,
                R.string.form_score to scoreValue
            )
        )
    }
    StatCard(title = stringResource(R.string.best_opponents_section)) {
        SortSelector(selectors.opponentSortIndex, selectors.onOpponentSortChange)
        Spacer(Modifier.height(11.dp))
        PodiumLabel(stringResource(R.string.stats_podium_top))
        PodiumRow(top.map(toEntry))
        Spacer(Modifier.height(8.dp))
        PodiumLabel(stringResource(R.string.stats_podium_flop))
        PodiumRow(flop.map(toEntry))
    }
}

/** Sélecteur occurrences / winrate / score (pill, sur carte sombre — occurrences défaut). */
@Composable
private fun ColumnScope.SortSelector(index: Int, onChange: (Int) -> Unit) {
    MKSegmentedSelector(
        items = listOf(
            stringResource(R.string.stats_sort_occurrences),
            stringResource(R.string.stats_sort_winrate),
            stringResource(R.string.stats_sort_score)
        ),
        page = index,
        onDark = true,
        onClick = onChange
    )
}

@Composable
private fun PodiumLabel(text: String) {
    MKText(text = text.uppercase(), font = Fonts.NunitoBD, textColor = Colors.white66, fontSize = 11, textAlign = TextAlign.Start, modifier = Modifier.padding(bottom = 4.dp))
}

// =====================================================================
// Composants de carte (style maquette, réutilisés par les deux onglets)
// =====================================================================

/**
 * En-tête : vignette (photo joueur / logo équipe) + nom (Bungee) + sous-titre.
 * [logo] = URL MKCentral déjà préfixée (avatar joueur en Individuelles, logo équipe
 * en Équipe) ; fallback = pastille d'initiales (joueur) ou default_logo (équipe).
 * Délègue à [StatHeaderCard] partagé (rule 16).
 */
@Composable
private fun HeaderCard(name: String, subtitle: String, color: Color, logo: String?, isTeam: Boolean) {
    StatHeaderCard(
        name = name,
        subtitle = subtitle,
        color = color,
        logo = logo,
        fallbackText = if (isTeam) null else initialsOf(name)
    )
}

/** Carte « bilan » : gros winrate + V/N/D + barre V/N/D (+ lien Résultats optionnel). */
@Composable
private fun BalanceCard(stats: Stats, showResultsLink: Boolean, onResults: (() -> Unit)?) {
    val played = stats.warStats.warsPlayed.takeIf { it > 0 } ?: 1
    val won = stats.warStats.warsWon
    val tied = stats.warStats.warsTied
    val loss = stats.warStats.warsLoss
    val winrate = stats.allTimeForm?.winrate ?: (won * 100 / played)
    StatCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(stringResource(R.string.stats_balance_title))
            Spacer(Modifier.weight(1f))
            if (showResultsLink && onResults != null) {
                MKText(
                    text = stringResource(R.string.stats_results_link),
                    font = Fonts.NunitoBD,
                    textColor = Colors.yellow,
                    fontSize = 12,
                    modifier = Modifier.clickable(onClick = onResults)
                )
            }
        }
        Spacer(Modifier.height(11.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            MKText(text = "$winrate%", font = Fonts.Urbanist, textColor = Colors.green, fontSize = 30, textAlign = TextAlign.Start)
            MKText(text = "$won V · $tied N · $loss D", textColor = Colors.white66, fontSize = 12)
        }
        Spacer(Modifier.height(11.dp))
        WinTieLossBar(won, tied, loss)
    }
}

/** Ligne icône + gros titre + sous-titre (contribution). */
@Composable
private fun IconLine(icon: Int, accent: Color, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.25f)).border(1.dp, accent.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(icon), contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            MKText(text = title, font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 15, textAlign = TextAlign.Start)
            if (subtitle.isNotEmpty()) MKText(text = subtitle, textColor = Colors.white66, fontSize = 12, textAlign = TextAlign.Start, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

/** Carte « forme & séries » : flamme + série en cours + forme sur 10 wars + record. */
@Composable
private fun FormStreakCard(stats: Stats, title: String) {
    val streak = stats.currentStreak
    val isWin = streak >= 0
    val accent = if (isWin) Colors.green else Colors.red
    val streakText = when {
        streak > 0 -> stringResource(R.string.home_win_streak, streak)
        streak < 0 -> stringResource(R.string.home_loss_streak, -streak)
        else -> stringResource(R.string.no_streak)
    }
    val delta = stats.recentForm10?.winrateDelta
    val record = if (isWin) stats.bestWinStreak else stats.worstLossStreak
    StatCard(title = title) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.25f)).border(1.dp, accent.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(R.drawable.ic_flame), contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                // Texte de la série en blanc (#50 pt.3) — l'accent (flamme) garde sa couleur V/D.
                MKText(text = streakText, font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 15, textAlign = TextAlign.Start)
                val deltaText = delta?.let { stringResource(R.string.stats_form_delta, if (it >= 0) "+$it" else "$it") } ?: ""
                MKText(
                    text = stringResource(R.string.stats_form_record, deltaText, record),
                    textColor = Colors.white66,
                    fontSize = 12,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}


// --- Distribution des positions (`.dist`) ------------------------------------

/**
 * Carte « Répartition des positions » : sélecteur de fenêtre (all-time / 5 / 10) +
 * histogramme P1→P12 recalculé sur la fenêtre + pied Top6/Bot6 (sur la même fenêtre).
 */
@Composable
private fun DistributionCard(stats: Stats, selectors: SectionSelectors) {
    val distribution = stats.positionDistributionFor(windowLastN(selectors.distributionWindowIndex))
    if (distribution.none { it.second > 0 }) return
    StatCard(title = stringResource(R.string.stats_distribution_title)) {
        WindowSelector(selectors.distributionWindowIndex, selectors.onDistributionWindowChange)
        // Chart/footer mutualisés (ui/stats/MKDistributionCard.kt) — rule 16.
        DistributionChart(distribution)
        DistributionFooter(distribution)
    }
}

// --- Contributeurs (`.lrow`) -------------------------------------------------

/**
 * Carte « Contributeurs » : sélecteur de fenêtre (all-time / 5 / 10) + classement du
 * roster recalculé par fenêtre (part de points + winrate). Rien à afficher si la
 * fenêtre est vide.
 */
@Composable
private fun ContributorsCard(
    byWindow: Map<Int, List<StatsFullViewModel.Contributor>>,
    selectors: SectionSelectors
) {
    val contributors = byWindow[selectors.contributorsWindowIndex].orEmpty()
    StatCard(title = stringResource(R.string.stats_contributors_title)) {
        WindowSelector(selectors.contributorsWindowIndex, selectors.onContributorsWindowChange)
        when {
            contributors.isEmpty() -> MKText(text = stringResource(R.string.stats_no_data), textColor = Colors.white66, fontSize = 12)
            else -> contributors.forEachIndexed { index, contributor -> ContributorRow(index + 1, contributor) }
        }
    }
}

@Composable
private fun ContributorRow(rank: Int, contributor: StatsFullViewModel.Contributor) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val rankColor = when (rank) {
            1 -> Colors.yellow
            2 -> Colors.white70
            3 -> Colors.purple
            else -> Colors.white55
        }
        MKText(text = rank.toString(), font = Fonts.Urbanist, textColor = rankColor, fontSize = 14, modifier = Modifier.width(18.dp))
        // Médaillon joueur mutualisé (#50 pt.4) : photo si dispo, initiales sinon/pendant le chargement.
        PlayerMedallion(
            initials = initialsOf(contributor.player.name),
            avatarColor = playerAvatarColor(contributor.player.id),
            avatarPath = contributor.player.avatar,
            size = 34.dp,
            initialsFontSize = 12
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MKText(text = contributor.player.name, font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 14, textAlign = TextAlign.Start, maxLines = 1)
                if (contributor.isMe) {
                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Colors.yellow).padding(horizontal = 5.dp, vertical = 1.dp)) {
                        MKText(text = stringResource(R.string.stats_me_tag), font = Fonts.NunitoBD, textColor = Colors.black, fontSize = 8)
                    }
                }
            }
            MKText(text = stringResource(R.string.stats_points_share, contributor.pointsShare), textColor = Colors.white66, fontSize = 11, textAlign = TextAlign.Start)
        }
        Column(horizontalAlignment = Alignment.End) {
            MKText(text = "${contributor.winrate}%", font = Fonts.Urbanist, textColor = Colors.white, fontSize = 14)
            MKText(text = stringResource(R.string.form_winrate).lowercase(), textColor = Colors.white55, fontSize = 9)
        }
    }
}
