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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.extension.displayName
import fr.harmoniamk.statsmkworld.extension.pointsToPosition
import fr.harmoniamk.statsmkworld.extension.positionColor
import fr.harmoniamk.statsmkworld.extension.trackScoreToDiff
import fr.harmoniamk.statsmkworld.extension.warScoreToDiff
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.screen.stats.ranking.RankingItem
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKSeasonDropdown
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.stats.DistributionChart
import fr.harmoniamk.statsmkworld.ui.stats.DistributionFooter
import fr.harmoniamk.statsmkworld.ui.stats.Eyebrow
import fr.harmoniamk.statsmkworld.ui.stats.MKStatInfoButton
import fr.harmoniamk.statsmkworld.ui.stats.PodiumEntry
import fr.harmoniamk.statsmkworld.ui.stats.PodiumRow
import fr.harmoniamk.statsmkworld.ui.stats.StatCard
import fr.harmoniamk.statsmkworld.ui.stats.StatCardRadius
import fr.harmoniamk.statsmkworld.ui.stats.StatHeaderCard
import fr.harmoniamk.statsmkworld.ui.stats.TopBottomColumns
import fr.harmoniamk.statsmkworld.ui.stats.hasDisplayableTopBottom
import fr.harmoniamk.statsmkworld.ui.stats.WinTieLossBar
import fr.harmoniamk.statsmkworld.ui.cells.PlayerMedallion
import fr.harmoniamk.statsmkworld.ui.cells.playerAvatarColor
import fr.harmoniamk.statsmkworld.ui.stats.initialsOf

private val CardRadius = StatCardRadius

/**
 * États hissés des sélecteurs (#68). Période GLOBALE à l'écran ([windowIndex], lue par
 * toutes les sections) ; tri des podiums circuits/adversaires propre aux sections (axe
 * indépendant de la période).
 */
private class SectionSelectors(
    // Période globale : 0 = all-time, 1 = 5 dernières, 2 = 10 dernières.
    val windowIndex: Int,
    // Tri podiums : 0 = occurrences (défaut), 1 = winrate, 2 = score.
    val trackSortIndex: Int,
    val onTrackSortChange: (Int) -> Unit,
    val opponentSortIndex: Int,
    val onOpponentSortChange: (Int) -> Unit
)

