package fr.harmoniamk.statsmkworld.ui.cells

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.diffColor
import fr.harmoniamk.statsmkworld.extension.displayName
import fr.harmoniamk.statsmkworld.model.local.PlayerScore
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.model.local.WarTrackDetails
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

/** Rayon uniforme des cartes translucides (maquette : radius 6px), aligné sur WelcomeScreen. */
val WarSummaryRadius = RoundedCornerShape(6.dp)

/**
 * Composants partagés du **résumé de war** (carte score, tracks, pastilles d'équipe) —
 * mutualisés entre `CurrentWarScreen` (war en cours) et `WarDetailsScreen` (war terminée),
 * mêmes écrans-frères du pôle Wars de la maquette 5 pôles (rule 16 : extraction dès un
 * 2ᵉ consommateur). Style pixel-perfect vs la maquette (rules 13/15).
 */

/** Carte dashboard : fond sombre translucide, bordure blanche, radius 6, padding 13. */
@Composable
fun WarDashboardCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Colors.blackAlphaed, WarSummaryRadius)
            .border(1.dp, Colors.whiteBorder, WarSummaryRadius)
            .padding(13.dp),
        content = content
    )
}

/** En-tête de section (eyebrow) : petit titre majuscule, blanc, espacé (cf. WelcomeScreen). */
@Composable
fun WarEyebrow(text: String) {
    MKText(
        text = text.uppercase(),
        fontSize = 12,
        font = Fonts.NunitoBD,
        textColor = Colors.white,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Carte « Score du match » (`.warscore`) : côté hôte VS côté(s) adversaire(s), chacun avec
 * pastille (avatar équipe ou initiales sur couleur), nom du roster et score (blanc).
 *
 * - [subtitle] optionnel affiché sous la ligne des scores, en blanc, centré (ex. « N courses
 *   restantes » pour la war en cours). Absent (null) pour une war terminée.
 * - En **24 j** (plusieurs adversaires), les côtés adverses sont empilés, sans score chiffré
 *   au niveau de la carte.
 *
 * La **différence de score seule** est affichée au centre, colorisée (vert > 0, rouge < 0,
 * blanc = 0). Un total de **shocks** de la war est affiché sous la ligne quand > 0.
 */
@Composable
fun WarScoreCard(
    teamHost: TeamEntity?,
    teamOpponent: List<TeamEntity>?,
    details: WarDetails,
    is24p: Boolean,
    subtitle: String? = null
) {
    // Écart signé du point de vue de l'hôte (avec pénalités) : colore la diff centrale.
    val margin = details.scoreMargin(is24p)
    val diffColor = margin.diffColor()
    // Total de pénalités par équipe (clé = teamId/rosterId). La clé hôte est war.teamHost
    // (rosterId), PAS teamHost.id (id d'équipe).
    val penaltyByTeam = details.war.penalties
        .groupBy { it.teamId }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
    // Total de shocks de la war (somme sur toutes les manches).
    val totalShocks = details.war.tracks.sumOf { it.shocks.orEmpty().sumOf { shock -> shock.count } }
    WarDashboardCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WarTeamSide(
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
                        WarTeamSide(
                            team = opponent,
                            score = null,
                            penalty = penaltyByTeam[opponent.id] ?: 0,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                else -> WarTeamSide(
                    team = teamOpponent?.firstOrNull(),
                    score = details.scoreOpponentWithPenalties,
                    penalty = teamOpponent?.firstOrNull()?.id?.let { penaltyByTeam[it] } ?: 0,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        // Sous-titre optionnel (ex. courses restantes), en blanc (non colorisé).
        subtitle?.let {
            Spacer(Modifier.height(6.dp))
            MKText(
                text = it,
                font = Fonts.Urbanist,
                textColor = Colors.white,
                fontSize = 14,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
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
fun WarTeamSide(
    team: TeamEntity?,
    score: Int?,
    penalty: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        WarTeamCrest(team = team)
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
fun WarTeamCrest(team: TeamEntity?) {
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
 * Section « Courses jouées » : **carte englobante** ([WarDashboardCard]) avec eyebrow
 * « Courses jouées · N » puis grille 2 colonnes des courses. Chaque cellule mène au détail
 * de la course (numéro 1-based passé à [onTrackDetails]).
 */
@Composable
fun WarTracksSection(
    tracks: List<WarTrackDetails>,
    is24p: Boolean,
    onTrackDetails: (WarTrackDetails, Int) -> Unit
) {
    WarDashboardCard {
        WarEyebrow(stringResource(R.string.currentwar_tracks_count, tracks.size))
        Spacer(Modifier.height(11.dp))
        // Grille 2 colonnes en lignes chunkées : évite d'imbriquer un LazyVerticalGrid
        // (même axe de scroll) dans le LazyColumn de l'écran.
        tracks.chunked(2).forEachIndexed { rowIndex, pair ->
            if (rowIndex > 0) Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { track ->
                    val courseNumber = tracks.indexOf(track) + 1
                    MKTrackCell(
                        modifier = Modifier.weight(1f),
                        track = track,
                        is24p = is24p,
                        onClick = { onTrackDetails(track, courseNumber) }
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * Carte « Classement joueurs » (`.card > .two > .b` de la maquette `wardetails`) : grille
 * 2 colonnes de tuiles (fond translucide) — **nom** en clé (petit, majuscule atténué) puis
 * **points** (gros chiffre Urbanist) + suffixe « pts ». Les joueurs sont classés par points
 * décroissants. Un compteur de shocks (icône + « xN ») s'affiche à côté du nom si applicable.
 *
 * Spécifique à `WarDetailsScreen` (war terminée). La war en cours utilise sa propre carte
 * « Joueurs » (lignes compactes), les deux dispositions différant dans la maquette.
 */
@Composable
fun WarPlayerRankingCard(title: String, players: List<PlayerScore>) {
    val ranked = players.sortedByDescending { it.score }
    WarDashboardCard {
        WarEyebrow(title)
        Spacer(Modifier.height(11.dp))
        ranked.chunked(2).forEachIndexed { rowIndex, pair ->
            if (rowIndex > 0) Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                pair.forEach { score ->
                    Column(
                        Modifier
                            .weight(1f)
                            .background(Colors.white30, WarSummaryRadius)
                            .padding(11.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MKText(
                                text = score.player?.name.orEmpty().displayName.uppercase(),
                                font = Fonts.NunitoBD,
                                textColor = Colors.white66,
                                fontSize = 10,
                                maxLines = 1,
                                textAlign = TextAlign.Start
                            )
                            score.shockCount.takeIf { it > 0 }?.let {
                                Spacer(Modifier.width(6.dp))
                                Image(
                                    painter = painterResource(R.drawable.shock),
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                if (it > 1) MKText(text = "x$it", fontSize = 10, textColor = Colors.white66)
                            }
                        }
                        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 5.dp)) {
                            MKText(
                                text = score.score.toString(),
                                font = Fonts.Urbanist,
                                textColor = Colors.white,
                                fontSize = 18,
                                textAlign = TextAlign.Start
                            )
                            MKText(
                                text = stringResource(R.string.wardetails_points_suffix),
                                textColor = Colors.white66,
                                fontSize = 10,
                                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                            )
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
