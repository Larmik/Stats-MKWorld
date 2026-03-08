package fr.harmoniamk.statsmkworld.screen.playerProfile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import coil.compose.AsyncImage
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.activity.MainActivity
import fr.harmoniamk.statsmkworld.extension.countryFlag
import fr.harmoniamk.statsmkworld.extension.displayedString
import fr.harmoniamk.statsmkworld.extension.getActivity
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
import fr.harmoniamk.statsmkworld.ui.MKDialog
import fr.harmoniamk.statsmkworld.ui.MKLoaderDialog
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.OnLifecycleEvent
import java.util.Date

@Composable
fun PlayerProfileScreen(
    viewModel: PlayerProfileViewModel,
    onBack: () -> Unit,
    onDisconnect: () -> Unit,
    onDebug: () -> Unit
) {
    val state = viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context.getActivity() as? MainActivity
    val showPopup = remember { mutableStateOf(false) }
    val showHelpPopup = remember { mutableStateOf(false) }
    BackHandler { onBack() }

    LaunchedEffect(Unit) {
        viewModel.backToLogin.collect {
            onDisconnect()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.showNotif.collect {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                val shouldShow = activity?.let {
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        it,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                } ?: false
                if (!granted && !shouldShow)
                    showPopup.value = true
                else
                    activity?.notificationPermissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    OnLifecycleEvent { _, event ->
        if (event == Lifecycle.Event.ON_RESUME)
            viewModel.onResume()
    }

    LaunchedEffect(activity?.sharedNotificationsGranted) {
        activity?.sharedNotificationsGranted?.collect {
            viewModel.onResume()
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

    if (showPopup.value) {
        MKDialog(
            title = "Activer les notifications",
            message = "Tu dois activer les notifications dans les paramètres",
            buttonText = "Ouvrir les paramètres",
            onButtonClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                )
            },
            onDismiss = { showPopup.value = false }
        )
    }
    if (showHelpPopup.value) {
        MKDialog(
            title = "Multi-roster",
            message = "Activé: Statistiques calculées sur l'activité de tous les rosters de l'équipe \n \n Désactivé: Statistiques calculées sur l'activité de votre roster uniquement. \n \n Un redémarrage de l'application est nécessaire pour prendre en compte le changement de ce paramètre.",
            buttonText = "OK",
            onButtonClick = {
                showHelpPopup.value = false
            },
            onDismiss = { showHelpPopup.value = false }
        )
    }

    BaseScreen(title = stringResource(R.string.profil_joueur)) {
        when (val avatar = state.value.player?.userSettings?.avatar) {
            null -> Image(
                painter = painterResource(R.drawable.default_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(35.dp)
                    .clip(CircleShape)
            )

            else -> AsyncImage(model = "https://mkcentral.com$avatar", contentDescription = null)
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
                            MKText(
                                text = stringResource(R.string.inscrit_depuis_le),
                                textColor = Colors.white
                            )
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
                                MKText(
                                    text = stringResource(R.string.code_ami),
                                    textColor = Colors.white
                                )
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
                                MKText(
                                    text = stringResource(R.string.tag_discord),
                                    textColor = Colors.white
                                )
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
                                MKText(
                                    text = stringResource(R.string.equipe_actuelle),
                                    textColor = Colors.white
                                )
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
                                MKText(
                                    text = stringResource(R.string.team_since),
                                    textColor = Colors.white
                                )
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
                                    MKText(
                                        text = stringResource(R.string.role),
                                        textColor = Colors.white
                                    )
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
                state.value.adminButtonLabel?.takeIf { state.value.role != null }?.let {
                    MKButton(
                        text = stringResource(it),
                        style = MKButtonStyle.Gradient,
                        onClick = viewModel::onSwitchRole
                    )
                }
                if (state.value.showMenu) {
                    LazyColumn {
                        item {
                            SettingCell(
                                label = "Mode 12 joueurs",
                                onClick = {},
                                endContent = {
                                    Switch(
                                        checked = true,
                                        onCheckedChange = {  },
                                        colors = SwitchDefaults.colors(
                                            checkedTrackColor = Colors.black.copy(alpha = 0.3f),
                                            checkedThumbColor = Colors.black,
                                            uncheckedTrackColor = Colors.blackAlphaed.copy(alpha = 0.3f),
                                            uncheckedThumbColor = Colors.blackAlphaed,
                                            uncheckedBorderColor = Colors.transparent,
                                            checkedBorderColor = Colors.transparent
                                        )
                                    )
                                }
                            )
                        }
                        item {
                            SettingCell(
                                label = stringResource(R.string.refresh),
                                onClick = viewModel::onRefresh
                            ) { }
                        }
                        item {
                            SettingCell(
                                label = stringResource(
                                    when (state.value.notificationsEnabled) {
                                        true -> R.string.notif_enabled
                                        else -> R.string.notif_disabled
                                    }
                                ), onClick = viewModel::onNotification, endContent = {
                                    Switch(
                                        checked = state.value.notificationsEnabled == true,
                                        onCheckedChange = { viewModel.onNotification() },
                                        colors = SwitchDefaults.colors(
                                            checkedTrackColor = Colors.black.copy(alpha = 0.3f),
                                            checkedThumbColor = Colors.black,
                                            uncheckedTrackColor = Colors.blackAlphaed.copy(alpha = 0.3f),
                                            uncheckedThumbColor = Colors.blackAlphaed,
                                            uncheckedBorderColor = Colors.transparent,
                                            checkedBorderColor = Colors.transparent
                                        )
                                    )
                                })
                        }
                        if (state.value.hasMultiRoster)
                            item {
                                SettingCell(
                                    label = stringResource(
                                        when (state.value.multiRosterEnabled) {
                                            true -> R.string.multi_roster_enabled
                                            else -> R.string.multi_roster_disabled
                                        }
                                    ), onClick = viewModel::onMultiRoster, endContent = {
                                        Switch(
                                            checked = state.value.multiRosterEnabled,
                                            onCheckedChange = { viewModel.onMultiRoster() },
                                            colors = SwitchDefaults.colors(
                                                checkedTrackColor = Colors.black.copy(alpha = 0.3f),
                                                checkedThumbColor = Colors.black,
                                                uncheckedTrackColor = Colors.blackAlphaed.copy(alpha = 0.3f),
                                                uncheckedThumbColor = Colors.blackAlphaed,
                                                uncheckedBorderColor = Colors.transparent,
                                                checkedBorderColor = Colors.transparent
                                            )
                                        )
                                    }
                                )
                            }
                        item {
                            SettingCell(
                                label = stringResource(R.string.logout),
                                onClick = viewModel::onLogoutClick
                            ) { }
                        }
                        if (state.value.player?.id.toString() == "18595" || state.value.isMatrixMode)
                            item { SettingCell(label = "Debug", onClick = onDebug) { } }
                    }
                    state.value.lastUpdate?.let {
                        MKText(
                            text = stringResource(R.string.last_update, it),
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }
            }
        }

    }
}

@Composable
fun SettingCell(label: String, onClick: () -> Unit, endContent: @Composable () -> Unit) {
    Column() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MKText(
                text = label,
                font = Fonts.Urbanist,
                modifier = Modifier.padding(vertical = 20.dp)
            )
            endContent()
        }
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Colors.blackAlphaed)
        )
    }
}