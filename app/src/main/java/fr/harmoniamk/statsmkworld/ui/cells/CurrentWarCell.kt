package fr.harmoniamk.statsmkworld.ui.cells

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

// Rayon aligné sur les cellules de résultat (WarCell12p) : cohérence visuelle.
private val CellRadius = RoundedCornerShape(6.dp)

/**
 * Cellule « war en cours », restylée à l'image des cellules de résultat
 * ([WarCell12p]) : carte `blackAlphaed` + bordure, pastille adversaire (avatar de
 * l'équipe ou tag sur cercle), « vs … », score + écart, et le **nombre de courses
 * restantes**. Utilisée dans [CurrentWarBanner] (Accueil + pôle Wars).
 * - **12p** (1 adversaire) : ligne de résultat façon `WarCell12p`.
 * - **24p** (3 équipes) : podium des 3 logos + score de l'hôte (style aligné).
 */
@Composable
fun CurrentWarCell(modifier: Modifier = Modifier, viewModel: CurrentWarCellViewModel, onClick: () -> Unit) {
    val state = viewModel.state.collectAsState()
    when (state.value.teamOpponent.orEmpty().size > 1) {
        true -> CurrentWarCell24p(modifier, state.value, onClick)
        else -> CurrentWarCell12p(modifier, state.value, onClick)
    }
}

/** Rendu 12p : ligne de résultat stylée (cf. [WarCell12p]) + courses restantes. */
@Composable
private fun CurrentWarCell12p(modifier: Modifier, state: CurrentWarCellViewModel.State, onClick: () -> Unit) {
    val opponent = state.teamOpponent?.firstOrNull()
    Row(
        modifier
            .background(Colors.blackAlphaed, CellRadius)
            .border(1.dp, Colors.whiteBorder, CellRadius)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        OpponentCrest(logo = opponent?.logo, tag = opponent?.tag)
        Column(Modifier.weight(1f)) {
            MKText(
                text = stringResource(R.string.home_vs, opponent?.name.orEmpty()),
                font = Fonts.NunitoBD,
                textColor = Colors.white,
                fontSize = 14,
                textAlign = TextAlign.Start,
                maxLines = 1
            )
            state.remaining?.let {
                MKText(
                    text = stringResource(R.string.remaining_courses, it.toString()),
                    textColor = Colors.white55,
                    fontSize = 11,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            MKText(text = state.score.orEmpty(), font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 14, maxLines = 1)
            state.diff?.takeIf { it.isNotEmpty() }?.let {
                MKText(text = it, textColor = Colors.white55, fontSize = 11, maxLines = 1)
            }
        }
    }
}

/** Rendu 24p : podium des 3 logos + score de l'hôte, carte alignée sur le style résultat. */
@Composable
private fun CurrentWarCell24p(modifier: Modifier, state: CurrentWarCellViewModel.State, onClick: () -> Unit) {
    Column(
        modifier
            .background(Colors.blackAlphaed, CellRadius)
            .border(1.dp, Colors.whiteBorder, CellRadius)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            (listOfNotNull(state.teamHost) + state.teamOpponent.orEmpty()).forEach { team ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    TeamLogo(team.logo)
                    MKText(text = team.name, maxLines = 1, textColor = Colors.white, fontSize = 13)
                }
            }
        }
        state.details?.scores?.firstOrNull { it.teamId == state.rosterId }?.let { score ->
            Spacer(Modifier.height(8.dp))
            MKText(text = score.score.toString(), fontSize = 20, maxLines = 1, font = Fonts.NunitoBD, textColor = Colors.white)
        }
        state.remaining?.let {
            Spacer(Modifier.height(4.dp))
            MKText(text = stringResource(R.string.remaining_courses, it.toString()), textColor = Colors.white55, fontSize = 11)
        }
    }
}

/** Pastille adversaire (cf. [WarCell12p]) : avatar équipe si dispo, sinon tag sur cercle. */
@Composable
private fun OpponentCrest(logo: String?, tag: String?) {
    when (logo) {
        null -> Box(
            Modifier.size(32.dp).clip(CircleShape).background(Colors.black).border(2.dp, Colors.white85, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MKText(text = tag.orEmpty(), font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 11, maxLines = 1)
        }
        else -> AsyncImage(
            model = "https://mkcentral.com$logo",
            contentDescription = null,
            modifier = Modifier.size(32.dp).clip(CircleShape).border(2.dp, Colors.white85, CircleShape)
        )
    }
}

/** Logo d'équipe (24p) : avatar MKCentral ou logo par défaut. */
@Composable
private fun TeamLogo(logo: String?) {
    when (logo) {
        null -> Image(
            painter = painterResource(R.drawable.default_logo),
            contentDescription = null,
            modifier = Modifier.size(30.dp)
        )
        else -> AsyncImage(
            model = "https://mkcentral.com$logo",
            contentDescription = null,
            modifier = Modifier.size(30.dp)
        )
    }
}
