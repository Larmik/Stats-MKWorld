package fr.harmoniamk.statsmkworld.screen.addTrack

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.local.Maps
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
import fr.harmoniamk.statsmkworld.ui.MKStepper
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.MKTextField
import fr.harmoniamk.statsmkworld.ui.VerticalGrid
import fr.harmoniamk.statsmkworld.ui.cells.MapCell
import fr.harmoniamk.statsmkworld.ui.cells.PlayerCell
import fr.harmoniamk.statsmkworld.ui.cells.PositionCell
import fr.harmoniamk.statsmkworld.ui.stats.Eyebrow
import fr.harmoniamk.statsmkworld.ui.stats.StatCard

/**
 * Écran d'ajout d'une course dans la war en cours (pôle Wars) — wizard **4 étapes sur un
 * seul écran** : `Circuit` → `Intermission` → `Positions` → `Résumé`, bascule **dynamique**
 * (aucune re-navigation, rule 11). L'étape courante vit dans le [AddTrackViewModel] : le
 * stepper et la saisie joueur-par-joueur la font évoluer, le retour arrière réinitialise
 * l'étape rejointe (rule 11 wizard).
 *
 * Rendu pixel-perfect vs la maquette prototype UX (écran `addtrack`, rule 13/15) : stepper
 * partagé [fr.harmoniamk.statsmkworld.ui.MKStepper], cellules partagées [MapCell] /
 * [PositionCell] / [PlayerCell] (rule 16). Écran du graphe racine poussé par-dessus
 * CurrentWar → **pas de bottombar**, aucune marge basse requise (rule 17).
 */
@Composable
fun AddTrackScreen(viewModel: AddTrackViewModel = hiltViewModel(), onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Champ de recherche : pur état UI éphémère (rule 11) survivant à la rotation.
    var search by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.backToWar.collect { onBack() }
    }

    BackHandler {
        when {
            // Depuis une étape avancée → revenir à la précédente (réinitialise l'étape rejointe).
            state.step > 0 -> viewModel.onStepChange(state.step - 1)
            else -> onBack()
        }
    }

    BaseScreen(title = stringResource(R.string.addtrack_title), modifier = Modifier.fillMaxSize()) {
        MKStepper(
            steps = listOf(
                stringResource(R.string.addtrack_step_circuit),
                stringResource(R.string.addtrack_step_intermission),
                stringResource(R.string.addtrack_step_positions),
                stringResource(R.string.addtrack_step_summary)
            ),
            step = state.step,
            enabled = { index ->
                when (index) {
                    // Circuit : toujours ; Intermission/Positions : circuit choisi ;
                    // Résumé : line-up de positions complète.
                    0 -> true
                    1, 2 -> state.mapPicked
                    else -> state.positionsComplete
                }
            },
            onStepClick = viewModel::onStepChange
        )
        Spacer(Modifier.height(13.dp))

        when (state.step) {
            0 -> CircuitStep(
                state = state,
                search = search,
                onSearch = {
                    search = it
                    viewModel.onSearch(it)
                },
                onMapSelected = {
                    search = ""
                    viewModel.onMapSelected(it)
                }
            )
            1 -> IntermissionStep(
                state = state,
                onIntermissionSelected = viewModel::onIntermissionSelected,
                onPrevious = { viewModel.onStepChange(0) },
                onNext = { viewModel.onStepChange(2) }
            )
            2 -> PositionsStep(
                state = state,
                onPositionClick = viewModel::onPositionClick,
                onPrevious = { viewModel.onStepChange(1) }
            )
            else -> SummaryStep(
                state = state,
                onPrevious = { viewModel.onStepChange(2) },
                onAddShock = viewModel::onAddShock,
                onRemoveShock = viewModel::onRemoveShock,
                onValidate = viewModel::onValidate
            )
        }
    }
}

/** Étape 1 — recherche + grille de circuits. Choisir un circuit avance à l'Intermission. */
@Composable
private fun ColumnScope.CircuitStep(
    state: AddTrackViewModel.State,
    search: String,
    onSearch: (String) -> Unit,
    onMapSelected: (Maps) -> Unit
) {
    MKTextField(
        value = search,
        onValueChange = onSearch,
        placeHolderRes = R.string.rechercher_un_circuit,
        backgroundColor = Colors.blackAlphaed
    )
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxWidth().weight(1f)
    ) {
        items(state.mapList, key = { it.name }) { map ->
            MapCell(Modifier.padding(5.dp), map = listOf(map), onClick = { onMapSelected(map) })
        }
    }
}

