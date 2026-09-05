package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.MKDialog

/**
 * Petit bouton rond d'information (ⓘ) placé à côté d'un indicateur de stat. Au clic,
 * ouvre [MKDialog] (composant de dialog unique de l'app, PAS de nouveau dialog) avec
 * le libellé de la stat en titre et son explication en message.
 *
 * L'état d'ouverture est un pur état UI éphémère, possédé par le bouton et conservé
 * en rotation ([rememberSaveable], rule 11). Aucune re-navigation.
 *
 * Réutilisé par toutes les sections de stats (rule 16). L'écran RÉELLEMENT rendu du
 * pôle Stats est [fr.harmoniamk.statsmkworld.screen.stats.full.StatsFullScreen] : ses
 * sections « Indicateurs / Détails équipe » (`IndicatorsCard`) et « Records & séries »
 * (`RecordsTilesCard`) émettent ce bouton sur CHAQUE tuile via `MetricTile.info`. Il est
 * aussi câblé dans le chemin legacy `MKWarDetailsStatsView` et `MKRecentFormCell`.
 */
@Composable
fun MKStatInfoButton(title: String, message: String, modifier: Modifier = Modifier) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    // Petite pastille ronde CONTRASTÉE (fond translucide `white30` + icône blanche nette) :
    // discrète mais nettement visible et cliquable à côté du libellé (ticket #87). Le fond
    // + le tint blanc plein la rendent lisible même sur carte sombre, contrairement à une
    // icône `white70` seule (trop discrète, invisible en pratique).
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(Colors.white30)
            .clickable { showDialog = true }
            .padding(2.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_info),
            contentDescription = title,
            colorFilter = ColorFilter.tint(Colors.white)
        )
    }

    if (showDialog) {
        MKDialog(
            title = title,
            message = message,
            buttonText = stringResource(R.string.close),
            onButtonClick = { showDialog = false },
            onDismiss = { showDialog = false }
        )
    }
}
