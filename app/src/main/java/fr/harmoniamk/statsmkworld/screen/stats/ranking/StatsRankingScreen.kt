package fr.harmoniamk.statsmkworld.screen.stats.ranking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.extension.trackScoreToDiff
import fr.harmoniamk.statsmkworld.screen.stats.StatsType
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.MKTextField
import fr.harmoniamk.statsmkworld.ui.stats.PodiumEntry
import fr.harmoniamk.statsmkworld.ui.stats.initialsOf
import fr.harmoniamk.statsmkworld.ui.stats.podiumRows

/**
 * Pôle Classements (#26) — écran unique à sous-onglets Joueurs / Adversaires /
 * Circuits (plus de menu intermédiaire). Palmarès triable (Winrate défaut / Score
 * moy. / compteur), cherchable, avec un **curseur « occurrences minimum »** (wars pour
 * Joueurs/Adversaires, maps pour Circuits) filtrant la liste. Chaque ligne mène à sa
 * fiche statistique. Cellules mutualisées avec les podiums de `StatsFullScreen`
 * (`PodiumCell`). L'onglet Joueurs est **sectionné** Membres / Alliés.
 *
 * Navigation vers les fiches : `StatsType.PlayerStats` → écran Stats joueur ;
 * `OpponentStats` / `MapStats` → fiches dédiées Adversaire/Circuit (#27), routées par
 * type dans `RootScreen` (`Opponent/{teamId}`, `Map/{trackIndex}`).
 */
@Composable
fun StatsRankingScreen(
    viewModel: StatsRankingViewModel,
    onStats: (StatsType) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val is24p = state.is24PEnabled == true

    // padding bas = hauteur de la bottom bar des 5 pôles, pour que le dernier élément de
    // la grille reste visible au-dessus d'elle (même valeur/approche que Accueil/Stats : 90.dp).
    BaseScreen(title = stringResource(R.string.classements), modifier = Modifier.padding(bottom = 90.dp)) {
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

        // Chips de tri (3 par onglet) : le chip d'OCCURRENCES est en 1ʳᵉ position et
        // sélectionné par défaut (libellé variable Wars / Occurrences / Fréquence),
        // suivi de Winrate puis Score moy. — ordre calé sur SortType.entries (COUNT en 0).
        MKSegmentedSelector(
            items = listOf(
                stringResource(
                    when (state.tab) {
                        RankingTab.PLAYERS -> R.string.rankings_sort_wars
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

        // Liste par onglet — cellules PodiumCell (3 par ligne).
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when (state.tab) {
                RankingTab.PLAYERS -> state.playerSections.forEach { section ->
                    item(key = "section-${section.titleRes}") { SectionHeader(stringResource(section.titleRes)) }
                    podiumRows(section.players.map { it.toPodiumEntry() }) { player ->
                        onStats(StatsType.PlayerStats(player.player.id, is24p = is24p))
                    }
                }

                RankingTab.OPPONENTS -> podiumRows(state.opponents.map { it.toPodiumEntry() }) { opponent ->
                    onStats(StatsType.OpponentStats(teamId = opponent.team.id, is24p = is24p))
                }

                RankingTab.TRACKS -> podiumRows(state.tracks.map { it.toPodiumEntry(is24p) }) { track ->
                    onStats(
                        StatsType.MapStats(
                            trackIndex = track.stats.map?.map { it.ordinal },
                            is24p = is24p
                        )
                    )
                }
            }
        }
    }
}

// =====================================================================
// Entrées PodiumEntry (grille `podiumRows` mutualisée — ui/stats/PodiumGrid.kt)
// =====================================================================

private fun RankingItem.PlayerRanking.toPodiumEntry(): Pair<PodiumEntry, RankingItem.PlayerRanking> =
    PodiumEntry(
        name = player.name,
        initials = initialsOf(player.name),
        stats = listOf(
            R.string.times_played_short to warsPlayedLabel,
            R.string.form_winrate to winrateLabel,
            R.string.form_score to averageLabel
        )
    ) to this

private fun RankingItem.OpponentRanking.toPodiumEntry(): Pair<PodiumEntry, RankingItem.OpponentRanking> =
    // Rule 12 : nom/tag du roster (porté par TeamEntity), avatar de l'équipe (logo), et
    // adversaire non résolu déjà dégradé en « Équipe inconnue » côté données (non effacé).
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

// =====================================================================
// Composants locaux
// =====================================================================

/** En-tête de section (Membres / Alliés) sur l'onglet Joueurs. */
@Composable
private fun SectionHeader(text: String) {
    MKText(
        text = text.uppercase(),
        font = Fonts.NunitoBD,
        textColor = Colors.black,
        fontSize = 13,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp)
    )
}

/**
 * Curseur « occurrences minimum » : `Slider` Material3 **continu** (piste sans
 * graduations : pas de tick marks — `steps = 0` + `tickColors` transparents), **pouce
 * cercle plein blanc opaque**, piste active verte. Le libellé de valeur est placé
 * **au-dessus** (pas à côté), et le Slider a un léger padding horizontal, pour que le
 * pouce en position minimale reste **entièrement visible** (pas de rognage à gauche).
 * Masqué si le max ne dépasse pas 1 (rien à filtrer).
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
    )
    Spacer(Modifier.height(11.dp))
}
