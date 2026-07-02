package fr.harmoniamk.statsmkworld.ui.cells

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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

@Composable
fun CurrentWarCell(modifier: Modifier = Modifier, viewModel: CurrentWarCellViewModel, onClick: () -> Unit) {
    val state = viewModel.state.collectAsState()
    Column(modifier.background(Colors.blackAlphaed, RoundedCornerShape(5.dp)).border(1.dp, Colors.white, RoundedCornerShape(5.dp)).clickable { onClick() }, horizontalAlignment = Alignment.CenterHorizontally) {

        when (state.value.teamOpponent.orEmpty().size > 1) {
            true -> Column(Modifier.padding(all = 15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                        val team = state.value.teamHost
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
                            MKText(text = team?.name.orEmpty(), maxLines = 1, textColor = Colors.white)

                        }
                    state.value.teamOpponent?.forEach {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            when (val logo = it.logo) {
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
                            MKText(text = it.name, maxLines = 1, textColor = Colors.white)

                        }
                    }
                    }
                state.value.details?.scores?.firstOrNull { it.teamId == state.value.rosterId }?.let { score ->
                    Spacer(Modifier.height(10.dp))
                    MKText(
                        text = score.score.toString(),
                        fontSize = 20,
                        maxLines = 1,
                        font = Fonts.NunitoBD,
                        textColor = Colors.white
                    )
                }
            }
            else -> Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        when (val logo = state.value.teamHost?.logo) {
                            null -> Image(
                                painter = painterResource(R.drawable.default_logo),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                            )
                            else -> AsyncImage(
                                model =  "https://mkcentral.com$logo",
                                contentDescription = null,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                            )
                        }

                        MKText(text = state.value.rosterName.orEmpty(), textColor = Colors.white, fontSize = 16, font = Fonts.NunitoRG, maxLines = 1)
                    }

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        MKText(text = state.value.score.orEmpty(), textColor = Colors.white, fontSize = 28, font = Fonts.NunitoBD, maxLines = 1)
                        MKText(text = state.value.diff.orEmpty(), textColor = Colors.white, fontSize = 20, font = Fonts.NunitoBD, maxLines = 1)
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        state.value.teamOpponent?.forEach {
                            when (val logo = it.logo) {
                                null -> Image(
                                    painter = painterResource(R.drawable.default_logo),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                )
                                else -> AsyncImage(
                                    model =  "https://mkcentral.com$logo",
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                )
                            }
                            MKText(text = it.name, textColor = Colors.white, fontSize = 16, font = Fonts.NunitoRG, maxLines = 1)

                        }
                    }

                }

        }

        MKText(text = stringResource(R.string.remaining_courses, state.value.remaining.toString()), textColor = Colors.white, modifier = Modifier.padding(bottom = 5.dp))
    }
}