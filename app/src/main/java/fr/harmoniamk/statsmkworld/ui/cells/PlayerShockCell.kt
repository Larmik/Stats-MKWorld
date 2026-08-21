package fr.harmoniamk.statsmkworld.ui.cells

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.extension.positionColor
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

/**
 * Cellule joueur avec **compteur de shocks** (et, optionnellement, **édition de la position**),
 * partagée (extraite du `SummaryPlayerCell` privé d'`AddTrackScreen`, rule 16 : mutualisée dès
 * un 2ᵉ écran consommateur — le Résumé d'AddTrack et la section Positions d'`EditTrackScreen`,
 * ticket #46).
 *
 * Carte translucide (`white30`, radius 6, padding 11) en **colonne verticale centrée**
 * (rules 13/15) :
 * - **en haut** : le **nom** du joueur (Nunito bold) ;
 * - **au milieu** : la **position** dans un **carré blanc semi-transparent** (`white85`),
 *   numéro en police `MKPosition` + couleur `positionColor`. Si [onDecreasePosition] /
 *   [onIncreasePosition] sont fournis, le carré est **encadré des boutons − / +** pour éditer
 *   la position en direct (bornés : − désactivé à 1, + désactivé à [maxPosition]) ;
 * - **en bas** : le **compteur de shocks** (illustration `R.drawable.shock` + contrôle `− N +`).
 *
 * Le compteur [shockCount] et la [position] reflètent l'état courant. Shocks **hors calcul du
 * score** ; la position, elle, alimente le recalcul du score à la validation.
 */
@Composable
fun PlayerShockCell(
    name: String,
    position: Int,
    is24p: Boolean,
    shockCount: Int,
    onAddShock: () -> Unit,
    onRemoveShock: () -> Unit,
    modifier: Modifier = Modifier,
    // Édition de position (optionnelle) : quand les deux callbacks sont fournis, le carré de
    // position est encadré de boutons − / + bornés à 1..[maxPosition].
    onDecreasePosition: (() -> Unit)? = null,
    onIncreasePosition: (() -> Unit)? = null,
    maxPosition: Int = if (is24p) 24 else 12
) {
    Column(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Colors.white30, RoundedCornerShape(6.dp))
            .padding(11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        // Haut : nom du joueur.
        MKText(
            text = name,
            font = Fonts.NunitoBD,
            textColor = Colors.white,
            fontSize = 13,
            maxLines = 1
        )
        // Milieu : position dans un carré blanc SEMI-TRANSPARENT (white85), numéro coloré,
        // éventuellement encadré des boutons − / + de position.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            onDecreasePosition?.let {
                StepperButton(symbol = "−", enabled = position > 1, onClick = it)
            }
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Colors.white85, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                MKText(
                    text = position.toString(),
                    font = Fonts.MKPosition,
                    textColor = position.positionColor(is24p),
                    fontSize = 34,
                    resizable = false
                )
            }
            onIncreasePosition?.let {
                StepperButton(symbol = "+", enabled = position < maxPosition, onClick = it)
            }
        }
        // Bas : illustration du shock + contrôle − N +.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(
                painter = painterResource(R.drawable.shock),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            StepperButton(symbol = "−", onClick = onRemoveShock)
            MKText(
                text = shockCount.toString(),
                font = Fonts.Urbanist,
                textColor = Colors.white,
                fontSize = 13,
                resizable = false,
                modifier = Modifier.width(14.dp)
            )
            StepperButton(symbol = "+", onClick = onAddShock)
        }
    }
}

/**
 * Bouton carré `−`/`+` du contrôle de shocks / de position (`.shk button` de la maquette :
 * 22 dp, radius 6). [enabled] borne le bouton aux extrémités (position min/max) : désactivé, il
 * est grisé et non cliquable.
 */
@Composable
private fun StepperButton(symbol: String, onClick: () -> Unit, enabled: Boolean = true) {
    Box(
        Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Colors.white30, RoundedCornerShape(6.dp))
            .border(1.dp, Colors.whiteBorderSoft, RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center
    ) {
        MKText(text = symbol, font = Fonts.Urbanist, textColor = Colors.white, fontSize = 15, resizable = false)
    }
}