/**
 * FormStats de la fenêtre (0 = all-time, 1 = 5, 2 = 10) pour Indicateurs/Records — sélectionne
 * la `FormStats` correspondante afin de conserver les deltas vs all-time (index 0 → pas de delta).
 */
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
    onResults: (() -> Unit)? = null,
    // « Classement entier » Circuits / Adversaires (#67) : `isTeam` remonte la portée courante
    // (false = joueur, true = équipe) pour naviguer vers le bon classement. Masqués si null.
    onMapsSeeAll: ((isTeam: Boolean) -> Unit)? = null,
    onOpponentsSeeAll: ((isTeam: Boolean) -> Unit)? = null
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    // 0 = Individuelles, 1 = Équipe. Sur statsfull (pas d'onglets) → toujours 0.
    var scopeIndex by rememberSaveable { mutableIntStateOf(0) }
    // Période globale (#68) : 0 = all-time, 1 = 5, 2 = 10. Un seul état lu par toutes les
    // sections (rule 11 : State, pas de re-nav). Survit à la rotation.
    var windowIndex by rememberSaveable { mutableIntStateOf(0) }
    // Tri podiums : 0 = occurrences (défaut), 1 = winrate, 2 = score. Axe indépendant de la période.
    var trackSortIndex by rememberSaveable { mutableIntStateOf(0) }
    var opponentSortIndex by rememberSaveable { mutableIntStateOf(0) }

    val subtitle = when (viewModel.showTabs) {
        true -> null
        else -> state.value.playerName
    }
    val selectors = SectionSelectors(
        windowIndex = windowIndex,
        trackSortIndex = trackSortIndex,
        onTrackSortChange = { trackSortIndex = it },
        opponentSortIndex = opponentSortIndex,
        onOpponentSortChange = { opponentSortIndex = it }
    )
    BaseScreen(
        title = stringResource(R.string.statistiques),
        subtitle = subtitle,
        // Retour d'appbar seulement en fiche poussée (showTabs=false) ; pas en onglet (rule 14).
        onBack = onBack?.takeIf { !viewModel.showTabs },
        // Sélecteur de saison (#70, MKSeasonDropdown partagé rule 16). Change l'état VM ⇒
        // recalcul à la volée des agrégats (rule 11, pas de re-nav).
        headerTrailing = {
            MKSeasonDropdown(
                seasons = state.value.seasons,
                selectedSeasonNumber = state.value.selectedSeasonNumber,
                onSeasonSelected = viewModel::onSeasonSelected
            )
        },
        modifier = Modifier.padding(bottom = if (viewModel.showTabs) 90.dp else 0.dp)
    ) {
        // Le header reste toujours visible ; seule la zone de données passe en chargement au
        // recompute (#73). Sélecteur 12j/24j retiré temporairement (#37, is24p figé false côté VM).
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
        // Sélecteur de période global (#68), au-dessus de toutes les sections. onDark = false
        // (fond clair de BaseScreen). Change l'état ⇒ recompose les sections (rule 11).
        MKSegmentedSelector(
            items = listOf(
                stringResource(R.string.all_time),
                stringResource(R.string.last_n_short, 5),
                stringResource(R.string.last_n_short, 10)
            ),
            page = windowIndex,
            onClick = { windowIndex = it }
        )
        Spacer(Modifier.height(11.dp))
        when {
            // Chargement circonscrit à la zone de données ; le header ci-dessus reste affiché.
            state.value.loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                when (scopeIndex) {
                    1 -> teamSections(
                        state.value, selectors,
                        onMapsSeeAll = onMapsSeeAll?.let { cb -> { cb(true) } },
                        onOpponentsSeeAll = onOpponentsSeeAll?.let { cb -> { cb(true) } }
                    )
                    else -> individualSections(
                        state.value, viewModel.showTabs, onResults, selectors,
                        onMapsSeeAll = onMapsSeeAll?.let { cb -> { cb(false) } },
                        onOpponentsSeeAll = onOpponentsSeeAll?.let { cb -> { cb(false) } }
                    )
                }
            }
        }
    }
}