/**
 * Étape 2 — intermission optionnelle (2ᵉ circuit enchaîné). « Aucune » = pas d'intermission ;
 * les autres cartes enchaînent un circuit. Boutons « Précédent » / « Suivant · Positions ».
 */
@Composable
private fun ColumnScope.IntermissionStep(
    state: AddTrackViewModel.State,
    onIntermissionSelected: (Maps?) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    MKText(
        text = stringResource(R.string.addtrack_intermission_hint),
        textColor = Colors.white55,
        fontSize = 12,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp)
    )
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxWidth().weight(1f)
    ) {
        // « Aucune » : chip de tête pour n'enchaîner aucun 2ᵉ circuit (état actif par défaut).
        item {
            IntermissionNoneChip(
                selected = state.intermissionSelected == null,
                onClick = { onIntermissionSelected(null) },
                modifier = Modifier.padding(5.dp)
            )
        }
        items(state.intermissionList.orEmpty(), key = { it.name }) { intermission ->
            MapCell(
                Modifier.padding(5.dp),
                map = listOf(intermission) + listOfNotNull(state.mapSelected),
                borderColor = if (state.intermissionSelected == intermission) Colors.green else Colors.white,
                onClick = { onIntermissionSelected(intermission) }
            )
        }
    }
    Spacer(Modifier.height(9.dp))
    WizNav(
        onPrevious = onPrevious,
        nextLabel = stringResource(R.string.addtrack_next_positions),
        onNext = onNext
    )
}

/** Chip « Aucune » de l'intermission : pastille arrondie active/inactive (style maquette). */
@Composable
private fun IntermissionNoneChip(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (selected) Colors.green else Colors.blackAlphaed)
            .border(2.dp, if (selected) Colors.green else Colors.white, RoundedCornerShape(5.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        MKText(
            text = stringResource(R.string.addtrack_intermission_none),
            font = Fonts.NunitoBD,
            textColor = if (selected) Colors.black else Colors.white
        )
    }
}

/**
 * Étape 3 — saisie **joueur par joueur** : progression `Joueur n / total`, rappel du joueur
 * courant, grille de positions cliquables (les positions prises sont verrouillées). La
 * dernière position bascule AUTOMATIQUEMENT sur le Résumé (dans le VM). « Précédent »
 * revient à l'Intermission (et réinitialise la saisie via [AddTrackViewModel.onStepChange]).
 */
@Composable
private fun ColumnScope.PositionsStep(
    state: AddTrackViewModel.State,
    onPositionClick: (Int) -> Unit,
    onPrevious: () -> Unit
) {
    val maps = remember(state.intermissionSelected, state.mapSelected) {
        listOfNotNull(state.intermissionSelected, state.mapSelected)
    }
    if (maps.isNotEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MapCell(map = maps, onClick = {})
        }
        Spacer(Modifier.height(11.dp))
    }
    // Carte de progression : compteur + barre (style AddWar/maquette).
    state.currentPlayer?.let {
        ProgressCard(current = state.selectedPositions.size + 1, total = state.players.size)
        Spacer(Modifier.height(9.dp))
        MKText(
            text = stringResource(R.string.addtrack_positions_hint, it.name),
            textColor = Colors.white55,
            fontSize = 12,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(11.dp))
        MKText(text = it.name, fontSize = 22, font = Fonts.NunitoBD, textColor = Colors.black)
        Spacer(Modifier.height(6.dp))
    }

    val takenPositions = state.selectedPositions.map { it.position.position }.toSet()
    state.totalPositions?.let { total ->
        val size = when (total) {
            12 -> 90.dp
            else -> 70.dp
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(size),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(total) {
                PositionCell(
                    position = it + 1,
                    is24p = total == 24,
                    modifier = Modifier.size(size).padding(5.dp),
                    isVisible = !takenPositions.contains(it + 1),
                    onClick = onPositionClick
                )
            }
        }
    }
    Spacer(Modifier.height(9.dp))
    // Un seul bouton « Précédent » : le passage au Résumé est automatique à la dernière position.
    MKButton(
        modifier = Modifier.fillMaxWidth(),
        style = MKButtonStyle.Minor(Colors.white),
        text = stringResource(R.string.addwar_previous),
        onClick = onPrevious
    )
}

