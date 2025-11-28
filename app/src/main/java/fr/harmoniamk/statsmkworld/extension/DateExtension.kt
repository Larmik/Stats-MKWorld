package fr.harmoniamk.statsmkworld.extension

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun Date.displayedString(pattern: String): String = SimpleDateFormat(pattern, Locale.getDefault()).format(this)

fun Date.get(field: Int = Calendar.DATE): Int {
    val c = Calendar.getInstance()
    c.time = this
    return c.get(field)
}

fun Date.format(format: String): String = SimpleDateFormat(format, Locale.getDefault()).format(this)