// Onglet Individuelles (et statsfull)
private fun androidx.compose.foundation.lazy.LazyListScope.individualSections(
    state: StatsFullViewModel.State,
    showTabs: Boolean,
    onResults: (() -> Unit)?,
    selectors: SectionSelectors,
    onMapsSeeAll: (() -> Unit)?,
    onOpponentsSeeAll: (() -> Unit)?
) {
    // Stats all-time (index 0) : source des sections FormStats (deltas vs all-time). Les
    // fenêtrer casserait les deltas déjà pré-calculés. Écran vide si aucune stat.
    val allTimeStats = state.playerStatsByWindow[0] ?: return
    // Stats de la fenêtre globale (#68) pour les sections recalculées (Bilan, Contribution,
    // Distribution, Podiums…). Fallback all-time si vide.
    val stats = state.playerStatsByWindow[selectors.windowIndex] ?: allTimeStats
    // 1. En-tête (onglet seulement, statsfull a déjà le sous-titre).
    if (showTabs) item {
        HeaderCard(
            name = state.playerName.orEmpty(),
            subtitle = stringResource(R.string.stats_player_subtitle, stats.warStats.warsPlayed),
            color = Colors.blue,
            logo = state.playerLogo,
            isTeam = false
        )
    }
    // 2. Bilan (#65 : lien « Résultats → » toujours visible → historique filtré sur le joueur).
    item { BalanceCard(stats, showResultsLink = true, onResults = onResults) }
    // 3. Indicateurs — vue joueur (points/war, position, marges, pénalités, maps, shocks…).
    item {
        IndicatorsCard(
            stats = allTimeStats,
            isPlayer = true,
            selectors = selectors,
            participationByWindow = state.participationRateByWindow
        )
    }
    // 4. Contribution (#69) : part de POINTS + part de SHOCKS, lues depuis le classement
    //    Contributeurs/Baggeurs de la même fenêtre (source unique → % identique au classement
    //    équipe). Ratios total/total. Rang = position de la ligne `isMe`.
    run {
        val contributors = state.contributorsByWindow[selectors.windowIndex].orEmpty()
        val baggers = state.baggersByWindow[selectors.windowIndex].orEmpty()
        val meContributorRank = contributors.indexOfFirst { it.isMe }.takeIf { it >= 0 }
        val meBaggerRank = baggers.indexOfFirst { it.isMe }.takeIf { it >= 0 }
        val me = contributors.firstOrNull { it.isMe }
        me?.let { contributor ->
            item {
                StatCard(title = stringResource(R.string.stats_contribution_title)) {
                    IconLine(
                        // Champignon = part de POINTS (#91 pt.11). tinted = false → Image (couleurs
                        // d'origine). iconSize 34dp compense le padding transparent du PNG (< 44dp médaillon).
                        icon = R.drawable.ic_mushroom,
                        accent = Colors.yellow,
                        tinted = false,
                        iconSize = 34.dp,
                        title = stringResource(R.string.stats_contribution_value, contributor.pointsShare),
                        subtitle = meContributorRank
                            ?.let { stringResource(R.string.stats_contribution_rank, it + 1) }
                            ?: ""
                    )
                    // Part de shocks (comme « Baggeurs »), masquée si aucun shock sur la fenêtre.
                    if (contributor.shockShare > 0) {
                        Spacer(Modifier.height(11.dp))
                        IconLine(
                            icon = R.drawable.shock,
                            accent = Colors.yellow,
                            title = stringResource(R.string.stats_bag_contribution_value, contributor.shockShare),
                            subtitle = meBaggerRank
                                ?.let { stringResource(R.string.stats_bag_contribution_rank, it + 1) }
                                ?: ""
                        )
                    }
                }
            }
        }
    }
    // 5. Forme & séries (série = all-time) + Records (grille 3×2, deltas vs all-time).
    item { FormStreakCard(allTimeStats, stringResource(R.string.stats_player_form_title)) }
    // Vue joueur : min/max = score perso brut (pas de diff — #67 ne concerne que l'équipe).
    item { RecordsTilesCard(allTimeStats, selectors, isTeam = false) }
    // 6. Distribution des positions (fenêtre globale).
    item { DistributionCard(stats, selectors) }
    // 7. Podium circuits (Top3 / Flop3 + tri).
    item { MapsPodiumCard(stats = stats, selectors = selectors, userId = state.targetUserId, onSeeAll = onMapsSeeAll) }
    // 8. Podium adversaires (perspective joueur).
    item {
        OpponentsPodiumCard(
            selectors = selectors,
            podiums = state.playerOpponentsByWindow[selectors.windowIndex] ?: StatsFullViewModel.OpponentPodiums(),
            userId = state.targetUserId,
            onSeeAll = onOpponentsSeeAll
        )
    }
}

