package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.graphics.Color
import fr.harmoniamk.statsmkworld.ui.Colors

/**
 * Ajoute à un `LazyListScope` les lignes de 3 `PodiumCell` correspondant à [items]
 * (grille de classement, 3 par ligne). [onClick] reçoit l'entrée métier cliquée.
 *
 * **Mutualisé** (rule 16) entre l'écran Classements (`StatsRankingScreen`) et les
 * classements complets filtrés des fiches détail Adversaire/Circuit (#27). [contentColor]
 * pilote la couleur du texte (noir sur fond clair type Classements, blanc sur carte sombre).
 */
fun <T> LazyListScope.podiumRows(
    items: List<Pair<PodiumEntry, T>>,
    contentColor: Color = Colors.black,
    onClick: ((T) -> Unit)? = null
) {
    items.chunked(3).forEachIndexed { rowIndex, rowItems ->
        item(key = "row-$rowIndex-${rowItems.firstOrNull()?.first?.name ?: rowItems.firstOrNull()?.first?.labelRes}") {
            Column {
                PodiumRow(
                    entries = rowItems.map { it.first },
                    contentColor = contentColor,
                    onClick = onClick?.let { click -> { indexInRow -> click(rowItems[indexInRow].second) } }
                )
            }
        }
    }
}
