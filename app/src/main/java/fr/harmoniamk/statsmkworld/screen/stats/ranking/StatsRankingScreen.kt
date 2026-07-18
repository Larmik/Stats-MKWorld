package fr.harmoniamk.statsmkworld.screen.stats.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.screen.stats.StatsType
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.MKTextField
import fr.harmoniamk.statsmkworld.ui.VerticalGrid
import fr.harmoniamk.statsmkworld.ui.cells.MapCell
import fr.harmoniamk.statsmkworld.ui.cells.PlayerCell
import fr.harmoniamk.statsmkworld.ui.cells.TeamCell

private val CardRadius = RoundedCornerShape(6.dp)

/**
 * Pôle Classements (#26) — écran unique à sous-onglets Joueurs / Adversaires /
 * Circuits (plus de menu intermédiaire). Palmarès triable (Winrate défaut / Score
 * moy. / compteur) et cherchable ; chaque ligne mène à sa fiche statistique. Onglets
 * Adversaires/Circuits : carte « En bref » (meilleur/pire winrate, seuil échantillon).
 *
 * Navigation vers les fiches : réutilise l'existant (`StatsType.PlayerStats` /
 * `OpponentStats` / `MapStats`) tant que les fiches dédiées adversaire/circuit (#27)
 * ne sont pas créées.
 */
@Composable
fun StatsRankingScreen(
    viewModel: StatsRankingViewModel,
    onStats: (StatsType) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val is24p = state.is24PEnabled == true

    BaseScreen(title = stringResource(R.string.classements)) {
        MKText(
            text = stringResource(R.string.rankings_hint),
            textColor = Colors.white66,
            fontSize = 12,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
        )
        MKSegmentedSelector(
            items = listOf(
                stringResource(R.string.rankings_tab_players),
                stringResource(R.string.rankings_tab_opponents),
                stringResource(R.string.rankings_tab_tracks)
            ),
            page = state.tab.ordinal,
            onClick = viewModel::onTabSelected
        )
        Spacer(Modifier.height(11.dp))

        // Carte « En bref » (adversaires / circuits uniquement).
        state.bestInsight?.let { best ->
            InsightCard(best = best, worst = state.worstInsight)
            Spacer(Modifier.height(11.dp))
        }

        // Recherche.
        MKTextField(
            value = state.search,
            backgroundColor = Colors.blackAlphaed,
            onValueChange = viewModel::onSearch,
            placeHolderRes = when (state.tab) {
                RankingTab.PLAYERS -> R.string.rankings_search_player
                RankingTab.OPPONENTS -> R.string.rankings_search_opponent
                RankingTab.TRACKS -> R.string.rankings_search_track
            }
        )
        Spacer(Modifier.height(11.dp))

        // Chips de tri (3 par onglet, Winrate défaut). Le 3ᵉ libellé change selon l'onglet.
        MKSegmentedSelector(
            items = listOf(
                stringResource(R.string.rankings_sort_winrate),
                stringResource(R.string.rankings_sort_score),
                stringResource(
                    when (state.tab) {
                        RankingTab.PLAYERS -> R.string.rankings_sort_wars
                        RankingTab.OPPONENTS -> R.string.rankings_sort_occurrences
                        RankingTab.TRACKS -> R.string.rankings_sort_frequency
                    }
                )
            ),
            page = state.sort.ordinal,
            onClick = viewModel::onSortSelected
        )
        Spacer(Modifier.height(11.dp))

        val listModifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
        when (state.tab) {
            RankingTab.PLAYERS -> VerticalGrid(listModifier) {
                state.players.forEach { player ->
                    PlayerCell(
                        modifier = Modifier.padding(5.dp).fillMaxWidth(0.48f),
                        textColor = Colors.white,
                        backgroundColor = Colors.blackAlphaed,
                        onClick = { onStats(StatsType.PlayerStats(player.player.id, is24p = is24p)) },
                        playerRanking = player,
                        player = null
                    )
                }
            }

            RankingTab.OPPONENTS -> VerticalGrid(listModifier) {
                state.opponents.forEach { opponent ->
                    TeamCell(
                        modifier = Modifier.padding(5.dp).fillMaxWidth(0.48f),
                        teamRanking = opponent,
                        team = null,
                        onClick = {
                            onStats(
                                StatsType.OpponentStats(
                                    teamId = opponent.team.id,
                                    is24p = is24p
                                )
                            )
                        }
                    )
                }
            }

            RankingTab.TRACKS -> VerticalGrid(listModifier) {
                state.tracks.forEach { track ->
                    MapCell(
                        modifier = Modifier.padding(5.dp).fillMaxWidth(0.48f),
                        trackRanking = track,
                        is24p = is24p,
                        onClick = { maps ->
                            onStats(
                                StatsType.MapStats(
                                    trackIndex = maps.map { it.ordinal },
                                    is24p = is24p
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * Carte « En bref » (insight) : deux blocs côte à côte — meilleur (vert) / pire (rouge),
 * chacun libellé + nom + winrate. Reprend le style `.insight` de la maquette.
 */
@Composable
private fun ColumnScope.InsightCard(
    best: StatsRankingViewModel.Insight,
    worst: StatsRankingViewModel.Insight?
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Colors.blackAlphaed, CardRadius)
            .border(1.dp, Colors.whiteBorder, CardRadius)
            .padding(13.dp)
    ) {
        MKText(
            text = stringResource(R.string.rankings_insight_title).uppercase(),
            fontSize = 12,
            font = Fonts.NunitoBD,
            textColor = Colors.white,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(bottom = 11.dp)
        )
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            InsightBlock(best, Colors.green)
            worst?.let { InsightBlock(it, Colors.red) }
        }
    }
}

@Composable
private fun RowScope.InsightBlock(insight: StatsRankingViewModel.Insight, valueColor: Color) {
    Column(
        Modifier
            .weight(1f)
            .background(Colors.white30, CardRadius)
            .padding(11.dp)
    ) {
        MKText(
            text = stringResource(insight.label).uppercase(),
            fontSize = 10,
            font = Fonts.NunitoBD,
            textColor = Colors.white66,
            textAlign = TextAlign.Start
        )
        MKText(
            text = insight.name,
            fontSize = 13,
            font = Fonts.NunitoBD,
            textColor = Colors.white,
            textAlign = TextAlign.Start,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp)
        )
        MKText(
            text = "${insight.winrate}%",
            fontSize = 16,
            font = Fonts.Urbanist,
            textColor = valueColor,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
