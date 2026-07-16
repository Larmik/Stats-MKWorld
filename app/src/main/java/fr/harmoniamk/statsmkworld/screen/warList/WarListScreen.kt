package fr.harmoniamk.statsmkworld.screen.warList

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKButton
import fr.harmoniamk.statsmkworld.ui.MKButtonStyle
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.cells.CurrentWarBanner
import fr.harmoniamk.statsmkworld.ui.cells.WarCell
import fr.harmoniamk.statsmkworld.ui.cells.WarCellViewModel

/** Filtres de résultat de l'historique (chips maquette). */
private enum class WarFilter(val labelRes: Int) {
    ALL(R.string.wars_filter_all),
    WINS(R.string.wars_filter_wins),
    TIES(R.string.wars_filter_ties),
    LOSSES(R.string.wars_filter_losses)
}

/** Résultat V/N/D d'une war (12j comme 24j) via la marge de score signée. */
private fun WarDetails.matches(filter: WarFilter): Boolean {
    if (filter == WarFilter.ALL) return true
    val margin = scoreMargin(is24p = war.teamOpponent.size > 1)
    return when (filter) {
        WarFilter.WINS -> margin > 0
        WarFilter.LOSSES -> margin < 0
        WarFilter.TIES -> margin == 0
        WarFilter.ALL -> true
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WarListScreen(
    viewModel: WarListViewModel = hiltViewModel(),
    onWarDetailsClick: (WarDetails) -> Unit,
    onAddWar: (Boolean) -> Unit,
    onCurrentWar: () -> Unit
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    // Filtre de résultat : pur état UI, survit à la rotation (rule 11).
    var filter by rememberSaveable { mutableStateOf(WarFilter.ALL) }

    BaseScreen(
        title = stringResource(R.string.wars),
        subtitle = stringResource(R.string.wars_count, state.value.warCount)
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            // 1. War en cours → bannière « Reprendre » ; sinon CTA « Nouvelle war »
            //    (masqué tant qu'une war est en cours : règle métier existante).
            item {
                when (val war = state.value.currentWar) {
                    null -> MKButton(
                        style = MKButtonStyle.Gradient,
                        text = stringResource(R.string.nouvelle_war),
                        onClick = { onAddWar(false) }
                    )
                    else -> CurrentWarBanner(
                        war = war,
                        withPlayers = false,
                        callToAction = stringResource(R.string.war_resume, war.tracks.size),
                        onClick = onCurrentWar
                    )
                }
            }

            // 2. Chips filtre Tous / Victoires / Nuls / Défaites.
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    WarFilter.entries.forEach { entry ->
                        FilterChip(
                            label = stringResource(entry.labelRes),
                            active = entry == filter,
                            onClick = { filter = entry }
                        )
                    }
                }
            }

            // 3. Historique groupé par mois (sticky headers), filtré par résultat.
            state.value.wars.forEach { (month, wars) ->
                val filtered = wars.filter { it.matches(filter) }
                if (filtered.isNotEmpty()) {
                    stickyHeader {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Colors.blackAlphaed, RoundedCornerShape(6.dp))
                                .border(1.dp, Colors.whiteBorder, RoundedCornerShape(6.dp))
                        ) {
                            MKText(
                                text = "$month (${filtered.size})",
                                font = Fonts.NunitoBD,
                                textColor = Colors.white,
                                fontSize = 14,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    items(filtered, key = { it.war.id }) {
                        WarCell(
                            viewModel = hiltViewModel(
                                key = it.war.id.toString(),
                                creationCallback = { factory: WarCellViewModel.Factory ->
                                    factory.create(it)
                                }
                            ),
                            onClick = onWarDetailsClick
                        )
                    }
                }
            }
        }
    }
}

/**
 * Chip de filtre (maquette `.chip`) : pilule arrondie ; actif = fond blanc/texte
 * sombre, inactif = fond blanc translucide/texte blanc.
 */
@Composable
private fun FilterChip(label: String, active: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .background(
                if (active) Colors.white else Colors.white30,
                RoundedCornerShape(20.dp)
            )
            .border(
                1.dp,
                if (active) Colors.white else Colors.whiteBorderSoft,
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        MKText(
            text = label,
            font = Fonts.NunitoBD,
            textColor = if (active) Colors.black else Colors.white,
            fontSize = 12
        )
    }
}
