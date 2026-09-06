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
 * Bouton rond d'information (ⓘ) à côté d'un indicateur : ouvre [MKDialog] (titre = libellé,
 * message = explication). État d'ouverture éphémère conservé en rotation ([rememberSaveable],
 * rule 11). Réutilisé par toutes les sections de stats (rule 16, via `MetricTile.info`).
 */
@Composable
fun MKStatInfoButton(title: String, message: String, modifier: Modifier = Modifier) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    // Pastille ronde contrastée (fond `white30` + icône blanche) : lisible sur carte sombre,
    // 23 dp pour la cliquabilité (#87).
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(23.dp)
            .clip(CircleShape)
            .background(Colors.white30)
            .clickable { showDialog = true }
            .padding(3.dp)
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
