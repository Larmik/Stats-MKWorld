package fr.harmoniamk.statsmkworld.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Stepper de wizard (`.stepper`/`.stp` maquette) : rangée d'étapes de poids égal, étape active =
 * pastille blanche/texte sombre, autres = texte atténué. LE composant stepper du projet, ne pas
 * recréer (rules 15/16). Utilisé par le wizard AddWar. Stateless : étape courante pilotée par
 * [step], index cliqué remonté via [onStepClick] ; cliquable seulement si [enabled] l'autorise.
 */
@Composable
fun MKStepper(
    steps: List<String>,
    step: Int = 0,
    enabled: (Int) -> Boolean = { true },
    onStepClick: (Int) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        steps.forEachIndexed { index, label ->
            val active = index == step
            val clickable = enabled(index)
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (active) Colors.white else Colors.white30)
                    .let { base -> if (clickable) base.clickable { onStepClick(index) } else base }
                    .padding(vertical = 7.dp, horizontal = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                MKText(
                    text = label,
                    font = Fonts.NunitoBD,
                    textColor = if (active) Colors.black else Colors.white55,
                    fontSize = 11,
                    maxLines = 1
                )
            }
        }
    }
}
