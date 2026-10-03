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
 * [top]/[flop] = [PodiumEntry] déjà triées et disjointes (`flopExcludingTop`, #102) ; chaque ligne
 * dégrade en message sous 3 entrées ([PodiumOrMessage]). [selector] optionnel rendu en tête.
 */
@Composable
fun PodiumSectionCard(
    title: String,
    top: List<PodiumEntry>,
    flop: List<PodiumEntry>,
    onSeeAll: (() -> Unit)? = null,
    topLabel: String = stringResource(R.string.stats_podium_top),
    flopLabel: String = stringResource(R.string.stats_podium_flop),
    selector: (@Composable ColumnScope.() -> Unit)? = null
) {
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
        PodiumOrMessage(topLabel, top)
        Spacer(Modifier.height(8.dp))
        PodiumOrMessage(flopLabel, flop)
    }
}

/**
 * Podium sous label (#91 pt.1) : [PodiumRow] complète si 3 entrées, sinon message de
 * dégradation — jamais un podium tronqué (seuil d'échantillon, top/flop disjoints). Le label
 * reste affiché → la section ne disparaît pas au changement de période/tri.
 */
@Composable
fun ColumnScope.PodiumOrMessage(label: String, entries: List<PodiumEntry>) {
    PodiumSubLabel(label)
    when (entries.size) {
        3 -> PodiumRow(entries)
        else -> MKText(
            text = stringResource(R.string.stats_podium_not_enough),
            textColor = Colors.white55,
            fontSize = 12,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        )
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
