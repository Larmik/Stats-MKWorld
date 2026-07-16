package fr.harmoniamk.statsmkworld.screen.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.cells.CurrentWarCell
import fr.harmoniamk.statsmkworld.ui.cells.CurrentWarCellViewModel
import fr.harmoniamk.statsmkworld.ui.cells.WarCell
import fr.harmoniamk.statsmkworld.ui.cells.WarCellViewModel

@Composable
fun WelcomeScreen(
    viewModel: WelcomeViewModel = hiltViewModel(),
    onTeamProfile: () -> Unit,
    onAddWar: (Boolean) -> Unit,
    onCurrentWar: () -> Unit,
    onWarDetailsClick: (WarDetails) -> Unit,
    onWarListClick: () -> Unit,
    onSearch: () -> Unit
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    // État UI local : profil affiché (0 = Moi, 1 = Équipe) et fenêtre du Momentum
    // (0 = 5 dernières, 1 = 10 dernières). Survivent à la rotation.
    var profileIndex by rememberSaveable { mutableIntStateOf(0) }
    var windowIndex by rememberSaveable { mutableIntStateOf(0) }
    BaseScreen(title = stringResource(R.string.accueil), modifier = Modifier.padding(bottom = 90.dp), onSearch = onSearch) {

        when (state.value.playerName.isNullOrEmpty()) {
            true -> CircularProgressIndicator()
            else -> {
                // Vue sélectionnée : joueur (Moi) ou équipe. Deux jeux de Stats déjà
                // calculés côté VM → le switch ne recalcule rien.
                val selectedStats = when (profileIndex) {
                    0 -> state.value.playerStats
                    else -> state.value.teamStats
                }
                LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Carte de salutation (→ profil) + segmenté Moi/Équipe.
                item {
                    GreetingCard(
                        greeting = stringResource(R.string.home_greeting, state.value.playerName.orEmpty()),
                        subtitle = stringResource(R.string.home_profile_subtitle, state.value.teamName.orEmpty()),
                        image = state.value.playerLogo,
                        onClick = onTeamProfile
                    )
                    Spacer(Modifier.height(8.dp))
                    MKSegmentedSelector(
                        items = listOf(stringResource(R.string.home_scope_me), stringResource(R.string.home_scope_team)),
                        page = profileIndex,
                        onClick = { profileIndex = it }
                    )
                }

                // 2. War en cours (→ CurrentWar).
                state.value.currentWar?.let { war ->
                    item {
                        SectionEyebrow(stringResource(R.string.war_en_cours))
                        CurrentWarCell(onClick = onCurrentWar, viewModel = hiltViewModel(
                            key = war.id.toString() + war.tracks.joinToString { it.id.toString() },
                            creationCallback = { factory: CurrentWarCellViewModel.Factory ->
                                factory.create(war)
                            }
                        ))
                    }
                }

                // 3/4/5. Momentum + chiffres clés + série (selon le profil sélectionné).
                selectedStats?.let { stats ->
                    item {
                        MomentumCard(
                            stats = stats,
                            windowIndex = windowIndex,
                            onWindowChange = { windowIndex = it },
                            isPlayer = profileIndex == 0
                        )
                    }
                    item { KeyFiguresCard(stats = stats, isPlayer = profileIndex == 0) }
                    stats.currentStreak.takeIf { it != 0 }?.let { item { StreakBanner(stats) } }
                }

                // 6. Derniers résultats (→ WarDetails) + « Voir tout » (→ historique Wars).
                when (state.value.recentResults.isEmpty()) {
                    true -> item {
                        Column(Modifier.padding(top = 15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            MKText(text = stringResource(R.string.welcome_title), font = Fonts.NunitoBD, fontSize = 16)
                            MKText(text = stringResource(R.string.welcome_text), fontSize = 16)
                        }
                    }
                    else -> {
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                SectionEyebrow(stringResource(R.string.last_results))
                                MKText(
                                    text = stringResource(R.string.see_all),
                                    font = Fonts.NunitoBD,
                                    fontSize = 14,
                                    modifier = Modifier.clickable(onClick = onWarListClick)
                                )
                            }
                        }
                        items(state.value.recentResults, key = { it.war.id }) {
                            WarCell(
                                modifier = Modifier.padding(vertical = 5.dp),
                                viewModel = hiltViewModel(
                                    key = it.war.id.toString(),
                                    creationCallback = { factory: WarCellViewModel.Factory ->
                                        factory.create(it)
                                    }
                                ),
                                onClick = onWarDetailsClick
                            )
                        }
                    }
                }
                }
            }
        }
    }
}

