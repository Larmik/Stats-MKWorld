package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

/**
 * Carte « podium » Top3 / Flop3 (lignes de 3 `PodiumCell`), mutualisée (#27) par les fiches
 * Adversaire et Circuit. En-tête + lien optionnel « Voir le classement en entier » ([onSeeAll]).
 * [top]/[flop] = [PodiumEntry] déjà triées (3 max). [selector] optionnel rendu en tête. Rien si vide.
 */
@Composable
fun PodiumSectionCard(
    title: String,
    top: List<PodiumEntry>,
    flop: List<PodiumEntry>,
    onSeeAll: (() -> Unit)? = null,
    selector: (@Composable ColumnScope.() -> Unit)? = null
) {
    if (top.isEmpty() && flop.isEmpty()) return
    StatCard(
        title = title,
        titleTrailing = onSeeAll?.let {
            {
                MKText(
                    text = stringResource(R.string.stats_see_full_ranking),
                    font = Fonts.NunitoBD,
                    textColor = Colors.yellow,
                    fontSize = 12,
                    modifier = Modifier.clickable(onClick = it)
                )
            }
        }
    ) {
        selector?.let {
            it()
            Spacer(Modifier.height(11.dp))
        }
        PodiumSubLabel(stringResource(R.string.stats_podium_top))
        PodiumRow(top)
        if (flop.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            PodiumSubLabel(stringResource(R.string.stats_podium_flop))
            PodiumRow(flop)
        }
    }
}

@Composable
private fun PodiumSubLabel(text: String) {
    MKText(
        text = text.uppercase(),
        font = Fonts.NunitoBD,
        textColor = Colors.white66,
        fontSize = 11,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
    )
}
