package fr.harmoniamk.statsmkworld.screen.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.cells.CurrentWarCell
import fr.harmoniamk.statsmkworld.ui.cells.WarCell
import fr.harmoniamk.statsmkworld.ui.cells.WarCellViewModel

@Composable
fun WelcomeScreen(
    viewModel: WelcomeViewModel = hiltViewModel(),
    onTeamProfile: () -> Unit,
    onPlayerProfile: () -> Unit,
    onAddWar: () -> Unit,
    onCurrentWar: () -> Unit,
    onWarDetailsClick: (WarDetails) -> Unit
) {
    val state = viewModel.state.collectAsState()
    BaseScreen(title = "Accueil", modifier = Modifier.padding(bottom = 90.dp)) {

        when (state.value.playerName.isNullOrEmpty()) {
            true -> CircularProgressIndicator()
            else -> {
                Row(
                    Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            state.value.teamLogo?.let {
                                AsyncImage(
                                    model = state.value.teamLogo,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MKText(
                                    text = state.value.teamName.orEmpty(),
                                    font = Fonts.NunitoBD,
                                    fontSize = 18,
                                )
                                Box(
                                    Modifier
                                        .border(
                                            1.dp,
                                            color = Colors.black,
                                            RoundedCornerShape(5.dp)
                                        )
                                        .clickable { onTeamProfile() }
                                ) {
                                    MKText(
                                        text = "Voir l'équipe",
                                        font = Fonts.NunitoIT,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onPlayerProfile() }) {
                        state.value.playerLogo?.let {
                            AsyncImage(
                                model = state.value.playerLogo,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                            )
                        }
                        MKText(
                            text = state.value.playerName.orEmpty()
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Spacer((Modifier.fillMaxWidth().height(1.dp).background(Colors.blackAlphaed)))
                state.value.currentWar?.let {
                    Spacer(Modifier.height(10.dp))
                    MKText(text = "War en cours :",  fontSize = 16, font = Fonts.NunitoBD, modifier = Modifier.padding(bottom = 5.dp))
                    CurrentWarCell(onClick = onCurrentWar)
                    Spacer(Modifier.height(10.dp))
                }
                when (state.value.wars.isEmpty()) {
                    true -> Column(Modifier.weight(1f).padding(top = 15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        MKText(text = "Bienvenue, joueur de MKWorld ! \n \n",  font = Fonts.NunitoBD, fontSize = 16)
                        MKText(text = "Ton équipe n'a pas encore enregistré de résultats sur cette application. Pour créer une nouvelle war, tu peux cliquer sur le bouton ci-dessous si tu as le rôle approprié. \n \n Si tu ne peux pas saisir de résultats, rapproche-toi d'un leader de ton équipe pour qu'il t'accorde le bon rôle.", fontSize = 16)
                    }
                    else -> {
                        MKText(text = "Derniers résultats :", fontSize = 16, font = Fonts.NunitoBD, modifier = Modifier.padding(top = 10.dp, bottom = 5.dp))
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
                        }
                    }
                }
                if (state.value.currentWar == null)
                    MKButton(style = MKButtonStyle.Gradient, text = "Nouvelle war", onClick = onAddWar, modifier = Modifier.padding(bottom = 5.dp), enabled = state.value.buttonVisible)
            }
        }

    }
}