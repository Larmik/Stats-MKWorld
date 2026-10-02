package fr.harmoniamk.statsmkworld.extension

import java.util.Locale

/**
 * Format unique des pourcentages (#99) : 2 décimales, séparateur de la locale, espace
 * insécable avant `%` (jamais de `%` seul renvoyé à la ligne). [signed] préfixe `+` les
 * valeurs positives (deltas).
 */
fun Double.toPercentString(signed: Boolean = false): String =
    String.format(Locale.getDefault(), if (signed) "%+.2f %%" else "%.2f %%", this)
