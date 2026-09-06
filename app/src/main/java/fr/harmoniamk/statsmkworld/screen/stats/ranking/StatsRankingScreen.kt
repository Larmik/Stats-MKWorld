package fr.harmoniamk.statsmkworld.screen.stats.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.extension.displayName
import fr.harmoniamk.statsmkworld.extension.trackScoreToDiff
import fr.harmoniamk.statsmkworld.screen.stats.StatsType
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKSeasonDropdown
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.MKTextField
import fr.harmoniamk.statsmkworld.ui.cells.playerAvatarColor
import fr.harmoniamk.statsmkworld.ui.stats.PodiumEntry
import fr.harmoniamk.statsmkworld.ui.stats.StatCardRadius
import fr.harmoniamk.statsmkworld.ui.stats.initialsOf
import fr.harmoniamk.statsmkworld.ui.stats.podiumRows

/**
 * Pôle Classements (#26) — sous-onglets Joueurs / Adversaires / Circuits. Palmarès triable,
 * cherchable, avec un curseur « occurrences minimum ». Chaque ligne mène à sa fiche (cellules
 * `PodiumCell` mutualisées). Onglet Joueurs sectionné Membres / Alliés.
 *
 * Navigation : `PlayerStats` → `StatsFullScreen(showTabs = false)` du joueur (#65) ;
 * `OpponentStats` / `MapStats` → fiches Adversaire/Circuit (#27), routées dans `RootScreen`.
 */
@Composable
fun StatsRankingScreen(
    viewModel: StatsRankingViewModel,
    onStats: (StatsType) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val is24p = state.is24PEnabled == true

    // padding bas = hauteur de la bottom bar (rule 17, 90.dp).
    BaseScreen(
        title = stringResource(R.string.classements),
        modifier = Modifier.padding(bottom = 90.dp),
        // Sélecteur de saison (#70, MKSeasonDropdown partagé rule 16). Change l'état VM ⇒
        // recalcul à la volée (rule 11, pas de re-nav).
        headerTrailing = {
            MKSeasonDropdown(
                seasons = state.seasons,
                selectedSeasonNumber = state.selectedSeasonNumber,
                onSeasonSelected = viewModel::onSeasonSelected
            )
        }
    ) {
        // Label « Palmarès triable » retiré (#50 pt.5) : la fonction est évidente.
        MKSegmentedSelector(
            items = listOf(
                stringResource(R.string.rankings_tab_players),
                stringResource(R.string.rankings_tab_opponents),
                stringResource(R.string.rankings_tab_tracks)
            ),
            page = state.tab.ordinal,
            onClick = viewModel::onTabSelected
        )
        Spacer(Modifier.height(6.dp))

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
        Spacer(Modifier.height(6.dp))

        // Chips de tri : COUNT (défaut, libellé variable Participation/Occurrences/Fréquence),
        // Winrate, Score moy. — ordre calé sur SortType.entries. Joueurs : COUNT = participation (#78).
        MKSegmentedSelector(
            items = listOf(
                stringResource(
                    when (state.tab) {
                        RankingTab.PLAYERS -> R.string.participation_rate_short
                        RankingTab.OPPONENTS -> R.string.rankings_sort_occurrences
                        RankingTab.TRACKS -> R.string.rankings_sort_frequency
                    }
                ),
                stringResource(R.string.rankings_sort_winrate),
                stringResource(R.string.rankings_sort_score)
            ),
            page = state.sort.ordinal,
            onClick = viewModel::onSortSelected
        )
        Spacer(Modifier.height(11.dp))

        // Curseur « occurrences minimum » (min = 1, max = plus haut compteur de l'onglet).
        MinOccurrencesSlider(
            value = state.minOccurrences,
            max = state.maxOccurrences,
            onChange = viewModel::onMinOccurrencesChange
        )

        // Zone de données : au changement de saison (#73), seul ce bloc passe en chargement ;
        // le header et les sélecteurs restent affichés.
        when {
            state.loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            // Liste par onglet — PodiumCell (3/ligne) sur cadre transparent-noir (#50 pt.7).
            else -> LazyColumn(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(StatCardRadius)
                    .background(Colors.blackAlphaed, StatCardRadius)
                    .border(1.dp, Colors.whiteBorder, StatCardRadius)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when (state.tab) {
                    RankingTab.PLAYERS -> state.playerSections.forEach { section ->
                        item(key = "section-${section.titleRes}") { SectionHeader(stringResource(section.titleRes)) }
                        podiumRows(section.players.map { it.toPodiumEntry() }, contentColor = Colors.white) { player ->
                            onStats(StatsType.PlayerStats(player.player.id, is24p = is24p))
                        }
                    }

                    RankingTab.OPPONENTS -> podiumRows(state.opponents.map { it.toPodiumEntry() }, contentColor = Colors.white) { opponent ->
                        // Propage la saison active (#91 pt.5) → la fiche détail est filtrée comme le classement.
                        onStats(StatsType.OpponentStats(teamId = opponent.team.id, is24p = is24p, seasonNumber = state.selectedSeasonNumber))
                    }

                    RankingTab.TRACKS -> podiumRows(state.tracks.map { it.toPodiumEntry(is24p) }, contentColor = Colors.white) { track ->
                        onStats(
                            StatsType.MapStats(
                                trackIndex = track.stats.map?.map { it.ordinal },
                                is24p = is24p,
                                // Propage la saison active (#91 pt.5).
                                seasonNumber = state.selectedSeasonNumber
                            )
                        )
                    }
                }
            }
        }
    }
}

