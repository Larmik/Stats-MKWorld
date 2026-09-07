package fr.harmoniamk.statsmkworld.ui.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.graphics.Color
import fr.harmoniamk.statsmkworld.ui.Colors

/**
 * Ajoute à un `LazyListScope` les lignes de 3 `PodiumCell` pour [items] (grille de classement).
 * [onClick] reçoit l'entrée métier cliquée. Mutualisé (rule 16) entre Classements et fiches
 * Adversaire/Circuit (#27). [contentColor] = couleur du texte (blanc par défaut).
 */
fun <T> LazyListScope.podiumRows(
    items: List<Pair<PodiumEntry, T>>,
    contentColor: Color = Colors.white,
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
