package fr.harmoniamk.statsmkworld.screen.playerProfile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import fr.harmoniamk.statsmkworld.BuildConfig
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.activity.MainActivity
import fr.harmoniamk.statsmkworld.model.ScoringConstants
import fr.harmoniamk.statsmkworld.extension.countryFlag
import fr.harmoniamk.statsmkworld.extension.displayedString
import fr.harmoniamk.statsmkworld.extension.getActivity
import fr.harmoniamk.statsmkworld.extension.toTeamColor
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
import fr.harmoniamk.statsmkworld.ui.MKDialog
import fr.harmoniamk.statsmkworld.ui.MKLoaderDialog
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.OnLifecycleEvent
import fr.harmoniamk.statsmkworld.ui.cells.MkcBadge
import fr.harmoniamk.statsmkworld.ui.cells.ProfileInfo
import fr.harmoniamk.statsmkworld.ui.cells.ProfileInfoCard
import fr.harmoniamk.statsmkworld.ui.cells.ProfilePersonCard
import fr.harmoniamk.statsmkworld.ui.cells.ProfileRole
import fr.harmoniamk.statsmkworld.ui.cells.ProfileSettingRow
import fr.harmoniamk.statsmkworld.ui.cells.RolePill
import fr.harmoniamk.statsmkworld.ui.stats.StatCard
import fr.harmoniamk.statsmkworld.ui.stats.initialsOf
import java.util.Date

/**
 * Écran profil joueur autonome (fiche d'un joueur donné, atteinte depuis l'Annuaire /
 * les résultats : route `Player/Profile/{id}`). Barre de titre propre + contenu.
 * Le contenu réel est [PlayerProfileContent], mutualisé avec le pôle Profil
 * (onglet Joueur du `ProfileScreen` fusionné, ticket #28) qui l'affiche sans barre
 * de titre propre. Rendu pixel-perfect maquette (écrans `profile` / `pplayer`).
 */
@Composable
fun PlayerProfileScreen(
    viewModel: PlayerProfileViewModel,
    onBack: () -> Unit,
    onDisconnect: () -> Unit,
    onDebug: () -> Unit
) {
    BackHandler { onBack() }
    BaseScreen(title = stringResource(R.string.profil_joueur)) {
        PlayerProfileContent(
            viewModel = viewModel,
            onDisconnect = onDisconnect,
            onDebug = onDebug
        )
    }
}

/** Mappe le libellé de rôle du VM (`@StringRes`) vers la [ProfileRole] de la pastille. */
private fun roleFromRes(res: Int?): ProfileRole? = when (res) {
    R.string.leader -> ProfileRole.LEADER
    R.string.admin -> ProfileRole.ADMIN
    R.string.membre -> ProfileRole.MEMBER
    else -> null
}

/**
 * Contenu du profil joueur (carte identité, informations, réglages), sans barre de
 * titre : posé dans le [ColumnScope] d'un `BaseScreen` par l'appelant. Mutualisé
 * entre [PlayerProfileScreen] (fiche autonome `pplayer`) et l'onglet Joueur du pôle
 * Profil (`ProfileScreen`, écran `profile`). Rendu fidèle à la maquette 5 pôles.
 */
