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
import fr.harmoniamk.statsmkworld.model.selectors.PlayerSelector
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
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
 * Écran « ACTIONS » de la war en cours (pôle Wars). Trois onglets (segmenté partagé
 * [MKSegmentedSelector], rules 15/16) basculés **dynamiquement** par un état local
 * (`rememberSaveable`, rule 11 — pas de re-navigation ni de pager animé) :
 *
 * - **Pénalités** : grille par équipe/montant (−10/−15/−20), sélection unique, CTA Valider ;
 * - **Remplacement** : joueur sortant / joueur entrant ([MKListRow]), CTA Remplacer ;
 * - **Annuler** : carte de confirmation + bouton danger (supprime la war en cours).
 *
 * Style repris de `docs/prototype/stats-mkworld-5poles.html` (écran `waractions`) :
 * hints, cartes translucides, grille `.penb` (fond translucide → `--loss` rouge quand
 * sélectionné), bouton danger `.btn2.danger` (bordure/texte rouge).
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

    BaseScreen(title = stringResource(R.string.actions), modifier = Modifier.fillMaxSize()) {
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
    // Grille 2 colonnes de « penb » (montant + équipe). Sélection unique.
    state.penalties.orEmpty().chunked(2).forEach { rowPenalties ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rowPenalties.forEach { selector ->
                // Nom du roster hôte / de l'équipe adverse (rule 12), résolu depuis le
                // teamId porté par la pénalité ; retombe sur l'hôte si non résolu.
                val teamName = teams.singleOrNull { it.id == selector.penalty.teamId }?.name ?: hostName
                PenaltyTile(
                    modifier = Modifier.weight(1f),
                    label = "-${selector.penalty.amount} " + stringResource(R.string.penalty_placeholder, teamName),
                    selected = selector.isSelected,
                    onClick = { viewModel.onPenaltySelected(selector) }
                )
            }
            repeat(2 - rowPenalties.size) { Spacer(Modifier.weight(1f)) }
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

/** Tuile de pénalité (`.penb`) : fond translucide, rouge plein quand sélectionnée. */
@Composable
private fun PenaltyTile(
    modifier: Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) Colors.red else Colors.white30
    val border = if (selected) Colors.red else Colors.whiteBorderSoft
    val textColor = if (selected) Colors.black else Colors.white
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
        MKText(text = label, font = Fonts.Urbanist, textColor = textColor, fontSize = 14, maxLines = 1)
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
        initials = initialsOf(selector.player.name),
        avatarColor = playerAvatarColor(selector.player.id),
        name = selector.player.name,
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
    // Bouton danger `.btn2.danger` : bordure + texte rouge sur fond translucide.
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Colors.white30, RoundedCornerShape(10.dp))
            .border(1.dp, Colors.red.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable(onClick = viewModel::cancelWar)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        MKText(
            text = stringResource(R.string.delete_war).uppercase(),
            font = Fonts.Urbanist,
            textColor = Colors.red,
            fontSize = 14,
            maxLines = 1
        )
    }
    Spacer(Modifier.height(2.dp))
    MKButton(
        modifier = Modifier.fillMaxWidth(),
        style = MKButtonStyle.Minor(Colors.white),
        text = stringResource(R.string.cancel),
        onClick = onBack
    )
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
            style = MKButtonStyle.Gradient,
            text = primaryLabel,
            enabled = primaryEnabled,
            onClick = onPrimary
        )
        MKButton(
            modifier = Modifier.weight(1f),
            style = MKButtonStyle.Minor(Colors.white),
            text = stringResource(R.string.cancel),
            onClick = onCancel
        )
    }
}
