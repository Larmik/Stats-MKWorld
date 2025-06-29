package fr.harmoniamk.statsmkworld.extension

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Date.displayedString(pattern: String): String = SimpleDateFormat(pattern, Locale.getDefault()).format(this)