@Composable
fun ColumnScope.PlayerProfileContent(
    viewModel: PlayerProfileViewModel,
    onDisconnect: () -> Unit,
    onDebug: () -> Unit
) {
    val state = viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context.getActivity() as? MainActivity
    val showPopup = remember { mutableStateOf(false) }
    val showHelpPopup = remember { mutableStateOf(false) }

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

    when (val player = state.value.player) {
        null -> CircularProgressIndicator()
        else -> {
            val roster = player.rosters?.firstOrNull { it.game == "mkworld" }
            val avatar = player.userSettings?.avatar?.let { "https://mkcentral.com$it" }
            val role = roleFromRes(state.value.role)

            LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                // Marge basse pour ne pas être masqué par la bottombar du pôle (rule 10).
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                // Carte identité (pcard) : avatar, nom, pays + rôle, bio, badge MKCentral.
                item {
                    ProfilePersonCard(
                        name = player.name,
                        avatarUrl = avatar,
                        avatarColor = roster?.teamColor?.toInt().toTeamColor(),
                        avatarFallback = initialsOf(player.name),
                        badgeRes = R.string.profile_badge_player,
                        bio = player.userSettings?.aboutMe
                    ) {
                        MKText(text = player.countryCode.countryFlag, fontSize = 16, resizable = false)
                        MKText(text = player.countryCode.uppercase(), fontSize = 13, textColor = Colors.white.copy(alpha = 0.72f), resizable = false)
                        role?.let {
                            MKText(text = "·", fontSize = 13, textColor = Colors.white.copy(alpha = 0.72f), resizable = false)
                            RolePill(it)
                        }
                    }
                }

                // Carte Informations : équipe/tag, membre depuis, code ami, discord,
                // inscription, rôle — grille 2 colonnes de la maquette.
                item {
                    val infos = buildList {
                        roster?.let {
                            add(ProfileInfo(stringResource(R.string.profile_info_team), it.teamName, it.teamTag))
                            add(ProfileInfo(stringResource(R.string.profile_info_member_since), Date(it.joinDate * 1000).displayedString("dd/MM/yyyy")))
                        }
                        player.friendCodes?.firstOrNull { it.type == "switch" }?.fc?.let {
                            add(ProfileInfo(stringResource(R.string.profile_info_friend_code), it))
                        }
                        player.discord?.username?.let {
                            add(ProfileInfo(stringResource(R.string.profile_info_discord), "@$it"))
                        }
                        add(ProfileInfo(stringResource(R.string.profile_info_join), Date(player.joinDate * 1000).displayedString("dd/MM/yyyy")))
                        state.value.role?.let {
                            add(ProfileInfo(stringResource(R.string.profile_info_role), stringResource(it)))
                        }
                    }
                    ProfileInfoCard(infos)
                }

                // Boutons de règles métier (fiche d'un autre joueur) : ajout ally,
                // changement de rôle Leader-only, message « déjà ally ». Boutons en
                // largeur intrinsèque, centrés (retour utilisateur #28 ; même traitement
                // que le profil équipe — solution d'attente avant le ticket UI boutons).
                if (state.value.buttonVisible) item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        MKButton(
                            style = MKButtonStyle.Gradient,
                            text = stringResource(R.string.ajouter_en_tant_qu_ally),
                            onClick = viewModel::onAddAlly
                        )
                    }
                }
                if (state.value.isAlly) item {
                    MKText(
                        text = stringResource(R.string.already_ally),
                        font = Fonts.NunitoIT,
                        fontSize = 14,
                        textColor = Colors.black,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
                state.value.adminButtonLabel?.takeIf { state.value.role != null }?.let { label ->
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            MKButton(
                                style = MKButtonStyle.Gradient,
                                text = stringResource(label),
                                onClick = viewModel::onSwitchRole
                            )
                        }
                    }
                }

                // Réglages (profil « me » uniquement) : carte à lignes setrow.
                if (state.value.showMenu) {
                    item {
                        val showDebug = state.value.player?.id.toString() == ScoringConstants.DEBUG_PLAYER_ID || state.value.isMatrixMode
                        StatCard(title = stringResource(R.string.reglages)) {
                            ProfileSettingRow(
                                title = stringResource(R.string.refresh),
                                leadingIcon = R.drawable.ic_refresh,
                                subtitle = stringResource(R.string.profile_setting_refresh_sub),
                                onClick = viewModel::onRefresh
                            )
                            ProfileSettingRow(
                                title = stringResource(R.string.notifications),
                                leadingIcon = R.drawable.ic_bell,
                                subtitle = stringResource(R.string.profile_setting_notif_sub),
                                onClick = viewModel::onNotification
                            ) {
                                ProfileSwitch(
                                    checked = state.value.notificationsEnabled == true,
                                    onChange = { viewModel.onNotification() }
                                )
                            }
                            if (state.value.hasMultiRoster)
                                ProfileSettingRow(
                                    title = stringResource(R.string.multi_roster),
                                    leadingIcon = R.drawable.ic_podium,
                                    subtitle = stringResource(R.string.profile_setting_multiroster_sub),
                                    onClick = viewModel::onMultiRoster
                                ) {
                                    ProfileSwitch(
                                        checked = state.value.multiRosterEnabled,
                                        onChange = { viewModel.onMultiRoster() }
                                    )
                                }
                            if (showDebug)
                                ProfileSettingRow(
                                    title = stringResource(R.string.profile_setting_debug),
                                    leadingIcon = R.drawable.ic_cog,
                                    subtitle = stringResource(R.string.profile_setting_debug_sub),
                                    onClick = onDebug
                                )
                            ProfileSettingRow(
                                title = stringResource(R.string.logout),
                                leadingIcon = R.drawable.ic_logout,
                                danger = true,
                                divider = false,
                                onClick = viewModel::onLogoutClick
                            ) { Spacer(Modifier) }
                        }
                    }
                    // Ligne version (maquette) : « Stats MKWorld · vX » + dernière synchro.
                    item {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            MKText(
                                text = "Stats MKWorld · v${BuildConfig.VERSION_NAME}",
                                font = Fonts.Urbanist,
                                fontSize = 11,
                                textColor = Colors.white.copy(alpha = 0.4f)
                            )
                            state.value.lastUpdate?.let {
                                MKText(
                                    text = stringResource(R.string.last_update, it),
                                    fontSize = 11,
                                    textColor = Colors.white.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Toggle des réglages, teinté à la charte (piste verte quand actif). */
@Composable
private fun ProfileSwitch(checked: Boolean, onChange: () -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = { onChange() },
        colors = SwitchDefaults.colors(
            checkedTrackColor = Colors.green,
            checkedThumbColor = Colors.white,
            uncheckedTrackColor = Colors.white30,
            uncheckedThumbColor = Colors.white,
            uncheckedBorderColor = Colors.whiteBorderSoft,
            checkedBorderColor = Colors.transparent
        )
    )
}