/**
 * Étape 4 — Résumé : carte circuit + **score de manche calculé** (barème `positionToPoints`),
 * puis grille « Positions & shocks » (une carte par joueur avec compteur de shocks − / +) et
 * CTA « Confirmer ». « Précédent » revient aux Positions.
 */
@Composable
private fun ColumnScope.SummaryStep(
    state: AddTrackViewModel.State,
    onPrevious: () -> Unit,
    onAddShock: (String) -> Unit,
    onRemoveShock: (String) -> Unit,
    onValidate: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        // Carte en-tête : circuit(s) + score de la manche calculé en direct.
        SummaryHeaderCard(state = state)

        Eyebrow(stringResource(R.string.addtrack_summary_positions))
        VerticalGrid {
            state.selectedPositions.forEach {
                PlayerCell(
                    player = it.player,
                    position = it.position.position,
                    modifier = Modifier.padding(5.dp),
                    shocksEnabled = true,
                    shockCount = state.shocks[it.player?.id],
                    is24p = state.teamOpponent.orEmpty().size > 1,
                    onAddShock = onAddShock,
                    onRemoveShock = onRemoveShock,
                    onClick = {}
                )
            }
        }
        MKText(
            text = stringResource(R.string.addtrack_summary_hint),
            textColor = Colors.white55,
            fontSize = 12,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
    Spacer(Modifier.height(9.dp))
    WizNav(
        onPrevious = onPrevious,
        nextLabel = stringResource(R.string.confirmer),
        onNext = onValidate
    )
}

/** Carte en-tête du Résumé : illustration du circuit + nom + score de manche calculé. */
@Composable
private fun SummaryHeaderCard(state: AddTrackViewModel.State) {
    val maps = listOfNotNull(state.intermissionSelected, state.mapSelected)
    val lastMap = maps.lastOrNull()
    StatCard {
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.CenterVertically) {
            lastMap?.let {
                Image(
                    painter = painterResource(it.picture),
                    contentDescription = null,
                    modifier = Modifier.width(64.dp).height(44.dp).clip(RoundedCornerShape(6.dp))
                )
            }
            Column(Modifier.weight(1f)) {
                lastMap?.let {
                    MKText(text = stringResource(it.label), font = Fonts.Bungee, textColor = Colors.white, fontSize = 15, textAlign = TextAlign.Start, maxLines = 2)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    MKText(text = "${stringResource(R.string.addtrack_summary_score)} · ", textColor = Colors.white66, fontSize = 12)
                    MKText(text = summaryScoreLabel(state), font = Fonts.NunitoBD, textColor = Colors.green, fontSize = 12)
                }
            }
        }
    }
}

/**
 * Libellé du score de manche affiché dans le résumé, selon le mode :
 * - 12p : `score hôte - score adverse (±diff)` ;
 * - 24p : `score war courant -> score war + manche` (l'adversaire est saisi ailleurs).
 */
private fun summaryScoreLabel(state: AddTrackViewModel.State): String = when (state.totalPositions) {
    24 -> {
        val base = state.scores.orEmpty().firstOrNull { it.teamId == state.rosterId }?.score ?: 0
        "$base → ${base + (state.teamHostTrackScore ?: 0)}"
    }
    else -> "${state.trackScore.orEmpty()} (${state.trackDiff.orEmpty()})"
}

/** Pied de wizard : bouton « Précédent » (secondaire) + CTA principal ([nextLabel]). */
@Composable
private fun WizNav(onPrevious: () -> Unit, nextLabel: String, onNext: () -> Unit) {
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
            text = nextLabel,
            onClick = onNext
        )
    }
}

/** Carte de progression de la saisie : compteur `n / total` + barre (identique à AddWar). */
@Composable
private fun ProgressCard(current: Int, total: Int) {
    StatCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            MKText(
                text = stringResource(R.string.addtrack_progress, current.coerceAtMost(total), total),
                font = Fonts.NunitoBD,
                fontSize = 14,
                textColor = Colors.white,
                resizable = false
            )
            Box(
                Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(20.dp)).background(Colors.white30)
            ) {
                val fraction = ((current - 1).toFloat() / total).coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth(fraction).height(6.dp).background(Colors.green))
            }
        }
    }
}
