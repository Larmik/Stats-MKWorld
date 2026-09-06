package fr.harmoniamk.statsmkworld.ui.cells

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.extension.positionColor
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText

/**
 * Cellule joueur avec compteur de shocks et édition optionnelle de la position, partagée (rule 16).
 *
 * Carte translucide, nom en haut puis grille 3 colonnes centrée : `−` | (position en haut, icône
 * shock + compteur en bas) | `+`, rangées à hauteur fixe pour aligner les steppers. Si
 * [onDecreasePosition]/[onIncreasePosition] sont fournis, la position porte des boutons ± bornés
 * (1..[maxPosition]) ; sinon lecture seule.
 *
 * Shocks **hors calcul du score** ; la position alimente le recalcul du score à la validation.
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
    // Édition de position : quand les deux callbacks sont fournis, boutons ± bornés à 1..[maxPosition].
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
        MKText(
            text = name,
            font = Fonts.NunitoBD,
            textColor = Colors.white,
            fontSize = 13,
            maxLines = 1
        )
        // Grille 3 colonnes à rangées de hauteur fixe (steppers alignés) ; position = numéro coloré seul.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Colonne des « − » : position (haut, optionnel) + shock (bas).
            Column(
                verticalArrangement = Arrangement.spacedBy(9.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StepperSlot(height = PositionRowHeight) {
                    onDecreasePosition?.let { StepperButton(symbol = "−", enabled = position > 1, onClick = it) }
                }
                StepperSlot(height = ShockRowHeight) {
                    StepperButton(symbol = "−", onClick = onRemoveShock)
                }
            }
            // Colonne centrale : valeur de position (haut) + [icône shock + compteur] (bas).
            Column(
                verticalArrangement = Arrangement.spacedBy(9.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StepperSlot(height = PositionRowHeight) {
                    MKText(
                        text = position.toString(),
                        font = Fonts.MKPosition,
                        textColor = position.positionColor(is24p),
                        fontSize = 34,
                        resizable = false
                    )
                }
                StepperSlot(height = ShockRowHeight) {
                    // Illustration du shock collée à GAUCHE du compteur.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.shock),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        MKText(
                            text = shockCount.toString(),
                            font = Fonts.Urbanist,
                            textColor = Colors.white,
                            fontSize = 13,
                            resizable = false
                        )
                    }
                }
            }
            // Colonne des « + » : position (haut, optionnel) + shock (bas).
            Column(
                verticalArrangement = Arrangement.spacedBy(9.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StepperSlot(height = PositionRowHeight) {
                    onIncreasePosition?.let { StepperButton(symbol = "+", enabled = position < maxPosition, onClick = it) }
                }
                StepperSlot(height = ShockRowHeight) {
                    StepperButton(symbol = "+", onClick = onAddShock)
                }
            }
        }
    }
}

/** Hauteur de rangée « position » (aligne le numéro et les boutons ± de position). */
private val PositionRowHeight = 34.dp

/** Hauteur de rangée « shock » (aligne l'icône+compteur et les boutons ± de shock). */
private val ShockRowHeight = 22.dp

/** Emplacement de grille à hauteur fixe : contenu centré, ou vide pour réserver la place. */
@Composable
private fun StepperSlot(height: Dp, content: @Composable () -> Unit) {
    Box(
        Modifier.height(height),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** Bouton carré `−`/`+` (`.shk button` maquette) ; [enabled] false = grisé et non cliquable (butée). */
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
