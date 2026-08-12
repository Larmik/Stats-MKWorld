package fr.harmoniamk.statsmkworld.screen.addTrack

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import fr.harmoniamk.statsmkworld.extension.diffColor
import fr.harmoniamk.statsmkworld.extension.positionColor
import fr.harmoniamk.statsmkworld.ui.cells.MKTrackCell
import fr.harmoniamk.statsmkworld.ui.cells.PositionCell
import fr.harmoniamk.statsmkworld.ui.stats.StatCard

/**
 * Écran d'ajout d'une course dans la war en cours (pôle Wars) — wizard **4 étapes sur un
 * seul écran** : `Circuit` → `Intermission` → `Positions` → `Résumé`, bascule **dynamique**
 * (aucune re-navigation, rule 11). L'étape courante vit dans le [AddTrackViewModel] : le
 * stepper et la saisie joueur-par-joueur la font évoluer, le retour arrière réinitialise
 * l'étape rejointe (rule 11 wizard).
 *
 * Rendu pixel-perfect vs la maquette prototype UX (écran `addtrack`, rule 13/15) : stepper
 * partagé [fr.harmoniamk.statsmkworld.ui.MKStepper], cellule circuit partagée [MKTrackCell]
 * (sélection Circuit, Intermission, aperçu Positions) et [PositionCell] (rule 16) ; cellule
 * joueur du Résumé fidèle à la maquette ([SummaryPlayerCell]). Écran du graphe racine poussé
 * par-dessus CurrentWar → **pas de bottombar**, aucune marge basse requise (rule 17).
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
        // Libellés d'étapes selon le mode : l'Intermission ne figure qu'en 24p (wizard à 3
        // étapes en 12p, 4 en 24p). L'ordre suit les index sémantiques du State.
        val steps = when (state.is24p) {
            true -> listOf(
                stringResource(R.string.addtrack_step_circuit),
                stringResource(R.string.addtrack_step_intermission),
                stringResource(R.string.addtrack_step_positions),
                stringResource(R.string.addtrack_step_summary)
            )
            else -> listOf(
                stringResource(R.string.addtrack_step_circuit),
                stringResource(R.string.addtrack_step_positions),
                stringResource(R.string.addtrack_step_summary)
            )
        }
        MKStepper(
            steps = steps,
            step = state.step,
            enabled = { index ->
                when (index) {
                    // Circuit : toujours ; Intermission/Positions : circuit choisi ;
                    // Résumé : line-up de positions complète.
                    state.stepCircuit -> true
                    state.stepSummary -> state.positionsComplete
                    else -> state.mapPicked
                }
            },
            onStepClick = viewModel::onStepChange
        )
        Spacer(Modifier.height(13.dp))

        when (state.step) {
            state.stepCircuit -> CircuitStep(
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
            // Intermission : 24p uniquement (en 12p, stepIntermission == -1, jamais atteint).
            state.stepIntermission -> IntermissionStep(
                state = state,
                onIntermissionSelected = viewModel::onIntermissionSelected,
                onPrevious = { viewModel.onStepChange(state.stepCircuit) },
                onNext = { viewModel.onStepChange(state.stepPositions) }
            )
            state.stepPositions -> PositionsStep(
                state = state,
                onPositionClick = viewModel::onPositionClick,
                onPrevious = { viewModel.onStepChange(state.step - 1) }
            )
            else -> SummaryStep(
                state = state,
                onPrevious = { viewModel.onStepChange(state.stepPositions) },
                onAddShock = viewModel::onAddShock,
                onRemoveShock = viewModel::onRemoveShock,
                onValidate = viewModel::onValidate
            )
        }
    }
}

/**
 * Étape Circuit — recherche + grille de circuits (`MKTrackCell` en mode sélection). Choisir un
 * circuit avance à l'Intermission (24p) ou directement aux Positions (12p).
 */
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
    Spacer(Modifier.height(9.dp))
    // Grille englobée dans un conteneur sombre (blackAlphaed, coins arrondis) pour le contraste.
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(11.dp),
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(Colors.blackAlphaed, RoundedCornerShape(6.dp))
    ) {
        // Cellule circuit MUTUALISÉE avec CurrentWar (rule 16 : MKTrackCell), en mode
        // sélection (image + nom, sans score).
        items(state.mapList, key = { it.name }) { map ->
            MKTrackCell(map = map, onClick = { onMapSelected(map) })
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
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().weight(1f)
    ) {
        // « Aucune » : chip de tête pour n'enchaîner aucun 2ᵉ circuit (état actif par défaut).
        item {
            IntermissionNoneChip(
                selected = state.intermissionSelected == null,
                onClick = { onIntermissionSelected(null) }
            )
        }
        // Cellules circuit MUTUALISÉES (MKTrackCell), en mode sélection : la cellule active
        // (intermission retenue) est liserée en vert.
        items(state.intermissionList.orEmpty(), key = { it.name }) { intermission ->
            MKTrackCell(
                map = intermission,
                selected = state.intermissionSelected == intermission,
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
            .height(84.dp) // aligné sur la hauteur des MKTrackCell voisines
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Colors.green else Colors.white30)
            .border(2.dp, if (selected) Colors.green else Colors.white55, RoundedCornerShape(6.dp))
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
 * revient à l'étape précédente (Intermission en 24p, Circuit en 12p) et réinitialise la
 * saisie via [AddTrackViewModel.onStepChange].
 */
@Composable
private fun ColumnScope.PositionsStep(
    state: AddTrackViewModel.State,
    onPositionClick: (Int) -> Unit,
    onPrevious: () -> Unit
) {
    // Aperçu du circuit en tête = MÊME cellule que la sélection Circuit (MKTrackCell unifié,
    // rule 16), en **pleine largeur** (moins les marges de l'écran). En intermission (24p), on
    // montre le circuit d'arrivée (`intermissionSelected`), sinon le circuit principal.
    val headerMap = state.intermissionSelected ?: state.mapSelected
    headerMap?.let {
        MKTrackCell(map = it, onClick = {}, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(11.dp))
    }
    // Carte de progression : compteur + barre (style AddWar/maquette) + joueur courant.
    state.currentPlayer?.let {
        ProgressCard(current = state.selectedPositions.size + 1, total = state.players.size)
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
                    // Police réduite (rendu plus harmonieux dans la grille AddTrack).
                    fontSize = if (total == 24) 34 else 48,
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

        // Grille des cellules joueurs, englobée dans le même conteneur sombre (blackAlphaed,
        // coins arrondis) que la grille de circuits, pour le contraste.
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Colors.blackAlphaed, RoundedCornerShape(6.dp))
                .padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            // Cartes joueur en 2 colonnes (`.two` de la maquette).
            state.selectedPositions.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    pair.forEach { playerPosition ->
                        SummaryPlayerCell(
                            name = playerPosition.player?.name.orEmpty(),
                            position = playerPosition.position.position,
                            is24p = state.is24p,
                            shockCount = state.shocks[playerPosition.player?.id] ?: 0,
                            onAddShock = { playerPosition.player?.id?.let(onAddShock) },
                            onRemoveShock = { playerPosition.player?.id?.let(onRemoveShock) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
    Spacer(Modifier.height(9.dp))
    WizNav(
        onPrevious = onPrevious,
        nextLabel = stringResource(R.string.confirmer),
        onNext = onValidate
    )
}

/**
 * Cellule joueur du Résumé, carte translucide (`white30`, radius 6, padding 11) en **colonne
 * verticale centrée** (rules 13/15) :
 * - **en haut** : le **nom** du joueur (Nunito bold) ;
 * - **au milieu** : la **position** dans un **carré blanc semi-transparent** (`white85`,
 *   contraste), numéro en police `MKPosition` + couleur `positionColor` (comme l'ancienne `PlayerCell`) ;
 * - **en bas** : le **compteur de shocks** (illustration `R.drawable.shock` + contrôle `− N +`).
 *
 * Le compteur [shockCount] reflète le nombre courant. Shocks **hors calcul du score**.
 */
@Composable
private fun SummaryPlayerCell(
    name: String,
    position: Int,
    is24p: Boolean,
    shockCount: Int,
    onAddShock: () -> Unit,
    onRemoveShock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Colors.white30, RoundedCornerShape(6.dp))
            .padding(11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        // Haut : nom du joueur.
        MKText(
            text = name,
            font = Fonts.NunitoBD,
            textColor = Colors.white,
            fontSize = 13,
            maxLines = 1
        )
        // Milieu : position dans un carré blanc SEMI-TRANSPARENT (white85 : léger alpha, la
        // lisibilité du numéro coloré positionColor restant assurée).
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Colors.white85, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            MKText(
                text = position.toString(),
                font = Fonts.MKPosition,
                textColor = position.positionColor(is24p),
                fontSize = 34,
                resizable = false
            )
        }
        // Bas : illustration du shock + contrôle − N +.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(
                painter = painterResource(R.drawable.shock),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            ShockStepperButton(symbol = "−", onClick = onRemoveShock)
            MKText(
                text = shockCount.toString(),
                font = Fonts.Urbanist,
                textColor = Colors.white,
                fontSize = 13,
                resizable = false,
                modifier = Modifier.width(14.dp)
            )
            ShockStepperButton(symbol = "+", onClick = onAddShock)
        }
    }
}

/** Bouton carré `−`/`+` du contrôle de shocks (`.shk button` de la maquette : 22 dp, radius 6). */
@Composable
private fun ShockStepperButton(symbol: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Colors.white30, RoundedCornerShape(6.dp))
            .border(1.dp, Colors.whiteBorderSoft, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        MKText(text = symbol, font = Fonts.Urbanist, textColor = Colors.white, fontSize = 15, resizable = false)
    }
}

/**
 * Carte en-tête du Résumé : illustration du circuit + nom + score de manche calculé.
 * **Score en blanc**, **diff colorisée** (vert/rouge/blanc selon le signe, via `Int.diffColor`
 * mutualisé avec CurrentWar).
 */
@Composable
private fun SummaryHeaderCard(state: AddTrackViewModel.State) {
    val maps = listOfNotNull(state.intermissionSelected, state.mapSelected)
    val lastMap = maps.lastOrNull()
    // Diff signé (hôte − adverse) = points de manche hôte − complément adverse. En 24p,
    // pas de diff par manche (l'adversaire est saisi ailleurs).
    val diff = (state.teamHostTrackScore ?: 0) - (state.teamOpponentScore ?: 0)
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
                    // Score en blanc.
                    MKText(text = summaryScoreLabel(state), font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 12)
                    // Diff colorisée (12p uniquement : en 24p, pas d'adverse par manche).
                    if (!state.is24p) {
                        MKText(
                            text = "  (${state.trackDiff.orEmpty()})",
                            font = Fonts.NunitoBD,
                            textColor = diff.diffColor(),
                            fontSize = 12
                        )
                    }
                }
            }
        }
    }
}

/**
 * Libellé du score de manche affiché dans le résumé, selon le mode :
 * - 12p : `score hôte - score adverse` (la diff est affichée à part, colorisée) ;
 * - 24p : `score war courant → score war + manche` (l'adversaire est saisi ailleurs).
 */
private fun summaryScoreLabel(state: AddTrackViewModel.State): String = when (state.totalPositions) {
    24 -> {
        val base = state.scores.orEmpty().firstOrNull { it.teamId == state.rosterId }?.score ?: 0
        "$base → ${base + (state.teamHostTrackScore ?: 0)}"
    }
    else -> state.trackScore.orEmpty()
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