// Onglet Équipe
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
private fun androidx.compose.foundation.lazy.LazyListScope.teamSections(
    state: StatsFullViewModel.State,
    selectors: SectionSelectors,
    onMapsSeeAll: (() -> Unit)?,
    onOpponentsSeeAll: (() -> Unit)?
) {
    // Stats d'équipe all-time (index 0) : source des sections FormStats (deltas vs all-time).
    // Écran vide si aucune stat.
    val allTimeStats = state.teamStatsByWindow[0] ?: return
    // Stats de la fenêtre globale (#68) pour les sections recalculées (Bilan, Tops/Bots,
    // Contributeurs, Podiums…). Fallback all-time si vide.
    val stats = state.teamStatsByWindow[selectors.windowIndex] ?: allTimeStats
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
    // 3. Détails équipe — score = écart de points (warScoreToDiff), score/manche, marges, pénalités…
    item { IndicatorsCard(stats = allTimeStats, isPlayer = false, selectors = selectors) }
    // 4. Forme & séries équipe (série = all-time) + Records.
    item { FormStreakCard(allTimeStats, stringResource(R.string.stats_team_form_title)) }
    // Vue équipe : min/max = écart de points de war (warScoreToDiff), pas le total (#67).
    item { RecordsTilesCard(allTimeStats, selectors, isTeam = true) }
    // 4bis. Top/Bot 5→2 sur la fenêtre (N=6 retiré, redondant avec Records & séries, #64) :
    //       équipe ET adversaire. Masqués si aucune ligne affichable.
    (state.teamMapStatsByWindow[selectors.windowIndex] ?: state.teamMapStatsByWindow[0])?.let { mapStats ->
        if (hasDisplayableTopBottom(mapStats.topsTable, mapStats.bottomsTable)) item {
            StatCard(title = stringResource(R.string.stats_top_bottom_team_title)) {
                TopBottomColumns(tops = mapStats.topsTable, bottoms = mapStats.bottomsTable)
            }
        }
        if (hasDisplayableTopBottom(mapStats.opponentTopsTable, mapStats.opponentBottomsTable)) item {
            StatCard(title = stringResource(R.string.stats_top_bottom_opponent_title)) {
                TopBottomColumns(tops = mapStats.opponentTopsTable, bottoms = mapStats.opponentBottomsTable)
            }
        }
    }
    // 5. Contributeurs (recalculés sur la fenêtre globale).
    item { ContributorsCard(state.contributorsByWindow, selectors) }
    // 5bis. Meilleurs baggeurs (#69) : même carte, part de SHOCKS. Masquée si aucun baggeur.
    if (state.baggersByWindow[selectors.windowIndex].orEmpty().any { it.shockShare > 0 }) {
        item {
            ContributorsCard(
                byWindow = state.baggersByWindow,
                selectors = selectors,
                title = stringResource(R.string.stats_baggers_title),
                axis = ContributionAxis.SHOCKS
            )
        }
    }
    // 6. Podium circuits équipe (Top3 / Flop3 + tri).
    item { MapsPodiumCard(stats = stats, selectors = selectors, userId = null, onSeeAll = onMapsSeeAll) }
    // 7. Podium adversaires équipe.
    item {
        OpponentsPodiumCard(
            selectors = selectors,
            podiums = state.teamOpponentsByWindow[selectors.windowIndex] ?: StatsFullViewModel.OpponentPodiums(),
            userId = null,
            onSeeAll = onOpponentsSeeAll
        )
    }
}

// Section Indicateurs (#36)
/**
 * Section « Indicateurs » : chaque tuile affiche la valeur de la fenêtre + une progression en %
 * (delta vs all-time) quand pertinent. Distingue vue joueur (points/war, position) et équipe
 * (points d'équipe/war, score/manche).
 */
