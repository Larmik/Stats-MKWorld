package fr.harmoniamk.statsmkworld.screen.trackDetails

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.local.Maps
import fr.harmoniamk.statsmkworld.model.local.WarTrackDetails
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.cells.MapCell
import fr.harmoniamk.statsmkworld.ui.cells.PlayerCell

@Composable
fun TrackDetailsScreen(viewModel: TrackDetailsViewModel,
                       onBack: () -> Unit,
                       onEditTrack: (WarTrackDetails, Boolean) -> Unit
) {
    val state = viewModel.state.collectAsState()
    BackHandler { onBack() }
    BaseScreen(title = stringResource(R.string.resume)) {
        state.value.track?.let {
            MapCell(map = it.index.map { Maps.entries[it.toInt()] }, backgroundColor = Colors.transparent, textColor = Colors.black, borderColor = Colors.transparent, onClick = {}, is24p = state.value.trackScore != null)
        }
        when (val score = state.value.trackScore) {
            null -> {
                MKText(text = state.value.score.orEmpty(), fontSize = 32)
                MKText(text = state.value.diff.orEmpty(), fontSize = 24)
            }
            else -> MKText(text = score.toString(), fontSize = 32)
        }

        Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(columns = GridCells.Adaptive(150.dp)) {
            items(state.value.positions) {
                PlayerCell(player = it.player, position = it.position.position, shockCount = state.value.track?.track?.shocks?.firstOrNull { shock -> shock.playerId == it.player?.id }?.count, modifier = Modifier.padding(5.dp), onClick = { }, is24p = state.value.trackScore != null)
            }
        }

        if (state.value.buttonVisible) {
            MKButton(style = MKButtonStyle.Minor(Colors.black), text = stringResource(R.string.editer), onClick = {
                state.value.track?.let{
                    onEditTrack(it, state.value.trackScore != null)
                }


            })
        }

    }
}