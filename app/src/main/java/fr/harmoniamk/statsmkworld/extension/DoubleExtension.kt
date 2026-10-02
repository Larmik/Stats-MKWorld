package fr.harmoniamk.statsmkworld.extension

import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Format unique des pourcentages (#99) : format compact (`50 %`, `33,5 %`, `33,33 %`, au plus
 * 2 décimales, zéros finaux retirés), séparateur de la locale, espace insécable avant `%`
 * (jamais de `%` seul renvoyé à la ligne). [signed] préfixe `+` les valeurs ≥ 0 (deltas).
 */
fun Double.toPercentString(signed: Boolean = false): String {
    val number = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
        roundingMode = RoundingMode.HALF_UP
        isGroupingUsed = false
    }.format(this)
    return "${if (signed && this >= 0) "+" else ""}$number %"
}
