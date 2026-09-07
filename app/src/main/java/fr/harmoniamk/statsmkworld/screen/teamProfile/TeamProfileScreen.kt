package fr.harmoniamk.statsmkworld.screen.teamProfile

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.extension.displayName
import fr.harmoniamk.statsmkworld.extension.displayedString
import fr.harmoniamk.statsmkworld.extension.toTeamColor
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.MKBottomSheet
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKDialog
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.MKTextField
import fr.harmoniamk.statsmkworld.ui.cells.PlayerCell
import fr.harmoniamk.statsmkworld.ui.cells.ProfileInfo
import fr.harmoniamk.statsmkworld.ui.cells.ProfileInfoCard
import fr.harmoniamk.statsmkworld.ui.cells.ProfileMemberRow
import fr.harmoniamk.statsmkworld.ui.cells.ProfilePersonCard
import fr.harmoniamk.statsmkworld.ui.cells.ProfileRole
import fr.harmoniamk.statsmkworld.ui.cells.RolePill
import fr.harmoniamk.statsmkworld.ui.stats.Eyebrow
import fr.harmoniamk.statsmkworld.ui.stats.StatCard
import fr.harmoniamk.statsmkworld.ui.stats.initialsOf
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Fiche profil équipe autonome (route `Team/Profile/{id}`) : barre de titre + [TeamProfileContent].
 * `id == "me"` = mon équipe (onglets Membres/Alliés, ajout d'ally) ; sinon fiche publique lecture
 * seule (#28). Contenu mutualisé avec l'onglet Équipe du pôle Profil.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TeamProfileScreen(
    viewModel: TeamProfileViewModel,
    onBack: () -> Unit,
    onPlayerClick: (String) -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()
    val state = viewModel.state.collectAsStateWithLifecycle()
    val playerSearch = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.onDismiss.collect {
            bottomSheetState.hide()
        }
    }

    MKBottomSheet(
        sheetState = bottomSheetState,
        onBack = onBack,
        sheetContent = {
            BaseScreen(title = stringResource(R.string.ajouter_un_ally)) {
                MKTextField(
                    backgroundColor = Colors.blackAlphaed,
                    placeHolderRes = R.string.rechercher_un_joueur,
                    value = playerSearch.value,
                    onValueChange = {
                        playerSearch.value = it
                        viewModel.onSearchPlayers(it)
                    }
                )
                LazyVerticalGrid(columns = GridCells.Adaptive(150.dp)) {
                    gridItems(state.value.playerList, key = { it.id }) { player ->
                        PlayerCell(
                            player = PlayerEntity(player, isAlly = false),
                            onClick = { viewModel.addAlly(player) }
                        )
                    }
                }
            }
        }
    ) {
        BaseScreen(title = stringResource(R.string.team_profile), onBack = onBack) {
            TeamProfileContent(
                viewModel = viewModel,
                onPlayerClick = onPlayerClick,
                onAddAllyClick = { scope.launch { bottomSheetState.show() } }
            )
        }
    }
}

/**
 * Contenu du profil équipe (identité, informations, membres / alliés + ajout), sans barre de
 * titre : posé dans un [ColumnScope]. Mutualisé entre [TeamProfileScreen] et l'onglet Équipe du
 * pôle Profil.
 *
 * @param onAddAllyClick ouvre le sheet « Ajouter un ally » (hébergé par l'appelant).
 */
