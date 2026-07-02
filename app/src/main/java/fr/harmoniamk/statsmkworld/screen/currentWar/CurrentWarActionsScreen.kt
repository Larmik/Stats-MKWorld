package fr.harmoniamk.statsmkworld.screen.currentWar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.selectors.PenaltyType
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
import fr.harmoniamk.statsmkworld.ui.MKSelectorViewPager
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.VerticalGrid
import fr.harmoniamk.statsmkworld.ui.cells.PlayerCell
import kotlinx.coroutines.launch

@Composable
fun CurrentWarActionsScreen(
    viewModel: CurrentWarActionsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onBackToWelcome: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val state = viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        launch {
            viewModel.backToWelcome.collect {
                onBackToWelcome()
            }
        }
        launch {
            viewModel.onBack.collect {
                onBack()
            }
        }
    }

    BaseScreen(title = stringResource(R.string.actions)) {
        val penaltyLabel = stringResource(R.string.penalties)
        val subLabel = stringResource(R.string.remplacement)
        val cancelLabel = stringResource(R.string.cancel_war)
        MKSelectorViewPager(pagerState, listOf(penaltyLabel, subLabel, cancelLabel)) {
            when (pagerState.currentPage) {
                0 -> {
                    Column {
                        LazyVerticalGrid(modifier = Modifier.weight(1f), columns = GridCells.Fixed(2)) {
                            items(state.value.penalties.orEmpty()) {
                                val backgroundColor = when (it.isSelected) {
                                    true -> Colors.whiteAlphaed
                                    else -> Colors.blackAlphaed
                                }
                                val textColor = when (it.isSelected) {
                                    true -> Colors.black
                                    else -> Colors.white
                                }
                                Box(Modifier.padding(5.dp)) {
                                    Row(Modifier
                                        .background(backgroundColor, RoundedCornerShape(5.dp))
                                        .border(1.dp, Colors.white, RoundedCornerShape(5.dp))
                                        .clickable { viewModel.onPenaltySelected(it) }
                                        .heightIn(min = 120.dp)
                                        .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                        val teamsList = listOf(state.value.teamHost) + state.value.teamOpponent.orEmpty()
                                        val text = when (val penalty = it.penalty) {
                                            is PenaltyType.Minus10 -> "-10 " + context.getString(
                                                R.string.penalty_placeholder,
                                                teamsList.singleOrNull { it?.id == penalty.teamId }?.name
                                                    ?: state.value.teamHost?.name.orEmpty()
                                            )
                                            is PenaltyType.Minus15 -> "-15 " + context.getString(
                                                R.string.penalty_placeholder,
                                                teamsList.singleOrNull { it?.id == penalty.teamId }?.name
                                                    ?: state.value.teamHost?.name.orEmpty()
                                            )
                                            is PenaltyType.Minus20 -> "-20 " + context.getString(
                                                R.string.penalty_placeholder,
                                                teamsList.singleOrNull { it?.id == penalty.teamId }?.name
                                                    ?: state.value.teamHost?.name.orEmpty()
                                            )
                                        }
                                        MKText(modifier = Modifier.padding(5.dp), text = text, font = Fonts.NunitoBD, textColor = textColor)
                                    }
                                }

                            }
                        }
                        Spacer(Modifier.height(30.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            MKButton(style = MKButtonStyle.Gradient, text = stringResource(R.string.valider), onClick = {
                                viewModel.onPenaltyValidated()
                                onBack()
                            }, enabled = state.value.penalties?.any { it.isSelected } == true)
                            Spacer(Modifier.width(10.dp))
                            MKButton(style = MKButtonStyle.Minor(Colors.black), text = stringResource(R.string.cancel), onClick = {
                                viewModel.clearPenalties()
                                onBack()
                            })
                        }
                    }
                }
                1 -> {
                    Column(Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {

                        MKText(text = stringResource(R.string.joueur_sortant), font = Fonts.NunitoBD, fontSize = 16)
                        VerticalGrid {
                            state.value.currentPlayers?.forEach {
                                val textColor = when (it.isSelected) {
                                    true -> Colors.black
                                    else -> Colors.white
                                }
                                val backgroundColor = when (it.isSelected) {
                                    true -> Colors.whiteAlphaed
                                    else -> Colors.blackAlphaed
                                }
                                PlayerCell(
                                    modifier = Modifier
                                        .padding(5.dp)
                                        .fillMaxWidth(0.48f),
                                    player = it.player,
                                    textColor = textColor,
                                    backgroundColor = backgroundColor,
                                    onClick = viewModel::onOldPlayerSelected
                                )
                            }

                        }
                        Spacer(Modifier.height(20.dp))
                        MKText(text = stringResource(R.string.joueur_entrant), font = Fonts.NunitoBD, fontSize = 16)
                        VerticalGrid {
                            state.value.otherPlayers?.forEach {
                                val textColor = when (it.isSelected) {
                                    true -> Colors.black
                                    else -> Colors.white
                                }
                                val backgroundColor = when (it.isSelected) {
                                    true -> Colors.whiteAlphaed
                                    else -> Colors.blackAlphaed
                                }
                                PlayerCell(
                                    modifier = Modifier
                                        .padding(5.dp)
                                        .fillMaxWidth(0.48f),
                                    player = it.player,
                                    textColor = textColor,
                                    backgroundColor = backgroundColor,
                                    onClick = viewModel::onNewPlayerSelected
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            MKButton(style = MKButtonStyle.Gradient, text = stringResource(R.string.remplacer), enabled = state.value.currentPlayers.orEmpty().any { it.isSelected } && state.value.otherPlayers.orEmpty().any { it.isSelected }, onClick = viewModel::onSub)
                            Spacer(Modifier.width(10.dp))
                            MKButton(style = MKButtonStyle.Minor(Colors.black), text = stringResource(R.string.cancel), onClick = onBack)
                        }

                    }
                }
                else -> {
                    MKText(text = stringResource(R.string.cancel_confirm))
                    Spacer(Modifier.height(15.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        MKButton(
                            style = MKButtonStyle.Gradient,
                            text = stringResource(R.string.delete_war),
                            onClick = viewModel::cancelWar
                        )
                        Spacer(Modifier.width(10.dp))
                        MKButton(style = MKButtonStyle.Minor(Colors.black), text = stringResource(R.string.cancel), onClick = onBack)
                    }
                }
            }
        }
    }


}

@Preview
@Composable
fun ActionsPreview() {
    CurrentWarActionsScreen(onBack = {}) {

    }
}