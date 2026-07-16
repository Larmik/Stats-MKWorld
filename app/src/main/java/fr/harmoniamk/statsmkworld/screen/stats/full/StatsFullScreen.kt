package fr.harmoniamk.statsmkworld.screen.stats.full

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
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.extension.positionColor
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKText

private val CardRadius = RoundedCornerShape(6.dp)

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

    val subtitle = when (viewModel.showTabs) {
        true -> null
        else -> state.value.playerName
    }
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
                        1 -> teamSections(state.value)
                        else -> individualSections(state.value, viewModel.showTabs, onResults)
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
    onResults: (() -> Unit)?
) {
    val stats = state.playerStats ?: return
    // 1. En-tête (seulement dans l'onglet, pas sur statsfull qui a déjà le sous-titre).
    if (showTabs) item {
        HeaderCard(
            name = state.playerName.orEmpty(),
            subtitle = stringResource(
                R.string.stats_player_subtitle,
                stats.warStats.warsPlayed,
                stats.warStats.list.sumOf { it.warTracks.size }
            ),
            color = Colors.blue
        )
    }
    // 2. Bilan.
    item { BalanceCard(stats, showResultsLink = !showTabs, onResults = onResults) }
    // 3. Indicateurs (tuiles).
    item {
        StatCard(stringResource(R.string.stats_player_indicators)) {
            Tiles(
                Tile(stats.averagePoints.toString(), stringResource(R.string.stats_points_per_war)),
                Tile(stats.averagePlayerPosLabel, stringResource(R.string.average_position_short)),
                Tile(stats.scoreStdDev?.let { "±$it" } ?: "-", stringResource(R.string.stats_regularity), isNew = true),
                Tile(shocksPerWar(stats), stringResource(R.string.shocks_per_war_short)),
                Tile(state.bestCourse?.name ?: "-", stringResource(R.string.stats_best_course), valueColor = Colors.green),
                Tile(state.worstCourse?.name ?: "-", stringResource(R.string.stats_worst_course), valueColor = Colors.red)
            )
        }
    }
    // 4. Contribution.
    stats.playerContribution?.let { contribution ->
        item {
            StatCard(stringResource(R.string.stats_contribution_title), isNew = true) {
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
    // 5. Forme & séries.
    item { FormStreakCard(stats, stringResource(R.string.stats_player_form_title)) }
    // 6. Distribution des positions.
    stats.positionDistribution.takeIf { it.any { entry -> entry.second > 0 } }?.let { distribution ->
        item {
            StatCard(stringResource(R.string.stats_distribution_title), isNew = true) {
                DistributionChart(distribution)
                DistributionFooter(stats)
            }
        }
    }
    // 7. Rythme de war.
    if (stats.firstHalfAvgPosition != null && stats.secondHalfAvgPosition != null) item {
        StatCard(stringResource(R.string.stats_pace_title), isNew = true) {
            PaceRow(
                stats.firstHalfAvgPosition.toString(),
                stats.secondHalfAvgPosition.toString()
            )
        }
    }
    // 8. Tes circuits.
    item {
        StatCard(stringResource(R.string.stats_player_tracks_title), isNew = true) {
            TwoTiles(
                left = { TrackBlock(stringResource(R.string.stats_best_winrate_perso), state.bestPlayerTrack, Colors.green) },
                right = { TrackBlock(stringResource(R.string.stats_worst), state.worstPlayerTrack, Colors.red) }
            )
        }
    }
    // 9. Comparatif 12/24.
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
    // statsfull : adversaires + circuits (variante pour un joueur donné).
    if (!showTabs) {
        item {
            StatCard(stringResource(R.string.statistiques_des_adversaires)) {
                TwoTiles(
                    left = { TileBlock(stringResource(R.string.stats_most_played), state.mostPlayedOpponent) },
                    right = { TileBlock(stringResource(R.string.stats_most_beaten), state.mostBeatenOpponent) }
                )
            }
        }
        item {
            StatCard(stringResource(R.string.stats_tracks_title)) {
                TwoTiles(
                    left = { TrackBlock(stringResource(R.string.stats_best), state.bestPlayerTrack, Colors.green) },
                    right = { TrackBlock(stringResource(R.string.stats_worst), state.worstPlayerTrack, Colors.red) }
                )
            }
        }
    }
}

// =====================================================================
// Onglet Équipe
// =====================================================================

private fun androidx.compose.foundation.lazy.LazyListScope.teamSections(state: StatsFullViewModel.State) {
    val stats = state.teamStats ?: return
    // 1. En-tête.
    item {
        HeaderCard(
            name = state.teamName.orEmpty(),
            subtitle = stringResource(R.string.stats_team_subtitle, stats.warStats.warsPlayed),
            color = Colors.purple
        )
    }
    // 2. Bilan équipe.
    item { BalanceCard(stats, showResultsLink = false, onResults = null) }
    // 3. Détails équipe (tuiles).
    item {
        StatCard(stringResource(R.string.stats_team_details)) {
            Tiles(
                Tile(stats.averagePoints.toString(), stringResource(R.string.form_score)),
                Tile(stats.mapsWon ?: "-", stringResource(R.string.maps_gagn_es)),
                Tile(stats.averageWinMargin?.let { "+$it" } ?: "-", stringResource(R.string.stats_avg_win_margin_short), isNew = true, valueColor = Colors.green)
            )
        }
    }
    // 4. Forme & séries équipe.
    item { FormStreakCard(stats, stringResource(R.string.stats_team_form_title)) }
    // 5. Contributeurs.
    if (state.contributors.isNotEmpty()) item {
        StatCard(stringResource(R.string.stats_contributors_title), isNew = true) {
            state.contributors.forEachIndexed { index, contributor ->
                ContributorRow(index + 1, contributor)
            }
        }
    }
    // 6. Adversaires.
    item {
        StatCard(stringResource(R.string.statistiques_des_adversaires)) {
            Tiles(
                Tile(state.mostPlayedOpponent?.name ?: "-", stringResource(R.string.stats_most_played), small = true),
                Tile(state.mostBeatenOpponent?.name ?: "-", stringResource(R.string.stats_most_beaten), small = true),
                Tile(state.leastBeatenOpponent?.name ?: "-", stringResource(R.string.stats_least_beaten), small = true)
            )
        }
    }
    // 7. Circuits équipe.
    item {
        StatCard(stringResource(R.string.stats_team_tracks_title)) {
            Tiles(
                Tile(state.teamMostPlayedTrack.trackName(), stringResource(R.string.stats_most_played), small = true),
                Tile(state.teamBestTrack.trackName(), stringResource(R.string.stats_best), small = true, valueColor = Colors.green),
                Tile(state.teamWorstTrack.trackName(), stringResource(R.string.stats_worst), small = true, valueColor = Colors.red)
            )
        }
    }
    // 8. Comparatif 12/24.
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
}

// =====================================================================
// Composants de carte (style maquette, réutilisés par les deux onglets)
// =====================================================================

/** Carte translucide (fond sombre, bordure blanche, radius 6, padding 13). */
@Composable
private fun StatCard(
    title: String? = null,
    isNew: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Colors.blackAlphaed, CardRadius)
            .border(1.dp, Colors.whiteBorder, CardRadius)
            .padding(13.dp)
    ) {
        title?.let { Eyebrow(it, isNew) }
        if (title != null) Spacer(Modifier.height(11.dp))
        content()
    }
}

/** Eyebrow (petit titre majuscule blanc) + pastille « Nouveau » optionnelle. */
@Composable
private fun Eyebrow(text: String, isNew: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MKText(
            text = text.uppercase(),
            fontSize = 12,
            font = Fonts.NunitoBD,
            textColor = Colors.white,
            textAlign = TextAlign.Start
        )
        if (isNew) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.clip(RoundedCornerShape(5.dp)).background(Colors.yellow).padding(horizontal = 6.dp, vertical = 2.dp)) {
                MKText(text = stringResource(R.string.stats_new_tag), font = Fonts.NunitoBD, textColor = Colors.black, fontSize = 9)
            }
        }
    }
}

