package fr.harmoniamk.statsmkworld.screen.currentWar

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.MKTextField
import fr.harmoniamk.statsmkworld.ui.VerticalGrid
import fr.harmoniamk.statsmkworld.ui.cells.PlayerCell
import kotlinx.coroutines.launch
import kotlin.text.isEmpty

@Composable
fun CurrentWarActionsScreen(
    viewModel: CurrentWarActionsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onBackToWelcome: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val state = viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val rows = viewModel.rows.collectAsState()
    val valuesListName = remember {
        mutableStateListOf(
            "", "", "", "", "", "", "", "", ""
        )
    }
    val valuesListScore = remember {
        mutableStateListOf(
            "", "", "", "", "", "", "", "", ""
        )
    }

    LaunchedEffect(Unit) {
        viewModel.toast.collect {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uri.collect {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, it)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Partager l'image"))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.backToWelcome.collect {
            onBackToWelcome()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.onBack.collect {
            onBack()
        }
    }

    BaseScreen(title = stringResource(R.string.actions)) {
        Row(
            Modifier
                .fillMaxWidth()
                .border(1.dp, Colors.blackAlphaed),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val text = when (iteration) {
                    0 -> stringResource(R.string.penalties)
                    1 -> stringResource(R.string.remplacement)
                    2 -> "Tab"
                    else -> stringResource(R.string.cancel_war)
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
                                               PenaltyType.REPICK_HOST -> context.getString(
                                                   R.string.repick_placeholder,
                                                   state.value.teamHost
                                               )
                                               PenaltyType.INTERMISSION_HOST -> context.getString(
                                                   R.string.intermission_placeholder,
                                                   state.value.teamHost
                                               )
                                               PenaltyType.REPICK_OPPONENT ->  context.getString(
                                                   R.string.repick_placeholder,
                                                   state.value.teamOpponent
                                               )
                                               PenaltyType.INTERMISSION_OPPONENT -> context.getString(
                                                   R.string.intermission_placeholder,
                                                   state.value.teamOpponent
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

                    2 -> {
                        when (state.value.war?.tracks?.size) {
                            12 -> {
                                MKText(
                                    modifier = Modifier.padding(bottom = 10.dp),
                                    text = "Génère un tableau de résultat à l'aide du pseudo des adversaires et de leurs scores. Tu peux rentrer jusqu'à 9 scores différents. \n Dans le cas d'une war incomplète, n'oublies pas d'indiquer le nombre de courses jouées entre parenthèses, après le pseudo. \n Il est inutile de rentrer les pénalités."
                                )
                                LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
                                    items(rows.value) { index ->
                                        Row {
                                            Box(Modifier.weight(2f)) {
                                                MKTextField(
                                                    value = valuesListName[index],
                                                    backgroundColor = Colors.blackAlphaed,
                                                    onValueChange = {
                                                        valuesListName[index] = it
                                                    },
                                                    placeHolder = "Nom adversaire ${index + 1}",
                                                    keyboardType = KeyboardType.Text,
                                                    imeAction = ImeAction.Next
                                                )
                                            }
                                            Box(Modifier.weight(1f)) {
                                                MKTextField(
                                                    value = valuesListScore[index],
                                                    backgroundColor = Colors.blackAlphaed,
                                                    onValueChange = {
                                                        valuesListScore[index] = it
                                                    },
                                                    placeHolder = "Score",
                                                    keyboardType = KeyboardType.Number,
                                                    imeAction = when (index == rows.value - 1) {
                                                        true -> ImeAction.Done
                                                        else -> ImeAction.Next
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    item {
                                        Row {
                                            MKButton(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(5.dp),
                                                style = MKButtonStyle.Minor(Colors.black),
                                                text = "Supprimer une ligne",
                                                enabled = rows.value > 6,
                                                onClick = { viewModel.onManageRows(false) })
                                            MKButton(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(5.dp),
                                                style = MKButtonStyle.Minor(Colors.black),
                                                text = "Ajouter une ligne",
                                                enabled = rows.value < 9,
                                                onClick = { viewModel.onManageRows(true) })
                                        }
                                    }
                                    item {
                                        MKButton(
                                            style = MKButtonStyle.Gradient,
                                            text = "Générer",
                                            onClick = {
                                                viewModel.onGenerate(
                                                    players = valuesListName.toList().filterNot { it.isEmpty() },
                                                    scores = valuesListScore.toList().filterNot { it.isEmpty() })
                                            })
                                    }
                                }
                            } else ->   MKText(
                            modifier = Modifier.padding(bottom = 10.dp),
                            text = "La war n'est pas encore terminée. L'édition de tab n'est possible qu'une fois que les 12 courses ont été saisies."
                        )
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


}

@Preview
@Composable
fun ActionsPreview() {
    CurrentWarActionsScreen(onBack = {}) {

    }
}