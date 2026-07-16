package fr.harmoniamk.statsmkworld.screen.welcome

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.cells.CurrentWarCell
import fr.harmoniamk.statsmkworld.ui.cells.CurrentWarCellViewModel
import fr.harmoniamk.statsmkworld.ui.cells.WarCellViewModel

// Rayon uniforme des cartes du dashboard (maquette : radius 6px). Bordure blanche
// translucide sur fond sombre translucide.
private val CardRadius = RoundedCornerShape(6.dp)

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
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    // 1. Carte de salutation (→ profil) + segmenté Moi/Équipe.
                    item {
                        GreetingCard(
                            greeting = stringResource(R.string.home_greeting, state.value.playerName.orEmpty()),
                            subtitle = stringResource(R.string.home_profile_subtitle, state.value.teamName.orEmpty()),
                            image = state.value.playerLogo,
                            initials = initialsOf(state.value.playerName),
                            crestColor = state.value.teamColor?.let { Color(it) } ?: Colors.blue,
                            profileIndex = profileIndex,
                            onProfileChange = { profileIndex = it },
                            onClick = onTeamProfile
                        )
                    }

                    // 2. War en cours (→ CurrentWar) : bannière « En direct ».
                    state.value.currentWar?.let { war ->
                        item {
                            Eyebrow(stringResource(R.string.war_en_cours))
                            Spacer(Modifier.height(6.dp))
                            CurrentWarBanner(war = war, onClick = onCurrentWar)
                        }
                    }

                    // 3. Momentum (selon profil + fenêtre).
                    selectedStats?.let { stats ->
                        item {
                            MomentumCard(
                                stats = stats,
                                windowIndex = windowIndex,
                                onWindowChange = { windowIndex = it }
                            )
                        }
                        // 4. Chiffres clés.
                        item { KeyFiguresCard(stats = stats, isPlayer = profileIndex == 0) }
                        // 5. Bandeau série en cours.
                        stats.currentStreak.takeIf { it != 0 }?.let { item { StreakBanner(stats) } }
                    }

                    // 6. Derniers résultats (→ WarDetails) + « Voir tout » (→ historique).
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
                                    Eyebrow(stringResource(R.string.last_results))
                                    MKText(
                                        text = stringResource(R.string.see_all),
                                        font = Fonts.NunitoBD,
                                        textColor = Colors.yellow,
                                        fontSize = 13,
                                        modifier = Modifier.clickable(onClick = onWarListClick)
                                    )
                                }
                            }
                            items(state.value.recentResults, key = { it.war.id }) { war ->
                                ResultRow(war = war, onClick = onWarDetailsClick)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** En-tête de section (eyebrow) : petit titre majuscule, blanc, espacé. */
@Composable
private fun Eyebrow(text: String) {
    MKText(
        text = text.uppercase(),
        fontSize = 12,
        font = Fonts.NunitoBD,
        textColor = Colors.white,
        textAlign = TextAlign.Start
    )
}

/** Carte dashboard : fond sombre translucide, bordure blanche, radius 6, padding 13. */
@Composable
private fun DashboardCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Colors.blackAlphaed, CardRadius)
            .border(1.dp, Colors.whiteBorder, CardRadius)
            .padding(13.dp),
        content = content
    )
}

/**
 * Carte de salutation cliquable (→ profil) : pastille couleur/avatar joueur,
 * « Salut, <prénom> » (Bungee), sous-titre équipe, puis segmenté Moi/Équipe.
 */