/** En-tête : pastille (initiales) + nom (Bungee) + sous-titre. */
@Composable
private fun HeaderCard(name: String, subtitle: String, color: Color) {
    StatCard {
        Row(horizontalArrangement = Arrangement.spacedBy(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(color).border(2.dp, Colors.white85, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                MKText(text = initialsOf(name), font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 16)
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
    StatCard(title, isNew = true) {
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

// --- Tuiles ------------------------------------------------------------------

private data class Tile(
    val value: String,
    val label: String,
    val isNew: Boolean = false,
    val valueColor: Color = Colors.white,
    val small: Boolean = false
)

/** Grille de tuiles 3 par ligne (style maquette `.tiles`). */
@Composable
private fun ColumnScope.Tiles(vararg tiles: Tile) {
    tiles.toList().chunked(3).forEach { rowTiles ->
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rowTiles.forEach { tile -> TileCell(tile) }
            repeat(3 - rowTiles.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun RowScope.TileCell(tile: Tile) {
    Column(
        Modifier
            .weight(1f)
            .background(Colors.white30, CardRadius)
            .then(if (tile.isNew) Modifier.border(1.dp, Colors.yellow, CardRadius) else Modifier)
            .padding(10.dp)
    ) {
        MKText(text = tile.value, font = Fonts.Urbanist, textColor = tile.valueColor, fontSize = if (tile.small) 14 else 20, textAlign = TextAlign.Start, maxLines = 1)
        MKText(text = tile.label, textColor = Colors.white70, fontSize = 10, textAlign = TextAlign.Start, modifier = Modifier.padding(top = 6.dp))
    }
}

// --- Deux blocs côte à côte (`.two`) -----------------------------------------

@Composable
private fun ColumnScope.TwoTiles(
    left: @Composable ColumnScope.() -> Unit,
    right: @Composable ColumnScope.() -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Column(Modifier.weight(1f).background(Colors.white30, CardRadius).padding(11.dp), content = left)
        Column(Modifier.weight(1f).background(Colors.white30, CardRadius).padding(11.dp), content = right)
    }
}

@Composable
private fun ColumnScope.TrackBlock(label: String, tile: StatsFullViewModel.NamedTile?, color: Color) {
    MKText(text = label.uppercase(), textColor = Colors.white66, fontSize = 10, textAlign = TextAlign.Start)
    Row(Modifier.padding(top = 5.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        MKText(text = tile.trackName(), font = Fonts.Urbanist, textColor = color, fontSize = 13, textAlign = TextAlign.Start, maxLines = 1)
        tile?.value?.let { MKText(text = it, textColor = Colors.white55, fontSize = 10) }
    }
}

@Composable
private fun ColumnScope.TileBlock(label: String, tile: StatsFullViewModel.NamedTile?) {
    MKText(text = label.uppercase(), textColor = Colors.white66, fontSize = 10, textAlign = TextAlign.Start)
    MKText(text = tile?.name ?: "-", font = Fonts.Urbanist, textColor = Colors.white, fontSize = 13, textAlign = TextAlign.Start, maxLines = 1, modifier = Modifier.padding(top = 5.dp))
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
    StatCard(stringResource(R.string.stats_comparison_title), isNew = true) {
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
        Modifier.fillMaxWidth().height(116.dp).padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        distribution.forEach { (position, count) ->
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                MKText(text = count.toString(), font = Fonts.Urbanist, textColor = Colors.white70, fontSize = 8)
                Spacer(Modifier.height(2.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.06f + 0.9f * (count.toFloat() / max))
                        .clip(RoundedCornerShape(3.dp))
                        .background(position.positionColor())
                )
                MKText(text = position.toString(), font = Fonts.MKPosition, textColor = Colors.white55, fontSize = 8, modifier = Modifier.padding(top = 2.dp))
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

@Composable
private fun StatsFullViewModel.NamedTile?.trackName(): String = when {
    this == null -> "-"
    labelRes != null -> stringResource(labelRes)
    else -> name ?: "-"
}

private fun shocksPerWar(stats: Stats): String =
    stats.allTimeForm?.shocksPerWar?.let { String.format(java.util.Locale.getDefault(), "%.1f", it) } ?: "-"

private fun initialsOf(name: String?): String = name
    ?.trim()
    ?.split(" ", "_", "-")
    ?.filter { it.isNotBlank() }
    ?.take(2)
    ?.joinToString("") { it.first().uppercase() }
    ?.takeIf { it.isNotEmpty() }
    ?: "?"
