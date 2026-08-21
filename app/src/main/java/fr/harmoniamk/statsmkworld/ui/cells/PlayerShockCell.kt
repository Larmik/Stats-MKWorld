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
 * Cellule joueur avec **compteur de shocks** (et, optionnellement, **édition de la position**),
 * partagée (extraite du `SummaryPlayerCell` privé d'`AddTrackScreen`, rule 16 : mutualisée dès
 * un 2ᵉ écran consommateur — le Résumé d'AddTrack et la section Positions d'`EditTrackScreen`,
 * ticket #46).
 *
 * Carte translucide (`white30`, radius 6, padding 11) en **colonne verticale centrée**
 * (rules 13/15) :
 * - **en haut** : le **nom** du joueur (Nunito bold) ;
 * - **au milieu** : une **petite grille alignée en 3 colonnes centrée** — colonne des `−` |
 *   colonne centrale (**position** en haut : numéro `MKPosition` + couleur `positionColor`,
 *   **sans fond blanc** ; **[icône shock + compteur]** en bas) | colonne des `+`. Les rangées
 *   ont une hauteur fixe → tous les `−` alignés sur une colonne, tous les `+` sur une autre.
 *   Si [onDecreasePosition] / [onIncreasePosition] sont fournis, la ligne de position porte des
 *   boutons − / + (bornés : − désactivé à 1, + désactivé à [maxPosition]) ; sinon la place est
 *   réservée mais vide (position en lecture seule — cas du Résumé d'AddTrack).
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
        // Milieu : petite GRILLE alignée en 3 colonnes, centrée —
        //   colonne des −  |  colonne centrale (position en haut, [icône shock + compteur] en bas)  |  colonne des +
        // Les 2 rangées ont une hauteur fixe : ainsi tous les « − » sont alignés sur une même
        // colonne, tous les « + » sur une autre, et position/shock partagent la colonne centrale.
        // Plus de fond blanc autour de la position (retour utilisateur) : numéro coloré seul.
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

/**
 * Emplacement d'une cellule de la grille d'alignement : hauteur fixe, contenu centré (ou vide,
 * pour réserver la place quand un contrôle est absent — ex. steppers de position non fournis).
 */
@Composable
private fun StepperSlot(height: Dp, content: @Composable () -> Unit) {
    Box(
        Modifier.height(height),
        contentAlignment = Alignment.Center
    ) {
        content()
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
