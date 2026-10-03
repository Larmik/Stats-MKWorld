package fr.harmoniamk.statsmkworld.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.model.local.Tournament

/**
 * Badge d'un tournoi officiel (#103) : logo embarqué ([Tournament.fallbackLogo]). Composant
 * unique (rule 16) de `WarCell`, du détail de war et du sélecteur d'ajout ; #152 y branchera le
 * logo officiel MKCentral sans toucher aux écrans. Logos larges → hauteur fixe, largeur bornée.
 */
@Composable
fun TournamentBadge(tournament: Tournament, modifier: Modifier = Modifier, height: Dp = 20.dp) {
    Image(
        painter = painterResource(tournament.fallbackLogo),
        contentDescription = stringResource(tournament.label),
        contentScale = ContentScale.Fit,
        modifier = modifier.height(height).widthIn(max = height * 3)
    )
}