@Composable
private fun IndicatorsCard(
    stats: Stats,
    isPlayer: Boolean,
    selectors: SectionSelectors,
    // Taux de participation (#78) par fenêtre — vue joueur uniquement (équipe = 100 %).
    participationByWindow: Map<Int, Int>? = null
) {
    val window = stats.windowForm(selectors.windowIndex)
    // Deltas seulement hors all-time (index 0 = pas de comparaison).
    val showDelta = selectors.windowIndex != 0
    val title = if (isPlayer) stringResource(R.string.stats_player_indicators) else stringResource(R.string.stats_team_details)
    StatCard(title = title) {
        val tiles = buildList {
            // Winrate NON répété ici (#91 pt.3) : déjà affiché en grand dans la carte « Bilan ».
            when (isPlayer) {
                true -> {
                    // Vue joueur : points/war (brut) + position moyenne.
                    add(MetricTile(stringResource(R.string.stats_points_per_war), window?.averageScore?.toString() ?: "-", if (showDelta) window?.scoreDelta else null, "", DeltaPolarity.HIGHER, stringResource(R.string.info_points_per_war)))
                    add(MetricTile(stringResource(R.string.average_position_short), window?.averagePosition?.toString() ?: "-", if (showDelta) window?.positionDelta else null, "", DeltaPolarity.LOWER, stringResource(R.string.info_average_position)))
                    // Participation (#78) : % de wars de l'équipe jouées par le joueur sur la fenêtre.
                    participationByWindow?.let { byWindow ->
                        val participation = byWindow[selectors.windowIndex]
                        val participationDelta = participation?.minus(byWindow[0] ?: 0)
                        add(MetricTile(stringResource(R.string.participation_rate), participation?.let { "$it%" } ?: "-", if (showDelta) participationDelta else null, "%", DeltaPolarity.HIGHER, stringResource(R.string.info_participation_rate)))
                    }
                }
                else -> {
                    // Vue équipe : « Score moyen » = écart de points (warScoreToDiff), pas le total.
                    add(MetricTile(stringResource(R.string.form_score), window?.averageScore?.warScoreToDiff(false) ?: "-", if (showDelta) window?.scoreDelta else null, "", DeltaPolarity.HIGHER, stringResource(R.string.info_form_score)))
                    // Score/map = écart de points par manche (trackScoreToDiff), pas le total (#67).
                    add(MetricTile(stringResource(R.string.average_map_score_short), window?.averageMapScore?.trackScoreToDiff(false) ?: "-", if (showDelta) window?.mapScoreDelta else null, "", DeltaPolarity.HIGHER, stringResource(R.string.info_average_map_score)))
                }
            }
            add(MetricTile(stringResource(R.string.maps_gagn_es), window?.mapsWonPercent?.let { "$it%" } ?: "-", if (showDelta) window?.mapsWonDelta else null, "%", DeltaPolarity.HIGHER, stringResource(R.string.info_maps_won)))
            add(MetricTile(stringResource(R.string.stats_regularity), window?.scoreStdDev?.let { "±$it" } ?: "-", null, "", DeltaPolarity.NONE, stringResource(R.string.info_score_std_dev)))
            add(MetricTile(stringResource(R.string.avg_win_margin), window?.winMargin?.let { "+$it" } ?: "-", null, "", DeltaPolarity.NONE, stringResource(R.string.info_avg_win_margin)))
            add(MetricTile(stringResource(R.string.avg_loss_margin), window?.lossMargin?.let { "-$it" } ?: "-", null, "", DeltaPolarity.NONE, stringResource(R.string.info_avg_loss_margin)))
            // Pénalités (points perdus par l'équipe hôte) sur la FENÊTRE choisie.
            add(MetricTile(stringResource(R.string.penalty_points_lost), (window?.penaltyPointsLost ?: 0).takeIf { it > 0 }?.let { "-$it" } ?: "-", null, "", DeltaPolarity.NONE, stringResource(R.string.info_penalty_points_lost)))
            add(MetricTile(stringResource(R.string.shocks_per_war_short), window?.shocksPerWar?.let { String.format(java.util.Locale.getDefault(), "%.1f", it) } ?: "-", null, "", DeltaPolarity.NONE, stringResource(R.string.info_shocks_per_war)))
        }
        MetricTiles(tiles)
    }
}

private enum class DeltaPolarity { HIGHER, LOWER, NONE }

/**
 * Tuile d'indicateur : valeur + delta signé optionnel. [info] non-null ⇒ bouton ⓘ
 * ([MKStatInfoButton]) affiché (ticket #87).
 */
private data class MetricTile(
    val label: String,
    val value: String,
    val delta: Int?,
    val deltaSuffix: String,
    val polarity: DeltaPolarity,
    val info: String? = null
)

// Hauteurs réservées pour figer la taille des tuiles quel que soit le contenu (#36) :
// delta (occupé même absent) et libellé (calé sur 2 lignes) → toutes de même hauteur.
private val DeltaSlotHeight = 15.dp
private val LabelSlotHeight = 26.dp

