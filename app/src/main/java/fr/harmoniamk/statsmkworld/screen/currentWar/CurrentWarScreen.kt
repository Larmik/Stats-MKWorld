package fr.harmoniamk.statsmkworld.screen.currentWar

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.model.local.WarTrackDetails
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.MKTextField
import fr.harmoniamk.statsmkworld.ui.WarScoreView
import fr.harmoniamk.statsmkworld.ui.cells.MapCell
import fr.harmoniamk.statsmkworld.ui.cells.TeamCell
import fr.harmoniamk.statsmkworld.model.ScoringConstants
import fr.harmoniamk.statsmkworld.ui.cells.WarPlayersCell
import kotlinx.coroutines.launch

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
    val pagerState = rememberPagerState(pageCount = {
        when (state.value.teamOpponent.orEmpty().size > 1) {
            true -> 2
            else -> 1
        }
    })
    val context = LocalContext.current


    LaunchedEffect(viewModel) {
        launch {
            viewModel.backToHome.collect {
                onWarValidated()
            }
        }
        launch {
            viewModel.onPage.collect {
                pagerState.animateScrollToPage(it)
            }
        }
        launch {
            viewModel.onToast.collect {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler { onBack() }
    HorizontalPager(
        modifier = Modifier.fillMaxWidth(),
        state = pagerState,
        userScrollEnabled = false
    ) {
        when (it) {
         0 -> BaseScreen(title = stringResource(R.string.war_en_cours)) {

             when (val details = state.value.details) {
                 null -> CircularProgressIndicator()
                 else -> {
                     val is24p = state.value.teamOpponent.orEmpty().size > 1
                     WarScoreView(
                         teamHost = state.value.teamHost,
                         teamOpponent = state.value.teamOpponent,
                         details = state.value.details,
                         rosterName = state.value.roster?.name,
                         rosterId = state.value.roster?.id.toString(),
                     )
                     Spacer(Modifier.height(20.dp))
                     WarPlayersCell(players = state.value.players, trackCount = details.warTracks.size)
                     CurrentWarActionButtons(
                         buttonsVisible = state.value.buttonsVisible,
                         isOver = state.value.isOver,
                         is24p = is24p,
                         onNext = { viewModel.onPageChange(1) },
                         onValidateWar = viewModel::onValidateWar,
                         onAddTrack = { onAddTrack(is24p) },
                         onActions = onActions
                     )
                     Spacer(
                         (Modifier
                             .fillMaxWidth()
                             .height(1.dp)
                             .background(Colors.blackAlphaed))
                     )

                     CurrentWarTracksGrid(
                         tracks = details.warTracks,
                         is24p = is24p,
                         onTrackDetails = onTrackDetails
                     )
                 }
             }

         }
            else -> BaseScreen(title = "Scores adversaires") {
                OpponentScoresPage(
                    teamOpponent = state.value.teamOpponent,
                    opponentsScores = state.value.opponentsScores,
                    onValueChange = viewModel::onValueChange,
                    onValidateScore = viewModel::onValidateScore
                )
            }
        }
    }

}

@Composable
private fun CurrentWarActionButtons(
    buttonsVisible: Boolean,
    isOver: Boolean,
    is24p: Boolean,
    onNext: () -> Unit,
    onValidateWar: () -> Unit,
    onAddTrack: () -> Unit,
    onActions: () -> Unit
) {
    Row(
        Modifier.padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (buttonsVisible) {
            MKButton(
                style = MKButtonStyle.Gradient, text = when (isOver) {
                    true -> if (is24p)
                        "Scores adversaires"
                    else
                        stringResource(R.string.valider_la_war)

                    else -> stringResource(R.string.course_suivante)
                }, onClick = {
                    when (isOver) {
                        true -> if (is24p)
                            onNext()
                        else
                            onValidateWar()

                        else -> onAddTrack()
                    }
                })
            MKButton(
                modifier = Modifier.weight(1f),
                style = MKButtonStyle.Minor(Colors.black),
                text = stringResource(R.string.more_actions),
                onClick = onActions
            )
        }
    }
}

@Composable
private fun CurrentWarTracksGrid(
    tracks: List<WarTrackDetails>,
    is24p: Boolean,
    onTrackDetails: (WarTrackDetails) -> Unit
) {
    tracks.takeIf { it.isNotEmpty() }?.let {
        MKText(
            text = stringResource(R.string.player_courses, it.size),
            font = Fonts.NunitoBD,
            modifier = Modifier.padding(top = 10.dp)
        )
        LazyVerticalGrid(columns = GridCells.Adaptive(150.dp)) {
            items(it, key = { it.track.id }) {
                val borderColor = when {
                    is24p -> Colors.transparent
                    it.displayedDiff.contains("+") -> Colors.green
                    it.displayedDiff.contains("-") -> Colors.red
                    else -> Colors.transparent
                }
                MapCell(
                    modifier = Modifier.padding(5.dp),
                    track = it,
                    onClick = {},
                    onTrackDetails = onTrackDetails,
                    is24p = is24p,
                    backgroundColor = Colors.whiteAlphaed,
                    textColor = Colors.black,
                    borderColor = borderColor
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.OpponentScoresPage(
    teamOpponent: List<TeamEntity>?,
    opponentsScores: Map<String, Int>,
    onValueChange: (String, String) -> Unit,
    onValidateScore: () -> Unit
) {
    MKText(text = "Veuillez renseigner les scores adverses dans les champs correspondants. \n Il faut inscrire les scores tels qu'ils ont été calculés par le jeu, le total des points étant de ${ScoringConstants.TOTAL_24P_SCORE}. \n \n Ne tenez pas compte des pénalités.")
    teamOpponent?.forEach { team ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            TeamCell(team = team, modifier = Modifier.size(120.dp), tagVisible = false) { }
            MKTextField(
                textColor = Colors.black,
                borderColor = Colors.black,
                backgroundColor = Colors.white,
                value = opponentsScores[team.id]?.toString().orEmpty(),
                onValueChange = { value -> onValueChange(team.id, value) }, modifier = Modifier.width(100.dp))
            Spacer(Modifier.weight(1f))
        }
    }
    MKButton(
        style = MKButtonStyle.Gradient,
        text = "Valider la war",
        onClick = onValidateScore
    )
    Spacer(Modifier.height(1.dp))
}