@Composable
private fun GreetingCard(
    greeting: String,
    subtitle: String,
    image: String?,
    initials: String,
    crestColor: Color,
    profileIndex: Int,
    onProfileChange: (Int) -> Unit,
    onClick: () -> Unit
) {
    DashboardCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Crest(image = image, initials = initials, color = crestColor)
            Column(Modifier.weight(1f)) {
                MKText(text = greeting, font = Fonts.Bungee, textColor = Colors.white, fontSize = 18, textAlign = TextAlign.Start)
                MKText(text = subtitle, textColor = Colors.white66, fontSize = 13, textAlign = TextAlign.Start, modifier = Modifier.padding(top = 3.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Segmented(
            items = listOf(stringResource(R.string.home_scope_me), stringResource(R.string.home_scope_team)),
            selected = profileIndex,
            onSelect = onProfileChange
        )
    }
}

/** Pastille joueur circulaire : avatar si présent, sinon initiales sur fond couleur. */
@Composable
private fun Crest(image: String?, initials: String, color: Color) {
    when (image) {
        null -> Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, Colors.white85, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MKText(text = initials, font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 15)
        }
        else -> AsyncImage(
            model = image,
            contentDescription = null,
            modifier = Modifier.size(46.dp).clip(CircleShape).border(2.dp, Colors.white85, CircleShape)
        )
    }
}

/**
 * Segmenté façon maquette : conteneur blanc translucide arrondi (radius 10) ;
 * onglet actif = fond blanc/texte sombre, inactif = texte blanc.
 */
@Composable
private fun Segmented(items: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Colors.white30, RoundedCornerShape(10.dp))
            .border(1.dp, Colors.whiteBorderSoft, RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        items.forEachIndexed { index, label ->
            val active = index == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) Colors.white else Colors.transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                MKText(text = label, font = Fonts.NunitoBD, textColor = if (active) Colors.black else Colors.white, fontSize = 13)
            }
        }
    }
}

/**
 * Bannière « War en cours » : dégradé vert→sombre, bordure verte, pastille « En
 * direct · N joueurs ». Le corps (roster vs adversaire, score, courses jouées)
 * provient de la vraie war via `CurrentWarCell` existante.
 */
@Composable
private fun CurrentWarBanner(war: War, onClick: () -> Unit) {
    // Nombre de joueurs : 12p (1 adversaire) → 12, 24p (3 équipes) → 24.
    val players = if (war.teamOpponent.size > 1) 24 else 12
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(Color(0x4081C995), Colors.blackAlphaed)),
                CardRadius
            )
            .border(1.dp, Colors.green, CardRadius)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(Colors.green))
            MKText(text = stringResource(R.string.home_live_players, players), font = Fonts.NunitoBD, textColor = Colors.green, fontSize = 11, textAlign = TextAlign.Start)
        }
        Spacer(Modifier.height(6.dp))
        CurrentWarCell(
            onClick = onClick,
            viewModel = hiltViewModel(
                key = war.id.toString() + war.tracks.joinToString { it.id.toString() },
                creationCallback = { factory: CurrentWarCellViewModel.Factory -> factory.create(war) }
            )
        )
    }
}

/**
 * Carte « Momentum » : eyebrow, segmenté 5/10 dernières, bande de pastilles V/N/D
 * (`chronologicalOutcomes`), puis ligne sparkline (`scoreTimeline`) + delta de forme.
 */
