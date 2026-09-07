package fr.harmoniamk.statsmkworld.screen.stats.map

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.extension.displayName
import fr.harmoniamk.statsmkworld.extension.trackScoreToDiff
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.stats.BalanceCard
import fr.harmoniamk.statsmkworld.ui.stats.PodiumEntry
import fr.harmoniamk.statsmkworld.ui.stats.PodiumSectionCard
import fr.harmoniamk.statsmkworld.ui.stats.StatCard
import fr.harmoniamk.statsmkworld.ui.stats.StatHeaderCard
import fr.harmoniamk.statsmkworld.ui.stats.StatTile
import fr.harmoniamk.statsmkworld.ui.stats.StatTiles
import fr.harmoniamk.statsmkworld.ui.cells.playerAvatarColor
import fr.harmoniamk.statsmkworld.ui.stats.initialsOf
import fr.harmoniamk.statsmkworld.ui.stats.mapStatsDetailSections
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

/**
 * Fiche détail circuit (#27). Sélecteur Indiv/Équipe (rule 11 : état réactif du VM). Sections :
 * en-tête, Performance (winrate de manche + V/N/D), Scores moyens (équipe + position joueur +
 * shocks), Répartition + Top/Bot 2→6 (mutualisées), et Pilotes/Baggeurs/Adversaires du circuit
 * (podiums, mode Équipe uniquement). Rules 13/15/16.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Composable
fun MapDetailScreen(
    viewModel: MapDetailViewModel,
    onBack: () -> Unit,
    onPilotsRanking: () -> Unit,
    onBaggersRanking: () -> Unit,
    onOpponentsRanking: () -> Unit
) {
    BackHandler { onBack() }
    val state by viewModel.state.collectAsStateWithLifecycle()

    BaseScreen(title = stringResource(R.string.map_detail_title), onBack = onBack) {
        when {
            state.loading -> CircularProgressIndicator()
            state.mapStats == null -> MKText(
                text = stringResource(R.string.stats_no_data),
                textColor = Colors.white66,
                fontSize = 13
            )
            else -> {
                val mapStats = state.mapStats!!
                val map = state.maps.firstOrNull()
                // Sélecteur Indiv/Équipe (rule 15 : composant partagé) — sur fond clair (onDark false).
                MKSegmentedSelector(
                    items = listOf(
                        stringResource(R.string.map_detail_scope_indiv),
                        stringResource(R.string.map_detail_scope_team)
                    ),
                    page = if (state.isIndiv) 0 else 1,
                    onClick = { index -> viewModel.onModeChange(index == 0) }
                )
                Spacer(Modifier.height(11.dp))
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    // 1. En-tête (pastille illustration + nom + « joué N fois »).
                    item {
                        StatHeaderCard(
                            name = map?.label?.let { stringResource(it) } ?: "-",
                            subtitle = stringResource(R.string.map_detail_header, mapStats.trackPlayed),
                            color = Colors.purple,
                            pictureRes = map?.picture
                        )
                    }
                    // 2. Performance (winrate de manche + V/N/D).
                    item {
                        val played = mapStats.trackPlayed.takeIf { it > 0 } ?: 1
                        BalanceCard(
                            title = stringResource(R.string.map_detail_performance),
                            winrate = (mapStats.trackWon * 100) / played,
                            won = mapStats.trackWon,
                            tied = mapStats.trackTie,
                            loss = mapStats.trackLoss,
                            subtitle = stringResource(R.string.map_detail_winrate_sub, mapStats.trackPlayed)
                        )
                    }
                    // 3. Scores moyens : score équipe + position joueur (fixes) + shocks (suit le mode).
                    item {
                        StatCard(title = stringResource(R.string.map_detail_avg_scores)) {
                            StatTiles(
                                tiles = listOf(
                                    StatTile(
                                        label = stringResource(R.string.map_detail_team_score),
                                        // Score équipe affiché en ÉCART de points par manche (#67).
                                        value = state.teamScore.trackScoreToDiff(false)
                                    ),
                                    StatTile(
                                        label = stringResource(R.string.map_detail_player_position),
                                        value = state.playerPositionLabel
                                    ),
                                    StatTile(
                                        label = stringResource(R.string.stats_shocks_played),
                                        value = state.shockCount.toString()
                                    )
                                )
                            )
                        }
                    }
                    // 4. Sections détaillées mutualisées : répartition des positions, Top/Bot 2→6.
                    mapStatsDetailSections(mapStats)
                    // 5. Pilotes (membres) — mode Équipe uniquement.
                    if (!state.isIndiv && state.pilots.isNotEmpty()) item {
                        PodiumSectionCard(
                            title = stringResource(R.string.map_detail_pilots),
                            top = state.pilots.take(3).map { it.toPodiumEntry() },
                            flop = state.pilots.takeLast(3).reversed().map { it.toPodiumEntry() },
                            onSeeAll = onPilotsRanking
                        )
                    }
                    // 5bis. Baggeurs sur ce circuit (#69) — mode Équipe uniquement.
                    if (!state.isIndiv && state.baggers.isNotEmpty()) item {
                        PodiumSectionCard(
                            title = stringResource(R.string.map_detail_baggers),
                            top = state.baggers.take(3).map { it.toPodiumEntry() },
                            flop = state.baggers.takeLast(3).reversed().map { it.toPodiumEntry() },
                            onSeeAll = onBaggersRanking
                        )
                    }
                    // 6. Adversaires rencontrés sur ce circuit — mode Équipe uniquement (#67).
                    if (!state.isIndiv && state.opponents.isNotEmpty()) item {
                        PodiumSectionCard(
                            title = stringResource(R.string.map_detail_opponents),
                            top = state.opponents.take(3).map { it.toPodiumEntry() },
                            flop = state.opponents.takeLast(3).reversed().map { it.toPodiumEntry() },
                            onSeeAll = onOpponentsRanking
                        )
                    }
                    item { Spacer(Modifier.height(90.dp)) }
                }
            }
        }
    }
}

/**
 * Pilote → entrée de podium : Nb joué / Winrate / Position moy. (#67). Le tri du classement
 * reste `averageScore`. Partagé fiche ↔ [MapPilotsRankingScreen].
 */
internal fun MapDetailViewModel.PilotRanking.toPodiumEntry(): PodiumEntry =
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
 * Baggeur → entrée de podium (#69) : Nb joué / Shocks / % shocks (ses shocks sur ce circuit /
 * total équipe). Partagé fiche ↔ [MapBaggersRankingScreen].
 */
internal fun MapDetailViewModel.BaggerRanking.toPodiumEntry(): PodiumEntry =
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

/**
 * Adversaire → entrée de podium : nb joué / winrate / écart d'équipe (`trackScoreToDiff`, #67).
 * Partagé fiche ↔ [MapOpponentsRankingScreen].
 */
internal fun MapDetailViewModel.OpponentRanking.toPodiumEntry(): PodiumEntry =
    PodiumEntry(
        // Rule 12 : nom du roster + logo de l'équipe parente.
        name = team.name,
        logo = team.logo,
        stats = listOf(
            R.string.times_played_short to played.toString(),
            R.string.form_winrate to "$winrate%",
            R.string.form_score to averageTeamScore.trackScoreToDiff(false)
        )
    )

