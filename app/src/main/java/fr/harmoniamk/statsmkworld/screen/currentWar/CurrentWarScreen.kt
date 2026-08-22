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
import fr.harmoniamk.statsmkworld.extension.diffColor
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.model.local.WarTrackDetails
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.MKTextField
import fr.harmoniamk.statsmkworld.ui.cells.MKTrackCell

// Rayon uniforme des cartes (maquette : radius 6px), aligné sur WelcomeScreen.
private val CardRadius = RoundedCornerShape(6.dp)

@Composable
fun CurrentWarScreen(
    viewModel: CurrentWarViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onAddTrack: (Boolean) -> Unit,
    onActions: () -> Unit,
    onTrackDetails: (WarTrackDetails, Int) -> Unit,
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

                    // 3. Actions. CTA principal + « Plus d'actions » sur une MÊME ligne (même
                    //    disposition en cours ET terminée) : le CTA vaut « Course suivante » tant
                    //    que la war n'est pas terminée, puis « Valider la war » en 12 j terminée.
                    //    En 24 j terminée, la validation passe par la saisie des scores adverses
                    //    (bloc dédié affiché ensuite).
                    if (state.value.buttonsVisible) {
                        item {
                            ActionsBlock(
                                isOver = state.value.isOver,
                                is24p = is24p,
                                onAddTrack = { onAddTrack(is24p) },
                                onActions = onActions,
                                onValidateWar = viewModel::onValidateWar
                            )
                        }
                        if (is24p && state.value.isOver) {
                            item {
                                OpponentScoresBlock(
                                    teamOpponent = state.value.teamOpponent,
                                    opponentsScores = state.value.opponentsScores,
                                    trackCount = details.warTracks.size,
                                    onValueChange = viewModel::onValueChange,
                                    onValidateScore = viewModel::onValidateScore
                                )
                            }
                        }
                    }

                    // 5. Section « Courses jouées » (carte englobante + grille, chacune → détail).
                    val tracks = details.warTracks
                    if (tracks.isNotEmpty()) {
                        item {
                            TracksSection(
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
 * Carte « Score du match » (`.warscore`) : côté hôte VS côté adversaire, chacun avec
 * pastille (avatar équipe ou initiales sur couleur), nom du roster et score (en blanc).
 * La **différence de score seule** est affichée **au centre** entre les deux scores,
 * **colorisée** (vert > 0, rouge < 0, blanc = 0). Sous-titre : **N courses restantes**
 * (12 − courses jouées), en **blanc** (non colorisé). En 24 j (plusieurs adversaires),
 * les côtés adverses sont empilés, sans score chiffré au niveau de la carte.
 */
@Composable
private fun ScoreCard(
    teamHost: TeamEntity?,
    teamOpponent: List<TeamEntity>?,
    details: WarDetails,
    is24p: Boolean
) {
    val trackCount = details.warTracks.size
    // Écart signé du point de vue de l'hôte (avec pénalités) : colore la diff centrale.
    val margin = details.scoreMargin(is24p)
    val diffColor = margin.diffColor()
    // Total de pénalités par équipe (clé = teamId/rosterId), même rattachement que
    // WarScoreView/PenaltiesSection. La clé hôte est war.teamHost (rosterId), PAS
    // teamHost.id (id d'équipe).
    val penaltyByTeam = details.war.penalties
        .groupBy { it.teamId }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
    // Total de shocks de la war (somme sur toutes les manches).
    val totalShocks = details.war.tracks.sumOf { it.shocks.orEmpty().sumOf { shock -> shock.count } }
    DashboardCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TeamSide(
                team = teamHost,
                score = details.scoreHostWithPenalties.takeUnless { is24p },
                penalty = penaltyByTeam[details.war.teamHost] ?: 0,
                modifier = Modifier.weight(1f)
            )
            // Différence de score seule, centrée entre les deux équipes, colorisée.
            MKText(
                text = if (margin > 0) "+$margin" else margin.toString(),
                font = Fonts.Urbanist,
                textColor = diffColor,
                fontSize = 20,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            when (is24p) {
                true -> Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    teamOpponent.orEmpty().forEach { opponent ->
                        TeamSide(
                            team = opponent,
                            score = null,
                            penalty = penaltyByTeam[opponent.id] ?: 0,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                else -> TeamSide(
                    team = teamOpponent?.firstOrNull(),
                    score = details.scoreOpponentWithPenalties,
                    penalty = teamOpponent?.firstOrNull()?.id?.let { penaltyByTeam[it] } ?: 0,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        // Sous-titre : courses restantes (12 − jouées), en blanc (non colorisé).
        MKText(
            text = stringResource(R.string.currentwar_tracks_remaining, 12 - trackCount),
            font = Fonts.Urbanist,
            textColor = Colors.white,
            fontSize = 14,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        // Total de shocks de la war, sous la diff (icône éclair + compteur).
        totalShocks.takeIf { it > 0 }?.let {
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.shock),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                MKText(
                    text = stringResource(R.string.currentwar_total_shocks, it),
                    font = Fonts.NunitoBD,
                    textColor = Colors.white,
                    fontSize = 13,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

/**
 * Un côté de la carte score : pastille (avatar/initiales), nom du roster, score (blanc),
 * et **pénalité de l'équipe** (« -N » en rouge) sous le score le cas échéant.
 */
@Composable
private fun TeamSide(
    team: TeamEntity?,
    score: Int?,
    penalty: Int,
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
                textColor = Colors.white,
                fontSize = 30,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        // Pénalité de l'équipe, sous son score (rouge).
        penalty.takeIf { it > 0 }?.let {
            MKText(
                text = "-$it",
                font = Fonts.NunitoBD,
                textColor = Colors.red,
                fontSize = 12,
                modifier = Modifier.padding(top = 2.dp)
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
 * Carte « Scores des joueurs » : cellules en **ligne compacte** (nom à gauche, score à
 * droite via `SpaceBetween`) disposées sur **deux colonnes** (6 joueurs → 2 × 3 lignes).
 * Une pastille de shock (icône + « xN ») s'affiche à côté du nom si applicable.
 */
@Composable
private fun PlayersCard(players: List<PlayerScore>, trackCount: Int) {
    DashboardCard {
        Eyebrow(stringResource(R.string.joueurs))
        Spacer(Modifier.height(11.dp))
        players.chunked(2).forEachIndexed { rowIndex, pair ->
            if (rowIndex > 0) Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                pair.forEach { score ->
                    PlayerRow(score = score, trackCount = trackCount, modifier = Modifier.weight(1f))
                }
                // Comble la 2ᵉ colonne si nombre impair de joueurs.
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Ligne joueur compacte : nom (+ nb de courses jouées + shocks) à gauche, « N pts » à droite. */
@Composable
private fun PlayerRow(score: PlayerScore, trackCount: Int, modifier: Modifier = Modifier) {
    val name = when (score.trackPlayed in 1 until trackCount) {
        true -> "${score.player?.name.orEmpty()} (${score.trackPlayed})"
        else -> score.player?.name.orEmpty()
    }
    Row(
        modifier
            .background(Colors.white30, CardRadius)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
            MKText(
                text = name,
                font = Fonts.NunitoBD,
                textColor = Colors.white,
                fontSize = 13,
                maxLines = 1,
                textAlign = TextAlign.Start
            )
            score.shockCount.takeIf { it > 0 }?.let {
                Spacer(Modifier.width(6.dp))
                Image(
                    painter = painterResource(R.drawable.shock),
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                if (it > 1) MKText(text = "x$it", fontSize = 11, textColor = Colors.white66)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            MKText(text = score.score.toString(), font = Fonts.Urbanist, textColor = Colors.white, fontSize = 16)
            MKText(
                text = stringResource(R.string.currentwar_points_short),
                textColor = Colors.white55,
                fontSize = 11,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/**
 * Bloc d'actions : **CTA principal + « Plus d'actions » côte à côte sur une même ligne**, même
 * structure quel que soit l'état (deux boutons `weight(1f)`, même espacement 9 dp, CTA en
 * Gradient / « Plus d'actions » en Minor). Le CTA principal (colonne de gauche) vaut :
 * - war **en cours** → « Course suivante » (→ AddTrack) ;
 * - war **terminée** (12 courses) en **12 j** → « Valider la war » directe (→ onValidateWar).
 *
 * En **24 j** terminée, il n'y a pas de CTA de validation ici (elle passe par la saisie des
 * scores adverses, cf. [OpponentScoresBlock]) : seule « Plus d'actions » est affichée, en pleine
 * largeur.
 */
@Composable
private fun ActionsBlock(
    isOver: Boolean,
    is24p: Boolean,
    onAddTrack: () -> Unit,
    onActions: () -> Unit,
    onValidateWar: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        // Colonne de gauche : CTA principal selon l'état (course suivante / valider la war).
        // Absente uniquement en 24 j terminée (validation via les scores adverses).
        when {
            !isOver -> MKButton(
                modifier = Modifier.weight(1f),
                style = MKButtonStyle.Gradient,
                text = stringResource(R.string.course_suivante),
                onClick = onAddTrack
            )
            !is24p -> MKButton(
                modifier = Modifier.weight(1f),
                style = MKButtonStyle.Gradient,
                text = stringResource(R.string.valider_la_war),
                onClick = onValidateWar
            )
        }
        MKButton(
            modifier = Modifier.weight(1f),
            style = MKButtonStyle.Minor(Colors.black),
            text = stringResource(R.string.more_actions),
            onClick = onActions
        )
    }
}

/**
 * Variante 24 j : carte « Scores des équipes adverses » (une ligne de saisie par
 * équipe adverse) + CTA « Saisir & valider ». La validation (contrôle du total +
 * écriture) reste côté ViewModel (justesse des scores prioritaire).
 */
@Composable
private fun OpponentScoresBlock(
    teamOpponent: List<TeamEntity>?,
    opponentsScores: Map<String, Int>,
    trackCount: Int,
    onValueChange: (String, String) -> Unit,
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
        MKButton(
            modifier = Modifier.fillMaxWidth(),
            style = MKButtonStyle.Gradient,
            text = stringResource(R.string.currentwar_save_scores),
            onClick = onValidateScore
        )
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
 * Section « Courses jouées » : **carte englobante** (même cadre `DashboardCard` que les
 * autres sections) avec eyebrow « Courses jouées · N » puis grille 2 colonnes des
 * courses. Chaque cellule mène au détail de la course.
 */
@Composable
private fun TracksSection(
    tracks: List<WarTrackDetails>,
    is24p: Boolean,
    onTrackDetails: (WarTrackDetails, Int) -> Unit
) {
    DashboardCard {
        Eyebrow(stringResource(R.string.currentwar_tracks_count, tracks.size))
        Spacer(Modifier.height(11.dp))
        // Grille 2 colonnes en lignes chunkées : évite d'imbriquer un LazyVerticalGrid
        // (même axe de scroll) dans le LazyColumn de l'écran.
        tracks.chunked(2).forEachIndexed { rowIndex, pair ->
            if (rowIndex > 0) Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { track ->
                    // Numéro de course (1-based) pour l'en-tête « Course N » de l'écran détail.
                    val courseNumber = tracks.indexOf(track) + 1
                    TrackCard(track = track, is24p = is24p, modifier = Modifier.weight(1f), onClick = { onTrackDetails(track, courseNumber) })
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * Cellule d'une course jouée — délègue à la cellule partagée [MKTrackCell] (extraite ici
 * puis mutualisée avec `AddTrackScreen`, rule 16). Clic → détail course.
 */
@Composable
private fun TrackCard(track: WarTrackDetails, is24p: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    MKTrackCell(modifier = modifier, track = track, is24p = is24p, onClick = onClick)
}
