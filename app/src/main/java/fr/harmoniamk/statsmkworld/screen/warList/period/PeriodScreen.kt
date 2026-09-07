package fr.harmoniamk.statsmkworld.screen.warList.period

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.extension.displayName
import fr.harmoniamk.statsmkworld.extension.displayedString
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.ui.BaseScreen
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKSegmentedSelector
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.cells.WarCell
import fr.harmoniamk.statsmkworld.ui.cells.WarCellViewModel
import fr.harmoniamk.statsmkworld.ui.stats.PodiumEntry
import fr.harmoniamk.statsmkworld.ui.stats.PodiumRow
import fr.harmoniamk.statsmkworld.ui.stats.initialsOf
import java.util.Date

/**
 * Écran « Voir par période » (#80) — aide à la composition des line-ups. Plage `[dateA, dateB]`
 * (semée sur la saison en cours) + deux onglets : historique (`WarCell`) et classement des
 * joueurs de la période (`PodiumRow`). 12p uniquement. Hors prototype.
 */
@Composable
fun PeriodScreen(
    viewModel: PeriodViewModel,
    onWarDetailsClick: (WarDetails) -> Unit,
    onBack: () -> Unit
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    // Onglet courant : 0 = Wars, 1 = Joueurs. Pur état UI, survit à la rotation (rule 11).
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }

    BaseScreen(
        title = stringResource(R.string.period_title),
        onBack = onBack
    ) {
        // Sélecteurs de dates (Du / Au) : bornes de la plage filtrée.
        DateRangeSelector(
            dateA = state.value.dateA,
            dateB = state.value.dateB,
            onRangeSelected = viewModel::onRangeSelected
        )
        Spacer(Modifier.height(11.dp))

        // Onglets Wars / Joueurs (segmented partagé, rule 15/16). Fond clair du dégradé → onDark = false.
        MKSegmentedSelector(
            items = listOf(
                stringResource(R.string.period_tab_wars),
                stringResource(R.string.period_tab_players)
            ),
            page = tabIndex,
            onClick = { tabIndex = it }
        )
        Spacer(Modifier.height(11.dp))

        val hasData = when (tabIndex) {
            1 -> state.value.players.isNotEmpty()
            else -> state.value.wars.isNotEmpty()
        }
        when {
            !hasData -> Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                MKText(
                    text = stringResource(R.string.period_empty),
                    font = Fonts.NunitoBD,
                    textColor = Colors.white,
                    fontSize = 15
                )
            }

            tabIndex == 1 -> PlayersTab(state.value.players)
            else -> WarsTab(state.value.wars, onWarDetailsClick)
        }
    }
}

/** Onglet Wars : compteur + liste des wars de la période via `WarCell` (rule 16). */
@Composable
private fun WarsTab(wars: List<WarDetails>, onWarDetailsClick: (WarDetails) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        item {
            MKText(
                text = stringResource(R.string.period_wars_count, wars.size),
                font = Fonts.NunitoBD,
                textColor = Colors.white,
                fontSize = 14,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
        items(wars, key = { it.war.id }) {
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

/**
 * Onglet Joueurs : classement de la période (nb wars, taux de participation, score moyen,
 * shocks) via la cellule podium mutualisée (`PodiumRow`, rule 16), 3 par ligne.
 */
@Composable
private fun PlayersTab(players: List<PeriodViewModel.PlayerPeriodStats>) {
    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Clé de ligne stable (String primitive, rule 10) : concat des ids de la ligne.
        items(players.chunked(3), key = { row -> row.joinToString("-") { it.player.id } }) { row ->
            // PodiumRow est une extension de ColumnScope.
            Column {
                PodiumRow(
                    entries = row.map { it.toPodiumEntry() },
                    columns = 3
                )
            }
        }
    }
}

/** Adapte un agrégat joueur en `PodiumEntry` (médaillon + stats), taux de participation en ligne dédiée (#80). */
@Composable
private fun PeriodViewModel.PlayerPeriodStats.toPodiumEntry(): PodiumEntry = PodiumEntry(
    name = player.name.displayName,
    initials = initialsOf(player.name.displayName),
    avatar = player.avatar,
    stats = listOf(
        R.string.period_players_wars_short to warsPlayed.toString(),
        R.string.participation_rate_short to "$participationRate %",
        R.string.period_players_average_short to averageScore.toString(),
        R.string.period_players_shocks_short to shockCount.toString()
    )
)

/**
 * Deux champs Du / Au ouvrant un `DatePickerDialog` Material3 (écart maquette documenté, rule 13).
 * Chaque validation remonte la plage au VM (`dateA ≤ dateB` borné côté VM).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeSelector(
    dateA: Long?,
    dateB: Long?,
    onRangeSelected: (Long, Long) -> Unit
) {
    var editing by rememberSaveable { mutableStateOf<Int?>(null) } // null / 0 (Du) / 1 (Au)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        DateField(
            modifier = Modifier.weight(1f),
            labelRes = R.string.period_from,
            value = dateA,
            onClick = { editing = 0 }
        )
        DateField(
            modifier = Modifier.weight(1f),
            labelRes = R.string.period_to,
            value = dateB,
            onClick = { editing = 1 }
        )
    }

    editing?.let { which ->
        val initial = if (which == 0) dateA else dateB
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { editing = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { selected ->
                        val newA = if (which == 0) selected else (dateA ?: selected)
                        val newB = if (which == 1) selected else (dateB ?: selected)
                        onRangeSelected(newA, newB)
                    }
                    editing = null
                }) {
                    MKText(text = stringResource(R.string.period_apply), font = Fonts.NunitoBD, textColor = Colors.black)
                }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) {
                    MKText(text = stringResource(R.string.cancel), font = Fonts.NunitoBD, textColor = Colors.black)
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** Champ de date (libellé + valeur formatée), style tuile translucide de la maquette. */
@Composable
private fun DateField(
    modifier: Modifier = Modifier,
    labelRes: Int,
    value: Long?,
    onClick: () -> Unit
) {
    Column(
        modifier
            .background(Colors.blackAlphaed, RoundedCornerShape(10.dp))
            .border(1.dp, Colors.whiteBorderSoft, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        MKText(
            text = stringResource(labelRes),
            textColor = Colors.white66,
            fontSize = 11,
            textAlign = TextAlign.Start
        )
        MKText(
            text = value?.let { Date(it).displayedString("dd/MM/yyyy") } ?: "-",
            font = Fonts.NunitoBD,
            textColor = Colors.white,
            fontSize = 15,
            maxLines = 1,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
