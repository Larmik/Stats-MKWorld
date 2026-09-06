package fr.harmoniamk.statsmkworld.screen.addWar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.displayName
import fr.harmoniamk.statsmkworld.extension.toTeamColor
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeamRoster
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKStepper
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.MKTextField
import fr.harmoniamk.statsmkworld.ui.cells.MKListRow
import fr.harmoniamk.statsmkworld.ui.cells.MKListRowCheck
import fr.harmoniamk.statsmkworld.ui.cells.MKListRowChevron
import fr.harmoniamk.statsmkworld.ui.cells.playerAvatarColor
import fr.harmoniamk.statsmkworld.ui.stats.Eyebrow
import fr.harmoniamk.statsmkworld.ui.stats.StatCard
import fr.harmoniamk.statsmkworld.ui.stats.StatCardRadius

/**
 * Création de war — wizard `Adversaire` → `Joueurs` → `Récap`, étape pilotée par le
 * [AddWarViewModel] (aucune re-navigation, rule 11/14).
 */
@Composable
fun AddWarScreen(
    viewModel: AddWarViewModel,
    onBack: () -> Unit,
    onCurrentWar: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Champ de recherche : pur état UI éphémère (rule 11) survivant à la rotation.
    var searchTeam by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.goToCurrent.collect { onCurrentWar() }
    }

    // Retour étape-conscient, partagé back système + appbar (#50 pt.2).
    val handleBack: () -> Unit = {
        when {
            // Sélecteur de roster déplié → le replier.
            state.expandedRosterTeamId != null -> viewModel.collapseRosterPicker()
            // Étape avancée → revenir à la précédente.
            state.step > 0 -> viewModel.onStepChange(state.step - 1)
            // Étape 1 avec un adversaire déjà retenu → le retirer.
            state.teamSelected?.isNotEmpty() == true -> viewModel.onRemoveTeam()
            else -> onBack()
        }
    }
    BackHandler { handleBack() }

    BaseScreen(title = stringResource(R.string.addwar_title), onBack = handleBack, modifier = Modifier.fillMaxSize()) {
        // Segmenté 12/24 masqué temporairement (#91 pt.7) : création en 12p seulement pour la
        // MEP. Ne PAS supprimer (à réactiver plus tard).
        // MKSegmentedSelector(
        //     items = listOf(
        //         stringResource(R.string.mode_12_players),
        //         stringResource(R.string.mode_24_players)
        //     ),
        //     page = if (state.is24p) 1 else 0,
        //     onClick = { selected -> viewModel.onModeChange(selected == 1) }
        // )
        // Spacer(Modifier.height(11.dp))
        // Stepper cliquable : Joueurs gaté par adversaire complet ; Récap par adversaire + 6 joueurs.
        MKStepper(
            steps = listOf(
                stringResource(R.string.addwar_step_opponent),
                stringResource(R.string.addwar_step_players),
                stringResource(R.string.addwar_step_recap)
            ),
            step = state.step,
            enabled = { index ->
                when (index) {
                    // Adversaire : toujours ; Joueurs : adversaire complet ; Récap :
                    // adversaire complet ET line-up complète (exactement 6 joueurs).
                    0 -> true
                    1 -> state.nextButtonEnabled
                    else -> state.nextButtonEnabled && state.buttonEnabled
                }
            },
            onStepClick = viewModel::onStepChange
        )
        Spacer(Modifier.height(13.dp))

        when (state.step) {
            0 -> OpponentStep(
                state = state,
                search = searchTeam,
                onSearch = {
                    searchTeam = it
                    viewModel.onSearchTeam(it)
                },
                onTeamSelected = viewModel::onTeamSelected,
                onRosterSelected = viewModel::onRosterSelected
            )
            1 -> PlayersStep(
                state = state,
                onPlayerSelected = viewModel::onPlayerSelected
            )
            else -> RecapStep(
                state = state,
                onPrevious = { viewModel.onStepChange(1) },
                onStart = viewModel::createWar
            )
        }
    }
}

/** Initiales (2 lettres) pour une pastille d'avatar. */
private fun initialsOf(name: String): String = name.trim()
    .split(" ", "_", "-")
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
    .ifEmpty { "?" }

/** Étape 1 — recherche + liste d'équipes (avec sélecteur de roster inline). */
@Composable
private fun ColumnScope.OpponentStep(
    state: AddWarViewModel.State,
    search: String,
    onSearch: (String) -> Unit,
    onTeamSelected: (TeamEntity) -> Unit,
    onRosterSelected: (TeamEntity, MKCTeamRoster) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        item {
            MKTextField(
                baseModifier = Modifier.semantics { contentDescription = "Recherche equipe" },
                value = search,
                onValueChange = onSearch,
                placeHolderRes = R.string.addwar_search_team,
                backgroundColor = Colors.blackAlphaed
            )
        }
        item {
            Eyebrow(stringResource(R.string.addwar_select_opponent))
        }
        items(state.teamList, key = { it.id }) { team ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val rosterCount = team.rosters.size
                MKListRow(
                    modifier = Modifier.fillMaxWidth(),
                    initials = team.tag.take(3),
                    avatarColor = team.color.toTeamColor(),
                    avatarUrl = team.logo?.let { "https://mkcentral.com$it" },
                    name = team.name,
                    subtitle = when {
                        rosterCount > 1 -> stringResource(R.string.addwar_rosters_count, rosterCount)
                        else -> "${stringResource(R.string.roster).lowercase()} · ${team.tag}"
                    },
                    onClick = { onTeamSelected(team) },
                    trailing = { MKListRowChevron() }
                )
                // Sélecteur de roster inline (équipe multi-rosters, cf. maquette `roster-pick`).
                if (state.expandedRosterTeamId == team.id) {
                    RosterPicker(
                        rosters = state.expandedRosters,
                        onRosterSelected = { onRosterSelected(team, it) }
                    )
                }
            }
        }
        item {
            MKText(
                text = stringResource(R.string.addwar_roster_hint),
                textColor = Colors.white55,
                fontSize = 12,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
            )
        }
    }
}

