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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
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
    BaseScreen(title = stringResource(R.string.accueil), modifier = Modifier.padding(bottom = 90.dp), onSearch = onSearch) {

        when (state.value.playerName.isNullOrEmpty()) {
            true -> CircularProgressIndicator()
            else -> LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Carte de salutation (→ profil).
                item {
                    GreetingCard(
                        greeting = stringResource(R.string.home_greeting, state.value.playerName.orEmpty()),
                        subtitle = stringResource(R.string.home_profile_subtitle, state.value.teamName.orEmpty()),
                        image = state.value.playerLogo,
                        onClick = onTeamProfile
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

                // 3/4/5. Momentum + chiffres clés + série (dépendent des stats calculées).
                state.value.stats?.let { stats ->
                    item { MomentumCard(stats) }
                    item { KeyFiguresCard(stats) }
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
 * Carte « Momentum » : bande de forme des 5 derniers résultats (pastilles V/N/D)
 * + delta de la forme récente (10 dernières wars) vs all-time sur le winrate.
 */
@Composable
private fun MomentumCard(stats: Stats) {
    DashboardCard(title = stringResource(R.string.home_momentum)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            stats.recentOutcomes.forEach { OutcomePill(it) }
            Spacer(Modifier.weight(1f))
            MKText(text = stringResource(R.string.home_last_5), font = Fonts.NunitoIT, textColor = Colors.black, fontSize = 12)
        }
        stats.recentForm10?.winrateDelta?.takeIf { it != 0 }?.let { delta ->
            Spacer(Modifier.height(6.dp))
            MKText(
                text = stringResource(R.string.home_form_delta, if (delta > 0) "+$delta%" else "$delta%"),
                font = Fonts.NunitoBD,
                textColor = if (delta > 0) Colors.green else Colors.red,
                fontSize = 13
            )
        }
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

/** Carte « Chiffres clés » : winrate · score moyen · position moyenne. */
@Composable
private fun KeyFiguresCard(stats: Stats) {
    DashboardCard(title = stringResource(R.string.home_key_figures)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            KeyFigure(value = stats.allTimeForm?.winrate?.let { "$it%" } ?: "-", label = stringResource(R.string.form_winrate))
            KeyFigure(value = stats.averagePointsLabel, label = stringResource(R.string.form_score))
            KeyFigure(value = stats.averagePlayerPosLabel, label = stringResource(R.string.average_position_short))
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
