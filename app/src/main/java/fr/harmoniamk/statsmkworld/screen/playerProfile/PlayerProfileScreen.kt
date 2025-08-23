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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import fr.harmoniamk.statsmkworld.R
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
import java.util.Date

@Composable
fun PlayerProfileScreen(viewModel: PlayerProfileViewModel, onBack: () -> Unit, onDisconnect: () -> Unit, onDebug: () -> Unit) {
    val state = viewModel.state.collectAsState()
    BackHandler { onBack() }

    LaunchedEffect(Unit) {
        viewModel.backToLogin.collect{
            onDisconnect()
        }
    }

    state.value.dialogTitle?.let {
        MKLoaderDialog(stringResource(it))
    }
    state.value.confirmDialog?.let {
        MKDialog(
            title = stringResource(R.string.logout),
            message = stringResource(it),
            buttonText = stringResource(R.string.logout_btn),
            secondButtonText = stringResource(R.string.back),
            onButtonClick = viewModel::onLogout,
            onSecondButtonClick = viewModel::dismissPopup
        )
    }
    BaseScreen(title = stringResource(R.string.profil_joueur)) {
        state.value.player?.userSettings?.avatar?.let {
            AsyncImage(model = "https://mkcentral.com$it", contentDescription = null)
        }
        when (val player = state.value.player) {
            null -> CircularProgressIndicator()
            else -> {

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
                    font = Fonts.NunitoIT,
                    resizable = false
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
                            MKText(text = stringResource(R.string.inscrit_depuis_le), textColor = Colors.white)
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
                                MKText(text = stringResource(R.string.code_ami), textColor = Colors.white)
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
                                MKText(text = stringResource(R.string.tag_discord), textColor = Colors.white)
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
                                MKText(text = stringResource(R.string.equipe_actuelle), textColor = Colors.white)
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
                                MKText(text = stringResource(R.string.team_since), textColor = Colors.white)
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
                                    MKText(text = stringResource(R.string.role), textColor = Colors.white)
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
                        text = stringResource(R.string.ajouter_en_tant_qu_ally),
                        onClick = viewModel::onAddAlly
                    )
                }
                if (state.value.isAlly) {
                    MKText(
                        text = stringResource(R.string.already_ally),
                        font = Fonts.NunitoIT,
                        fontSize = 16,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
                state.value.adminButtonLabel?.let {
                    MKButton(text = stringResource(it), style = MKButtonStyle.Gradient, onClick = viewModel::onSwitchRole)
                }
                if (state.value.showMenu) {
                    LazyColumn {
                        item {
                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onRefresh() }) {
                                MKText(text = stringResource(R.string.refresh), font = Fonts.Urbanist, modifier = Modifier.padding(vertical = 20.dp))

                            }
                            Spacer(Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Colors.blackAlphaed))

                        }
                        item {
                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onLogoutClick() }) {
                                MKText(text = stringResource(R.string.logout), font = Fonts.Urbanist, modifier = Modifier.padding(vertical = 20.dp))
                            }
                            Spacer(Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Colors.blackAlphaed))

                        }
                        if (state.value.player?.id.toString() == "18595" || state.value.isMatrixMode)
                            item {
                                Row(modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDebug() }) {
                                    MKText(text = "Debug", font = Fonts.Urbanist, modifier = Modifier.padding(vertical = 20.dp))
                                }
                                Spacer(Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Colors.blackAlphaed))
                            }
                    }
                    state.value.lastUpdate?.let {
                        MKText(text = stringResource(R.string.last_update, it), modifier = Modifier.padding(top = 10.dp))
                    }
                }
            }
        }

    }
}