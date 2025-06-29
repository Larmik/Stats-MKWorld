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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import fr.harmoniamk.statsmkworld.model.local.PenaltyType
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
import fr.harmoniamk.statsmkworld.ui.MKText
import kotlinx.coroutines.launch

@Composable
fun CurrentWarActionsScreen(
    viewModel: CurrentWarActionsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onBackToWelcome: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val state = viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.bavkToWelcome.collect {
            onBackToWelcome()
        }
    }

    BaseScreen(title = "Actions") {
        Row(
            Modifier
                .fillMaxWidth()
                .border(1.dp, Colors.blackAlphaed),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val text = when (iteration) {
                    0 -> "Pénalité"
                    1 -> "Remplacement"
                    else -> "Annuler la war"
                }
                val bgColor = when (iteration == pagerState.currentPage) {
                    true -> Colors.blackAlphaed
                    else -> Colors.transparent
                }
                val textColor = when (iteration == pagerState.currentPage) {
                    true -> Colors.white
                    else -> Colors.black
                }

                Box(Modifier
                    .weight(1f)
                    .background(bgColor)
                    .clickable {
                        scope.launch {
                            pagerState.animateScrollToPage(iteration)
                        }
                    }) {
                    MKText(
                        text = text,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(vertical = 10.dp),
                        font = Fonts.Urbanist,
                        textColor = textColor,
                        fontSize = 16,
                        maxLines = 1
                    )
                }

            }
        }
        HorizontalPager(
            beyondViewportPageCount = 2,
            state = pagerState
        ) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                when (pagerState.currentPage) {
                    0 -> {
                        Column {
                           LazyVerticalGrid(columns = GridCells.Fixed(2)) {
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
                                           val text = when (it.penalty) {
                                               PenaltyType.REPICK_HOST -> "Repick de ${state.value.teamHost}"
                                               PenaltyType.INTERMISSION_HOST -> "Intermission votée par ${state.value.teamHost}"
                                               PenaltyType.REPICK_OPPONENT -> "Repick de ${state.value.teamOpponent}"
                                               PenaltyType.INTERMISSION_OPPONENT -> "Intermission votée par ${state.value.teamOpponent}"
                                           }
                                           MKText(modifier = Modifier.padding(5.dp), text = text, font = Fonts.NunitoBD, textColor = textColor)
                                       }
                                   }

                               }
                           }
                            Spacer(Modifier.height(30.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                MKButton(style = MKButtonStyle.Gradient, text = "Valider", onClick = {
                                    viewModel.onPenaltyValidated()
                                    onBack()
                                }, enabled = state.value.penalties?.any { it.isSelected } == true)
                                Spacer(Modifier.width(10.dp))
                                MKButton(style = MKButtonStyle.Minor(Colors.black), text = "Annuler", onClick = {
                                    viewModel.clearPenalties()
                                    onBack()
                                })
                            }
                        }
                    }

                    1 -> {
                        MKText(text = "Les remplacements ne sont pas encore disponible. Reviens plus tard !")
                    }

                    else -> {
                        MKText(text = "En annulant la war, tu perdras toute la progression ainsi que les statistiques générées par celle-ci. \n \n Es-tu sûr(e) de vouloir effacer la partie ?")
                        Spacer(Modifier.height(15.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            MKButton(
                                style = MKButtonStyle.Gradient,
                                text = "Effacer la war",
                                onClick = viewModel::cancelWar
                            )
                            Spacer(Modifier.width(10.dp))
                            MKButton(style = MKButtonStyle.Minor(Colors.black), text = "Annuler", onClick = onBack)
                        }
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