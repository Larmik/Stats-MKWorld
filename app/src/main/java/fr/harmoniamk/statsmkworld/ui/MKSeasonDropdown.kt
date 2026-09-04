package fr.harmoniamk.statsmkworld.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.database.entities.SeasonEntity

/**
 * Menu déroulant de sélection de **saison** (#70), composant **partagé unique** (rule 16)
 * réutilisé par les headers Accueil, Wars, Stats et Classements — jamais dupliqué.
 *
 * **Stateless** : la sélection est pilotée par [selectedSeasonNumber] (`null` = « Tout
 * l'historique ») et le choix remonte à l'appelant via [onSeasonSelected] (`null` = tout).
 * Seul l'état d'ouverture du menu est local (`mutableStateOf`, pur état UI éphémère, rule 11).
 *
 * **Style** : pastille alignée à droite dans l'app bar (fond blanc translucide `white30`,
 * bordure douce, coins 10 dp, texte + chevron blancs), cohérente avec les boutons d'app bar
 * de `BaseScreen`. ⚠️ **Écart assumé vs maquette (rules 13/15)** : le prototype ne prévoit
 * PAS de dropdown de saison dans les headers — style aligné au plus proche des boutons d'app bar.
 *
 * Renvoie sans rien afficher si [seasons] est vide (aucune saison hydratée → rien à filtrer).
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

    // Libellé courant : « Saison N » si une saison précise est choisie, sinon « Tout l'historique ».
    val currentLabel = selectedSeasonNumber
        ?.let { number -> stringResource(R.string.season_label, number) }
        ?: stringResource(R.string.all_seasons)

    val shape = RoundedCornerShape(10.dp)
    // Le trigger ET le menu sont dans un même Box aligné à droite (`wrapContentSize(TopEnd)`) :
    // le popup s'ancre alors sur le bord DROIT du sélecteur et s'ouvre juste en dessous
    // (bord droit aligné), au lieu de déborder vers la gauche (retour utilisateur, point 3).
    Box(modifier = modifier.wrapContentSize(Alignment.TopEnd)) {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(Colors.white30, shape)
                .border(1.dp, Colors.whiteBorderSoft, shape)
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            MKText(
                text = currentLabel,
                font = Fonts.NunitoBD,
                textColor = Colors.white,
                fontSize = 12,
                textAlign = TextAlign.Start,
                maxLines = 1
            )
            // Chevron « ▾ » (aucun drawable de flèche vers le bas dans le projet ; le texte
            // évite d'ajouter un asset — écart mineur documenté).
            MKText(text = "▾", font = Fonts.NunitoBD, textColor = Colors.white, fontSize = 12)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            // Ancre le popup à droite : dans un parent aligné TopEnd, le menu se colle au
            // bord droit du sélecteur (offset y pour l'ouvrir juste sous la pastille).
            offset = DpOffset(x = 0.dp, y = 4.dp)
        ) {
            // « Tout l'historique » en tête (valeur null), puis une entrée par saison.
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

/**
 * Item du menu : libellé + mise en évidence de l'entrée sélectionnée en **vert**
 * (`Colors.green`, accent existant de la palette) — plus lisible sur le fond clair du
 * `DropdownMenu` que le jaune précédent (retour utilisateur, point 2).
 */
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
