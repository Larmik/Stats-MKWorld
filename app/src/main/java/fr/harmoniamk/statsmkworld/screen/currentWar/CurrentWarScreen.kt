package fr.harmoniamk.statsmkworld.screen.currentWar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import fr.harmoniamk.statsmkworld.model.local.WarTrackDetails
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.WarScoreView
import fr.harmoniamk.statsmkworld.ui.cells.MapCell
import fr.harmoniamk.statsmkworld.ui.cells.WarPlayersCell

@Composable
fun CurrentWarScreen(
    viewModel: CurrentWarViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onAddTrack: () -> Unit,
    onActions: () -> Unit,
    onTrackDetails: (WarTrackDetails) -> Unit
) {
    val state = viewModel.state.collectAsState()

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        viewModel.onResume()
    }

    LaunchedEffect(Unit) {
        viewModel.backToHome.collect {
            onBack()
        }
    }
    BackHandler { onBack() }
    BaseScreen(title = "War en cours") {

        when (val details = state.value.details) {
            null -> CircularProgressIndicator()
            else -> {
                WarScoreView(
                    teamHost = state.value.teamHost,
                    teamOpponent = state.value.teamOpponent,
                    score = state.value.details?.displayedScore.orEmpty(),
                    diff = state.value.details?.displayedDiff.orEmpty(),
                    penalties = state.value.details?.war?.penalties.orEmpty()
                )
                Spacer(Modifier.height(20.dp))
                WarPlayersCell(players = state.value.players)
                Row(
                    Modifier.padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (state.value.buttonsVisible) {

                        MKButton(
                            style = MKButtonStyle.Gradient, text = when (state.value.isOver) {
                                true -> "Valider la war"
                                else -> "Course suivante"
                            }, onClick = {
                                when (state.value.isOver) {
                                    true -> viewModel.onValidateWar()
                                    else -> onAddTrack()
                                }
                            })
                        MKButton(
                            modifier = Modifier.weight(1f),
                            style = MKButtonStyle.Minor(Colors.black),
                            text = "Plus d'actions",
                            onClick = onActions
                        )
                    }
                }
                Spacer(
                    (Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Colors.blackAlphaed))
                )

                details.warTracks.takeIf { it.isNotEmpty() }?.let {
                    MKText(
                        text = "Courses jouées (${it.size}/12):",
                        font = Fonts.NunitoBD,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                    LazyVerticalGrid(columns = GridCells.Adaptive(150.dp)) {
                        items(it) {
                            val borderColor = when {
                                it.displayedDiff.contains("+") -> Colors.green
                                it.displayedDiff.contains("-") -> Colors.red
                                else -> Colors.transparent
                            }
                            MapCell(
                                modifier = Modifier.padding(5.dp),
                                track = it,
                                onClick = {},
                                onTrackDetails = onTrackDetails,
                                backgroundColor = Colors.whiteAlphaed,
                                textColor = Colors.black,
                                borderColor = borderColor
                            )
                        }
                    }
                }
            }
            }

    }
}

