package fr.harmoniamk.statsmkworld.ui.cells

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import coil.compose.AsyncImage
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.extension.safeSubList
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

@Composable
fun WarCell(modifier: Modifier = Modifier, viewModel: WarCellViewModel, onClick: (WarDetails) -> Unit) {
    val state = viewModel.state.collectAsState()
    val scores = state.value.details?.scores?.sortedByDescending { it.score }.orEmpty()
    val is24p = state.value.details?.war?.teamOpponent.orEmpty().size > 1

    val borderColor = when {
        is24p && scores.safeSubList(0,2).map { it.teamId }.contains(state.value.rosterId) -> Colors.green
        is24p && !scores.safeSubList(0,2).map { it.teamId }.contains(state.value.rosterId) -> Colors.red
        state.value.diff?.startsWith("-") == true -> Colors.red
        state.value.diff?.startsWith("+") == true -> Colors.green
        else -> Colors.transparent
    }
    Column(modifier.background(Colors.whiteAlphaed, RoundedCornerShape(5.dp)).border(2.dp, borderColor, RoundedCornerShape(5.dp)).clickable { onClick(viewModel.details) }, horizontalAlignment = Alignment.CenterHorizontally) {
        MKText(text = state.value.date.orEmpty(), modifier = Modifier.padding(top = 5.dp), fontSize = 12, maxLines = 1)

        when (is24p) {
            true ->  Column(Modifier.padding(all = 15.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    scores.forEach { score ->
                        val team =
                            state.value.teamOpponent.orEmpty().firstOrNull { it.id == score.teamId } ?: state.value.teamHost
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
                            MKText(text = when (is24p){
                                true -> team?.tag
                                else -> team?.name
                            }.orEmpty(), maxLines = 1)
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
                    state.value.details?.diffs?.forEach {
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
            else ->  Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    when (val logo = state.value.teamHost?.logo) {
                        null -> Image(
                            painter = painterResource(R.drawable.default_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(35.dp)
                                .clip(CircleShape)
                        )
                        else -> AsyncImage(
                            model =  "https://mkcentral.com$logo",
                            contentDescription = null,
                            modifier = Modifier
                                .size(35.dp)
                                .clip(CircleShape)
                        )
                    }
                    MKText(text = state.value.rosterName.orEmpty(), maxLines = 1, fontSize = 12)
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    MKText(text = state.value.score.orEmpty(), fontSize = 20, font = Fonts.NunitoBD, maxLines = 1)
                    MKText(text = state.value.diff.orEmpty(), font = Fonts.NunitoBD)
                    state.value.mapsWon?.let {
                        MKText(text = stringResource(R.string.maps_won, it.toString()), fontSize = 12, font = Fonts.NunitoIT, maxLines = 1, modifier = Modifier.padding(bottom = 5.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    state.value.teamOpponent?.forEach {
                        when (val logo = it.logo) {
                            null -> Image(
                                painter = painterResource(R.drawable.default_logo),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(35.dp)
                                    .clip(CircleShape)
                            )
                            else ->  AsyncImage(
                                model = "https://mkcentral.com$logo",
                                contentDescription = null,
                                modifier = Modifier
                                    .size(35.dp)
                                    .clip(CircleShape)
                            )
                        }

                        MKText(text = it.name, textColor = Colors.black, maxLines = 1, fontSize = 12)

                    }
                }
            }
        }

    }

}