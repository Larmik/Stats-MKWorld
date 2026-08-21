package fr.harmoniamk.statsmkworld.screen.warDetails

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
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.local.WarDetails
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
fun WarDetailsScreen(
    viewModel: WarDetailsViewModel,
    onBack: () -> Unit,
    onTrackClick: (WarTrackDetails, Int, Boolean) -> Unit,
    onTab: (WarDetails) -> Unit
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    BackHandler { onBack() }
    BaseScreen(title = stringResource(R.string.details_war)) {
        state.value.details?.let {
            WarScoreView(
                teamHost = state.value.teamHost,
                teamOpponent = state.value.teamOpponent,
                details = state.value.details,
                rosterName = state.value.roster?.name,
            )
            Spacer(Modifier.height(20.dp))
            WarPlayersCell(players = state.value.players, trackCount = state.value.details?.warTracks.orEmpty().size)
            if (state.value.details?.war?.teamOpponent.orEmpty().size == 1)
                Row(
                    Modifier.padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    MKButton(style = MKButtonStyle.Minor(Colors.black), text = "Tab") {
                        viewModel.warDetails?.let { onTab(it) }
                    }
                }
            it.warTracks.takeIf { it.isNotEmpty() }?.let {
                MKText(
                    text = stringResource(R.string.courses_jouees),
                    font = Fonts.NunitoBD,
                    modifier = Modifier.padding(top = 10.dp)
                )
                val tracks = it
                LazyVerticalGrid(columns = GridCells.Adaptive(150.dp)) {
                    items(tracks, key = { it.track.id }) { track ->
                        val borderColor = when {
                            state.value.teamOpponent.orEmpty().size > 1 -> Colors.transparent
                            track.displayedDiff.contains("+") -> Colors.green
                            track.displayedDiff.contains("-") -> Colors.red
                            else -> Colors.transparent
                        }
                        // Numéro de course (1-based) + course finale = dernière de la war (ticket #47).
                        val courseNumber = tracks.indexOf(track) + 1
                        val isFinal = courseNumber == tracks.size
                        MapCell(
                            modifier = Modifier.padding(5.dp),
                            track = track,
                            onClick = {},
                            is24p = state.value.teamOpponent.orEmpty().size > 1,
                            onTrackDetails = { onTrackClick(it, courseNumber, isFinal) },
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
