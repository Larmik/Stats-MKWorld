package fr.harmoniamk.statsmkworld.screen.stats.opponent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.cells.WarCell
import fr.harmoniamk.statsmkworld.ui.cells.WarCellViewModel
import fr.harmoniamk.statsmkworld.ui.cells.playerAvatarColor
import fr.harmoniamk.statsmkworld.ui.stats.initialsOf
import fr.harmoniamk.statsmkworld.ui.stats.BalanceCard
import fr.harmoniamk.statsmkworld.ui.stats.PodiumEntry
import fr.harmoniamk.statsmkworld.ui.stats.PodiumSectionCard
import fr.harmoniamk.statsmkworld.ui.stats.StatCard
import fr.harmoniamk.statsmkworld.ui.stats.StatHeaderCard
import fr.harmoniamk.statsmkworld.ui.stats.mapStatsDetailSections
import fr.harmoniamk.statsmkworld.extension.displayName
import fr.harmoniamk.statsmkworld.extension.pointsToPosition
import fr.harmoniamk.statsmkworld.extension.trackScoreToDiff
import fr.harmoniamk.statsmkworld.model.local.TrackStats
import fr.harmoniamk.statsmkworld.screen.stats.ranking.SortType
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

/**
 * Fiche détail adversaire (#27). Sélecteur Indiv/Équipe (rule 11 : état réactif du VM). Sections :
 * en-tête, Bilan face à eux, 5 dernières, Séries & scores, Circuits/Pilotes/Baggeurs contre eux
 * (podiums), sections détaillées mutualisées (rule 16) et historique des wars → WarDetailsScreen.
 * Rules 13/15/16.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Composable
fun OpponentDetailScreen(
    viewModel: OpponentDetailViewModel,
    onBack: () -> Unit,
    onWarDetailsClick: (WarDetails) -> Unit,
    onTracksRanking: () -> Unit,
    onPilotsRanking: () -> Unit,
    onBaggersRanking: () -> Unit
) {
    BackHandler { onBack() }
    val state by viewModel.state.collectAsStateWithLifecycle()

    BaseScreen(title = stringResource(R.string.opponent_detail_title), onBack = onBack) {
        when {
            state.loading -> CircularProgressIndicator()
            else -> {
                val team = state.team
                val stats = state.stats
                // Sélecteur Indiv/Équipe (rule 15 : composant partagé).
                MKSegmentedSelector(
                    items = listOf(
                        stringResource(R.string.opponent_detail_scope_indiv),
                        stringResource(R.string.opponent_detail_scope_team)
                    ),
                    page = if (state.isIndiv) 0 else 1,
                    onClick = { index -> viewModel.onModeChange(index == 0) }
                )
                Spacer(Modifier.height(11.dp))
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    // 1. En-tête.
                    item {
                        StatHeaderCard(
                            name = team?.name.orEmpty(),
                            subtitle = stringResource(
                                R.string.opponent_detail_header,
                                stats?.warStats?.warsPlayed ?: 0,
                                state.lastMeeting ?: "-"
                            ),
                            color = Colors.red,
                            logo = team?.logo?.let { "https://mkcentral.com$it" },
                            fallbackText = team?.tag
                        )
                    }
                    // 2. Bilan face à eux.
                    stats?.let { s ->
                        val played = s.warStats.warsPlayed.takeIf { it > 0 } ?: 1
                        item {
                            BalanceCard(
                                title = stringResource(R.string.opponent_detail_balance),
                                winrate = (s.warStats.warsWon * 100) / played,
                                won = s.warStats.warsWon,
                                tied = s.warStats.warsTied,
                                loss = s.warStats.warsLoss,
                                subtitle = stringResource(R.string.opponent_detail_winrate_sub, s.warStats.warsPlayed)
                            )
                        }
                    }
                    // 3. 5 dernières face à eux.
                    if (state.recentOutcomes.isNotEmpty()) item {
                        StatCard(title = stringResource(R.string.opponent_detail_recent)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Plus récente à droite (chronologique).
                                state.recentOutcomes.forEach { OutcomeChipSmall(it) }
                            }
                        }
                    }
                    // 4. Séries & scores — grille 3 lignes × 2 cellules.
                    stats?.let { s -> item { StreaksScoresCard(state, s) } }
                    // 5. Circuits contre eux (podium Top3/Flop3 trié) + sélecteur + voir en entier.
                    if (state.topTracks.isNotEmpty() || state.flopTracks.isNotEmpty()) item {
                        PodiumSectionCard(
                            title = stringResource(R.string.opponent_detail_best_tracks),
                            top = state.topTracks.map { it.toPodiumEntry(state.isIndiv) },
                            flop = state.flopTracks.map { it.toPodiumEntry(state.isIndiv) },
                            onSeeAll = onTracksRanking,
                            selector = {
                                TracksSortSelector(state.tracksSort, onDark = true, onSelect = viewModel::onTracksSortSelected)
                            }
                        )
                    }
                    // 5bis. Pilotes contre eux — mode Équipe uniquement (#67).
                    if (!state.isIndiv && state.pilots.isNotEmpty()) item {
                        PodiumSectionCard(
                            title = stringResource(R.string.opponent_detail_pilots),
                            top = state.pilots.take(3).map { it.toPodiumEntry() },
                            flop = state.pilots.takeLast(3).reversed().map { it.toPodiumEntry() },
                            onSeeAll = onPilotsRanking
                        )
                    }
                    // 5ter. Baggeurs contre eux (part de shocks, #69) — mode Équipe uniquement.
                    if (!state.isIndiv && state.baggers.isNotEmpty()) item {
                        PodiumSectionCard(
                            title = stringResource(R.string.opponent_detail_baggers),
                            top = state.baggers.take(3).map { it.toPodiumEntry() },
                            flop = state.baggers.takeLast(3).reversed().map { it.toPodiumEntry() },
                            onSeeAll = onBaggersRanking
                        )
                    }
                    // 6. Sections détaillées mutualisées, scopées à cet adversaire (répartition, Top/Bot 2→6).
                    state.mapStats?.let { mapStatsDetailSections(it) }
                    // 7. Historique des wars → WarDetailsScreen.
                    if (state.history.isNotEmpty()) {
                        item { SectionLabel(stringResource(R.string.opponent_detail_history)) }
                        items(state.history, key = { it.war.id }) { war ->
                            WarCell(
                                viewModel = hiltViewModel(
                                    key = war.war.id.toString(),
                                    creationCallback = { factory: WarCellViewModel.Factory -> factory.create(war) }
                                ),
                                onClick = onWarDetailsClick
                            )
                        }
                        item { Spacer(Modifier.height(90.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    MKText(
        text = text.uppercase(),
        font = Fonts.NunitoBD,
        textColor = Colors.white66,
        fontSize = 11,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp)
    )
}

/** Pastille V/N/D compacte pour la bande « 5 dernières » (vert/blanc/rouge). */
@Composable
private fun OutcomeChipSmall(outcome: Int) {
    val (label, color) = when {
        outcome > 0 -> stringResource(R.string.v) to Colors.green
        outcome < 0 -> stringResource(R.string.d) to Colors.red
        else -> stringResource(R.string.n) to Colors.white
    }
    Box(
        Modifier.size(30.dp).background(color, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        MKText(text = label, font = Fonts.NunitoBD, textColor = Colors.black, fontSize = 13)
    }
}

@Composable
private fun streakLabel(streak: Int): String = when {
    streak > 0 -> "$streak ${stringResource(R.string.v)}"
    streak < 0 -> "${-streak} ${stringResource(R.string.d)}"
    else -> stringResource(R.string.no_streak)
}

private fun streakColor(streak: Int) = when {
    streak > 0 -> Colors.green
    streak < 0 -> Colors.red
    else -> Colors.white
}

/**
 * Section « Séries & scores » — grille 3×2 : L1 score/diff (mode-aware) · série en cours ;
 * L2 records de séries V/D ; L3 shocks obtenus · shocks/war (illustration shock à gauche).
 */
@Composable
private fun StreaksScoresCard(state: OpponentDetailViewModel.State, stats: Stats) {
    // Cellule score : Indiv = score moyen joueur ; Équipe = différence moyenne signée.
    val scoreValue: String
    val scoreColor: Color
    val scoreLabel: String
    when (state.isIndiv) {
        true -> {
            scoreValue = state.playerAverageScore.toString()
            scoreColor = Colors.white
            scoreLabel = stringResource(R.string.opponent_detail_player_score)
        }
        else -> {
            scoreValue = with(state.averageScoreDiff) { if (this > 0) "+$this" else toString() }
            scoreColor = when {
                state.averageScoreDiff > 0 -> Colors.green
                state.averageScoreDiff < 0 -> Colors.red
                else -> Colors.white
            }
            scoreLabel = stringResource(R.string.opponent_detail_avg_diff)
        }
    }
    val ratioValue = String.format(java.util.Locale.getDefault(), "%.1f", state.shocksPerWar)

    StatCard(title = stringResource(R.string.opponent_detail_streaks_scores)) {
        // L1
        StreakRow {
            StreakCell(scoreLabel, scoreValue, scoreColor)
            StreakCell(stringResource(R.string.opponent_detail_current_streak), streakLabel(stats.currentStreak), streakColor(stats.currentStreak))
        }
        Spacer(Modifier.height(8.dp))
        // L2
        StreakRow {
            StreakCell(stringResource(R.string.opponent_detail_record_wins), "${stats.bestWinStreak} ${stringResource(R.string.v)}", Colors.green)
            StreakCell(stringResource(R.string.opponent_detail_record_losses), "${stats.worstLossStreak} ${stringResource(R.string.d)}", Colors.red)
        }
        Spacer(Modifier.height(8.dp))
        // L3 — cellules avec illustration shock à gauche.
        StreakRow {
            ShockCell(stringResource(R.string.stats_shocks_played), state.shockCount.toString())
            ShockCell(stringResource(R.string.stats_shocks_per_war), ratioValue)
        }
    }
}

@Composable
private fun ColumnScope.StreakRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

/** Cellule valeur + libellé (fond translucide), même style que StatTiles. */
@Composable
private fun RowScope.StreakCell(label: String, value: String, valueColor: Color) {
    Column(Modifier.weight(1f).background(Colors.white30, RoundedCornerShape(6.dp)).padding(10.dp)) {
        MKText(text = value, font = Fonts.Urbanist, textColor = valueColor, fontSize = 18, textAlign = TextAlign.Start, maxLines = 1)
        MKText(text = label, textColor = Colors.white70, fontSize = 10, textAlign = TextAlign.Start, maxLines = 2, modifier = Modifier.padding(top = 6.dp))
    }
}

/** Cellule « shock » : illustration éclair à gauche (centrée verticalement) + valeur/libellé. */
@Composable
private fun RowScope.ShockCell(label: String, value: String) {
    Row(
        Modifier.weight(1f).background(Colors.white30, RoundedCornerShape(6.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(painter = painterResource(R.drawable.shock), contentDescription = null, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            MKText(text = value, font = Fonts.Urbanist, textColor = Colors.white, fontSize = 18, textAlign = TextAlign.Start, maxLines = 1)
            MKText(text = label, textColor = Colors.white70, fontSize = 10, textAlign = TextAlign.Start, maxLines = 2, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

/**
 * Sélecteur de tri des circuits (Occurrences / Winrate / Score moy., rules 15/16). [onDark] =
 * carte sombre (fiche) ; false = fond clair (écran complet).
 */
@Composable
internal fun TracksSortSelector(sort: SortType, onDark: Boolean, onSelect: (Int) -> Unit) {
    MKSegmentedSelector(
        items = listOf(
            stringResource(R.string.rankings_sort_occurrences),
            stringResource(R.string.rankings_sort_winrate),
            stringResource(R.string.rankings_sort_score)
        ),
        page = sort.ordinal,
        onDark = onDark,
        onClick = onSelect
    )
}

/**
 * Circuit → entrée de podium. Vue Équipe : écart d'équipe (`trackScoreToDiff`) ; vue Indiv :
 * position moyenne (`pointsToPosition`) — #67. Partagé avec [OpponentTracksRankingScreen].
 */
internal fun TrackStats.toPodiumEntry(isIndiv: Boolean): PodiumEntry {
    val map = map?.firstOrNull()
    val (scoreLabel, scoreValue) = when {
        isIndiv -> R.string.average_position_short to
                (playerScore.pointsToPosition(false).firstOrNull()?.toString() ?: "-")
        else -> R.string.form_score to (teamScore?.trackScoreToDiff(false) ?: "-")
    }
    return PodiumEntry(
        labelRes = map?.label,
        pictureRes = map?.picture,
        stats = listOf(
            R.string.times_played_short to totalPlayed.toString(),
            R.string.form_winrate to "${winRate ?: 0}%",
            scoreLabel to scoreValue
        )
    )
}

/**
 * Pilote → entrée de podium : Nb joué / Winrate / Position moy. (#67), sur les manches contre
 * cet adversaire. Partagé fiche ↔ [OpponentPilotsRankingScreen].
 */
internal fun OpponentDetailViewModel.PilotRanking.toPodiumEntry(): PodiumEntry =
    PodiumEntry(
        name = player.name.displayName,
        initials = initialsOf(player.name.displayName),
        // Photo si dispo (#50 pt.4), sinon initiales.
        avatar = player.avatar,
        avatarColor = playerAvatarColor(player.id),
        stats = listOf(
            R.string.times_played_short to played.toString(),
            R.string.form_winrate to "$winrate%",
            R.string.average_position_short to averagePosition.toString()
        )
    )

/**
 * Baggeur → entrée de podium (#69) : Nb joué / Shocks / % shocks (ses shocks / total équipe face
 * à eux). Partagé fiche ↔ [OpponentBaggersRankingScreen].
 */
internal fun OpponentDetailViewModel.BaggerRanking.toPodiumEntry(): PodiumEntry =
    PodiumEntry(
        name = player.name.displayName,
        initials = initialsOf(player.name.displayName),
        avatar = player.avatar,
        avatarColor = playerAvatarColor(player.id),
        stats = listOf(
            R.string.times_played_short to played.toString(),
            R.string.stats_bag_share_short to shockCount.toString(),
            R.string.stats_bag_share_pct to "$shockShare%"
        )
    )
