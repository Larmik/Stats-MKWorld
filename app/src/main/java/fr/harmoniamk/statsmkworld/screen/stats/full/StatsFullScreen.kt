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
import androidx.compose.foundation.layout.IntrinsicSize
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
import fr.harmoniamk.statsmkworld.extension.warScoreToDiff
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.screen.stats.ranking.RankingItem
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKText

private val CardRadius = RoundedCornerShape(6.dp)

/** États hissés des sélecteurs de section (fenêtres Indicateurs & Records, tri podiums). */
private class SectionSelectors(
    val windowIndex: Int,
    val onWindowChange: (Int) -> Unit,
    val recordsWindowIndex: Int,
    val onRecordsWindowChange: (Int) -> Unit,
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
        recordsWindowIndex = recordsWindowIndex,
        onRecordsWindowChange = { recordsWindowIndex = it },
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
    // 5. Forme & séries + Records (tuiles : série en cours, amplitude, Top6/Bot6, invaincu).
    item { FormStreakCard(stats, stringResource(R.string.stats_player_form_title)) }
    item { RecordsTilesCard(stats, selectors) }
    // 6. Distribution des positions (barres ancrées en bas, labels alignés).
    stats.positionDistribution.takeIf { it.any { entry -> entry.second > 0 } }?.let { distribution ->
        item {
            StatCard(stringResource(R.string.stats_distribution_title)) {
                DistributionChart(distribution)
                DistributionFooter(stats)
            }
        }
    }
    // 7. Podium circuits (Top3 / Flop3 sur une ligne + sélecteur winrate/score).
    item {
        MapsPodiumCard(stats = stats, selectors = selectors, userId = state.targetUserId)
    }
    // 8. Podium adversaires (Top3 / Flop3 sur une ligne + sélecteur), perspective joueur.
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
    // 4. Forme & séries équipe + Records (tuiles).
    item { FormStreakCard(stats, stringResource(R.string.stats_team_form_title)) }
    item { RecordsTilesCard(stats, selectors) }
    // 5. Contributeurs.
    if (state.contributors.isNotEmpty()) item {
        StatCard(stringResource(R.string.stats_contributors_title)) {
            state.contributors.forEachIndexed { index, contributor ->
                ContributorRow(index + 1, contributor)
            }
        }
    }
    // 6. Podium circuits équipe (Top3 / Flop3 sur une ligne + sélecteur winrate/score).
    item {
        MapsPodiumCard(stats = stats, selectors = selectors, userId = null)
    }
    // 7. Podium adversaires équipe (Top3 / Flop3 sur une ligne + sélecteur).
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
            // Pénalités (points perdus, cumul all-time — remplace l'amplitude, déplacée
            // vers Records & séries). N'apparaît que si > 0.
            add(MetricTile(stringResource(R.string.penalty_points_lost), stats.penaltyPointsLost.takeIf { it > 0 }?.let { "-$it" } ?: "-", null, "", DeltaPolarity.NONE))
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

// Hauteur RÉSERVÉE pour la ligne de progression (delta %) : occupée même quand le
// delta est absent → la tuile ne change JAMAIS de taille selon la fenêtre choisie
// (ticket #36 point 1 : plus de « saut » de layout). Cale toutes les tuiles sur la
// hauteur de la plus grande (celle avec delta).
private val DeltaSlotHeight = 15.dp

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
        MKText(text = tile.label, textColor = Colors.white70, fontSize = 10, textAlign = TextAlign.Start, modifier = Modifier.padding(top = 6.dp))
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
    StatCard(stringResource(R.string.records_series)) {
        MKSegmentedSelector(
            items = listOf(
                stringResource(R.string.all_time),
                stringResource(R.string.last_n_short, 5),
                stringResource(R.string.last_n_short, 10)
            ),
            page = selectors.recordsWindowIndex,
            onDark = true,
            onClick = selectors.onRecordsWindowChange
        )
        Spacer(Modifier.height(11.dp))
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

/** Données d'affichage compactes d'une entrée de podium (circuit OU adversaire). */
private class PodiumEntry(
    val labelRes: Int? = null,     // circuit : @StringRes du nom de map
    val name: String? = null,      // adversaire : nom (roster > équipe)
    val pictureRes: Int? = null,   // circuit : illustration @DrawableRes
    val logo: String? = null,      // adversaire : chemin logo MKCentral
    val value: String              // valeur (winrate % ou score) selon le tri
)

/**
 * Podium circuits : sélecteur **winrate / score**, Top 3 / Flop 3 chacun sur **une
 * ligne** de 3 `PodiumCell` compactes (illustration + nom + valeur). Perspective
 * joueur ([userId] non-null : score du joueur) ou équipe (null : score d'équipe).
 */
@Composable
private fun MapsPodiumCard(stats: Stats, selectors: SectionSelectors, userId: String?) {
    val byWinrate = selectors.trackSortIndex == 0
    val top = if (byWinrate) stats.topMapsByWinrate else stats.topMapsByScore
    val flop = if (byWinrate) stats.flopMapsByWinrate else stats.flopMapsByScore
    if (top.isEmpty() && flop.isEmpty()) return
    val toEntry: (fr.harmoniamk.statsmkworld.model.local.TrackStats) -> PodiumEntry = { track ->
        val map = track.map?.firstOrNull()
        val value = when {
            byWinrate -> "${track.winRate ?: 0}%"
            userId != null -> (track.playerScore ?: 0).toString()
            else -> (track.teamScore ?: 0).toString()
        }
        PodiumEntry(labelRes = map?.label, pictureRes = map?.picture, value = value)
    }
    StatCard(stringResource(R.string.best_maps_section)) {
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
 * Podium adversaires : sélecteur **winrate / score**, Top 3 / Flop 3 chacun sur **une
 * ligne** de 3 `PodiumCell` compactes (logo + nom + valeur). [userId] non-null ⇒ score
 * du joueur (Individuelles) ; null ⇒ écart d'équipe (Équipe).
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
    val toEntry: (RankingItem.OpponentRanking) -> PodiumEntry = { opponent ->
        val value = when {
            byWinrate -> "${opponent.winrate}%"
            userId != null -> opponent.stats.averagePoints.toString() // score du joueur
            else -> opponent.stats.averagePointsLabel               // écart d'équipe
        }
        PodiumEntry(name = opponent.team.name, logo = opponent.team.logo, value = value)
    }
    StatCard(stringResource(R.string.best_opponents_section)) {
        SortSelector(selectors.opponentSortIndex, selectors.onOpponentSortChange)
        Spacer(Modifier.height(11.dp))
        PodiumLabel(stringResource(R.string.stats_podium_top))
        PodiumRow(top.map(toEntry))
        Spacer(Modifier.height(8.dp))
        PodiumLabel(stringResource(R.string.stats_podium_flop))
        PodiumRow(flop.map(toEntry))
    }
}

/**
 * Une ligne de podium : jusqu'à 3 `PodiumCell` à poids égal, hauteur uniforme
 * (`IntrinsicSize.Min`). Comble avec des `Spacer` si moins de 3 entrées.
 */
@Composable
private fun ColumnScope.PodiumRow(entries: List<PodiumEntry>) {
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        entries.forEach { entry -> PodiumCell(entry) }
        repeat(3 - entries.size) { Spacer(Modifier.weight(1f)) }
    }
}

/**
 * Cellule podium compacte, **structure identique circuit/adversaire** : image en haut
 * (illustration de map arrondie OU logo d'équipe en cercle, fallback `default_logo`),
 * nom au centre (2 lignes max), valeur (winrate/score) en gras dessous.
 */
@Composable
private fun RowScope.PodiumCell(entry: PodiumEntry) {
    Column(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(Colors.white30, CardRadius)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            entry.pictureRes != null -> Image(
                painter = painterResource(entry.pictureRes),
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(5.dp))
            )
            else -> when (entry.logo) {
                null -> Image(
                    painter = painterResource(R.drawable.default_logo),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                )
                else -> AsyncImage(
                    model = "https://mkcentral.com${entry.logo}",
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        MKText(
            text = entry.labelRes?.let { stringResource(it) } ?: entry.name ?: "-",
            font = Fonts.NunitoBD,
            textColor = Colors.white,
            fontSize = 11,
            maxLines = 2,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        MKText(text = entry.value, font = Fonts.Urbanist, textColor = Colors.white, fontSize = 14)
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
