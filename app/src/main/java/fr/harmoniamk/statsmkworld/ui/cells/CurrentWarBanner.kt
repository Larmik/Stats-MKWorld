package fr.harmoniamk.statsmkworld.ui.cells

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

// Rayon des cartes du dashboard (maquette : radius 6px).
private val BannerRadius = RoundedCornerShape(6.dp)

/**
 * Bannière « War en cours », partagée par l'Accueil et l'historique Wars (rule 16). Dégradé
 * vert→sombre (`.cbanner`), pastille « En direct » ; corps issu de [CurrentWarCell].
 *
 * @param callToAction texte d'appel à l'action au pied ; masqué si `null`.
 */
@Composable
fun CurrentWarBanner(
    war: War,
    withPlayers: Boolean = true,
    callToAction: String? = null,
    onClick: () -> Unit
) {
    // Nombre de joueurs : 12p (1 adversaire) → 12, 24p (3 équipes) → 24.
    val players = if (war.teamOpponent.size > 1) 24 else 12
    val eyebrow = when (withPlayers) {
        true -> stringResource(R.string.home_live_players, players)
        else -> stringResource(R.string.war_live)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(Color(0x4081C995), Colors.blackAlphaed)),
                BannerRadius
            )
            .border(1.dp, Colors.green, BannerRadius)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(Colors.green))
            MKText(text = eyebrow, font = Fonts.NunitoBD, textColor = Colors.green, fontSize = 11, textAlign = TextAlign.Start)
        }
        Spacer(Modifier.height(6.dp))
        CurrentWarCell(
            onClick = onClick,
            viewModel = hiltViewModel(
                key = war.id.toString() + war.tracks.joinToString { it.id.toString() },
                creationCallback = { factory: CurrentWarCellViewModel.Factory -> factory.create(war) }
            )
        )
        callToAction?.let {
            Spacer(Modifier.height(6.dp))
            MKText(text = it, font = Fonts.NunitoBD, textColor = Colors.green, fontSize = 13, textAlign = TextAlign.Start)
        }
    }
}
