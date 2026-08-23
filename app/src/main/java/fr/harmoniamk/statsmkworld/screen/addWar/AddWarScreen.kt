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
import fr.harmoniamk.statsmkworld.extension.toTeamColor
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeamRoster
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKStepper
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.MKTextField
import fr.harmoniamk.statsmkworld.ui.cells.MKListRow
import fr.harmoniamk.statsmkworld.ui.cells.MKListRowCheck
import fr.harmoniamk.statsmkworld.ui.cells.MKListRowChevron
import fr.harmoniamk.statsmkworld.ui.cells.PlayerMedallion
import fr.harmoniamk.statsmkworld.ui.cells.playerAvatarColor
import fr.harmoniamk.statsmkworld.ui.stats.Eyebrow
import fr.harmoniamk.statsmkworld.ui.stats.StatCard
import fr.harmoniamk.statsmkworld.ui.stats.StatCardRadius

/**
 * Écran de création de war (pôle Wars) — wizard **2 étapes sur un seul écran** :
 * `1 · Adversaire` → `2 · Joueurs`, bascule **dynamique** (aucune re-navigation, rule
 * 11/14). Le segmenté 12/24 et le stepper pilotent l'état réactif du ViewModel.
 *
 * Rendu pixel-perfect vs la maquette prototype UX (écran `addwar`, rule 13/15) :
 * segmenté partagé [MKSegmentedSelector], stepper partagé [MKStepper], lignes de liste
 * partagées [MKListRow] (rule 16).
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

    // Retour étape-conscient, partagé entre le back système et le bouton retour de
    // l'appbar (#50 pt.2) pour un comportement cohérent.
    val handleBack: () -> Unit = {
        when {
            // Sélecteur de roster déplié → le replier.
            state.expandedRosterTeamId != null -> viewModel.collapseRosterPicker()
            // Étape 3 (Récap) → revenir à l'étape 2 ; étape 2 → étape 1.
            state.step > 0 -> viewModel.onStepChange(state.step - 1)
            // Étape 1 avec un adversaire déjà retenu → le retirer.
            state.teamSelected?.isNotEmpty() == true -> viewModel.onRemoveTeam()
            else -> onBack()
        }
    }
    BackHandler { handleBack() }

    BaseScreen(title = stringResource(R.string.addwar_title), onBack = handleBack, modifier = Modifier.fillMaxSize()) {
        // Segmenté 12/24 : c'est ICI que vit le sélecteur de mode (pôle Wars). Le
        // changer met à jour l'état réactif du VM SANS re-navigation.
        MKSegmentedSelector(
            items = listOf(
                stringResource(R.string.mode_12_players),
                stringResource(R.string.mode_24_players)
            ),
            page = if (state.is24p) 1 else 0,
            onClick = { selected -> viewModel.onModeChange(selected == 1) }
        )
        Spacer(Modifier.height(11.dp))
        // Stepper cliquable : l'étape Joueurs n'est accessible que si l'adversaire est
        // complet ; le Récap qu'une fois l'adversaire complet ET les 6 joueurs sélectionnés.
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
 * Étape 2 — progression + sélection des joueurs + roster adverse indicatif. Aucun CTA :
 * la composition complète (6 joueurs) bascule AUTOMATIQUEMENT sur l'étape 3 (Récap).
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
                    initials = initialsOf(selector.player.name),
                    avatarColor = playerAvatarColor(selector.player.id),
                    avatarUrl = state.playerAvatars[selector.player.id],
                    name = selector.player.name,
                    onClick = { onPlayerSelected(selector.player) },
                    trailing = { MKListRowCheck(selected = selector.isSelected) }
                )
            }
        }

        // 3. Roster adverse : lignes indicatives (non saisies côté app).
        state.opponentPreviews.forEach { preview ->
            if (preview.players.isNotEmpty()) {
                item {
                    Eyebrow("${stringResource(R.string.addwar_opponent_roster)} · ${preview.name}")
                }
                items(preview.players, key = { "${preview.tag}-${it.playerId}" }) { player ->
                    OpponentPlayerRow(name = player.name, color = preview.color.toTeamColor())
                }
            }
        }

        item {
            MKText(
                text = stringResource(R.string.addwar_opponent_indicative),
                textColor = Colors.white55,
                fontSize = 12,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Étape 3 — Récap : rappel des adversaire(s) retenu(s) (nom+tag roster, avatar équipe,
 * rule 12) et des 6 joueurs sélectionnés, puis bouton « Démarrer la war » (le seul CTA
 * lançant réellement [onStart]). « Précédent » revient à l'étape Joueurs.
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
                initials = initialsOf(player.name),
                avatarColor = playerAvatarColor(player.id),
                avatarUrl = state.playerAvatars[player.id],
                name = player.name,
                trailing = { MKListRowCheck(selected = true) }
            )
        }
    }
    // Pied : Précédent + CTA « Démarrer la war » (unique bouton de lancement du wizard).
    Spacer(Modifier.height(9.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        MKButton(
            modifier = Modifier.weight(1f),
            style = MKButtonStyle.Minor(Colors.white),
            text = stringResource(R.string.addwar_previous),
            onClick = onPrevious
        )
        MKButton(
            modifier = Modifier.weight(1f),
            style = MKButtonStyle.Gradient,
            text = stringResource(R.string.addwar_start_war),
            enabled = state.buttonEnabled,
            onClick = onStart
        )
    }
}

/** Ligne indicative d'un joueur adverse (`.lrow.static` de la maquette) : pastille + nom, atténué. */
@Composable
private fun OpponentPlayerRow(name: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        // Médaillon mutualisé (#50 pt.4) : joueur adverse → pas d'avatar dispo, initiales seules.
        PlayerMedallion(
            initials = initialsOf(name),
            avatarColor = color,
            size = 28.dp,
            initialsFontSize = 11,
            borderWidth = 2.dp,
            borderColor = Colors.white.copy(alpha = 0.75f)
        )
        MKText(text = name, font = Fonts.NunitoBD, fontSize = 13, textColor = Colors.white.copy(alpha = 0.8f), textAlign = TextAlign.Start)
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
