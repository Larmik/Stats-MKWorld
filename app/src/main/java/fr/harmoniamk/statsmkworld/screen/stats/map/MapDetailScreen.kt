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
 * Fiche détail CIRCUIT (`map` du prototype, pôle Classements #27). Sélecteur Indiv/Équipe
 * (rule 11 : bascule un état réactif du VM, l'écran reste monté). Sections :
 * 1. Sélecteur Indiv/Équipe + en-tête (nom, nb de fois joué) ;
 * 2. Performance (winrate de manche coloré selon seuil + V/N/D + barre) ;
 * 3. Scores moyens — score moyen ÉQUIPE + position moyenne JOUEUR (fixes, indépendants du
 *    mode) + shocks joués (dynamique) ;
 * 4. Répartition des positions + Top/Bot 2→6 (sections détaillées mutualisées, mode-scopées) ;
 * 5. Pilotes sur ce circuit (podium Top3/Flop3 par score moyen, MEMBRES uniquement +
 *    « Voir le classement en entier ») — **mode Équipe uniquement**.
 *
 * Rendu pixel-perfect maquette (rules 13/15), cartes/podiums partagés (rule 16), données réelles.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Composable
fun MapDetailScreen(
    viewModel: MapDetailViewModel,
    onBack: () -> Unit,
    onPilotsRanking: () -> Unit
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
                    // 3. Scores moyens : score ÉQUIPE + position JOUEUR (FIXES, indépendants
                    //    du mode) + shocks joués (DYNAMIQUE, suit le mode).
                    item {
                        StatCard(title = stringResource(R.string.map_detail_avg_scores)) {
                            StatTiles(
                                tiles = listOf(
                                    StatTile(
                                        label = stringResource(R.string.map_detail_team_score),
                                        value = state.teamScore.toString()
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
                    // 5. Classement des pilotes (MEMBRES) — mode ÉQUIPE uniquement (point 8).
                    if (!state.isIndiv && state.pilots.isNotEmpty()) item {
                        PodiumSectionCard(
                            title = stringResource(R.string.map_detail_pilots),
                            top = state.pilots.take(3).map { it.toPodiumEntry() },
                            flop = state.pilots.takeLast(3).reversed().map { it.toPodiumEntry() },
                            onSeeAll = onPilotsRanking
                        )
                    }
                    item { Spacer(Modifier.height(90.dp)) }
                }
            }
        }
    }
}

/**
 * Pilote → entrée de podium. Stat principale = **score perso moyen** (identique au critère
 * de TRI — transparence), puis position moyenne et nombre de manches jouées en infos
 * secondaires. Partagé entre la fiche (podium Top3/Flop3) et le classement complet
 * [MapPilotsRankingScreen].
 */
internal fun MapDetailViewModel.PilotRanking.toPodiumEntry(): PodiumEntry =
    PodiumEntry(
        name = player.name,
        initials = initialsOf(player.name),
        // Photo de profil MKCentral si dispo (#50 pt.4), sinon initiales sur pastille colorée.
        avatar = player.avatar,
        avatarColor = playerAvatarColor(player.id),
        stats = listOf(
            R.string.form_score to averageScore.toString(),
            R.string.average_position_short to averagePosition.toString(),
            R.string.times_played_short to played.toString()
        )
    )

