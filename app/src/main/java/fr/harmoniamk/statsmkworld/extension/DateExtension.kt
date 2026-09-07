package fr.harmoniamk.statsmkworld.extension

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Formate cette date selon un patron `SimpleDateFormat` et la locale courante.
 * @param pattern Patron de formatage (ex. `"dd/MM/yyyy"`).
 */
fun Date.displayedString(pattern: String): String = SimpleDateFormat(pattern, Locale.getDefault()).format(this)

/**
 * Extrait un champ calendaire de cette date (via [Calendar]).
 * @param field Champ [Calendar] à extraire (défaut : jour du mois).
 */
fun Date.get(field: Int = Calendar.DATE): Int {
    val calendar = Calendar.getInstance()
    calendar.time = this
    return calendar.get(field)
}

/**
 * Formate cette date selon un patron `SimpleDateFormat` et la locale courante (alias de
 * [displayedString]).
 * @param format Patron de formatage.
 */
fun Date.format(format: String): String = SimpleDateFormat(format, Locale.getDefault()).format(this)
