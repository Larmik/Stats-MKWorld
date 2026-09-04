package fr.harmoniamk.statsmkworld.screen.warList

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import fr.harmoniamk.statsmkworld.ui.MKChip
import fr.harmoniamk.statsmkworld.ui.MKHeaderChip
import fr.harmoniamk.statsmkworld.ui.MKSeasonDropdown
import fr.harmoniamk.statsmkworld.ui.MKText
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
    viewModel: WarListViewModel,
    onWarDetailsClick: (WarDetails) -> Unit,
    onAddWar: (Boolean) -> Unit,
    onBack: (() -> Unit)? = null,
    // Ouvre l'écran « Voir par période » (#80) : aide à la composition des line-ups sur
    // une plage de dates. Null = non proposé (ex. historique filtré sur un joueur, #65).
    onPeriodView: (() -> Unit)? = null
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    // Filtre de résultat : pur état UI, survit à la rotation (rule 11).
    var filter by rememberSaveable { mutableStateOf(WarFilter.ALL) }

    // Sous-titre : « wars de <joueur> » si l'historique est filtré sur un joueur (#65),
    // sinon le décompte habituel « N wars ».
    val subtitle = state.value.playerName
        ?.let { stringResource(R.string.wars_of_player, it, state.value.warCount) }
        ?: stringResource(R.string.wars_count, state.value.warCount)
    BaseScreen(
        title = stringResource(R.string.wars),
        subtitle = subtitle,
        onBack = onBack,
        // « Créer une war » dans l'action droite du header (#50), affichée UNIQUEMENT
        // s'il n'y a pas de war en cours (même condition qu'auparavant, juste déplacée).
        onSearch = { onAddWar(false) }.takeIf { state.value.currentWar == null },
        actionIcon = R.drawable.ic_add,
        actionContentDescription = stringResource(R.string.nouvelle_war),
        // Header trailing (#70 + #80) : « Voir par période » à gauche du dropdown de saison,
        // tous deux dans la même Row, avec le MÊME style de pastille (MKHeaderChip).
        // « Voir par période » n'apparaît que quand onPeriodView est fourni (pas dans
        // l'historique filtré sur un joueur, #65) — comportement conservé.
        headerTrailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                onPeriodView?.let { periodView ->
                    MKHeaderChip(
                        label = stringResource(R.string.period_view),
                        onClick = periodView
                    )
                }
                MKSeasonDropdown(
                    seasons = state.value.seasons,
                    selectedSeasonNumber = state.value.selectedSeasonNumber,
                    onSeasonSelected = viewModel::onSeasonSelected
                )
            }
        }
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            // La war en cours n'apparaît PLUS sur l'historique (bannière « Reprendre »
            // retirée, #65) : l'écran ne liste que les wars terminées. Le bouton « Créer
            // une war » du header reste masqué tant qu'une war est en cours (voir plus haut).

            // 1. Chips filtre Tous / Victoires / Nuls / Défaites.
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    WarFilter.entries.forEach { entry ->
                        MKChip(
                            label = stringResource(entry.labelRes),
                            active = entry == filter,
                            onClick = { filter = entry }
                        )
                    }
                }
            }

            // 2. Historique groupé par mois (sticky headers), filtré par résultat.
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