/** En-tête de section (eyebrow) : petit titre aligné à gauche. */
@Composable
private fun SectionEyebrow(text: String) {
    MKText(text = text, fontSize = 16, font = Fonts.NunitoBD, modifier = Modifier.padding(vertical = 5.dp))
}

/**
 * Carte de salutation cliquable (→ profil). Réutilise le style des cartes de
 * l'écran ; pastille joueur (avatar) + « Salut, <prénom> » + sous-titre équipe.
 */
@Composable
private fun GreetingCard(greeting: String, subtitle: String, image: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Colors.whiteAlphaed, RoundedCornerShape(10.dp))
            .border(1.dp, Colors.black, RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (image) {
            null -> Image(
                painter = painterResource(R.drawable.default_logo),
                contentDescription = null,
                modifier = Modifier.size(50.dp).clip(CircleShape)
            )
            else -> AsyncImage(
                model = image,
                contentDescription = null,
                modifier = Modifier.size(50.dp).clip(CircleShape)
            )
        }
        Column {
            MKText(text = greeting, font = Fonts.NunitoBD, textColor = Colors.black, fontSize = 18)
            MKText(text = subtitle, font = Fonts.NunitoIT, textColor = Colors.black, fontSize = 14)
        }
    }
}

/**
 * Carte « Momentum » : sélecteur de fenêtre (5 / 10 dernières), bande de forme en
 * pastilles V/N/D, sparkline des scores de la fenêtre, et delta de forme
 * (winrate de la fenêtre vs all-time). [windowIndex] : 0 = 5 dernières, 1 = 10.
 */
@Composable
private fun MomentumCard(stats: Stats, windowIndex: Int, onWindowChange: (Int) -> Unit, isPlayer: Boolean) {
    val count = if (windowIndex == 0) 5 else 10
    val outcomes = stats.chronologicalOutcomes.takeLast(count)
    val scores = stats.scoreTimeline.takeLast(count)
    val form = if (windowIndex == 0) stats.recentForm5 else stats.recentForm10
    DashboardCard(title = stringResource(R.string.home_momentum)) {
        MKSegmentedSelector(
            items = listOf(stringResource(R.string.home_last_5), stringResource(R.string.home_last_10)),
            page = windowIndex,
            onClick = onWindowChange
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            outcomes.forEach { OutcomePill(it) }
        }
        scores.takeIf { it.size >= 2 }?.let {
            Spacer(Modifier.height(8.dp))
            Sparkline(it)
        }
        form?.winrateDelta?.takeIf { it != 0 }?.let { delta ->
            Spacer(Modifier.height(6.dp))
            MKText(
                text = stringResource(R.string.home_form_delta, if (delta > 0) "+$delta%" else "$delta%", count),
                font = Fonts.NunitoBD,
                textColor = if (delta > 0) Colors.green else Colors.red,
                fontSize = 13
            )
        }
    }
}

/**
 * Sparkline minimale (rule 13 : pas de composant graphe soigné, aucun réutilisable
 * dans le projet) : tracé Compose des scores de la fenêtre, normalisés entre min et
 * max. Un seul segment par intervalle, sans axes ni polish.
 */
