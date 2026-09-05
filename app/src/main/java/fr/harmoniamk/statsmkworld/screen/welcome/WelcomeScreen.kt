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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Icon
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
import fr.harmoniamk.statsmkworld.extension.displayName
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKSeasonDropdown
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.cells.CurrentWarBanner
import fr.harmoniamk.statsmkworld.ui.cells.WarCell
import fr.harmoniamk.statsmkworld.ui.cells.WarCellViewModel

// Rayon uniforme des cartes du dashboard (maquette : radius 6px). Bordure blanche
// translucide sur fond sombre translucide.
private val CardRadius = RoundedCornerShape(6.dp)

@Composable
fun WelcomeScreen(
    viewModel: WelcomeViewModel = hiltViewModel(),
    onTeamProfile: () -> Unit,
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
    BaseScreen(
        title = stringResource(R.string.accueil),
        modifier = Modifier.padding(bottom = 90.dp),
        onSearch = onSearch,
        // Dropdown de saison (#70) : filtre TOUS les agrégats du dashboard (momentum, séries,
        // records, chiffres clés, derniers résultats). Aligné à droite, avant la loupe.
        headerTrailing = {
            MKSeasonDropdown(
                seasons = state.value.seasons,
                selectedSeasonNumber = state.value.selectedSeasonNumber,
                onSeasonSelected = viewModel::onSeasonSelected
            )
        }
    ) {

        when {
            // 1er chargement (aucune métadonnée encore) : spinner plein écran.
            state.value.playerName.isNullOrEmpty() -> CircularProgressIndicator()
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
                    // 1. Carte de salutation (→ profil) + segmenté Moi/Équipe. TOUJOURS visible
                    // (métadonnées équipe/joueur invariantes au changement de saison) → le
                    // segmented reste affiché pendant le recalcul (#73).
                    item {
                        GreetingCard(
                            greeting = stringResource(R.string.home_greeting, state.value.playerName.orEmpty().displayName),
                            subtitle = stringResource(R.string.home_profile_subtitle, state.value.teamName.orEmpty()),
                            image = state.value.playerLogo,
                            initials = initialsOf(state.value.playerName?.displayName),
                            crestColor = state.value.teamColor?.let { Color(it) } ?: Colors.blue,
                            profileIndex = profileIndex,
                            onProfileChange = { profileIndex = it },
                            onClick = onTeamProfile
                        )
                    }

                    when {
                        // Recalcul en cours (changement de saison) : spinner sur la ZONE DE
                        // DONNÉES seulement ; header (dropdown) + carte de salutation restent
                        // affichés. Les agrégats de saison réapparaissent une fois le compute fini.
                        state.value.loading -> item {
                            Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        else -> {
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
                                        WarCell(
                                            viewModel = hiltViewModel(
                                                key = war.war.id.toString(),
                                                creationCallback = { factory: WarCellViewModel.Factory -> factory.create(war) }
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
                // La saison en cours est désormais montrée par le dropdown de saison du header
                // (#70, retour utilisateur) — plus de pastille redondante ici.
            }
        }
        Spacer(Modifier.height(12.dp))
        MKSegmentedSelector(
            items = listOf(stringResource(R.string.home_scope_me), stringResource(R.string.home_scope_team)),
            page = profileIndex,
            onDark = true,
            onClick = onProfileChange
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
        MKSegmentedSelector(
            items = listOf(stringResource(R.string.home_last_5), stringResource(R.string.home_last_10)),
            page = windowIndex,
            onDark = true,
            onClick = onWindowChange
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
            // Couleur de tendance : delta ≥ 0 (ou indisponible) → vert, sinon rouge.
            val trendColor = if ((form?.winrateDelta ?: 0) < 0) Colors.red else Colors.green
            Spacer(Modifier.height(12.dp))
            // Deux colonnes, chacune graphe/valeur AU-DESSUS de son hint (#91 pt.10) : GAUCHE =
            // sparkline + hint « évolution score » ; DROITE = delta de forme + hint « forme N wars
            // vs all-time ». Row en IntrinsicSize.Min + colonnes fillMaxHeight/SpaceBetween → les
            // contenus hauts (sparkline 44dp vs texte delta) restent en haut, les DEUX hints sont
            // poussés en bas et **alignés sur la même ligne basse** (retour utilisateur), même si un
            // hint wrappe sur deux lignes (maxLines non forcé). Styles inchangés (white66, NunitoBD).
            Row(
                Modifier.height(IntrinsicSize.Min),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                // Colonne GAUCHE : graphe en haut, hint poussé en bas.
                Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    Sparkline(values, trendColor, Modifier.width(110.dp).height(44.dp))
                    MKText(
                        text = stringResource(R.string.home_score_evolution_cap, count),
                        textColor = Colors.white66,
                        fontSize = 12,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                // Colonne DROITE : delta en haut, hint poussé en bas (aligné sur celui de gauche).
                // Contenu aligné à DROITE (#91, retour user) → vrai « space between » avec le graphe :
                // sparkline collée à gauche, delta+hint collés à droite, espace au milieu.
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    form?.winrateDelta?.let { delta ->
                        MKText(
                            text = if (delta >= 0) "↗ +$delta%" else "↘ $delta%",
                            font = Fonts.NunitoBD,
                            textColor = if (delta >= 0) Colors.green else Colors.red,
                            fontSize = 20,
                            textAlign = TextAlign.End
                        )
                    }
                    MKText(
                        text = stringResource(R.string.home_form_delta_cap, count),
                        textColor = Colors.white66,
                        fontSize = 12,
                        textAlign = TextAlign.End,
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
 * Sparkline stylée maquette : aire dégradée + ligne + point de fin, teintés selon
 * la TENDANCE ([trendColor] : vert si delta ≥ 0, rouge sinon). Padding 9 px.
 */
@Composable
private fun Sparkline(values: List<Int>, trendColor: Color, modifier: Modifier = Modifier) {
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

        // Aire dégradée sous la courbe (couleur de tendance : opaque → transparent).
        val fill = Path().apply {
            moveTo(pointX(0), size.height - pad)
            values.forEachIndexed { index, value -> lineTo(pointX(index), pointY(value)) }
            lineTo(pointX(values.size - 1), size.height - pad)
            close()
        }
        drawPath(
            path = fill,
            brush = Brush.verticalGradient(listOf(trendColor.copy(alpha = 0.45f), trendColor.copy(alpha = 0f)))
        )
        // Ligne.
        val line = Path().apply {
            values.forEachIndexed { index, value ->
                if (index == 0) moveTo(pointX(index), pointY(value)) else lineTo(pointX(index), pointY(value))
            }
        }
        drawPath(path = line, color = trendColor, style = Stroke(width = 6f))
        // Point sur la dernière valeur.
        drawCircle(color = trendColor, radius = 9f, center = Offset(pointX(values.size - 1), pointY(values.last())))
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
            KeyTile(value = stats.allTimeForm?.winrate?.let { "$it%" } ?: "-", label = stringResource(R.string.form_winrate))
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

/** Tuile d'un chiffre clé : grande valeur (blanche) + libellé. */
@Composable
private fun RowScope.KeyTile(value: String, label: String) {
    Column(
        Modifier
            .weight(1f)
            .background(Colors.white30, CardRadius)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MKText(text = value, font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 22, textAlign = TextAlign.Center)
        MKText(text = label, textColor = Colors.white70, fontSize = 11, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
    }
}

/**
 * Bandeau highlight série : flamme colorée (vert = série de victoires, rouge =
 * série de défaites) dans un cercle assorti + titre + record.
 */
@Composable
private fun StreakBanner(stats: Stats) {
    val streak = stats.currentStreak
    val isWin = streak > 0
    val (title, record) = when {
        isWin -> stringResource(R.string.home_win_streak, streak) to stats.bestWinStreak
        else -> stringResource(R.string.home_loss_streak, -streak) to stats.worstLossStreak
    }
    val accent = if (isWin) Colors.green else Colors.red
    DashboardCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.25f))
                    .border(1.dp, accent.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_flame),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                MKText(text = title, font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 15, textAlign = TextAlign.Start)
                MKText(text = stringResource(R.string.home_streak_record, record), textColor = Colors.white66, fontSize = 12, textAlign = TextAlign.Start, modifier = Modifier.padding(top = 2.dp))
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
