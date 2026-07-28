package fr.harmoniamk.statsmkworld.screen.teamProfile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.extension.displayedString
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeamRoster
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKBottomSheet
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
import fr.harmoniamk.statsmkworld.ui.MKSelectorViewPager
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.MKTextField
import fr.harmoniamk.statsmkworld.ui.VerticalGrid
import fr.harmoniamk.statsmkworld.ui.cells.PlayerCell
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
    onStats: (() -> Unit)? = null,
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
            BaseScreen(title = "Ajouter un ally") {
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
                    items(state.value.playerList, key = { it.id }) { player ->
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
                onStats = onStats,
                onConfrontations = onConfrontations
            )
        }
    }

}

/**
 * Contenu du profil équipe (en-tête, rosters / membres, alliés + ajout, CTA stats),
 * sans barre de titre : posé dans le [ColumnScope] d'un `BaseScreen` par l'appelant.
 * Mutualisé entre [TeamProfileScreen] (fiche autonome) et l'onglet Équipe du pôle
 * Profil (`ProfileScreen`).
 *
 * @param onAddAllyClick ouvre le sheet « Ajouter un ally » (hébergé par l'appelant).
 * @param onStats CTA « Voir les stats de l'équipe » (mon équipe, pôle Profil) → pôle
 *   Stats portée Équipe ; `null` ⇒ masqué.
 * @param onConfrontations CTA « Voir nos confrontations » (fiche équipe publique) →
 *   fiche adversaire (#27) ; `null` ⇒ masqué.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.TeamProfileContent(
    viewModel: TeamProfileViewModel,
    onPlayerClick: (String) -> Unit,
    onAddAllyClick: () -> Unit,
    onStats: (() -> Unit)? = null,
    onConfrontations: (() -> Unit)? = null
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val state = viewModel.state.collectAsStateWithLifecycle()
    when (val team = state.value.team) {
        null -> CircularProgressIndicator()
        else -> {
            TeamProfileHeader(
                logo = team.logo,
                name = team.name,
                description = team.description,
                creationDate = team.creationDate
            )
            onStats?.let {
                MKButton(
                    style = MKButtonStyle.Gradient,
                    text = stringResource(R.string.profile_see_team_stats),
                    onClick = it
                )
                Spacer(Modifier.height(10.dp))
            }
            onConfrontations?.let {
                MKButton(
                    style = MKButtonStyle.Gradient,
                    text = stringResource(R.string.profile_see_confrontations),
                    onClick = it
                )
                Spacer(Modifier.height(10.dp))
            }
            val rosters = remember(team) { team.rosters.filter { it.game == "mkworld" } }
            if (viewModel.id == "me")
                MKSelectorViewPager(pagerState, listOf("Membres", "Allies")) {
                    when (pagerState.currentPage) {
                        0 -> LazyColumn(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            rosters.forEach { roster ->
                                if (rosters.size > 1)
                                    item {
                                        RosterHeader(text = roster.name)
                                    }
                                item {
                                    RosterPlayersGrid(roster = roster, onPlayerClick = onPlayerClick)
                                }
                            }
                        }
                        1 ->  LazyColumn(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (state.value.addAllyVisible)
                                item {
                                    MKButton(style = MKButtonStyle.Gradient, text = "Ajouter un ally", onClick = onAddAllyClick)
                                }
                            state.value.allyList.takeIf { it.isNotEmpty() }?.let {
                                item {
                                    VerticalGrid {
                                        it.forEach {
                                            PlayerCell(
                                                modifier = Modifier.padding(5.dp).fillParentMaxWidth(0.48f),
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
            else
                LazyColumn(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    rosters.forEach { roster ->
                        item {
                            RosterHeader(
                                text = when (rosters.size) {
                                    1 -> stringResource(R.string.roster)
                                    else -> roster.name
                                }
                            )
                        }
                        item {
                            RosterPlayersGrid(roster = roster, onPlayerClick = onPlayerClick)
                        }
                    }
                }
        }
    }
}

@Composable
private fun TeamProfileHeader(
    logo: String?,
    name: String,
    description: String,
    creationDate: Long
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (logo) {
            null -> Image(
                painter = painterResource(R.drawable.default_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
            )

            else -> AsyncImage(
                model = "https://mkcentral.com$logo",
                contentDescription = null,
                modifier = Modifier.size(80.dp)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MKText(text = name, fontSize = 24, font = Fonts.NunitoBD)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MKText(
                    text = stringResource(R.string.created_date),
                    font = Fonts.NunitoIT
                )
                MKText(
                    text = Date(creationDate * 1000).displayedString("dd MMMM yyyy"),
                    font = Fonts.NunitoBD
                )
            }
        }
    }
    MKText(
        text = description,
        modifier = Modifier.padding(bottom = 10.dp),
        font = Fonts.NunitoIT,
        resizable = false
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun RosterHeader(text: String) {
    Box(Modifier
        .fillMaxWidth()
        .background(Colors.blackAlphaed, RoundedCornerShape(5.dp))
        .border(1.dp, Colors.white, RoundedCornerShape(5.dp))
    ) {
        MKText(
            modifier = Modifier.padding(10.dp).align(Alignment.Center),
            fontSize = 18,
            font = Fonts.NunitoBD,
            textColor = Colors.white,
            text = text
        )
    }
}

@Composable
private fun LazyItemScope.RosterPlayersGrid(
    roster: MKCTeamRoster,
    onPlayerClick: (String) -> Unit
) {
    VerticalGrid {
        roster.players.forEach {
            PlayerCell(
                modifier = Modifier.padding(5.dp).fillParentMaxWidth(0.48f),
                player = PlayerEntity(
                    player = it,
                    rosterId = roster.id.toString()
                ),
                textColor = Colors.white,
                backgroundColor = Colors.blackAlphaed,
                onClick = { onPlayerClick(it.id) }
            )
        }
    }
}