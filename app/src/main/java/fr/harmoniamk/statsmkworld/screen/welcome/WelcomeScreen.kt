package fr.harmoniamk.statsmkworld.screen.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.cells.CurrentWarCell
import fr.harmoniamk.statsmkworld.ui.cells.CurrentWarCellViewModel
import fr.harmoniamk.statsmkworld.ui.cells.WarCell
import fr.harmoniamk.statsmkworld.ui.cells.WarCellViewModel

@Composable
fun WelcomeScreen(
    viewModel: WelcomeViewModel = hiltViewModel(),
    onTeamProfile: () -> Unit,
    onPlayerProfile: () -> Unit,
    onAddWar: (Boolean) -> Unit,
    onCurrentWar: () -> Unit,
    onWarDetailsClick: (WarDetails) -> Unit,
    onWarListClick: () -> Unit
) {
    val state = viewModel.state.collectAsState()
    BaseScreen(title = stringResource(R.string.accueil), modifier = Modifier.padding(bottom = 90.dp)) {

        when (state.value.playerName.isNullOrEmpty()) {
            true -> CircularProgressIndicator()
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileCell(
                        title = "Joueur",
                        modifier = Modifier.weight(1f),
                        name = state.value.playerName.orEmpty(),
                        image = state.value.playerLogo,
                        onClick = onPlayerProfile
                    )
                    Spacer(Modifier.width(20.dp))
                    ProfileCell(
                        title = "Equipe",
                        modifier = Modifier.weight(1f),
                        name = state.value.teamName.orEmpty(),
                        image = state.value.teamLogo,
                        onClick = onTeamProfile
                    )

                }
                Spacer(Modifier.height(10.dp))

                MKSegmentedSelector(
                    items = listOf(
                        "12 joueurs",
                        "24 joueurs"
                    ),
                    page = when (state.value.is24PEnabled) {
                        true -> 1
                        else -> 0
                    },
                    onClick = viewModel::onWarTypeSwitch
                )
                Spacer(Modifier.height(10.dp))

                if (state.value.currentWar == null && state.value.buttonVisible)
                    MKButton(style = MKButtonStyle.Gradient, text = stringResource(R.string.nouvelle_war), onClick = { onAddWar(state.value.is24PEnabled) }, modifier = Modifier.padding(bottom = 5.dp))

                Spacer((Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Colors.blackAlphaed)))

                state.value.currentWar?.let {
                    Spacer(Modifier.height(10.dp))
                    MKText(text = stringResource(R.string.war_en_cours),  fontSize = 16, font = Fonts.NunitoBD, modifier = Modifier.padding(bottom = 5.dp))
                    CurrentWarCell(onClick = onCurrentWar, viewModel = hiltViewModel(
                        key = it.id.toString() + it.tracks.joinToString { it.id.toString() },
                        creationCallback = { factory : CurrentWarCellViewModel.Factory ->
                            factory.create(it)
                        }
                    ))
                    Spacer(Modifier.height(10.dp))
                }
                when (state.value.wars.isEmpty()) {
                    true -> Column(Modifier
                        .weight(1f)
                        .padding(top = 15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        MKText(text = stringResource(R.string.welcome_title),  font = Fonts.NunitoBD, fontSize = 16)
                        MKText(text = stringResource(R.string.welcome_text), fontSize = 16)
                    }
                    else -> {
                        MKText(text = stringResource(R.string.last_results), fontSize = 16, font = Fonts.NunitoBD, modifier = Modifier.padding(top = 10.dp, bottom = 5.dp))
                        LazyColumn(Modifier.weight(1f)) {
                            items(state.value.wars) {
                                WarCell(
                                    modifier = Modifier.padding(vertical = 5.dp),
                                    viewModel = hiltViewModel(
                                        key = it.war.id.toString(),
                                        creationCallback = { factory: WarCellViewModel.Factory ->
                                            factory.create(it)
                                        }
                                    ),
                                    onClick = onWarDetailsClick)
                            }
                            item {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    MKButton(style = MKButtonStyle.Minor(Colors.black), text = stringResource(R.string.see_more), onClick = onWarListClick)
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}

@Composable
fun ProfileCell(title: String, modifier: Modifier = Modifier, name: String, image: String?, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier.clickable(onClick = onClick).background(Colors.whiteAlphaed, RoundedCornerShape(10.dp)).border(1.dp, Colors.black, RoundedCornerShape(10.dp))) {
        MKText(font = Fonts.NunitoIT, text = title, modifier = Modifier.padding(top = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
            when (image) {
                null -> Image(
                    painter = painterResource(R.drawable.default_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                )
                else -> AsyncImage(
                    model = image,
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                )
            }
            MKText(text = name, font = Fonts.NunitoBD, textColor = Colors.black, fontSize = 16)
        }
    }
}