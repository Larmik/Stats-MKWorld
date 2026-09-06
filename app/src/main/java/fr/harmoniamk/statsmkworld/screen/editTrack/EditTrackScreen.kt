package fr.harmoniamk.statsmkworld.screen.editTrack

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.extension.displayName
import fr.harmoniamk.statsmkworld.model.local.Maps
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKTextField
import fr.harmoniamk.statsmkworld.ui.cells.MKTrackCell
import fr.harmoniamk.statsmkworld.ui.cells.PlayerShockCell

/**
 * Édition d'une course saisie — 2 onglets (segmented partagé, état `rememberSaveable`, rule 11) :
 * `Circuit` (grille de sélection) et `Positions` (positions & shocks fusionnés, #46). « Confirmer »
 * n'est actif que si toutes les positions sont distinctes. Graphe racine → pas de bottombar (rule 17).
 */
@Composable
fun EditTrackScreen(
    viewModel: EditTrackViewModel,
    onBack: () -> Unit,
    onBackToCurrent: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Onglet courant et recherche : purs états UI éphémères (rule 11), survivant à la rotation.
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var search by rememberSaveable { mutableStateOf("") }

    BackHandler { onBack() }
    LaunchedEffect(Unit) {
        viewModel.backToCurrent.collect { onBackToCurrent() }
    }

    BaseScreen(title = stringResource(R.string.edition_circuit), onBack = onBack, modifier = Modifier.fillMaxSize()) {
        MKSegmentedSelector(
            items = listOf(
                stringResource(R.string.circuit),
                stringResource(R.string.edittrack_tab_positions_shocks)
            ),
            page = tab,
            onClick = { tab = it }
        )
        Spacer(Modifier.height(13.dp))

        when (tab) {
            0 -> CircuitTab(
                state = state,
                search = search,
                onSearch = {
                    search = it
                    viewModel.onSearch(it)
                },
                onMapSelected = {
                    search = ""
                    viewModel.onMapSelected(listOf(it))
                }
            )
            else -> PositionsTab(
                state = state,
                onDecreasePosition = { viewModel.onPositionChange(it, -1) },
                onIncreasePosition = { viewModel.onPositionChange(it, 1) },
                onAddShock = viewModel::onAddShock,
                onRemoveShock = viewModel::onRemoveShock
            )
        }

        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            MKButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.cancel),
                onClick = onBack
            )
            MKButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.confirmer),
                enabled = state.buttonEnabled,
                onClick = viewModel::onValidate
            )
        }
    }
}

/**
 * Onglet Circuit — recherche + grille ([MKTrackCell]). Circuit courant liseré ; choisir met à
 * jour la sélection (score recalculé à la validation).
 */
@Composable
private fun ColumnScope.CircuitTab(
    state: EditTrackViewModel.State,
    search: String,
    onSearch: (String) -> Unit,
    onMapSelected: (Maps) -> Unit
) {
    when (state.mapList.isEmpty()) {
        true -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        else -> {
            MKTextField(
                value = search,
                onValueChange = onSearch,
                placeHolderRes = R.string.rechercher_un_circuit,
                backgroundColor = Colors.blackAlphaed
            )
            Spacer(Modifier.height(9.dp))
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
                items(state.mapList, key = { it.name }) { map ->
                    MKTrackCell(
                        map = map,
                        selected = state.mapSelected?.contains(map) == true,
                        onClick = { onMapSelected(map) }
                    )
                }
            }
        }
    }
}

/**
 * Onglet Positions (positions & shocks fusionnés) — une ligne par joueur ([PlayerShockCell])
 * portant deux contrôles ± (position bornée 1..12 / 1..24, et shocks). Score recalculé à la validation.
 */
@Composable
private fun ColumnScope.PositionsTab(
    state: EditTrackViewModel.State,
    onDecreasePosition: (String) -> Unit,
    onIncreasePosition: (String) -> Unit,
    onAddShock: (String) -> Unit,
    onRemoveShock: (String) -> Unit
) {
    when (state.selectedPositions.isEmpty()) {
        true -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        else -> Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Colors.blackAlphaed, RoundedCornerShape(6.dp))
                    .padding(11.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                state.selectedPositions.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        pair.forEach { playerPosition ->
                            val playerId = playerPosition.player?.id
                            PlayerShockCell(
                                name = playerPosition.player?.name.orEmpty().displayName,
                                position = playerPosition.position.position,
                                is24p = state.is24p,
                                maxPosition = state.maxPosition,
                                shockCount = state.shocks[playerId] ?: 0,
                                onDecreasePosition = { playerId?.let(onDecreasePosition) },
                                onIncreasePosition = { playerId?.let(onIncreasePosition) },
                                onAddShock = { playerId?.let(onAddShock) },
                                onRemoveShock = { playerId?.let(onRemoveShock) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