@Composable
fun ColumnScope.TeamProfileContent(
    viewModel: TeamProfileViewModel,
    onPlayerClick: (String) -> Unit,
    onAddAllyClick: () -> Unit
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    // 0 = Membres, 1 = Alliés (sous-onglets `pf2` de la maquette, via segmented partagé).
    var subTab by rememberSaveable { mutableIntStateOf(0) }
    val isMe = viewModel.id == "me"
    // Résolu hors du LazyListScope (stringResource n'y est pas appelable).
    val membersHeader = stringResource(R.string.profile_members)

    // Confirmation « Démarrer une nouvelle saison » (leader strict, #30) : action irréversible.
    if (state.value.newSeasonDialog) {
        MKDialog(
            title = stringResource(R.string.new_season_dialog_title),
            message = stringResource(R.string.new_season_dialog_message),
            buttonText = stringResource(R.string.new_season_confirm),
            secondButtonText = stringResource(R.string.back),
            onButtonClick = viewModel::onConfirmNewSeason,
            onSecondButtonClick = viewModel::dismissNewSeason,
            onDismiss = viewModel::dismissNewSeason
        )
    }

    when (val team = state.value.team) {
        null -> CircularProgressIndicator()
        else -> {
            val members = state.value.members
            val allies = state.value.allyList

            LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                // Marge basse pour ne pas être masqué par la bottombar du pôle (rule 10).
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                // Carte identité équipe (pcard) : logo, nom, tag + création, bio, badge.
                item {
                    ProfilePersonCard(
                        name = team.name,
                        avatarUrl = team.logo?.let { "https://mkcentral.com$it" },
                        avatarColor = team.color.toInt().toTeamColor(),
                        avatarFallback = team.tag,
                        badgeRes = R.string.profile_badge_team,
                        bio = team.description
                    ) {
                        // Tag en pastille « membre » grise + date de création (maquette).
                        RolePill(ProfileRole.MEMBER, text = "TAG ${team.tag}")
                        MKText(text = "·", fontSize = 13, textColor = Colors.white.copy(alpha = 0.72f), resizable = false)
                        MKText(
                            text = Date(team.creationDate * 1000).displayedString("dd/MM/yyyy"),
                            fontSize = 13,
                            textColor = Colors.white.copy(alpha = 0.72f),
                            resizable = false
                        )
                    }
                }

                // Carte Informations : Membres, Alliés (si mon équipe), Créée le (date exacte).
                item {
                    val infos = buildList {
                        add(ProfileInfo(stringResource(R.string.profile_info_members), members.size.toString()))
                        if (isMe) add(ProfileInfo(stringResource(R.string.profile_info_allies), allies.size.toString()))
                        add(ProfileInfo(stringResource(R.string.profile_info_created), Date(team.creationDate * 1000).displayedString("dd MMMM yyyy")))
                    }
                    ProfileInfoCard(infos)
                }

                // Action leader strict (role == 2, #30) : démarrer une nouvelle saison (confirmée
                // par MKDialog). Bouton centré en largeur intrinsèque.
                if (state.value.newSeasonVisible) item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        MKButton(
                            text = stringResource(R.string.profile_setting_new_season),
                            onClick = viewModel::onNewSeasonClick
                        )
                    }
                }

                when (isMe) {
                    // Mon équipe : sous-onglets Membres / Alliés (segmented partagé).
                    true -> {
                        item {
                            MKSegmentedSelector(
                                items = listOf(
                                    stringResource(R.string.profile_members),
                                    stringResource(R.string.profile_allies)
                                ),
                                page = subTab,
                                onClick = { subTab = it }
                            )
                        }
                        when (subTab) {
                            1 -> {
                                if (state.value.addAllyVisible) item {
                                    // Bouton « Ajouter un ally » centré en largeur intrinsèque (#28).
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                        MKButton(
                                            text = stringResource(R.string.ajouter_un_ally),
                                            onClick = onAddAllyClick
                                        )
                                    }
                                }
                                items(allies, key = { it.id }) { ally ->
                                    ProfileMemberRow(
                                        initials = initialsOf(ally.name.displayName),
                                        color = Colors.grey50,
                                        name = ally.name.displayName,
                                        role = ProfileRole.ALLY,
                                        // Photo de profil MKCentral si dispo (#50 pt.4), sinon initiales.
                                        avatarUrl = ally.avatar?.let { "https://mkcentral.com$it" },
                                        subtitle = stringResource(R.string.profile_ally_external),
                                        onClick = { onPlayerClick(ally.id) }
                                    )
                                }
                            }
                            // Onglet Membres : en-tête par roster si l'équipe en a ≥ 2 (le segmented nomme déjà « Membres »).
                            else -> memberRows(members, singleRosterHeader = null, onPlayerClick)
                        }
                    }
                    // Autre équipe (pteam) : en-tête « Membres » si roster unique, sinon un par roster.
                    false -> memberRows(members, singleRosterHeader = membersHeader, onPlayerClick)
                }
            }
        }
    }
}

/**
 * Lignes de membres. Si ≥ 2 rosters, une section par roster titrée par son nom (`Eyebrow`) ;
 * sinon liste plate précédée de [singleRosterHeader] si fourni.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.memberRows(
    members: List<TeamProfileViewModel.MemberInfo>,
    singleRosterHeader: String?,
    onPlayerClick: (String) -> Unit
) {
    val byRoster = members.groupBy { it.rosterId }
    when {
        byRoster.size > 1 -> byRoster.forEach { (_, rosterMembers) ->
            item(key = "roster-${rosterMembers.first().rosterId}") {
                Eyebrow(rosterMembers.first().rosterName)
            }
            memberItems(rosterMembers, onPlayerClick)
        }
        else -> {
            singleRosterHeader?.let { header -> item { Eyebrow(header) } }
            memberItems(members, onPlayerClick)
        }
    }
}

/** Émet une `ProfileMemberRow` par membre (sans en-tête). */
private fun androidx.compose.foundation.lazy.LazyListScope.memberItems(
    members: List<TeamProfileViewModel.MemberInfo>,
    onPlayerClick: (String) -> Unit
) {
    members.forEach { member ->
        item(key = member.playerId) {
            ProfileMemberRow(
                initials = initialsOf(member.name.displayName),
                color = member.rosterColor.toInt().toTeamColor(),
                name = member.name.displayName,
                role = ProfileRole.fromFirebaseRole(member.role),
                avatarUrl = member.avatarUrl,
                onClick = { onPlayerClick(member.playerId) }
            )
        }
    }
}