/** Grille régulière : [columns] colonnes à poids égal, delta réservé → pas de redim au changement de fenêtre. */
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
    // Contenu en Column ; bouton ⓘ optionnel ancré en haut-à-droite (Box). La valeur réserve
    // une marge à droite pour ne pas chevaucher le bouton.
    Box(Modifier.weight(1f).background(Colors.white30, CardRadius)) {
        Column(Modifier.padding(10.dp)) {
            MKText(
                text = tile.value,
                font = Fonts.Urbanist,
                textColor = Colors.white,
                fontSize = 18,
                textAlign = TextAlign.Start,
                maxLines = 1,
                modifier = if (tile.info != null) Modifier.padding(end = 24.dp) else Modifier
            )
            // Ligne de progression à hauteur réservée (même vide) → hauteur de tuile figée.
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
            // Libellé à hauteur réservée (2 lignes) → tuiles de même hauteur.
            Box(Modifier.padding(top = 6.dp).height(LabelSlotHeight)) {
                MKText(text = tile.label, textColor = Colors.white70, fontSize = 10, textAlign = TextAlign.Start, maxLines = 2)
            }
        }
        tile.info?.let { info ->
            MKStatInfoButton(
                title = tile.label,
                message = info,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
            )
        }
    }
}

// Section Records & séries (#36)
/**
 * Records & séries : grille 3×2 sur la fenêtre (`FormStats`). Amplitude (min | max), records
 * de séries V | D, Top6 | Bot6. La série en cours est dans « Forme & séries ».
 */
@Composable
private fun RecordsTilesCard(stats: Stats, selectors: SectionSelectors, isTeam: Boolean) {
    val window = stats.windowForm(selectors.windowIndex)
    // Vue équipe : min/max en écart de points (warScoreToDiff) ; vue joueur : score perso brut (#67).
    val formatScore: (Int?) -> String = { value ->
        value?.let { if (isTeam) it.warScoreToDiff(false) else it.toString() } ?: "-"
    }
    StatCard(title = stringResource(R.string.records_series)) {
        val tiles = buildList {
            // Ligne 1 — Amplitude scindée en min | max (par fenêtre).
            add(MetricTile(stringResource(R.string.stats_amplitude_min), formatScore(window?.scoreMin), null, "", DeltaPolarity.NONE, stringResource(R.string.info_score_amplitude)))
            add(MetricTile(stringResource(R.string.stats_amplitude_max), formatScore(window?.scoreMax), null, "", DeltaPolarity.NONE, stringResource(R.string.info_score_amplitude)))
            // Ligne 2 — records de série (par fenêtre).
            add(MetricTile(stringResource(R.string.best_win_streak), (window?.bestWinStreak ?: 0).toString(), null, "", DeltaPolarity.NONE, stringResource(R.string.info_best_win_streak)))
            add(MetricTile(stringResource(R.string.worst_loss_streak), (window?.worstLossStreak ?: 0).toString(), null, "", DeltaPolarity.NONE, stringResource(R.string.info_worst_loss_streak)))
            // Ligne 3 — Top6 / Bot6 (compte, par fenêtre).
            add(MetricTile(stringResource(R.string.top6_count), (window?.top6Count ?: 0).toString(), null, "", DeltaPolarity.NONE, stringResource(R.string.info_top6_count)))
            add(MetricTile(stringResource(R.string.bot6_count), (window?.bot6Count ?: 0).toString(), null, "", DeltaPolarity.NONE, stringResource(R.string.info_bot6_count)))
        }
        MetricTiles(tiles, columns = 2)
    }
}

// Podiums circuits & adversaires (#36)
/**
 * Podium circuits : tri occurrences / winrate / score, Top3 / Flop3 (3 `PodiumCell` chacun).
 * [userId] non-null ⇒ score/position du joueur ; null ⇒ score d'équipe.
 */
