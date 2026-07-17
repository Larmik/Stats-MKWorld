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
import fr.harmoniamk.statsmkworld.extension.positionColor
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.screen.stats.StatsType
import fr.harmoniamk.statsmkworld.screen.stats.ranking.RankingItem
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.cells.MapCell
import fr.harmoniamk.statsmkworld.ui.cells.TeamCell
import fr.harmoniamk.statsmkworld.ui.stats.MKAdvancedStatsCell

private val CardRadius = RoundedCornerShape(6.dp)

/** États hissés des sélecteurs de section (fenêtre Indicateurs, tri podiums). */
private class SectionSelectors(
    val windowIndex: Int,
    val onWindowChange: (Int) -> Unit,
    val trackSortIndex: Int,
    val onTrackSortChange: (Int) -> Unit,
    val opponentSortIndex: Int,
    val onOpponentSortChange: (Int) -> Unit
)

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
    // Fenêtre des Indicateurs : 0 = all-time, 1 = 5 dernières, 2 = 10 dernières.
    var windowIndex by rememberSaveable { mutableIntStateOf(0) }
    // Critère des podiums circuits / adversaires : 0 = winrate, 1 = score.
    var trackSortIndex by rememberSaveable { mutableIntStateOf(0) }
    var opponentSortIndex by rememberSaveable { mutableIntStateOf(0) }

    val subtitle = when (viewModel.showTabs) {
        true -> null
        else -> state.value.playerName
    }
    val selectors = SectionSelectors(
        windowIndex = windowIndex,
        onWindowChange = { windowIndex = it },
        trackSortIndex = trackSortIndex,
        onTrackSortChange = { trackSortIndex = it },
        opponentSortIndex = opponentSortIndex,
        onOpponentSortChange = { opponentSortIndex = it }
    )
    BaseScreen(
        title = stringResource(R.string.statistiques),
        subtitle = subtitle,
        modifier = Modifier.padding(bottom = if (viewModel.showTabs) 90.dp else 0.dp)
    ) {
        when {
            state.value.loading -> CircularProgressIndicator()
            else -> {
                // Toggle 12 j / 24 j.
                MKSegmentedSelector(
                    items = listOf(stringResource(R.string.mode_12j), stringResource(R.string.mode_24j)),
                    page = if (state.value.is24p) 1 else 0,
                    onClick = viewModel::onWarTypeSwitch
                )
                Spacer(Modifier.height(11.dp))
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
    val playerType = StatsType.PlayerStats(userId = state.targetUserId.orEmpty(), is24p = state.is24p)
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
    // 3. Indicateurs (tuiles) — vue JOUEUR : score = points/war, position, amplitude…
    item { IndicatorsCard(stats = stats, isPlayer = true, selectors = selectors) }
    // 4. Contribution.
    stats.playerContribution?.let { contribution ->
        item {
            StatCard(stringResource(R.string.stats_contribution_title)) {
                IconLine(
                    icon = R.drawable.stats,
                    accent = Colors.yellow,
                    title = stringResource(R.string.stats_contribution_value, contribution),
                    subtitle = state.contributors
                        .indexOfFirst { it.isMe }
                        .takeIf { it >= 0 }
                        ?.let { stringResource(R.string.stats_contribution_rank, it + 1) }
                        ?: ""
                )
            }
        }
    }
    // 5. Forme & séries + Records (restylés en tuiles, cf. point 8 du ticket #36).
    item { FormStreakCard(stats, stringResource(R.string.stats_player_form_title)) }
    item { RecordsTilesCard(stats) }
    // 6. Distribution des positions (barres ancrées en bas, labels alignés).
    stats.positionDistribution.takeIf { it.any { entry -> entry.second > 0 } }?.let { distribution ->
        item {
            StatCard(stringResource(R.string.stats_distribution_title)) {
                DistributionChart(distribution)
                DistributionFooter(stats)
            }
        }
    }
    // 7. Rythme de war (position moyenne du joueur, 1ʳᵉ vs 2ᵉ moitié).
    if (stats.firstHalfAvgPosition != null && stats.secondHalfAvgPosition != null) item {
        StatCard(stringResource(R.string.stats_pace_title)) {
            PaceRow(stats.firstHalfAvgPosition.toString(), stats.secondHalfAvgPosition.toString())
        }
    }
    // 8. Comparatif 12/24 (vue joueur : pts/war).
    item {
        ComparisonCard(
            currentIs24p = state.is24p,
            currentWinrate = stats.allTimeForm?.winrate,
            currentScore = stats.averagePoints,
            otherWinrate = state.playerOtherMode?.winrate,
            otherScore = state.playerOtherMode?.averageScore,
            scoreLabel = stringResource(R.string.stats_points_per_war_short)
        )
    }
    // 9. Podium circuits (Top3/Flop3 + sélecteur winrate/score), perspective joueur.
    item {
        MapsPodiumCard(stats = stats, selectors = selectors, userId = state.targetUserId, is24p = state.is24p)
    }
    // 10. Podium adversaires (Top3/Flop3 + sélecteur), perspective joueur.
    item {
        OpponentsPodiumCard(
            selectors = selectors,
            topByWinrate = state.playerTopOpponentsByWinrate,
            flopByWinrate = state.playerFlopOpponentsByWinrate,
            topByScore = state.playerTopOpponentsByScore,
            flopByScore = state.playerFlopOpponentsByScore,
            userId = state.targetUserId
        )
    }
    // Indicateurs résiduels non couverts par les tuiles (invaincu, pts pénalités) —
    // section accordéon, aucune perte vs l'ancien StatsScreen.
    item { MKAdvancedStatsCell(stats = stats, type = playerType) }
}

// =====================================================================
// Onglet Équipe
// =====================================================================

private fun androidx.compose.foundation.lazy.LazyListScope.teamSections(
    state: StatsFullViewModel.State,
    selectors: SectionSelectors
) {
    val stats = state.teamStats ?: return
    val teamType = StatsType.TeamStats(is24p = state.is24p)
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
    // 3. Indicateurs équipe (tuiles) — vue ÉQUIPE : score = points d'équipe/war,
    //    score moyen/manche, maps gagnées, marges d'équipe… (fenêtre all/5/10).
    item { IndicatorsCard(stats = stats, isPlayer = false, selectors = selectors) }
    // 4. Forme & séries équipe + Records (restylés en tuiles).
    item { FormStreakCard(stats, stringResource(R.string.stats_team_form_title)) }
    item { RecordsTilesCard(stats) }
    // 5. Contributeurs.
    if (state.contributors.isNotEmpty()) item {
        StatCard(stringResource(R.string.stats_contributors_title)) {
            state.contributors.forEachIndexed { index, contributor ->
                ContributorRow(index + 1, contributor)
            }
        }
    }
    // 6. Comparatif 12/24 (vue équipe : score).
    item {
        ComparisonCard(
            currentIs24p = state.is24p,
            currentWinrate = stats.allTimeForm?.winrate,
            currentScore = stats.averagePoints,
            otherWinrate = state.teamOtherMode?.winrate,
            otherScore = state.teamOtherMode?.averageScore,
            scoreLabel = stringResource(R.string.form_score)
        )
    }
    // 7. Podium circuits équipe (Top3/Flop3 + sélecteur winrate/score).
    item {
        MapsPodiumCard(stats = stats, selectors = selectors, userId = null, is24p = state.is24p)
    }
    // 8. Podium adversaires équipe (Top3/Flop3 + sélecteur).
    item {
        OpponentsPodiumCard(
            selectors = selectors,
            topByWinrate = state.topOpponentsByWinrate,
            flopByWinrate = state.flopOpponentsByWinrate,
            topByScore = state.topOpponentsByScore,
            flopByScore = state.flopOpponentsByScore,
            userId = null
        )
    }
    // Indicateurs résiduels (invaincu, pts pénalités) — accordéon, aucune perte.
    item { MKAdvancedStatsCell(stats = stats, type = teamType) }
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
    val window = when (selectors.windowIndex) {
        1 -> stats.recentForm5
        2 -> stats.recentForm10
        else -> stats.allTimeForm
    }
    // Deltas seulement hors all-time (index 0 = pas de comparaison).
    val showDelta = selectors.windowIndex != 0
    val title = if (isPlayer) stringResource(R.string.stats_player_indicators) else stringResource(R.string.stats_team_details)
    StatCard(title) {
        MKSegmentedSelector(
            items = listOf(
                stringResource(R.string.all_time),
                stringResource(R.string.last_n_short, 5),
                stringResource(R.string.last_n_short, 10)
            ),
            page = selectors.windowIndex,
            onDark = true,
            onClick = selectors.onWindowChange
        )
        Spacer(Modifier.height(11.dp))
        val tiles = buildList {
            add(MetricTile(stringResource(R.string.form_winrate), window?.winrate?.let { "$it%" } ?: "-", if (showDelta) window?.winrateDelta else null, "%", DeltaPolarity.HIGHER))
            when (isPlayer) {
                true -> {
                    add(MetricTile(stringResource(R.string.stats_points_per_war), window?.averageScore?.toString() ?: "-", if (showDelta) window?.scoreDelta else null, "", DeltaPolarity.HIGHER))
                    add(MetricTile(stringResource(R.string.average_position_short), window?.averagePosition?.toString() ?: "-", if (showDelta) window?.positionDelta else null, "", DeltaPolarity.LOWER))
                }
                else -> {
                    add(MetricTile(stringResource(R.string.form_score), window?.averageScore?.toString() ?: "-", if (showDelta) window?.scoreDelta else null, "", DeltaPolarity.HIGHER))
                    add(MetricTile(stringResource(R.string.average_map_score_short), window?.averageMapScore?.toString() ?: "-", if (showDelta) window?.mapScoreDelta else null, "", DeltaPolarity.HIGHER))
                }
            }
            add(MetricTile(stringResource(R.string.maps_gagn_es), window?.mapsWonPercent?.let { "$it%" } ?: "-", if (showDelta) window?.mapsWonDelta else null, "%", DeltaPolarity.HIGHER))
            add(MetricTile(stringResource(R.string.stats_regularity), window?.scoreStdDev?.let { "±$it" } ?: "-", null, "", DeltaPolarity.NONE))
            add(MetricTile(stringResource(R.string.score_amplitude), amplitude(window), null, "", DeltaPolarity.NONE))
            add(MetricTile(stringResource(R.string.avg_win_margin), window?.winMargin?.let { "+$it" } ?: "-", null, "", DeltaPolarity.NONE))
            add(MetricTile(stringResource(R.string.avg_loss_margin), window?.lossMargin?.let { "-$it" } ?: "-", null, "", DeltaPolarity.NONE))
            add(MetricTile(stringResource(R.string.shocks_per_war_short), window?.shocksPerWar?.let { String.format(java.util.Locale.getDefault(), "%.1f", it) } ?: "-", null, "", DeltaPolarity.NONE))
        }
        MetricTiles(tiles)
    }
}

/** Amplitude min–max des scores de la fenêtre (« min – max »), « - » si absente. */
private fun amplitude(window: fr.harmoniamk.statsmkworld.model.local.FormStats?): String {
    val min = window?.scoreMin
    val max = window?.scoreMax
    return if (min != null && max != null) "$min – $max" else "-"
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

/** Grille 3-colonnes de tuiles d'indicateur (valeur blanche + delta coloré). */
@Composable
private fun ColumnScope.MetricTiles(tiles: List<MetricTile>) {
    tiles.chunked(3).forEach { rowTiles ->
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rowTiles.forEach { tile -> MetricTileCell(tile) }
            repeat(3 - rowTiles.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun RowScope.MetricTileCell(tile: MetricTile) {
    Column(Modifier.weight(1f).background(Colors.white30, CardRadius).padding(10.dp)) {
        // Valeur : toujours BLANCHE (point 4 du ticket #36 : aucune couleur).
        MKText(text = tile.value, font = Fonts.Urbanist, textColor = Colors.white, fontSize = 18, textAlign = TextAlign.Start, maxLines = 1)
        // Delta coloré (uniquement, cf. Momentum) : ↗ vert / ↘ rouge selon la polarité.
        tile.delta?.takeIf { it != 0 && tile.polarity != DeltaPolarity.NONE }?.let { delta ->
            val improved = if (tile.polarity == DeltaPolarity.LOWER) delta < 0 else delta > 0
            val arrow = if (delta > 0) "↗" else "↘"
            MKText(
                text = "${if (delta > 0) "+" else ""}$delta${tile.deltaSuffix} $arrow",
                font = Fonts.NunitoBD,
                textColor = if (improved) Colors.green else Colors.red,
                fontSize = 10,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        MKText(text = tile.label, textColor = Colors.white70, fontSize = 10, textAlign = TextAlign.Start, modifier = Modifier.padding(top = 6.dp))
    }
}

// =====================================================================
// Section Records & séries (restylée en tuiles) — ticket #36 point 8
// =====================================================================

/** Records & séries en tuiles (même vocabulaire visuel que les Indicateurs). */
@Composable
private fun RecordsTilesCard(stats: Stats) {
    StatCard(stringResource(R.string.records_series)) {
        val tiles = buildList {
            add(MetricTile(stringResource(R.string.current_streak), streakValue(stats.currentStreak), null, "", DeltaPolarity.NONE))
            add(MetricTile(stringResource(R.string.best_win_streak), stats.bestWinStreak.toString(), null, "", DeltaPolarity.NONE))
            add(MetricTile(stringResource(R.string.worst_loss_streak), stats.worstLossStreak.toString(), null, "", DeltaPolarity.NONE))
            if (stats.top6Count > 0) add(MetricTile(stringResource(R.string.top6_count), stats.top6Count.toString(), null, "", DeltaPolarity.NONE))
            if (stats.bot6Count > 0) add(MetricTile(stringResource(R.string.bot6_count), stats.bot6Count.toString(), null, "", DeltaPolarity.NONE))
            if (stats.unbeatenStreak > 0) add(MetricTile(stringResource(R.string.unbeaten_streak), stats.unbeatenStreak.toString(), null, "", DeltaPolarity.NONE))
        }
        MetricTiles(tiles)
    }
}

/** Série en cours signée → « N V » / « N D » / « — ». */
private fun streakValue(streak: Int): String = when {
    streak > 0 -> "$streak V"
    streak < 0 -> "${-streak} D"
    else -> "—"
}

// =====================================================================
// Podiums circuits & adversaires (Top3/Flop3 + sélecteur) — ticket #36
// =====================================================================

/**
 * Podium circuits : sélecteur **winrate / score**, puis Top 3 / Flop 3 rendus avec
 * la **cellule circuit historique** (`MapCell` : illustration + infos). Perspective
 * joueur ([userId] non-null) ou équipe (null) — `MapCell` adapte déjà le score.
 */
@Composable
private fun MapsPodiumCard(stats: Stats, selectors: SectionSelectors, userId: String?, is24p: Boolean) {
    val byWinrate = selectors.trackSortIndex == 0
    val top = if (byWinrate) stats.topMapsByWinrate else stats.topMapsByScore
    val flop = if (byWinrate) stats.flopMapsByWinrate else stats.flopMapsByScore
    if (top.isEmpty() && flop.isEmpty()) return
    StatCard(stringResource(R.string.best_maps_section)) {
        SortSelector(selectors.trackSortIndex, selectors.onTrackSortChange)
        Spacer(Modifier.height(11.dp))
        PodiumLabel(stringResource(R.string.stats_podium_top))
        top.forEach { track ->
            MapCell(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                track = null, userId = userId, is24p = is24p,
                trackRanking = RankingItem.TrackRanking(track), onClick = {}
            )
        }
        Spacer(Modifier.height(8.dp))
        PodiumLabel(stringResource(R.string.stats_podium_flop))
        flop.forEach { track ->
            MapCell(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                track = null, userId = userId, is24p = is24p,
                trackRanking = RankingItem.TrackRanking(track), onClick = {}
            )
        }
    }
}

/**
 * Podium adversaires : sélecteur **winrate / score**, Top 3 / Flop 3 rendus avec la
 * **cellule adversaire historique** (`TeamCell`). [userId] non-null ⇒ score du joueur
 * (perspective Individuelles) ; null ⇒ écart d'équipe (perspective Équipe).
 */
@Composable
private fun OpponentsPodiumCard(
    selectors: SectionSelectors,
    topByWinrate: List<RankingItem.OpponentRanking>,
    flopByWinrate: List<RankingItem.OpponentRanking>,
    topByScore: List<RankingItem.OpponentRanking>,
    flopByScore: List<RankingItem.OpponentRanking>,
    userId: String?
) {
    val byWinrate = selectors.opponentSortIndex == 0
    val top = if (byWinrate) topByWinrate else topByScore
    val flop = if (byWinrate) flopByWinrate else flopByScore
    if (top.isEmpty() && flop.isEmpty()) return
    StatCard(stringResource(R.string.best_opponents_section)) {
        SortSelector(selectors.opponentSortIndex, selectors.onOpponentSortChange)
        Spacer(Modifier.height(11.dp))
        PodiumLabel(stringResource(R.string.stats_podium_top))
        top.forEach { opponent ->
            TeamCell(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), team = null, teamRanking = opponent, userId = userId, onClick = {})
        }
        Spacer(Modifier.height(8.dp))
        PodiumLabel(stringResource(R.string.stats_podium_flop))
        flop.forEach { opponent ->
            TeamCell(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), team = null, teamRanking = opponent, userId = userId, onClick = {})
        }
    }
}

/** Sélecteur winrate / score (pill, sur carte sombre). */
@Composable
private fun ColumnScope.SortSelector(index: Int, onChange: (Int) -> Unit) {
    MKSegmentedSelector(
        items = listOf(stringResource(R.string.stats_sort_winrate), stringResource(R.string.stats_sort_score)),
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

/** Carte translucide (fond sombre, bordure blanche, radius 6, padding 13). */
@Composable
private fun StatCard(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Colors.blackAlphaed, CardRadius)
            .border(1.dp, Colors.whiteBorder, CardRadius)
            .padding(13.dp)
    ) {
        title?.let { Eyebrow(it) }
        if (title != null) Spacer(Modifier.height(11.dp))
        content()
    }
}

/** Eyebrow (petit titre majuscule blanc). */
@Composable
private fun Eyebrow(text: String) {
    MKText(
        text = text.uppercase(),
        fontSize = 12,
        font = Fonts.NunitoBD,
        textColor = Colors.white,
        textAlign = TextAlign.Start
    )
}

/**
 * En-tête : vignette (photo joueur / logo équipe) + nom (Bungee) + sous-titre.
 * [logo] = URL MKCentral déjà préfixée (avatar joueur en Individuelles, logo équipe
 * en Équipe) ; fallback = pastille d'initiales sur fond [color].
 */
@Composable
private fun HeaderCard(name: String, subtitle: String, color: Color, logo: String?, isTeam: Boolean) {
    StatCard {
        Row(horizontalArrangement = Arrangement.spacedBy(13.dp), verticalAlignment = Alignment.CenterVertically) {
            when (logo) {
                null -> Box(
                    Modifier.size(52.dp).clip(CircleShape).background(color).border(2.dp, Colors.white85, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Équipe sans logo → default_logo ; joueur sans avatar → initiales.
                    when (isTeam) {
                        true -> Image(
                            painter = painterResource(R.drawable.default_logo),
                            contentDescription = null,
                            modifier = Modifier.size(52.dp).clip(CircleShape)
                        )
                        else -> MKText(text = initialsOf(name), font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 16)
                    }
                }
                else -> AsyncImage(
                    model = logo,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp).clip(CircleShape).border(2.dp, Colors.white85, CircleShape)
                )
            }
            Column(Modifier.weight(1f)) {
                MKText(text = name, font = Fonts.Bungee, textColor = Colors.white, fontSize = 17, textAlign = TextAlign.Start)
                MKText(text = subtitle, textColor = Colors.white66, fontSize = 12, textAlign = TextAlign.Start, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
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

/** Barre horizontale V/N/D proportionnelle (vert / blanc / rouge). */
@Composable
private fun WinTieLossBar(won: Int, tied: Int, loss: Int) {
    val total = (won + tied + loss).takeIf { it > 0 } ?: 1
    Row(
        Modifier.fillMaxWidth().height(13.dp).clip(RoundedCornerShape(20.dp)).background(Color(0x38000000))
    ) {
        if (won > 0) Box(Modifier.weight(won.toFloat() / total).fillMaxHeight().background(Colors.green))
        if (tied > 0) Box(Modifier.weight(tied.toFloat() / total).fillMaxHeight().background(Colors.white))
        if (loss > 0) Box(Modifier.weight(loss.toFloat() / total).fillMaxHeight().background(Colors.red))
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
    StatCard(title) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.25f)).border(1.dp, accent.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(R.drawable.ic_flame), contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                MKText(text = streakText, font = Fonts.NunitoBD, textColor = accent, fontSize = 15, textAlign = TextAlign.Start)
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

// --- Rythme de war (`.pace`) -------------------------------------------------

@Composable
private fun ColumnScope.PaceRow(first: String, second: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        PaceCell(first, stringResource(R.string.stats_pace_first), Colors.green)
        MKText(text = "→", textColor = Colors.white55, font = Fonts.NunitoBD, fontSize = 18)
        PaceCell(second, stringResource(R.string.stats_pace_second), Colors.red)
    }
}

@Composable
private fun RowScope.PaceCell(value: String, label: String, color: Color) {
    Column(Modifier.weight(1f).background(Colors.white30, CardRadius).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        MKText(text = value, font = Fonts.Urbanist, textColor = color, fontSize = 18)
        MKText(text = label, textColor = Colors.white70, fontSize = 10, modifier = Modifier.padding(top = 3.dp))
    }
}

// --- Comparatif 12/24 (`.cmp`) -----------------------------------------------

@Composable
private fun ComparisonCard(
    currentIs24p: Boolean,
    currentWinrate: Int?,
    currentScore: Int?,
    otherWinrate: Int?,
    otherScore: Int?,
    scoreLabel: String
) {
    StatCard(stringResource(R.string.stats_comparison_title)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            // Colonne du mode courant (surlignée) à gauche selon le mode.
            ComparisonColumn(
                mode = if (currentIs24p) stringResource(R.string.mode_24j) else stringResource(R.string.mode_12j),
                winrate = currentWinrate,
                score = currentScore,
                scoreLabel = scoreLabel,
                highlighted = true
            )
            ComparisonColumn(
                mode = if (currentIs24p) stringResource(R.string.mode_12j) else stringResource(R.string.mode_24j),
                winrate = otherWinrate,
                score = otherScore,
                scoreLabel = scoreLabel,
                highlighted = false
            )
        }
    }
}

@Composable
private fun RowScope.ComparisonColumn(mode: String, winrate: Int?, score: Int?, scoreLabel: String, highlighted: Boolean) {
    Column(
        Modifier
            .weight(1f)
            .background(Colors.white30, CardRadius)
            .then(if (highlighted) Modifier.border(1.dp, Colors.yellow, CardRadius) else Modifier)
            .padding(11.dp)
    ) {
        MKText(text = mode, font = Fonts.Urbanist, textColor = Colors.white, fontSize = 13, textAlign = TextAlign.Start)
        ComparisonRow(stringResource(R.string.form_winrate), winrate?.let { "$it %" } ?: "-", if (highlighted) Colors.green else Colors.white)
        ComparisonRow(scoreLabel, score?.toString() ?: "-", Colors.white)
    }
}

@Composable
private fun ComparisonRow(label: String, value: String, valueColor: Color) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        MKText(text = label, textColor = Colors.white66, fontSize = 12)
        MKText(text = value, font = Fonts.NunitoBD, textColor = valueColor, fontSize = 13)
    }
}

// --- Distribution des positions (`.dist`) ------------------------------------

@Composable
private fun ColumnScope.DistributionChart(distribution: List<Pair<Int, Int>>) {
    val max = distribution.maxOf { it.second }.takeIf { it > 0 } ?: 1
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        distribution.forEach { (position, count) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                // Zone des barres à hauteur FIXE (116dp) : les barres poussent depuis
                // une ligne de base commune (align Bottom) → les labels sous les barres
                // s'alignent horizontalement (point 7 du ticket #36).
                Box(Modifier.fillMaxWidth().height(116.dp), contentAlignment = Alignment.BottomCenter) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        MKText(text = count.toString(), font = Fonts.Urbanist, textColor = Colors.white70, fontSize = 8)
                        Spacer(Modifier.height(2.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height((6 + 100 * (count.toFloat() / max)).dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(position.positionColor())
                        )
                    }
                }
                MKText(text = position.toString(), font = Fonts.MKPosition, textColor = Colors.white55, fontSize = 8, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** Pied de la distribution : Top6 / Bot6 (compte + %). */
@Composable
private fun ColumnScope.DistributionFooter(stats: Stats) {
    val total = stats.positionDistribution.sumOf { it.second }.takeIf { it > 0 } ?: 1
    val top6 = stats.positionDistribution.filter { it.first in 1..6 }.sumOf { it.second }
    val bot6 = stats.positionDistribution.filter { it.first in 7..12 }.sumOf { it.second }
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        FooterStat(top6, "Top 6", (top6 * 100) / total, Colors.green)
        FooterStat(bot6, "Bot 6", (bot6 * 100) / total, Colors.red)
    }
}

@Composable
private fun FooterStat(count: Int, label: String, percent: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        MKText(text = count.toString(), font = Fonts.Urbanist, textColor = color, fontSize = 13)
        MKText(text = "$label · $percent %", textColor = Colors.white70, fontSize = 11)
    }
}

// --- Contributeurs (`.lrow`) -------------------------------------------------

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
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(Colors.blue),
            contentAlignment = Alignment.Center
        ) {
            MKText(text = initialsOf(contributor.player.name), font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 12)
        }
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

// --- Helpers -----------------------------------------------------------------

private fun initialsOf(name: String?): String = name
    ?.trim()
    ?.split(" ", "_", "-")
    ?.filter { it.isNotBlank() }
    ?.take(2)
    ?.joinToString("") { it.first().uppercase() }
    ?.takeIf { it.isNotEmpty() }
    ?: "?"
