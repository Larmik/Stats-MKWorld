package fr.harmoniamk.statsmkworld.screen.currentWar

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.model.ScoringConstants
import fr.harmoniamk.statsmkworld.model.local.Maps
import fr.harmoniamk.statsmkworld.model.local.PlayerScore
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.model.local.WarTrackDetails
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.MKTextField

// Rayon uniforme des cartes (maquette : radius 6px), aligné sur WelcomeScreen.
private val CardRadius = RoundedCornerShape(6.dp)

@Composable
fun CurrentWarScreen(
    viewModel: CurrentWarViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onAddTrack: (Boolean) -> Unit,
    onActions: () -> Unit,
    onTrackDetails: (WarTrackDetails) -> Unit,
    onWarValidated: () -> Unit,
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.backToHome.collect { onWarValidated() }
    }
    LaunchedEffect(viewModel) {
        viewModel.onToast.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    BackHandler { onBack() }

    BaseScreen(
        title = stringResource(R.string.currentwar_title),
        modifier = Modifier.padding(bottom = 90.dp)
    ) {
        when (val details = state.value.details) {
            null -> CircularProgressIndicator()
            else -> {
                val is24p = state.value.teamOpponent.orEmpty().size > 1
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    // 1. Carte score : hôte VS adversaire(s), diff + nb de courses.
                    item {
                        ScoreCard(
                            teamHost = state.value.teamHost,
                            teamOpponent = state.value.teamOpponent,
                            details = details,
                            is24p = is24p
                        )
                    }

                    // 2. Carte joueurs : nom + points.
                    item {
                        PlayersCard(
                            players = state.value.players,
                            trackCount = details.warTracks.size
                        )
                    }

                    // 3. CTA « Course suivante » (masqué à 12 courses jouées).
                    if (state.value.buttonsVisible && !state.value.isOver) {
                        item {
                            MKButton(
                                modifier = Modifier.fillMaxWidth(),
                                style = MKButtonStyle.Gradient,
                                text = stringResource(R.string.course_suivante),
                                onClick = { onAddTrack(is24p) }
                            )
                        }
                    }

                    // 4. Actions selon la variante. La VALIDATION (12 j) / la saisie des
                    //    scores adverses (24 j) n'apparaît qu'à 12 courses jouées (isOver) ;
                    //    « Plus d'actions » reste accessible tout au long de la war.
                    if (state.value.buttonsVisible) {
                        item {
                            when {
                                is24p && state.value.isOver -> OpponentScoresBlock(
                                    teamOpponent = state.value.teamOpponent,
                                    opponentsScores = state.value.opponentsScores,
                                    trackCount = details.warTracks.size,
                                    onValueChange = viewModel::onValueChange,
                                    onActions = onActions,
                                    onValidateScore = viewModel::onValidateScore
                                )
                                else -> ValidationBlock12p(
                                    isOver = state.value.isOver,
                                    is24p = is24p,
                                    onActions = onActions,
                                    onValidateWar = viewModel::onValidateWar
                                )
                            }
                        }
                    }

                    // 5. Grille des courses jouées (chacune → détail course).
                    val tracks = details.warTracks
                    if (tracks.isNotEmpty()) {
                        item {
                            Eyebrow(stringResource(R.string.currentwar_tracks_count, tracks.size))
                        }
                        item {
                            TracksGrid(
                                tracks = tracks,
                                is24p = is24p,
                                onTrackDetails = onTrackDetails
                            )
                        }
                    }
                }
            }
        }
    }
}