@Composable
private fun MapsPodiumCard(stats: Stats, selectors: SectionSelectors, userId: String?, onSeeAll: (() -> Unit)? = null) {
    val (top, flop) = when (selectors.trackSortIndex) {
        1 -> stats.topMapsByWinrate to stats.flopMapsByWinrate
        2 -> stats.topMapsByScore to stats.flopMapsByScore
        else -> stats.topMapsByCount to stats.flopMapsByCount
    }
    // Carte masquée seulement si aucun circuit jouable, tous tris confondus (#91 pt.1) : un tri
    // sans échantillon dégrade en message (`PodiumOrMessage`) plutôt que de masquer la section.
    val hasAnyMap = stats.topMapsByCount.isNotEmpty() || stats.topMapsByWinrate.isNotEmpty() || stats.topMapsByScore.isNotEmpty()
    if (!hasAnyMap) return
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
    StatCard(
        title = stringResource(R.string.best_maps_section),
        titleTrailing = onSeeAll?.let { { SeeAllLink(it) } }
    ) {
        SortSelector(selectors.trackSortIndex, selectors.onTrackSortChange)
        Spacer(Modifier.height(11.dp))
        PodiumOrMessage(stringResource(R.string.stats_podium_top), top.map(toEntry))
        Spacer(Modifier.height(8.dp))
        PodiumOrMessage(stringResource(R.string.stats_podium_flop), flop.map(toEntry))
    }
}

/**
 * Podium adversaires : tri occurrences / winrate / score, Top3 / Flop3. [userId] non-null ⇒
 * score du joueur (Individuelles) ; null ⇒ écart d'équipe (Équipe).
 */
@Composable
private fun OpponentsPodiumCard(
    selectors: SectionSelectors,
    podiums: StatsFullViewModel.OpponentPodiums,
    userId: String?,
    onSeeAll: (() -> Unit)? = null
) {
    val (top, flop) = when (selectors.opponentSortIndex) {
        1 -> podiums.topByWinrate to podiums.flopByWinrate
        2 -> podiums.topByScore to podiums.flopByScore
        else -> podiums.topByCount to podiums.flopByCount
    }
    // Carte affichée dès qu'un adversaire est classable, tous tris confondus (#91 pt.1) : sur
    // une fenêtre réduite, MIN_RANKING_SAMPLE peut vider un tri alors qu'un autre reste peuplé →
    // un tri sans données affiche un message (`PodiumOrMessage`), pas un podium tronqué.
    val hasAnyOpponent = podiums.topByCount.isNotEmpty() || podiums.topByWinrate.isNotEmpty() || podiums.topByScore.isNotEmpty()
    if (!hasAnyOpponent) return
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
    StatCard(
        title = stringResource(R.string.best_opponents_section),
        titleTrailing = onSeeAll?.let { { SeeAllLink(it) } }
    ) {
        SortSelector(selectors.opponentSortIndex, selectors.onOpponentSortChange)
        Spacer(Modifier.height(11.dp))
        PodiumOrMessage(stringResource(R.string.stats_podium_top), top.map(toEntry))
        Spacer(Modifier.height(8.dp))
        PodiumOrMessage(stringResource(R.string.stats_podium_flop), flop.map(toEntry))
    }
}

/**
 * Podium sous label (#91 pt.1) : [PodiumRow] complète si ≥ 3 entrées, sinon message de
 * dégradation — jamais un podium tronqué (2 sur 3, artefact de MIN_RANKING_SAMPLE). Le label
 * reste toujours affiché → la section ne disparaît pas au changement de période/tri.
 */
