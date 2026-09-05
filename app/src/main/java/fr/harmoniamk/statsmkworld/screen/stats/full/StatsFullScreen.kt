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
 * États hissés des sélecteurs de l'écran (#68). La **période** est désormais GLOBALE à
 * l'écran (un seul index [windowIndex], sélecteur unique en haut) et toutes les sections
 * la lisent. Restent PROPRES aux sections : le **tri** des podiums circuits/adversaires
 * (occurrences/winrate/score) — axe indépendant de la période, conservé (#68).
 */
private class SectionSelectors(
    // Fenêtre de période GLOBALE : 0 = all-time, 1 = 5 dernières, 2 = 10 dernières.
    val windowIndex: Int,
    // Tri des podiums : 0 = occurrences (défaut), 1 = winrate, 2 = score.
    val trackSortIndex: Int,
    val onTrackSortChange: (Int) -> Unit,
    val opponentSortIndex: Int,
    val onOpponentSortChange: (Int) -> Unit
)

/**
 * FormStats de la fenêtre sélectionnée pour les Indicateurs/Records (0 = all-time, 1 = 5,
 * 2 = 10). Note : les `stats` reçues sont déjà filtrées sur la fenêtre globale (VM #68) ;
 * on sélectionne ici la `FormStats` correspondante pour conserver l'affichage des **deltas
 * vs all-time** (index 0 → allTimeForm = pas de delta, cf. `showDelta`).
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
    // « Classement entier » des sections Circuits / Adversaires → classement complet **scopé
    // au périmètre actif** (#67 round 3) : le booléen `isTeam` remonte la portée courante
    // (Individuelles = false → données du joueur ; Équipe = true → données d'équipe), pour que
    // l'appelant navigue vers le bon classement. Optionnels (masqués si null).
    onMapsSeeAll: ((isTeam: Boolean) -> Unit)? = null,
    onOpponentsSeeAll: ((isTeam: Boolean) -> Unit)? = null
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    // 0 = Individuelles, 1 = Équipe. Sur statsfull (pas d'onglets) → toujours 0.
    var scopeIndex by rememberSaveable { mutableIntStateOf(0) }
    // Période GLOBALE de l'écran (#68) : 0 = all-time, 1 = 5 dernières, 2 = 10 dernières.
    // UN SEUL état, un sélecteur unique en haut ; TOUTES les sections lisent cette fenêtre
    // et se recomposent au changement (rule 11 : State, pas de re-nav). Survit à la rotation.
    var windowIndex by rememberSaveable { mutableIntStateOf(0) }
    // Critère des podiums circuits / adversaires : 0 = occurrences (défaut), 1 = winrate, 2 = score.
    // Axe INDÉPENDANT de la période (#68) : tri conservé en plus du filtre de période.
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
        // Bouton retour uniquement en fiche poussée (showTabs=false) ; en onglet du pôle
        // Stats (showTabs=true), pas de retour d'appbar (rule 14, comportement onglet).
        onBack = onBack?.takeIf { !viewModel.showTabs },
        // Sélecteur de SAISON (#70) : menu déroulant aligné à droite dans le header (composant
        // partagé MKSeasonDropdown, rule 16). Défaut = saison en cours. Change l'état VM ⇒
        // recalcul à la volée des agrégats sur l'intervalle [start, end] (rule 11, pas de re-nav).
        headerTrailing = {
            MKSeasonDropdown(
                seasons = state.value.seasons,
                selectedSeasonNumber = state.value.selectedSeasonNumber,
                onSeasonSelected = viewModel::onSeasonSelected
            )
        },
        modifier = Modifier.padding(bottom = if (viewModel.showTabs) 90.dp else 0.dp)
    ) {
        // Le header (dropdown de saison + segmented indiv/équipe + période) reste TOUJOURS
        // visible ; seule la zone de DONNÉES (LazyColumn) passe en chargement au recompute
        // (changement de saison, #73). Ainsi le dropdown qu'on vient de sélectionner ne
        // disparaît pas et le ressenti est immédiat.
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
        // Sélecteur de période GLOBAL (#68), juste sous le sélecteur indiv/équipe et
        // AU-DESSUS de toutes les sections. Reste visible même quand indiv/équipe est
        // masqué (showTabs == false, stats d'un joueur donné). Sur le fond clair du
        // dégradé de BaseScreen → onDark = false (défaut). Change l'état global ⇒
        // toutes les sections se recomposent (rule 11), aucune re-navigation.
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

// =====================================================================
// Onglet Individuelles (et statsfull)
// =====================================================================

private fun androidx.compose.foundation.lazy.LazyListScope.individualSections(
    state: StatsFullViewModel.State,
    showTabs: Boolean,
    onResults: (() -> Unit)?,
    selectors: SectionSelectors,
    onMapsSeeAll: (() -> Unit)?,
    onOpponentsSeeAll: (() -> Unit)?
) {
    // Stats ALL-TIME (index 0) : source des sections « FormStats » (Indicateurs, Records,
    // Forme & séries) qui affichent la valeur de la fenêtre choisie ET son delta vs all-time.
    // Ces objets pré-calculent déjà allTimeForm/recentForm5/recentForm10 (deltas corrects) ;
    // les fenêtrer casserait les deltas. Écran vide si aucune stat.
    val allTimeStats = state.playerStatsByWindow[0] ?: return
    // Stats de la FENÊTRE de période globale (#68), pour les sections recalculées sur la
    // fenêtre (Bilan, Contribution, Distribution, Podiums…). Fallback all-time si vide.
    val stats = state.playerStatsByWindow[selectors.windowIndex] ?: allTimeStats
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
    // 2. Bilan (#65 : lien « Résultats → » TOUJOURS visible, dans les deux contextes —
    //    pôle Stats comme fiche joueur — vers l'historique filtré sur le joueur). Fenêtré.
    item { BalanceCard(stats, showResultsLink = true, onResults = onResults) }
    // 3. Indicateurs (grille régulière) — vue JOUEUR : points/war, position, régularité,
    //    marges V/D, pénalités, maps gagnées, shocks… (fenêtre globale + deltas vs all-time).
    item {
        IndicatorsCard(
            stats = allTimeStats,
            isPlayer = true,
            selectors = selectors,
            participationByWindow = state.participationRateByWindow
        )
    }
    // 4. Contribution (#69, retour PR #75) : deux lignes (part de POINTS + part de SHOCKS),
    //    LUES depuis le classement Contributeurs/Baggeurs de la MÊME fenêtre (source de vérité
    //    UNIQUE), pour que le % du joueur soit IDENTIQUE ici et dans le classement équipe.
    //    Les DEUX parts sont désormais des ratios TOTAL/TOTAL (la ligne points n'est plus une
    //    moyenne par war). Rang = position de la ligne `isMe` dans le classement correspondant.
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
                        // Icône champignon vectorielle EN COULEUR (#91 pt.11, retour user : ic_mushroom
                        // — chapeau rouge à taches, pied crème) illustrant la part de POINTS de l'équipe.
                        // tinted = false → dessinée en Image (pas d'aplat par le tint accent).
                        icon = R.drawable.ic_mushroom,
                        accent = Colors.yellow,
                        tinted = false,
                        title = stringResource(R.string.stats_contribution_value, contributor.pointsShare),
                        subtitle = meContributorRank
                            ?.let { stringResource(R.string.stats_contribution_rank, it + 1) }
                            ?: ""
                    )
                    // 2ᵉ ligne : part de shocks du joueur (même valeur que dans « Baggeurs »).
                    // Masquée si le joueur n'a aucun shock sur la fenêtre.
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
    // 5. Forme & séries (série en cours = all-time par nature) + Records (grille 3×2 :
    //    FormStats de la fenêtre + deltas vs all-time → all-time stats).
    item { FormStreakCard(allTimeStats, stringResource(R.string.stats_player_form_title)) }
    // Vue JOUEUR : min/max = score PERSO brut (pas de diff — #67 vise la seule vue Équipe).
    item { RecordsTilesCard(allTimeStats, selectors, isTeam = false) }
    // 6. Distribution des positions (barres ancrées en bas, sur la fenêtre globale).
    item { DistributionCard(stats, selectors) }
    // 7. Podium circuits (Top3 / Flop3 sur une ligne + sélecteur occ./winrate/score).
    item { MapsPodiumCard(stats = stats, selectors = selectors, userId = state.targetUserId, onSeeAll = onMapsSeeAll) }
    // 8. Podium adversaires (perspective joueur), sur la fenêtre globale.
    item {
        OpponentsPodiumCard(
            selectors = selectors,
            podiums = state.playerOpponentsByWindow[selectors.windowIndex] ?: StatsFullViewModel.OpponentPodiums(),
            userId = state.targetUserId,
            onSeeAll = onOpponentsSeeAll
        )
    }
}

// =====================================================================
// Onglet Équipe
// =====================================================================

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
private fun androidx.compose.foundation.lazy.LazyListScope.teamSections(
    state: StatsFullViewModel.State,
    selectors: SectionSelectors,
    onMapsSeeAll: (() -> Unit)?,
    onOpponentsSeeAll: (() -> Unit)?
) {
    // Stats d'équipe ALL-TIME (index 0) : source des sections FormStats (Indicateurs,
    // Records, Forme & séries — deltas vs all-time). Écran vide si aucune stat.
    val allTimeStats = state.teamStatsByWindow[0] ?: return
    // Stats d'équipe de la FENÊTRE de période globale (#68) pour les sections recalculées
    // sur la fenêtre (Bilan, Tops/Bots, Contributeurs, Podiums…). Fallback all-time si vide.
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
    // 3. Détails équipe (grille régulière) — vue ÉQUIPE : score = ÉCART de points
    //    (warScoreToDiff), score moyen/manche, maps gagnées, marges d'équipe, pénalités…
    //    (fenêtre globale + deltas vs all-time → all-time stats).
    item { IndicatorsCard(stats = allTimeStats, isPlayer = false, selectors = selectors) }
    // 4. Forme & séries équipe (série en cours = all-time) + Records (FormStats fenêtre).
    item { FormStreakCard(allTimeStats, stringResource(R.string.stats_team_form_title)) }
    // Vue ÉQUIPE : min/max = ÉCART de points de war (warScoreToDiff), pas le total (#67).
    item { RecordsTilesCard(allTimeStats, selectors, isTeam = true) }
    // 4bis. Top/Bot 5→2 sur la FENÊTRE globale (détail que RecordsTilesCard n'affiche pas ;
    //       ligne N=6 retirée car redondante avec « Records & séries », #64) : équipe ET
    //       adversaire (complément des positions). Masqués si aucune ligne affichable.
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
    // 5. Contributeurs (classement recalculé sur la fenêtre globale).
    item { ContributorsCard(state.contributorsByWindow, selectors) }
    // 5bis. Meilleurs baggeurs (#69) : MÊME carte, part de SHOCKS (total/total). Masquée
    //       si aucun baggeur avec shock sur la fenêtre.
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
    // 6. Podium circuits équipe (Top3 / Flop3 sur une ligne + sélecteur occ./winrate/score).
    item { MapsPodiumCard(stats = stats, selectors = selectors, userId = null, onSeeAll = onMapsSeeAll) }
    // 7. Podium adversaires équipe, sur la fenêtre globale.
    item {
        OpponentsPodiumCard(
            selectors = selectors,
            podiums = state.teamOpponentsByWindow[selectors.windowIndex] ?: StatsFullViewModel.OpponentPodiums(),
            userId = null,
            onSeeAll = onOpponentsSeeAll
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
private fun IndicatorsCard(
    stats: Stats,
    isPlayer: Boolean,
    selectors: SectionSelectors,
    // Taux de participation (#78) par fenêtre — vue JOUEUR uniquement (l'équipe est
    // à 100 % par définition). Delta vs all-time (index 0) affiché comme les autres tuiles.
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
                    // Vue JOUEUR : score = points/war (brut) + position moyenne.
                    add(MetricTile(stringResource(R.string.stats_points_per_war), window?.averageScore?.toString() ?: "-", if (showDelta) window?.scoreDelta else null, "", DeltaPolarity.HIGHER, stringResource(R.string.info_points_per_war)))
                    add(MetricTile(stringResource(R.string.average_position_short), window?.averagePosition?.toString() ?: "-", if (showDelta) window?.positionDelta else null, "", DeltaPolarity.LOWER, stringResource(R.string.info_average_position)))
                    // Taux de participation (#78) : % de wars de l'équipe jouées par le joueur sur
                    // la fenêtre. Delta vs all-time (polarité « plus haut = mieux »).
                    participationByWindow?.let { byWindow ->
                        val participation = byWindow[selectors.windowIndex]
                        val participationDelta = participation?.minus(byWindow[0] ?: 0)
                        add(MetricTile(stringResource(R.string.participation_rate), participation?.let { "$it%" } ?: "-", if (showDelta) participationDelta else null, "%", DeltaPolarity.HIGHER, stringResource(R.string.info_participation_rate)))
                    }
                }
                else -> {
                    // Vue ÉQUIPE : « Score moyen » = ÉCART de points (warScoreToDiff), pas le total.
                    add(MetricTile(stringResource(R.string.form_score), window?.averageScore?.warScoreToDiff(false) ?: "-", if (showDelta) window?.scoreDelta else null, "", DeltaPolarity.HIGHER, stringResource(R.string.info_form_score)))
                    // « Score moyen/map » = ÉCART de points par MANCHE (trackScoreToDiff), pas le total (#67).
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
 * Donnée d'une tuile d'indicateur : valeur + delta signé optionnel. [info] (facultatif)
 * porte le texte d'explication de la stat : quand il est non-null, la tuile affiche un
 * bouton ⓘ ([MKStatInfoButton]) à côté du libellé (ticket #87).
 */
private data class MetricTile(
    val label: String,
    val value: String,
    val delta: Int?,
    val deltaSuffix: String,
    val polarity: DeltaPolarity,
    val info: String? = null
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
    // Le contenu (valeur + delta + libellé) reste dans son flux normal en Column ; le
    // bouton ⓘ (facultatif) est ancré dans le coin SUPÉRIEUR DROIT de la tuile via un Box
    // englobant, avec une petite marge (rule 15/13). La valeur réserve une marge à droite
    // (end) pour ne jamais chevaucher le bouton.
    Box(Modifier.weight(1f).background(Colors.white30, CardRadius)) {
        Column(Modifier.padding(10.dp)) {
            // Valeur : toujours BLANCHE (aucune couleur). Marge droite réservée si un bouton
            // ⓘ est ancré en haut-à-droite, pour éviter tout chevauchement.
            MKText(
                text = tile.value,
                font = Fonts.Urbanist,
                textColor = Colors.white,
                fontSize = 18,
                textAlign = TextAlign.Start,
                maxLines = 1,
                modifier = if (tile.info != null) Modifier.padding(end = 24.dp) else Modifier
            )
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
        // Bouton ⓘ ancré en HAUT-À-DROITE, émis seulement si [tile.info] est fourni.
        tile.info?.let { info ->
            MKStatInfoButton(
                title = tile.label,
                message = info,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
            )
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
private fun RecordsTilesCard(stats: Stats, selectors: SectionSelectors, isTeam: Boolean) {
    val window = stats.windowForm(selectors.windowIndex)
    // Vue ÉQUIPE : min/max de war affichés en ÉCART de points (warScoreToDiff) ; vue JOUEUR :
    // score perso brut (#67). Formatage local selon le mode.
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
private fun MapsPodiumCard(stats: Stats, selectors: SectionSelectors, userId: String?, onSeeAll: (() -> Unit)? = null) {
    val (top, flop) = when (selectors.trackSortIndex) {
        1 -> stats.topMapsByWinrate to stats.flopMapsByWinrate
        2 -> stats.topMapsByScore to stats.flopMapsByScore
        else -> stats.topMapsByCount to stats.flopMapsByCount
    }
    // La carte disparaît seulement si AUCUN circuit n'est jouable, TOUS tris confondus (#91
    // pt.1) — pas seulement pour le tri courant : un tri sans échantillon dégrade en message
    // (cf. `PodiumOrMessage`) au lieu de faire disparaître toute la section au changement de tri.
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
 * Podium adversaires : sélecteur **occurrences / winrate / score**, Top 3 / Flop 3
 * chacun sur **une ligne** de 3 `PodiumCell`. Chaque cellule reprend les infos de
 * `TeamCell` (nb de confrontations, winrate, score). [userId] non-null ⇒ score du
 * joueur (Individuelles) ; null ⇒ écart d'équipe (Équipe).
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
    // La carte reste affichée dès qu'au moins un adversaire est classable, TOUS tris confondus
    // (#91 pt.1) : sur une fenêtre réduite (5/10 dernières), le seuil MIN_RANKING_SAMPLE peut
    // vider le tri winrate/score alors que le tri occurrences reste peuplé → on ne fait plus
    // disparaître la section au changement de fenêtre/tri ; un tri sans données affiche un
    // message (cf. `PodiumOrMessage`) plutôt qu'un podium tronqué (2 sur 3) incohérent.
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
 * Podium sous label (#91 pt.1) : rend une ligne [PodiumRow] complète quand il y a **assez
 * d'entrées** pour un podium cohérent (≥ 3), sinon un message de dégradation « pas assez de
 * données sur cette période » — jamais un podium tronqué « 2 sur 3 » (artefact du seuil
 * MIN_RANKING_SAMPLE sur une fenêtre réduite). Le label reste toujours affiché → la section
 * ne disparaît plus au changement de période/tri.
 */
@Composable
private fun ColumnScope.PodiumOrMessage(label: String, entries: List<PodiumEntry>) {
    PodiumLabel(label)
    when (entries.size) {
        // Podium complet (3 cellules) : rendu normal, cohérent.
        3 -> PodiumRow(entries)
        // Aucun / trop peu d'entrées classables sur cette fenêtre → message, pas de podium
        // tronqué (2 sur 3) qui donnerait un rendu incohérent.
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

/**
 * Ligne icône + gros titre + sous-titre (contribution). [tinted] = true : l'icône est
 * teintée par [accent] (`Icon`, cas monochrome comme le shock). false : icône dessinée en
 * couleurs d'origine (`Image`) — pour un vecteur multicolore comme `ic_mushroom` (#91), qu'un
 * tint aplatirait. Taille de l'icône (22 dp) et médaillon accent identiques dans les deux cas.
 */
@Composable
private fun IconLine(icon: Int, accent: Color, title: String, subtitle: String, tinted: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.25f)).border(1.dp, accent.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when (tinted) {
                true -> Icon(painter = painterResource(icon), contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                else -> Image(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp))
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
    // La fenêtre globale filtre déjà les wars de `stats` (VM #68). La distribution est donc
    // calculée sur TOUTES les wars de `stats` (lastN = null) : appliquer un second takeLast
    // ici doublerait le filtrage. On garde le calcul all-time DE CETTE FENÊTRE.
    val distribution = stats.positionDistributionFor(lastN = null)
    if (distribution.none { it.second > 0 }) return
    StatCard(title = stringResource(R.string.stats_distribution_title)) {
        // Chart/footer mutualisés (ui/stats/MKDistributionCard.kt) — rule 16.
        DistributionChart(distribution)
        DistributionFooter(distribution)
    }
}

// --- Contributeurs / Baggeurs (`.lrow`) --------------------------------------

/** Axe de contribution affiché par [ContributorsCard] : part de POINTS ou de SHOCKS (#69). */
private enum class ContributionAxis { POINTS, SHOCKS }

/**
 * Carte de classement du roster par fenêtre (all-time / 5 / 10), MUTUALISÉE (#69) entre
 * « Contributeurs » (part de points) et « Meilleurs baggeurs » (part de shocks, total/total).
 * [axis] pilote la valeur affichée sous chaque nom ; les lignes reçues sont déjà triées
 * selon l'axe (VM). Rien à afficher si la fenêtre est vide.
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
        // Médaillon joueur mutualisé (#50 pt.4) : photo si dispo, initiales sinon/pendant le chargement.
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
