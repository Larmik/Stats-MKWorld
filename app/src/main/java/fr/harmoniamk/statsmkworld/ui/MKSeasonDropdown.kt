package fr.harmoniamk.statsmkworld.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.database.entities.SeasonEntity

/**
 * Menu déroulant de sélection de **saison** (#70), composant **partagé unique** (rule 16)
 * des headers Accueil/Wars/Stats/Classements. Stateless : sélection pilotée par
 * [selectedSeasonNumber] (`null` = tout l'historique), choix remonté via [onSeasonSelected] ;
 * seul l'état d'ouverture est local (rule 11). Rien affiché si [seasons] est vide.
 *
 * ⚠️ Écart assumé vs maquette (rules 13/15) : le prototype ne prévoit pas de dropdown de saison.
 */
@Composable
fun MKSeasonDropdown(
    seasons: List<SeasonEntity>,
    selectedSeasonNumber: Int?,
    onSeasonSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (seasons.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }

    val currentLabel = selectedSeasonNumber
        ?.let { number -> stringResource(R.string.season_label, number) }
        ?: stringResource(R.string.all_seasons)

    // Trigger + menu dans un Box aligné TopEnd : popup ancré au bord droit, sans déborder à gauche.
    Box(modifier = modifier.wrapContentSize(Alignment.TopEnd)) {
        // Pastille de header partagée (rule 16) ; chevron « ▾ » en trailing (pas de drawable dédié).
        MKHeaderChip(
            label = currentLabel,
            onClick = { expanded = true },
            trailing = {
                MKText(text = "▾", font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 12)
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(x = 0.dp, y = 4.dp)
        ) {
            // « Tout l'historique » (null) en tête, puis une entrée par saison.
            SeasonMenuItem(
                label = stringResource(R.string.all_seasons),
                selected = selectedSeasonNumber == null
            ) {
                onSeasonSelected(null)
                expanded = false
            }
            seasons.forEach { season ->
                SeasonMenuItem(
                    label = stringResource(R.string.season_label, season.number),
                    selected = selectedSeasonNumber == season.number
                ) {
                    onSeasonSelected(season.number)
                    expanded = false
                }
            }
        }
    }
}

/** Item du menu : entrée sélectionnée mise en évidence en vert (lisible sur le fond clair). */
@Composable
private fun SeasonMenuItem(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            MKText(
                text = label,
                font = if (selected) Fonts.NunitoBD else Fonts.NunitoRG,
                textColor = if (selected) Colors.green else MaterialTheme.colorScheme.onSurface,
                fontSize = 14,
                textAlign = TextAlign.Start
            )
        },
        onClick = onClick
    )
}
