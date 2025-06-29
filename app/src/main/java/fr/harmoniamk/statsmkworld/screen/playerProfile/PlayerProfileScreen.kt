package fr.harmoniamk.statsmkworld.screen.playerProfile

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import fr.harmoniamk.statsmkworld.extension.countryFlag
import fr.harmoniamk.statsmkworld.extension.displayedString
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
import fr.harmoniamk.statsmkworld.ui.MKDialog
import fr.harmoniamk.statsmkworld.ui.MKLoaderDialog
import fr.harmoniamk.statsmkworld.ui.MKText
import kotlinx.coroutines.flow.collect
import java.util.Date

@Composable
fun PlayerProfileScreen(viewModel: PlayerProfileViewModel, onBack: () -> Unit, onDisconnect: () -> Unit) {
    val state = viewModel.state.collectAsState()
    BackHandler { onBack() }

    LaunchedEffect(Unit) {
        viewModel.backToLogin.collect{
            onDisconnect()
        }
    }

    state.value.dialogTitle?.let {
        MKLoaderDialog(it)
    }
    state.value.confirmDialog?.let {
        MKDialog(
            title = "Se déconnecter",
            message = it,
            buttonText = "Déconnection",
            secondButtonText = "Retour",
            onButtonClick = viewModel::onLogout,
            onSecondButtonClick = viewModel::dismissPopup
        )
    }
    BaseScreen(title = "Profil joueur") {
        when (val player = state.value.player) {
            null -> CircularProgressIndicator()
            else -> {
                player.userSettings?.avatar?.let {
                    AsyncImage(model = "https://mkcentral.com$it", contentDescription = null)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    MKText(text = player.countryCode.countryFlag, fontSize = 30)
                    MKText(text = player.name, fontSize = 18, font = Fonts.NunitoBD)
                }
                MKText(
                    text = player.userSettings?.aboutMe.orEmpty(),
                    modifier = Modifier.padding(bottom = 10.dp),
                    fontSize = 16,
                    font = Fonts.NunitoIT
                )

                Column(
                    Modifier.background(Colors.blackAlphaed, RoundedCornerShape(5.dp))
                        .border(1.dp, Colors.white, RoundedCornerShape(5.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.padding(vertical = 10.dp)) {
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            MKText(text = "Inscrit depuis le", textColor = Colors.white)
                            MKText(
                                text = Date(player.joinDate * 1000).displayedString("dd MMMM yyyy"),
                                textColor = Colors.white,
                                font = Fonts.NunitoBD
                            )
                        }
                        player.friendCodes?.firstOrNull { it.type == "switch" }?.fc?.let {
                            Column(
                                Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                MKText(text = "Code ami", textColor = Colors.white)
                                MKText(text = it, font = Fonts.NunitoBD, textColor = Colors.white)
                            }
                        }

                    }
                    state.value.player?.discord?.let {

                        Row(modifier = Modifier.padding(vertical = 10.dp)) {
                            Column(
                                Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                MKText(text = "Tag Discord", textColor = Colors.white)
                                MKText(
                                    text = it.username,
                                    textColor = Colors.white,
                                    font = Fonts.NunitoBD
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                player.rosters?.firstOrNull { it.game == "mkworld" }?.let { roster ->
                    Column(
                        Modifier.background(Colors.blackAlphaed, RoundedCornerShape(5.dp))
                            .border(1.dp, Colors.white, RoundedCornerShape(5.dp)),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(modifier = Modifier.padding(vertical = 10.dp)) {
                            Column(
                                Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                MKText(text = "Equipe actuelle", textColor = Colors.white)
                                MKText(
                                    text = roster.teamName,
                                    textColor = Colors.white,
                                    font = Fonts.NunitoBD
                                )
                            }

                            Column(
                                Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                MKText(text = "Entrée dans l'équipe", textColor = Colors.white)
                                MKText(
                                    text = Date(roster.joinDate * 1000).displayedString("dd MMMM yyyy"),
                                    textColor = Colors.white,
                                    font = Fonts.NunitoBD
                                )
                            }

                        }
                        state.value.role?.let {
                            Row(modifier = Modifier.padding(vertical = 10.dp)) {
                                Column(
                                    Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    MKText(text = "Rôle", textColor = Colors.white)
                                    MKText(
                                        text = it,
                                        textColor = Colors.white,
                                        font = Fonts.NunitoBD
                                    )
                                }
                            }
                        }


                    }
                }

                if (state.value.buttonVisible) {
                    MKButton(
                        style = MKButtonStyle.Gradient,
                        text = "Ajouter en tant qu'ally",
                        onClick = viewModel::onAddAlly
                    )
                }
                if (state.value.isAlly) {
                    MKText(
                        text = "Ce joueur est un ally de l'équipe",
                        font = Fonts.NunitoIT,
                        fontSize = 16,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
                if (state.value.showMenu) {
                    LazyColumn {
                        item {
                            Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.onRefresh() }) {
                                MKText(text = "Rafraîchir les données", font = Fonts.Urbanist, modifier = Modifier.padding(vertical = 20.dp))

                            }
                            Spacer(Modifier.fillMaxWidth().height(1.dp).background(Colors.blackAlphaed))

                        }
                        item {
                            Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.onLogoutClick() }) {
                                MKText(text = "Se déconnecter", font = Fonts.Urbanist, modifier = Modifier.padding(vertical = 20.dp))
                            }
                            Spacer(Modifier.fillMaxWidth().height(1.dp).background(Colors.blackAlphaed))

                        }
                    }
                }
                state.value.lastUpdate?.let {
                    MKText(text = "Dernière mise à jour : $it", modifier = Modifier.padding(top = 10.dp))
                }
            }
        }

    }
}