/** Sélecteur de roster déplié (`.roster-pick`) : cadre pointillé translucide + lignes rosters. */
@Composable
private fun RosterPicker(rosters: List<MKCTeamRoster>, onRosterSelected: (MKCTeamRoster) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(StatCardRadius)
            .background(Colors.white30, StatCardRadius)
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Eyebrow(stringResource(R.string.addwar_pick_roster))
        rosters.forEach { roster ->
            MKListRow(
                modifier = Modifier.fillMaxWidth(),
                initials = roster.tag.take(3),
                avatarColor = roster.color.toInt().toTeamColor(),
                name = "${roster.name} — ${roster.tag}",
                subtitle = "${stringResource(R.string.roster).lowercase()} · ${roster.tag}",
                avatarSize = 28.dp,
                onClick = { onRosterSelected(roster) },
                trailing = { MKListRowChevron() }
            )
        }
    }
}

/**
 * Étape 2 — sélection des joueurs de ton roster (roster adverse retiré, #91 pt.8). Aucun CTA :
 * la 6ᵉ sélection bascule automatiquement sur le Récap.
 */
@Composable
private fun ColumnScope.PlayersStep(
    state: AddWarViewModel.State,
    onPlayerSelected: (PlayerEntity) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        // 1. Carte de progression (compteur + barre).
        item { ProgressCard(selected = state.selectedPlayerCount, total = 6) }

        // 2. Ton roster : lignes sélectionnables (toggle pastille verte ✓).
        state.playerList.forEach { (rosterName, list) ->
            item {
                Eyebrow(
                    when (rosterName.isEmpty()) {
                        true -> stringResource(R.string.allies)
                        else -> "${stringResource(R.string.addwar_your_roster)} · $rosterName"
                    }
                )
            }
            items(list, key = { it.player.id }) { selector ->
                MKListRow(
                    modifier = Modifier.fillMaxWidth(),
                    initials = initialsOf(selector.player.name.displayName),
                    avatarColor = playerAvatarColor(selector.player.id),
                    avatarUrl = state.playerAvatars[selector.player.id],
                    name = selector.player.name.displayName,
                    onClick = { onPlayerSelected(selector.player) },
                    trailing = { MKListRowCheck(selected = selector.isSelected) }
                )
            }
        }
        // Roster adverse indicatif retiré (#91 pt.8) : previews conservées pour le Récap et la création.
    }
}

/**
 * Étape 3 — Récap : adversaire(s) (nom+tag roster, avatar équipe, rule 12) et 6 joueurs
 * retenus, puis « Démarrer la war » ([onStart]). « Précédent » revient aux Joueurs.
 */
@Composable
private fun ColumnScope.RecapStep(
    state: AddWarViewModel.State,
    onPrevious: () -> Unit,
    onStart: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        // Adversaire(s) : nom/tag du roster, avatar de l'équipe (rule 12).
        item { Eyebrow(stringResource(R.string.addwar_recap_opponent)) }
        items(state.opponentPreviews, key = { "${it.tag}-${it.name}" }) { preview ->
            MKListRow(
                modifier = Modifier.fillMaxWidth(),
                initials = preview.tag.take(3),
                avatarColor = preview.color.toTeamColor(),
                avatarUrl = preview.logo?.let { "https://mkcentral.com$it" },
                name = preview.name,
                subtitle = preview.tag
            )
        }

        // Line-up : les 6 joueurs retenus.
        item { Eyebrow(stringResource(R.string.addwar_recap_players)) }
        items(state.selectedPlayers, key = { it.id }) { player ->
            MKListRow(
                modifier = Modifier.fillMaxWidth(),
                initials = initialsOf(player.name.displayName),
                avatarColor = playerAvatarColor(player.id),
                avatarUrl = state.playerAvatars[player.id],
                name = player.name.displayName,
                trailing = { MKListRowCheck(selected = true) }
            )
        }
    }
    // Pied : Précédent + CTA « Démarrer la war » (unique bouton de lancement du wizard).
    Spacer(Modifier.height(9.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        MKButton(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.addwar_previous),
            onClick = onPrevious
        )
        MKButton(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.addwar_start_war),
            enabled = state.buttonEnabled,
            onClick = onStart
        )
    }
}

/** Carte de progression de la sélection : compteur `n / total` + barre. */
@Composable
private fun ProgressCard(selected: Int, total: Int) {
    StatCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            MKText(
                text = stringResource(R.string.addwar_progress, selected, total),
                font = Fonts.Urbanist,
                fontSize = 14,
                textColor = Colors.white,
                resizable = false
            )
            Box(
                Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(20.dp)).background(Color(0x3D000000))
            ) {
                val fraction = (selected.toFloat() / total).coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth(fraction).height(6.dp).background(Colors.green))
            }
        }
        Spacer(Modifier.height(8.dp))
        MKText(
            text = stringResource(R.string.addwar_progress_hint),
            textColor = Colors.white55,
            fontSize = 12,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