// Entrées PodiumEntry (grille `podiumRows` mutualisée — ui/stats/PodiumGrid.kt)
private fun RankingItem.PlayerRanking.toPodiumEntry(): Pair<PodiumEntry, RankingItem.PlayerRanking> =
    PodiumEntry(
        name = player.name.displayName,
        initials = initialsOf(player.name.displayName),
        // Photo si dispo (#50 pt.4), sinon initiales.
        avatar = player.avatar,
        avatarColor = playerAvatarColor(player.id),
        stats = listOf(
            R.string.times_played_short to warsPlayedLabel,
            // Taux de participation (#78).
            R.string.participation_rate_short to participationRateLabel,
            R.string.form_winrate to winrateLabel,
            R.string.form_score to averageLabel
        )
    ) to this

private fun RankingItem.OpponentRanking.toPodiumEntry(): Pair<PodiumEntry, RankingItem.OpponentRanking> =
    // Rule 12 : nom/tag du roster, logo de l'équipe (non résolu déjà dégradé côté données).
    PodiumEntry(
        name = team.name,
        logo = team.logo,
        stats = listOf(
            R.string.times_played_short to warsPlayedLabel,
            R.string.form_winrate to winrateLabel,
            R.string.form_score to averageLabel
        )
    ) to this

private fun RankingItem.TrackRanking.toPodiumEntry(is24p: Boolean): Pair<PodiumEntry, RankingItem.TrackRanking> {
    val map = stats.map?.firstOrNull()
    val scoreValue = when (is24p) {
        true -> stats.teamScore?.toString() ?: "-"
        else -> stats.teamScore?.trackScoreToDiff(false) ?: "-"
    }
    return PodiumEntry(
        labelRes = map?.label,
        pictureRes = map?.picture,
        stats = listOf(
            R.string.times_played_short to stats.totalPlayed.toString(),
            R.string.form_winrate to "${stats.winRate ?: 0}%",
            R.string.form_score to scoreValue
        )
    ) to this
}

// Composants locaux
/** En-tête de section (Membres / Alliés) sur l'onglet Joueurs. */
@Composable
private fun SectionHeader(text: String) {
    MKText(
        text = text.uppercase(),
        font = Fonts.NunitoBD,
        textColor = Colors.white,
        fontSize = 13,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp)
    )
}

/**
 * Curseur « occurrences minimum » : `Slider` Material3 continu (`steps = 0`, ticks transparents),
 * libellé de valeur au-dessus, léger padding horizontal (pouce au min entièrement visible).
 * Masqué si max ≤ 1 (rien à filtrer).
 */
@Composable
private fun ColumnScope.MinOccurrencesSlider(value: Int, max: Int, onChange: (Int) -> Unit) {
    if (max <= 1) return
    MKText(
        text = stringResource(R.string.rankings_min_occurrences, value),
        textColor = Colors.black,
        font = Fonts.NunitoBD,
        fontSize = 12,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )
    Slider(
        value = value.toFloat(),
        onValueChange = { onChange(it.toInt()) },
        valueRange = 1f..max.toFloat(),
        steps = 0, // piste continue, aucune graduation
        colors = SliderDefaults.colors(
            thumbColor = Colors.white,
            activeTrackColor = Colors.green,
            inactiveTrackColor = Colors.blackAlphaed,
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent
        ),
        // Zone tactile contrainte à 24 dp (défaut ~48) pour resserrer la barre (#50 pt.5).
        modifier = Modifier.fillMaxWidth().height(24.dp).padding(horizontal = 6.dp)
    )
    Spacer(Modifier.height(6.dp))
}
