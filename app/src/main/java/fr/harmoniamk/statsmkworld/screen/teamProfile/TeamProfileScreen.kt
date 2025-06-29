package fr.harmoniamk.statsmkworld.screen.teamProfile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.extension.countryFlag
import fr.harmoniamk.statsmkworld.extension.displayedString
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.VerticalGrid
import fr.harmoniamk.statsmkworld.ui.cells.PlayerCell
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TeamProfileScreen(viewModel: TeamProfileViewModel, onBack: () -> Unit, onPlayerClick: (String) -> Unit) {
    val state = viewModel.state.collectAsState()
    BackHandler { onBack() }
    BaseScreen(title = "Profil équipe") {
        when (val team = state.value.team) {
            null -> CircularProgressIndicator()
            else -> {
                team.logo?.let {
                    AsyncImage(model = "https://mkcentral.com$it", contentDescription = null, modifier = Modifier.size(80.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    MKText(text = team.language.uppercase().countryFlag, fontSize = 30)
                    MKText(text = team.name, fontSize = 18, font = Fonts.NunitoBD)

                }
                MKText(
                    text = team.description,
                    modifier = Modifier.padding(bottom = 10.dp),
                    fontSize = 16,
                    font = Fonts.NunitoIT
                )


                Column(
                    Modifier
                        .background(Colors.blackAlphaed, RoundedCornerShape(5.dp))
                        .border(1.dp, Colors.white, RoundedCornerShape(5.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.padding(vertical = 10.dp)) {
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            MKText(text = "Date de création", textColor = Colors.white)
                            MKText(
                                text = Date(team.creationDate * 1000).displayedString("dd MMMM yyyy"),
                                textColor = Colors.white,
                                font = Fonts.NunitoBD
                            )
                        }
                    }

                }
                Spacer(Modifier.height(10.dp))
                team.rosters.firstOrNull { it.game == "mkworld" }?.let { roster ->
                    LazyColumn(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        item {
                            Box(Modifier
                                .fillMaxWidth()
                                .background(Colors.blackAlphaed, RoundedCornerShape(5.dp))
                                .border(1.dp, Colors.white, RoundedCornerShape(5.dp))) {
                                MKText(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .align(Alignment.Center),
                                    fontSize = 18,
                                    font = Fonts.NunitoBD,
                                    textColor = Colors.white,
                                    text = "Roster"
                                )
                            }
                        }
                        item {
                            VerticalGrid {
                                roster.players.forEach {
                                    PlayerCell(
                                        modifier = Modifier
                                            .padding(5.dp)
                                            .fillParentMaxWidth(0.48f),
                                        player = PlayerEntity(it),
                                        textColor = Colors.white,
                                        backgroundColor = Colors.blackAlphaed,
                                        onClick = { onPlayerClick(it.id) }
                                    )
                                }
                            }
                        }
                        state.value.allyList.takeIf { it.isNotEmpty() }?.let {
                            item {
                                Box(Modifier
                                    .fillMaxWidth()
                                    .background(Colors.blackAlphaed, RoundedCornerShape(5.dp))
                                    .border(1.dp, Colors.white, RoundedCornerShape(5.dp))) {
                                    MKText(
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .align(Alignment.Center),
                                        fontSize = 18,
                                        font = Fonts.NunitoBD,
                                        textColor = Colors.white,
                                        text = "Allies"
                                    )
                                }
                            }
                            item {
                                VerticalGrid {
                                    it.forEach {
                                        PlayerCell(
                                            modifier = Modifier
                                                .padding(5.dp)
                                                .fillParentMaxWidth(0.48f),
                                            player = it,
                                            textColor = Colors.white,
                                            backgroundColor = Colors.blackAlphaed,
                                            onClick = { onPlayerClick(it.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}