package fr.harmoniamk.statsmkworld.screen.currentWar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.extension.displayName
import fr.harmoniamk.statsmkworld.model.selectors.PlayerSelector
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.cells.MKListRow
import fr.harmoniamk.statsmkworld.ui.cells.MKListRowCheck
import fr.harmoniamk.statsmkworld.ui.cells.playerAvatarColor
import fr.harmoniamk.statsmkworld.ui.stats.Eyebrow
import fr.harmoniamk.statsmkworld.ui.stats.StatCard
import fr.harmoniamk.statsmkworld.ui.stats.initialsOf
import kotlinx.coroutines.launch

/**
 * Écran « ACTIONS » de la war en cours. Trois onglets (segmenté partagé, état local
 * `rememberSaveable`, rule 11) : Pénalités (grille équipe/montant, sélection unique),
 * Remplacement (sortant/entrant), Annuler (confirmation + suppression de la war).
 */
@Composable
fun CurrentWarActionsScreen(
    viewModel: CurrentWarActionsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onBackToWelcome: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(viewModel) {
        launch { viewModel.backToWelcome.collect { onBackToWelcome() } }
        launch { viewModel.onBack.collect { onBack() } }
    }

    BaseScreen(title = stringResource(R.string.actions), onBack = onBack, modifier = Modifier.fillMaxSize()) {
        MKSegmentedSelector(
            items = listOf(
                stringResource(R.string.penalties),
                stringResource(R.string.remplacement),
                stringResource(R.string.cancel_war)
            ),
            page = tab,
            onClick = { tab = it }
        )
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            when (tab) {
                0 -> PenaltiesPanel(state, viewModel, onBack)
                1 -> SubPanel(state, viewModel, onBack)
                else -> CancelPanel(viewModel, onBack)
            }
        }
    }
}

/** Onglet Pénalités : hint + grille par équipe/montant + CTA Valider / Annuler. */
@Composable
private fun ColumnScope.PenaltiesPanel(
    state: CurrentWarActionsViewModel.State,
    viewModel: CurrentWarActionsViewModel,
    onBack: () -> Unit
) {
    val teams = listOfNotNull(state.teamHost) + state.teamOpponent.orEmpty()
    val hostName = state.teamHost?.name.orEmpty()
    MKText(
        text = stringResource(R.string.penalties_hint),
        textColor = Colors.white55,
        fontSize = 12,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )
    // Une colonne par équipe (hôte + adverse(s)) empilant ses montants −10/−15/−20. Sélection
    // unique toutes équipes confondues ; `groupBy` conserve l'ordre de première apparition.
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        state.penalties.orEmpty().groupBy { it.penalty.teamId }.forEach { (teamId, teamPenalties) ->
            // Nom du roster/équipe (rule 12) ; retombe sur l'hôte si non résolu.
            val teamName = teams.singleOrNull { it.id == teamId }?.name ?: hostName
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MKText(
                    text = teamName,
                    font = Fonts.NunitoBD,
                    textColor = Colors.white,
                    fontSize = 12,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
                teamPenalties.forEach { selector ->
                    PenaltyTile(
                        modifier = Modifier.fillMaxWidth(),
                        label = "-${selector.penalty.amount}",
                        selected = selector.isSelected,
                        onClick = { viewModel.onPenaltySelected(selector) }
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(5.dp))
    ActionButtons(
        primaryLabel = stringResource(R.string.valider),
        primaryEnabled = state.penalties?.any { it.isSelected } == true,
        onPrimary = {
            viewModel.onPenaltyValidated()
            onBack()
        },
        onCancel = {
            viewModel.clearPenalties()
            onBack()
        }
    )
}

/** Tuile de pénalité : fond translucide clair, `blackAlphaed` quand sélectionnée (texte blanc). */
@Composable
private fun PenaltyTile(
    modifier: Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) Colors.blackAlphaed else Colors.white30
    val border = if (selected) Colors.white else Colors.whiteBorderSoft
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background, RoundedCornerShape(8.dp))
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        MKText(text = label, font = Fonts.Urbanist, textColor = Colors.white, fontSize = 14, maxLines = 1)
    }
}

/** Onglet Remplacement : joueur sortant / entrant + CTA Remplacer / Annuler. */
@Composable
private fun ColumnScope.SubPanel(
    state: CurrentWarActionsViewModel.State,
    viewModel: CurrentWarActionsViewModel,
    onBack: () -> Unit
) {
    Eyebrow(stringResource(R.string.joueur_sortant))
    state.currentPlayers.orEmpty().forEach { selector ->
        PlayerSelectRow(selector) { viewModel.onOldPlayerSelected(selector.player) }
    }
    Spacer(Modifier.height(9.dp))
    Eyebrow(stringResource(R.string.joueur_entrant))
    state.otherPlayers.orEmpty().forEach { selector ->
        PlayerSelectRow(selector) { viewModel.onNewPlayerSelected(selector.player) }
    }
    Spacer(Modifier.height(5.dp))
    ActionButtons(
        primaryLabel = stringResource(R.string.remplacer),
        primaryEnabled = state.currentPlayers.orEmpty().any { it.isSelected } &&
                state.otherPlayers.orEmpty().any { it.isSelected },
        onPrimary = viewModel::onSub,
        onCancel = onBack
    )
}

/** Ligne joueur sélectionnable ([MKListRow] partagée) : pastille ✓ verte si choisi. */
@Composable
private fun PlayerSelectRow(selector: PlayerSelector, onClick: () -> Unit) {
    MKListRow(
        initials = initialsOf(selector.player.name.displayName),
        avatarColor = playerAvatarColor(selector.player.id),
        name = selector.player.name.displayName,
        // Photo de profil MKCentral si dispo (#50 pt.4), sinon initiales.
        avatarUrl = selector.player.avatar,
        onClick = onClick,
        trailing = { MKListRowCheck(selected = selector.isSelected) }
    )
}

/** Onglet Annuler : carte de confirmation + bouton danger (supprime la war). */
@Composable
private fun ColumnScope.CancelPanel(
    viewModel: CurrentWarActionsViewModel,
    onBack: () -> Unit
) {
    StatCard {
        Eyebrow(stringResource(R.string.cancel_war))
        Spacer(Modifier.height(8.dp))
        MKText(
            text = stringResource(R.string.cancel_war_hint),
            textColor = Colors.white55,
            fontSize = 12,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
    Spacer(Modifier.height(2.dp))
    // Deux actions sur une ligne, largeurs égales (weight 1f, rule 16). Bouton danger aplati
    // sur le style unique de MKButton (plus de fond rouge ad hoc, #67).
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        MKButton(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.delete_war),
            onClick = viewModel::cancelWar
        )
        MKButton(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.cancel),
            onClick = onBack
        )
    }
}

/** Pied d'onglet : CTA principal (dégradé) + bouton Annuler secondaire. */
@Composable
private fun ActionButtons(
    primaryLabel: String,
    primaryEnabled: Boolean,
    onPrimary: () -> Unit,
    onCancel: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        MKButton(
            modifier = Modifier.weight(1f),
            text = primaryLabel,
            enabled = primaryEnabled,
            onClick = onPrimary
        )
        MKButton(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.cancel),
            onClick = onCancel
        )
    }
}
