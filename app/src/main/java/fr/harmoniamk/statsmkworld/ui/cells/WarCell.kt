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
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

private val CellRadius = RoundedCornerShape(6.dp)

/**
 * Cellule de résultat de war, partagée par l'Accueil, les Wars et les stats. Deux rendus :
 * - **12p** : ligne de résultat (pastille V/N/D, adversaire, « vs … » + date, score + écart + maps) ;
 * - **24p** : podium des 3 scores.
 */
@Composable
fun WarCell(modifier: Modifier = Modifier, viewModel: WarCellViewModel, onClick: (WarDetails) -> Unit) {
    val state = viewModel.state.collectAsState()
    val is24p = state.value.teamOpponent.orEmpty().size > 1
    when (is24p) {
        true -> WarCell24p(modifier, state.value, onClick = { onClick(viewModel.details) })
        else -> WarCell12p(modifier, state.value, onClick = { onClick(viewModel.details) })
    }
}

/** Rendu 12p : ligne de résultat stylée + nombre de maps gagnées. */
@Composable
private fun WarCell12p(modifier: Modifier, state: WarCellViewModel.State, onClick: () -> Unit) {
    val opponent = state.teamOpponent?.firstOrNull()
    val diff = state.diff.orEmpty()
    val outcome = when {
        diff.startsWith("+") -> 1
        diff.startsWith("-") -> -1
        else -> 0
    }
    Row(
        modifier
            .background(Colors.blackAlphaed, CellRadius)
            .border(1.dp, Colors.whiteBorder, CellRadius)
            .clickable(onClick = onClick)
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
            MKText(text = state.date.orEmpty(), textColor = Colors.white55, fontSize = 11, textAlign = TextAlign.Start, modifier = Modifier.padding(top = 2.dp))
        }
        Column(horizontalAlignment = Alignment.End) {
            MKText(text = state.score.orEmpty(), font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 14, maxLines = 1)
            if (diff.isNotEmpty()) {
                MKText(text = diff, textColor = Colors.white55, fontSize = 11, maxLines = 1)
            }
            state.mapsWon?.let {
                MKText(text = stringResource(R.string.maps_won, it.toString()), textColor = Colors.white55, fontSize = 10, maxLines = 1, modifier = Modifier.padding(top = 1.dp))
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
 * Rendu 24p (3 équipes) : podium des scores, repris de l'implémentation historique.
 * Bordure verte/rouge selon que l'hôte est dans le top 2 des scores.
 */
@Composable
private fun WarCell24p(modifier: Modifier, state: WarCellViewModel.State, onClick: () -> Unit) {
    val scores = state.details?.scores?.sortedByDescending { it.score }.orEmpty()
    val hostInTop2 = scores.take(2).map { it.teamId }.contains(state.rosterId)
    val borderColor = if (hostInTop2) Colors.green else Colors.red
    Column(
        modifier
            .background(Colors.whiteAlphaed, RoundedCornerShape(5.dp))
            .border(2.dp, borderColor, RoundedCornerShape(5.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MKText(text = state.date.orEmpty(), modifier = Modifier.padding(top = 5.dp), fontSize = 12, maxLines = 1)
        Column(Modifier.padding(all = 15.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                scores.forEach { score ->
                    val team = state.teamOpponent.orEmpty().firstOrNull { it.id == score.teamId } ?: state.teamHost
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        when (val logo = team?.logo) {
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
                        MKText(text = team?.tag.orEmpty(), maxLines = 1)
                        MKText(
                            text = score.score.toString(),
                            fontSize = 20,
                            maxLines = 1,
                            font = Fonts.NunitoBD
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                state.details?.diffs?.forEach {
                    MKText(
                        modifier = Modifier.weight(3f),
                        text = it,
                        textAlign = TextAlign.Center,
                        font = Fonts.NunitoBD
                    )
                }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}
