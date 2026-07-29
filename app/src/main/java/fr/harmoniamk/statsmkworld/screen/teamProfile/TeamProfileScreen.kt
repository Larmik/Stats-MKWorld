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
import fr.harmoniamk.statsmkworld.extension.displayedString
import fr.harmoniamk.statsmkworld.extension.toTeamColor
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.MKBottomSheet
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
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
 * Écran profil équipe autonome (fiche d'une équipe, atteinte depuis l'Annuaire :
 * route `Team/Profile/{id}`). Barre de titre propre + contenu.
 *
 * - `id == "me"` : mon équipe (onglets Membres / Alliés, ajout d'ally).
 * - sinon : fiche d'une autre équipe (fiche publique `pteam`, #28) — lecture seule,
 *   membres → `pplayer`, CTA « Voir nos confrontations » → fiche adversaire (#27).
 *
 * Le contenu réel est [TeamProfileContent], mutualisé avec le pôle Profil (onglet
 * Équipe du `ProfileScreen` fusionné, #28) qui l'affiche sans barre de titre propre.
 * Rendu pixel-perfect maquette (écrans `profile` / `pteam`).
 *
 * @param onConfrontations CTA « Voir nos confrontations » (fiche équipe publique) →
 *   fiche adversaire ; `null` ⇒ masqué (mon équipe).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TeamProfileScreen(
    viewModel: TeamProfileViewModel,
    onBack: () -> Unit,
    onPlayerClick: (String) -> Unit,
    onConfrontations: (() -> Unit)? = null
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
        BaseScreen(title = stringResource(R.string.team_profile)) {
            TeamProfileContent(
                viewModel = viewModel,
                onPlayerClick = onPlayerClick,
                onAddAllyClick = { scope.launch { bottomSheetState.show() } },
                onConfrontations = onConfrontations
            )
        }
    }
}

/**
 * Contenu du profil équipe (carte identité, informations, membres / alliés + ajout),
 * sans barre de titre : posé dans le [ColumnScope] d'un `BaseScreen` par l'appelant.
 * Mutualisé entre [TeamProfileScreen] (fiche autonome) et l'onglet Équipe du pôle
 * Profil (`ProfileScreen`). Rendu fidèle à la maquette 5 pôles.
 *
 * @param onAddAllyClick ouvre le sheet « Ajouter un ally » (hébergé par l'appelant).
 * @param onConfrontations CTA « Voir nos confrontations » (fiche équipe publique) →
 *   fiche adversaire (#27) ; `null` ⇒ masqué (mon équipe).
 */
@Composable
fun ColumnScope.TeamProfileContent(
    viewModel: TeamProfileViewModel,
    onPlayerClick: (String) -> Unit,
    onAddAllyClick: () -> Unit,
    onConfrontations: (() -> Unit)? = null
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    // 0 = Membres, 1 = Alliés (sous-onglets `pf2` de la maquette, via segmented partagé).
    var subTab by rememberSaveable { mutableIntStateOf(0) }
    val isMe = viewModel.id == "me"

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

                // CTA « Voir nos confrontations » (fiche équipe publique) : affiché
                // uniquement s'il existe au moins une war contre cette équipe (#28).
                // Bouton en largeur intrinsèque, centré (solution d'attente avant le
                // ticket UI dédié aux boutons).
                onConfrontations?.takeIf { state.value.hasConfrontations }?.let {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            MKButton(
                                style = MKButtonStyle.Gradient,
                                text = stringResource(R.string.profile_see_confrontations),
                                onClick = it
                            )
                        }
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
                                    // Bouton « Ajouter un ally » en largeur intrinsèque, centré
                                    // (retour utilisateur #28 ; solution d'attente avant le ticket UI).
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                        MKButton(
                                            style = MKButtonStyle.Gradient,
                                            text = stringResource(R.string.ajouter_un_ally),
                                            onClick = onAddAllyClick
                                        )
                                    }
                                }
                                items(allies, key = { it.id }) { ally ->
                                    ProfileMemberRow(
                                        initials = initialsOf(ally.name),
                                        color = Colors.grey50,
                                        name = ally.name,
                                        role = ProfileRole.ALLY,
                                        subtitle = stringResource(R.string.profile_ally_external),
                                        onClick = { onPlayerClick(ally.id) }
                                    )
                                }
                            }
                            else -> memberRows(members, onPlayerClick)
                        }
                    }
                    // Autre équipe (pteam) : liste des membres, section « Membres ».
                    false -> {
                        item { Eyebrow(stringResource(R.string.profile_members)) }
                        memberRows(members, onPlayerClick)
                    }
                }
            }
        }
    }
}

/** Lignes de membres (`.lrow`) : avatar/initiales + nom + rôle réel + chevron. */
private fun androidx.compose.foundation.lazy.LazyListScope.memberRows(
    members: List<TeamProfileViewModel.MemberInfo>,
    onPlayerClick: (String) -> Unit
) {
    members.forEach { member ->
        item(key = member.playerId) {
            ProfileMemberRow(
                initials = initialsOf(member.name),
                color = member.rosterColor.toInt().toTeamColor(),
                name = member.name,
                role = ProfileRole.fromFirebaseRole(member.role),
                avatarUrl = member.avatarUrl,
                onClick = { onPlayerClick(member.playerId) }
            )
        }
    }
}