@Composable
private fun Sparkline(values: List<Int>) {
    val min = values.min()
    val max = values.max()
    val range = (max - min).takeIf { it > 0 } ?: 1
    Canvas(Modifier.fillMaxWidth().height(40.dp)) {
        val stepX = if (values.size > 1) size.width / (values.size - 1) else 0f
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * stepX
            // y inversé (0 en haut) : score max en haut, min en bas.
            val y = size.height - ((value - min).toFloat() / range) * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = Colors.black, style = Stroke(width = 3f))
        // Point sur la dernière valeur pour repérer le plus récent.
        val lastX = (values.size - 1) * stepX
        val lastY = size.height - ((values.last() - min).toFloat() / range) * size.height
        drawCircle(color = Colors.black, radius = 5f, center = Offset(lastX, lastY))
    }
}

/** Pastille de résultat : V (victoire), N (nul), D (défaite). */
@Composable
private fun OutcomePill(outcome: Int) {
    val (label, color) = when {
        outcome > 0 -> stringResource(R.string.v) to Colors.green
        outcome < 0 -> stringResource(R.string.d) to Colors.red
        else -> stringResource(R.string.n) to Colors.grey40
    }
    Box(
        modifier = Modifier.size(26.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center
    ) {
        MKText(text = label, font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 13)
    }
}

/**
 * Carte « Chiffres clés » : winrate · score moyen · position moyenne.
 * En vue joueur ([isPlayer]) le score est le score BRUT du joueur et la 3ᵉ colonne
 * la position moyenne du joueur ; en vue équipe le score est l'écart moyen
 * ([averagePointsLabel]) et la 3ᵉ colonne le % de manches gagnées.
 */
@Composable
private fun KeyFiguresCard(stats: Stats, isPlayer: Boolean) {
    DashboardCard(title = stringResource(R.string.home_key_figures)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            KeyFigure(value = stats.allTimeForm?.winrate?.let { "$it%" } ?: "-", label = stringResource(R.string.form_winrate))
            when (isPlayer) {
                true -> {
                    KeyFigure(value = stats.averagePoints.toString(), label = stringResource(R.string.form_score))
                    KeyFigure(value = stats.averagePlayerPosLabel, label = stringResource(R.string.average_position_short))
                }
                else -> {
                    KeyFigure(value = stats.averagePointsLabel, label = stringResource(R.string.form_score))
                    KeyFigure(value = stats.mapsWon ?: "-", label = stringResource(R.string.maps_gagn_es))
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.KeyFigure(value: String, label: String) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        MKText(text = value, font = Fonts.NunitoBD, textColor = Colors.black, fontSize = 20, textAlign = TextAlign.Center)
        MKText(text = label, font = Fonts.NunitoIT, textColor = Colors.black, fontSize = 12, textAlign = TextAlign.Center)
    }
}

/** Bandeau highlight de la série en cours + record (victoires ou défaites). */
@Composable
private fun StreakBanner(stats: Stats) {
    val streak = stats.currentStreak
    val (title, record) = when {
        streak > 0 -> stringResource(R.string.home_win_streak, streak) to stats.bestWinStreak
        else -> stringResource(R.string.home_loss_streak, -streak) to stats.worstLossStreak
    }
    Column(
        Modifier.fillMaxWidth()
            .background(Colors.whiteAlphaed, RoundedCornerShape(10.dp))
            .border(1.dp, Colors.black, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        MKText(text = title, font = Fonts.NunitoBD, textColor = Colors.black, fontSize = 16)
        MKText(text = stringResource(R.string.home_streak_record, record), font = Fonts.NunitoIT, textColor = Colors.black, fontSize = 12)
    }
}

/** Carte de dashboard : titre + contenu, style réutilisé des cartes de l'écran. */
@Composable
private fun DashboardCard(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .background(Colors.whiteAlphaed, RoundedCornerShape(10.dp))
            .border(1.dp, Colors.black, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        MKText(text = title, font = Fonts.NunitoBD, textColor = Colors.black, fontSize = 16, modifier = Modifier.padding(bottom = 8.dp))
        content()
    }
}
