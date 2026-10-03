package fr.harmoniamk.statsmkworld.ui

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import fr.harmoniamk.statsmkworld.extension.mkcentralUrl
import fr.harmoniamk.statsmkworld.model.local.Tournament

/**
 * Badge d'un tournoi officiel (#103), composant unique (rule 16) de `WarCell`, du détail de war et
 * du sélecteur d'ajout. Logo officiel MKCentral en cache (#152) ; [Tournament.fallbackLogo] pendant
 * le chargement, en cas d'échec et tant que rien n'est synchronisé. Logos larges → hauteur fixe,
 * largeur bornée.
 */
@Composable
fun TournamentBadge(
    tournament: Tournament,
    modifier: Modifier = Modifier,
    height: Dp = 20.dp,
    viewModel: TournamentBadgeViewModel = hiltViewModel()
) {
    val logos by viewModel.logos.collectAsStateWithLifecycle()
    val fallback = painterResource(tournament.fallbackLogo)
    AsyncImage(
        model = logos[tournament.name]?.mkcentralUrl,
        contentDescription = stringResource(tournament.label),
        placeholder = fallback,
        error = fallback,
        fallback = fallback,
        contentScale = ContentScale.Fit,
        modifier = modifier.height(height).widthIn(max = height * 3)
    )
}