@Composable
private fun MomentumCard(stats: Stats, windowIndex: Int, onWindowChange: (Int) -> Unit) {
    val count = if (windowIndex == 0) 5 else 10
    val outcomes = stats.chronologicalOutcomes.takeLast(count)
    val scores = stats.scoreTimeline.takeLast(count)
    val form = if (windowIndex == 0) stats.recentForm5 else stats.recentForm10
    DashboardCard {
        Eyebrow(stringResource(R.string.home_momentum))
        Spacer(Modifier.height(11.dp))
        Segmented(
            items = listOf(stringResource(R.string.home_last_5), stringResource(R.string.home_last_10)),
            selected = windowIndex,
            onSelect = onWindowChange
        )
        Spacer(Modifier.height(11.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            outcomes.forEach { OutcomeChip(it) }
            Spacer(Modifier.weight(1f))
            MKText(
                text = stringResource(if (windowIndex == 0) R.string.home_last_5 else R.string.home_last_10),
                textColor = Colors.white55,
                fontSize = 11
            )
        }
        scores.takeIf { it.size >= 2 }?.let { values ->
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                Sparkline(values, Modifier.width(110.dp).height(44.dp))
                Column(Modifier.weight(1f)) {
                    form?.winrateDelta?.let { delta ->
                        MKText(
                            text = if (delta >= 0) "↗ +$delta%" else "↘ $delta%",
                            font = Fonts.NunitoBD,
                            textColor = if (delta >= 0) Colors.green else Colors.red,
                            fontSize = 20,
                            textAlign = TextAlign.Start
                        )
                    }
                    MKText(
                        text = stringResource(R.string.home_form_delta_cap, count),
                        textColor = Colors.white66,
                        fontSize = 12,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

/** Pastille de résultat carrée arrondie : V (vert), N (blanc), D (rouge). */
@Composable
private fun OutcomeChip(outcome: Int) {
    val (label, color) = when {
        outcome > 0 -> stringResource(R.string.v) to Colors.green
        outcome < 0 -> stringResource(R.string.d) to Colors.red
        else -> stringResource(R.string.n) to Colors.white
    }
    Box(
        modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(color),
        contentAlignment = Alignment.Center
    ) {
        MKText(text = label, font = Fonts.NunitoBD, textColor = Colors.black, fontSize = 13)
    }
}

/**
 * Sparkline stylée maquette : aire dégradée verte (opaque → transparent) sous la
 * courbe, ligne verte, point vert sur la valeur la plus récente. Padding 9 px.
 */
@Composable
private fun Sparkline(values: List<Int>, modifier: Modifier = Modifier) {
    val min = values.min()
    val max = values.max()
    val range = (max - min).takeIf { it > 0 } ?: 1
    Canvas(modifier) {
        val pad = 9f
        val innerW = size.width - 2 * pad
        val innerH = size.height - 2 * pad
        val stepX = if (values.size > 1) innerW / (values.size - 1) else 0f
        val pointX = { index: Int -> pad + index * stepX }
        val pointY = { value: Int -> size.height - pad - ((value - min).toFloat() / range) * innerH }

        // Aire dégradée sous la courbe.
        val fill = Path().apply {
            moveTo(pointX(0), size.height - pad)
            values.forEachIndexed { index, value -> lineTo(pointX(index), pointY(value)) }
            lineTo(pointX(values.size - 1), size.height - pad)
            close()
        }
        drawPath(
            path = fill,
            brush = Brush.verticalGradient(listOf(Color(0x7381C995), Color(0x0081C995)))
        )
        // Ligne.
        val line = Path().apply {
            values.forEachIndexed { index, value ->
                if (index == 0) moveTo(pointX(index), pointY(value)) else lineTo(pointX(index), pointY(value))
            }
        }
        drawPath(path = line, color = Colors.green, style = Stroke(width = 6f))
        // Point sur la dernière valeur.
        drawCircle(color = Colors.green, radius = 9f, center = Offset(pointX(values.size - 1), pointY(values.last())))
    }
}

/**
 * Carte « Chiffres clés » : trois tuiles (winrate coloré vert · score moyen ·
 * 3ᵉ colonne selon la vue). Vue Moi : score brut + position ; Équipe : écart + %.
 */
@Composable
private fun KeyFiguresCard(stats: Stats, isPlayer: Boolean) {
    DashboardCard {
        Eyebrow(stringResource(R.string.home_key_figures))
        Spacer(Modifier.height(11.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            KeyTile(value = stats.allTimeForm?.winrate?.let { "$it%" } ?: "-", label = stringResource(R.string.form_winrate), accent = true)
            when (isPlayer) {
                true -> {
                    KeyTile(value = stats.averagePoints.toString(), label = stringResource(R.string.form_score))
                    KeyTile(value = stats.averagePlayerPosLabel, label = stringResource(R.string.average_position_short))
                }
                else -> {
                    KeyTile(value = stats.averagePointsLabel, label = stringResource(R.string.form_score))
                    KeyTile(value = stats.mapsWon ?: "-", label = stringResource(R.string.maps_gagn_es))
                }
            }
        }
    }
}

/** Tuile d'un chiffre clé : grande valeur (verte si accent) + libellé. */
@Composable
private fun RowScope.KeyTile(value: String, label: String, accent: Boolean = false) {
    Column(
        Modifier
            .weight(1f)
            .background(Colors.white30, CardRadius)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MKText(text = value, font = Fonts.NunitoBD, textColor = if (accent) Colors.green else Colors.white, fontSize = 22, textAlign = TextAlign.Center)
        MKText(text = label, textColor = Colors.white70, fontSize = 11, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
    }
}

/** Bandeau highlight série : icône flamme dans un cercle vert + titre + record. */
@Composable
private fun StreakBanner(stats: Stats) {
    val streak = stats.currentStreak
    val (title, record) = when {
        streak > 0 -> stringResource(R.string.home_win_streak, streak) to stats.bestWinStreak
        else -> stringResource(R.string.home_loss_streak, -streak) to stats.worstLossStreak
    }
    DashboardCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x4081C995))
                    .border(1.dp, Color(0x8081C995), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                MKText(text = "🔥", fontSize = 18)
            }
            Column(Modifier.weight(1f)) {
                MKText(text = title, font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 15, textAlign = TextAlign.Start)
                MKText(text = stringResource(R.string.home_streak_record, record), textColor = Colors.white66, fontSize = 12, textAlign = TextAlign.Start, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

/**
 * Ligne de résultat façon maquette : pastille V/N/D, pastille adversaire (tag +
 * couleur), nom « vs … » + date, score + écart à droite. Réutilise
 * `WarCellViewModel` (roster/nom/tag/score/diff réels), pas les données de démo.
 */
@Composable
private fun ResultRow(war: WarDetails, onClick: (WarDetails) -> Unit) {
    val viewModel = hiltViewModel<WarCellViewModel, WarCellViewModel.Factory>(
        key = war.war.id.toString(),
        creationCallback = { factory -> factory.create(war) }
    )
    val cell = viewModel.state.collectAsStateWithLifecycle()
    val opponent = cell.value.teamOpponent?.firstOrNull()
    val diff = cell.value.diff.orEmpty()
    val outcome = when {
        diff.startsWith("+") -> 1
        diff.startsWith("-") -> -1
        else -> 0
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(Colors.blackAlphaed, CardRadius)
            .border(1.dp, Colors.whiteBorder, CardRadius)
            .clickable { onClick(war) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        OutcomeChip(outcome)
        // Pastille adversaire (avatar équipe si dispo, sinon tag sur cercle noir).
        when (val logo = opponent?.logo) {
            null -> Box(
                Modifier.size(32.dp).clip(CircleShape).background(Colors.black).border(2.dp, Colors.white85, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                MKText(text = opponent?.tag.orEmpty(), font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 11, maxLines = 1)
            }
            else -> AsyncImage(
                model = "https://mkcentral.com$logo",
                contentDescription = null,
                modifier = Modifier.size(32.dp).clip(CircleShape).border(2.dp, Colors.white85, CircleShape)
            )
        }
        Column(Modifier.weight(1f)) {
            MKText(text = stringResource(R.string.home_vs, opponent?.name.orEmpty()), font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 14, textAlign = TextAlign.Start, maxLines = 1)
            MKText(text = war.date, textColor = Colors.white55, fontSize = 11, textAlign = TextAlign.Start, modifier = Modifier.padding(top = 2.dp))
        }
        Column(horizontalAlignment = Alignment.End) {
            MKText(text = cell.value.score.orEmpty(), font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 14, maxLines = 1)
            if (diff.isNotEmpty()) {
                MKText(text = diff, textColor = Colors.white55, fontSize = 11, maxLines = 1)
            }
        }
    }
}

/** Initiales (2 lettres majuscules) d'un nom de joueur pour la pastille. */
private fun initialsOf(name: String?): String = name
    ?.trim()
    ?.split(" ", "_", "-")
    ?.filter { it.isNotBlank() }
    ?.take(2)
    ?.joinToString("") { it.first().uppercase() }
    ?.takeIf { it.isNotEmpty() }
    ?: "?"