/** En-tête de section (eyebrow) : petit titre majuscule, blanc, espacé (cf. WelcomeScreen). */
@Composable
private fun Eyebrow(text: String) {
    MKText(
        text = text.uppercase(),
        fontSize = 12,
        font = Fonts.NunitoBD,
        textColor = Colors.white,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
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
 * Carte score de la maquette (`.warscore`) : côté hôte VS côté adversaire, chacun avec
 * pastille (avatar équipe ou initiales sur couleur), nom du roster et score coloré ;
 * sous-titre « ±diff après N courses ». En 24 j (plusieurs adversaires), les côtés
 * adverses sont empilés, sans score chiffré au niveau de la manche (saisi plus bas).
 */
@Composable
private fun ScoreCard(
    teamHost: TeamEntity?,
    teamOpponent: List<TeamEntity>?,
    details: WarDetails,
    is24p: Boolean
) {
    val trackCount = details.warTracks.size
    // Écart signé du point de vue de l'hôte (avec pénalités) : détermine la couleur.
    val margin = details.scoreMargin(is24p)
    val diffColor = when {
        margin > 0 -> Colors.green
        margin < 0 -> Colors.red
        else -> Colors.white
    }
    DashboardCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TeamSide(
                team = teamHost,
                score = details.scoreHostWithPenalties.takeUnless { is24p },
                scoreColor = diffColor,
                modifier = Modifier.weight(1f)
            )
            MKText(
                text = "VS",
                font = Fonts.Urbanist,
                textColor = Colors.white55,
                fontSize = 15,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            when (is24p) {
                true -> Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    teamOpponent.orEmpty().forEach { opponent ->
                        TeamSide(team = opponent, score = null, scoreColor = diffColor, modifier = Modifier.fillMaxWidth())
                    }
                }
                else -> TeamSide(
                    team = teamOpponent?.firstOrNull(),
                    score = details.scoreOpponentWithPenalties,
                    scoreColor = Colors.white,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        MKText(
            text = stringResource(
                R.string.currentwar_diff_after,
                if (margin > 0) "+$margin" else margin.toString(),
                trackCount
            ),
            font = Fonts.Urbanist,
            textColor = diffColor,
            fontSize = 14,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

/** Un côté de la carte score : pastille (avatar/initiales), nom du roster, score coloré. */
@Composable
private fun TeamSide(
    team: TeamEntity?,
    score: Int?,
    scoreColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        TeamCrest(team = team)
        MKText(
            text = team?.name.orEmpty(),
            font = Fonts.NunitoBD,
            textColor = Colors.white,
            fontSize = 13,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp)
        )
        score?.let {
            MKText(
                text = it.toString(),
                font = Fonts.Urbanist,
                textColor = scoreColor,
                fontSize = 30,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/** Pastille d'équipe : avatar MKCentral si présent, sinon initiales du tag sur couleur. */
@Composable
private fun TeamCrest(team: TeamEntity?) {
    val color = team?.color?.let { Color(it) } ?: Colors.blue
    when (val logo = team?.logo) {
        null -> Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, Colors.white85, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MKText(
                text = team?.tag?.take(2)?.uppercase().orEmpty(),
                font = Fonts.Urbanist,
                textColor = Colors.white,
                fontSize = 14
            )
        }
        else -> AsyncImage(
            model = "https://mkcentral.com$logo",
            contentDescription = null,
            modifier = Modifier.size(42.dp).clip(CircleShape).border(2.dp, Colors.white85, CircleShape)
        )
    }
}

/**
 * Carte « Joueurs » (`.two`) : tuiles nom + points, en deux colonnes. Une pastille de
 * shock (icône + « xN ») s'affiche si le joueur a provoqué des éclairs.
 */
@Composable
private fun PlayersCard(players: List<PlayerScore>, trackCount: Int) {
    DashboardCard {
        Eyebrow(stringResource(R.string.joueurs))
        Spacer(Modifier.height(11.dp))
        players.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().padding(bottom = 9.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                pair.forEach { PlayerTile(score = it, trackCount = trackCount, modifier = Modifier.weight(1f)) }
                // Comble la 2ᵉ colonne si nombre impair de joueurs.
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Tuile joueur : libellé (nom + éventuel nb de courses jouées) + valeur « N pts ». */
@Composable
private fun PlayerTile(score: PlayerScore, trackCount: Int, modifier: Modifier = Modifier) {
    val name = when (score.trackPlayed in 1 until trackCount) {
        true -> "${score.player?.name.orEmpty()} (${score.trackPlayed})"
        else -> score.player?.name.orEmpty()
    }
    Column(
        modifier
            .background(Colors.white30, CardRadius)
            .padding(11.dp)
    ) {
        MKText(
            text = name.uppercase(),
            textColor = Colors.white66,
            fontSize = 11,
            maxLines = 1,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp)) {
            MKText(text = score.score.toString(), font = Fonts.Urbanist, textColor = Colors.white, fontSize = 16)
            MKText(
                text = stringResource(R.string.currentwar_points_short),
                textColor = Colors.white55,
                fontSize = 11,
                modifier = Modifier.padding(start = 4.dp)
            )
            score.shockCount.takeIf { it > 0 }?.let {
                Spacer(Modifier.weight(1f))
                Image(
                    painter = painterResource(R.drawable.shock),
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                if (it > 1) MKText(text = "x$it", fontSize = 11, textColor = Colors.white66)
            }
        }
    }
}

/**
 * Bloc d'actions « Plus d'actions » (+ « Valider la war » directe dès 12 courses en
 * 12 j) + hint. « Plus d'actions » reste accessible pendant toute la war ; « Valider »
 * n'apparaît qu'une fois la war terminée (12 courses).
 */
@Composable
private fun ValidationBlock12p(isOver: Boolean, is24p: Boolean, onActions: () -> Unit, onValidateWar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            MKButton(
                modifier = Modifier.weight(1f),
                style = MKButtonStyle.Minor(Colors.black),
                text = stringResource(R.string.more_actions),
                onClick = onActions
            )
            // « Valider la war » directe : uniquement en 12 j terminée (en 24 j, la
            // validation passe par la saisie des scores adverses, cf. OpponentScoresBlock).
            if (isOver && !is24p) MKButton(
                modifier = Modifier.weight(1f),
                style = MKButtonStyle.Gradient,
                text = stringResource(R.string.valider_la_war),
                onClick = onValidateWar
            )
        }
        // Hint 12 j : n'a de sens que dans la variante 12 j.
        if (!is24p) MKText(
            text = stringResource(R.string.currentwar_hint_12),
            textColor = Colors.white55,
            fontSize = 12,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Variante 24 j : carte « Scores des équipes adverses » (une ligne de saisie par
 * équipe adverse), « Plus d'actions » + CTA « Saisir & valider ». La validation
 * (contrôle du total + écriture) reste côté ViewModel (justesse des scores prioritaire).
 */
@Composable
private fun OpponentScoresBlock(
    teamOpponent: List<TeamEntity>?,
    opponentsScores: Map<String, Int>,
    trackCount: Int,
    onValueChange: (String, String) -> Unit,
    onActions: () -> Unit,
    onValidateScore: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        DashboardCard {
            Eyebrow(stringResource(R.string.currentwar_opponent_scores))
            teamOpponent.orEmpty().forEachIndexed { index, team ->
                if (index > 0) Spacer(Modifier.height(8.dp))
                ScoreSetRow(
                    team = team,
                    value = opponentsScores[team.id]?.toString().orEmpty(),
                    onValueChange = { onValueChange(team.id, it) }
                )
            }
            Spacer(Modifier.height(9.dp))
            MKText(
                text = stringResource(R.string.currentwar_opponent_scores_hint, trackCount * ScoringConstants.MAX_POINTS_PER_TRACK_24P),
                textColor = Colors.white55,
                fontSize = 12,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            MKButton(
                modifier = Modifier.weight(1f),
                style = MKButtonStyle.Minor(Colors.black),
                text = stringResource(R.string.more_actions),
                onClick = onActions
            )
            MKButton(
                modifier = Modifier.weight(1f),
                style = MKButtonStyle.Gradient,
                text = stringResource(R.string.currentwar_save_scores),
                onClick = onValidateScore
            )
        }
    }
}

/** Ligne de saisie d'un score adverse (`.scoreset`) : pastille + nom + champ centré. */
@Composable
private fun ScoreSetRow(team: TeamEntity, value: String, onValueChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        TeamCrestSmall(team = team)
        MKText(
            text = team.name,
            font = Fonts.NunitoBD,
            textColor = Colors.white,
            fontSize = 13,
            maxLines = 1,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f)
        )
        MKTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardType = KeyboardType.Number,
            textColor = Colors.white,
            borderColor = Colors.whiteBorderSoft,
            backgroundColor = Colors.white30,
            modifier = Modifier.width(80.dp)
        )
    }
}

/** Petite pastille d'équipe (30 dp) pour les lignes de saisie de score. */
@Composable
private fun TeamCrestSmall(team: TeamEntity) {
    val color = team.color?.let { Color(it) } ?: Colors.blue
    when (val logo = team.logo) {
        null -> Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).background(color).border(2.dp, Colors.white85, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MKText(text = team.tag.take(2).uppercase(), font = Fonts.Urbanist, textColor = Colors.white, fontSize = 11)
        }
        else -> AsyncImage(
            model = "https://mkcentral.com$logo",
            contentDescription = null,
            modifier = Modifier.size(30.dp).clip(CircleShape).border(2.dp, Colors.white85, CircleShape)
        )
    }
}

/**
 * Grille des courses jouées (`.trackgrid`) : cellules compactes (nom du circuit +
 * score + diff), bordure gauche colorée (vert manche gagnée / rouge perdue en 12 j) ;
 * chaque cellule mène au détail de la course.
 */
@Composable
private fun TracksGrid(
    tracks: List<WarTrackDetails>,
    is24p: Boolean,
    onTrackDetails: (WarTrackDetails) -> Unit
) {
    // Grille 2 colonnes en lignes chunkées : évite d'imbriquer un LazyVerticalGrid
    // (même axe de scroll) dans le LazyColumn de l'écran.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tracks.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { track ->
                    Box(Modifier.weight(1f)) {
                        TrackCard(track = track, is24p = is24p, onClick = { onTrackDetails(track) })
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Cellule compacte d'une course : bordure gauche colorée, nom du circuit, score + diff. */
@Composable
private fun TrackCard(track: WarTrackDetails, is24p: Boolean, onClick: () -> Unit) {
    // Couleur de la bordure gauche : vert manche gagnée, rouge perdue (12 j uniquement).
    val accent = when {
        is24p -> Colors.white30
        track.displayedDiff.startsWith("+") -> Colors.green
        track.displayedDiff.startsWith("-") -> Colors.red
        else -> Colors.white30
    }
    // Libellé du circuit : dernier segment (arrivée) d'une éventuelle intermission.
    val label = track.index.lastOrNull()?.toInt()?.let { Maps.entries.getOrNull(it)?.label }
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(CardRadius)
            .background(Colors.white30, CardRadius)
            .clickable(onClick = onClick)
    ) {
        // Bordure gauche colorée (maquette : border-left 3px).
        Box(Modifier.width(3.dp).fillMaxHeight().background(accent))
        Column(Modifier.padding(vertical = 9.dp, horizontal = 10.dp)) {
            MKText(
                text = label?.let { stringResource(it) } ?: "-",
                font = Fonts.NunitoBD,
                textColor = Colors.white,
                fontSize = 13,
                maxLines = 1,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                MKText(
                    text = track.teamScore.toString(),
                    font = Fonts.Urbanist,
                    textColor = Colors.white,
                    fontSize = 15
                )
                if (!is24p) MKText(
                    text = track.displayedDiff,
                    textColor = Colors.white55,
                    fontSize = 11,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}
