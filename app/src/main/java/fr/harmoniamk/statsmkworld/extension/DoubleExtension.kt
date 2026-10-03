package fr.harmoniamk.statsmkworld.extension

import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Nombre au format compact de la locale : au plus [maxFractionDigits] décimales, zéros finaux
 * retirés, sans séparateur de milliers (`4`, `4,5`). Sert aux pourcentages et aux positions
 * moyennes (#102).
 */
fun Double.toCompactString(maxFractionDigits: Int = 1): String =
    NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = maxFractionDigits
        roundingMode = RoundingMode.HALF_UP
        isGroupingUsed = false
    }.format(this)

/**
 * Format unique des pourcentages (#99) : format compact (`50 %`, `33,5 %`, `33,33 %`, au plus
 * 2 décimales, zéros finaux retirés), séparateur de la locale, espace insécable avant `%`
 * (jamais de `%` seul renvoyé à la ligne). [signed] préfixe `+` les valeurs ≥ 0 (deltas).
 */
fun Double.toPercentString(signed: Boolean = false): String =
    "${if (signed && this >= 0) "+" else ""}${toCompactString(maxFractionDigits = 2)} %"