@Composable
private fun ColumnScope.PodiumOrMessage(label: String, entries: List<PodiumEntry>) {
    PodiumLabel(label)
    when (entries.size) {
        3 -> PodiumRow(entries)
        else -> MKText(
            text = stringResource(R.string.stats_podium_not_enough),
            textColor = Colors.white55,
            fontSize = 12,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        )
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

/** Lien « Classement entier → » (même style que PodiumSectionCard, #67). */
@Composable
private fun SeeAllLink(onClick: () -> Unit) {
    MKText(
        text = stringResource(R.string.stats_see_full_ranking),
        font = Fonts.NunitoBD,
        textColor = Colors.yellow,
        fontSize = 12,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

// Composants de carte (réutilisés par les deux onglets)
/**
 * En-tête : vignette (photo joueur / logo équipe) + nom + sous-titre. [logo] = URL MKCentral
 * préfixée ; fallback = initiales (joueur) ou default_logo (équipe). Délègue à [StatHeaderCard] (rule 16).
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

/**
 * Ligne icône + titre + sous-titre. [tinted] true ⇒ icône teintée par [accent] (`Icon`,
 * monochrome comme le shock) ; false ⇒ `Image` couleurs d'origine (multicolore, ex. champignon #91).
 * [iconSize] défaut 22 dp (34 dp pour un asset à padding transparent), borné par le médaillon 44 dp.
 */
@Composable
private fun IconLine(icon: Int, accent: Color, title: String, subtitle: String, tinted: Boolean = true, iconSize: Dp = 22.dp) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.25f)).border(1.dp, accent.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when (tinted) {
                true -> Icon(painter = painterResource(icon), contentDescription = null, tint = accent, modifier = Modifier.size(iconSize))
                else -> Image(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(iconSize))
            }
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
                // Texte de la série en blanc (#50 pt.3) ; la flamme garde sa couleur V/D.
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


/** Carte « Répartition des positions » : histogramme P1→P12 sur la fenêtre + pied Top6/Bot6. */
@Composable
private fun DistributionCard(stats: Stats, selectors: SectionSelectors) {
    // La fenêtre filtre déjà `stats` (VM #68) → calcul sur toutes ses wars (lastN = null) ;
    // un second takeLast doublerait le filtrage.
    val distribution = stats.positionDistributionFor(lastN = null)
    if (distribution.none { it.second > 0 }) return
    StatCard(title = stringResource(R.string.stats_distribution_title)) {
        // Chart/footer mutualisés (ui/stats/MKDistributionCard.kt) — rule 16.
        DistributionChart(distribution)
        DistributionFooter(distribution)
    }
}

/** Axe de [ContributorsCard] : part de POINTS ou de SHOCKS (#69). */
private enum class ContributionAxis { POINTS, SHOCKS }

/**
 * Classement du roster par fenêtre, mutualisé (#69) entre « Contributeurs » (part de points) et
 * « Meilleurs baggeurs » (part de shocks). [axis] pilote la valeur affichée ; lignes déjà triées (VM).
 */
@Composable
private fun ContributorsCard(
    byWindow: Map<Int, List<StatsFullViewModel.Contributor>>,
    selectors: SectionSelectors,
    title: String = stringResource(R.string.stats_contributors_title),
    axis: ContributionAxis = ContributionAxis.POINTS
) {
    val contributors = byWindow[selectors.windowIndex].orEmpty()
    StatCard(title = title) {
        when {
            contributors.isEmpty() -> MKText(text = stringResource(R.string.stats_no_data), textColor = Colors.white66, fontSize = 12)
            else -> contributors.forEachIndexed { index, contributor -> ContributorRow(index + 1, contributor, axis) }
        }
    }
}

@Composable
private fun ContributorRow(rank: Int, contributor: StatsFullViewModel.Contributor, axis: ContributionAxis) {
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
        // Médaillon mutualisé (#50 pt.4) : photo si dispo, initiales sinon.
        PlayerMedallion(
            initials = initialsOf(contributor.player.name.displayName),
            avatarColor = playerAvatarColor(contributor.player.id),
            avatarPath = contributor.player.avatar,
            size = 34.dp,
            initialsFontSize = 12
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MKText(text = contributor.player.name.displayName, font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 14, textAlign = TextAlign.Start, maxLines = 1)
                if (contributor.isMe) {
                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Colors.yellow).padding(horizontal = 5.dp, vertical = 1.dp)) {
                        MKText(text = stringResource(R.string.stats_me_tag), font = Fonts.NunitoBD, textColor = Colors.black, fontSize = 8)
                    }
                }
            }
            val shareText = when (axis) {
                ContributionAxis.POINTS -> stringResource(R.string.stats_points_share, contributor.pointsShare)
                ContributionAxis.SHOCKS -> stringResource(R.string.stats_shocks_share, contributor.shockShare)
            }
            MKText(text = shareText, textColor = Colors.white66, fontSize = 11, textAlign = TextAlign.Start)
        }
        Column(horizontalAlignment = Alignment.End) {
            MKText(text = "${contributor.winrate}%", font = Fonts.Urbanist, textColor = Colors.white, fontSize = 14)
            MKText(text = stringResource(R.string.form_winrate).lowercase(), textColor = Colors.white55, fontSize = 9)
        }
    }
}
