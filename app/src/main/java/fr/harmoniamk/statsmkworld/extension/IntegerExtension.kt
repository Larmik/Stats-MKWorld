package fr.harmoniamk.statsmkworld.extension

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

fun Int?.toTeamColor()  = Color(
    when (this) {
        1 -> "#ef5350"
        2 -> "#ffa726"
        3 -> "#d4e157"
        4 -> "#66bb6a"
        5 -> "#26a69a"
        6 -> "#29b6f6"
        7 -> "#5c6bc0"
        8 -> "#7e57c2"
        9 -> "#ec407a"
        10 -> "#888888"
        11 -> "#c62828"
        12 -> "#ef6c00"
        13 -> "#9e9d24"
        14 -> "#2e7d32"
        15 -> "#00897b"
        16 -> "#0277bd"
        17 -> "#283593"
        18 -> "#4527a0"
        19 -> "#ad1457"
        20 -> "#444444"
        21 -> "#d44a48"
        22 -> "#e69422"
        23 -> "#bdc74e"
        24 -> "#4a874c"
        25 -> "#208c81"
        26 -> "#25a5db"
        27 -> "#505ca6"
        28 -> "#6c4ca8"
        29 -> "#d13b6f"
        30 -> "#545454"
        31 -> "#ab2424"
        32 -> "#d45f00"
        33 -> "#82801e"
        34 -> "#205723"
        35 -> "#006e61"
        36 -> "#0369a3"
        37 -> "#222d78"
        38 -> "#382185"
        39 -> "#91114b"
        else -> "#000000"
    }.toColorInt())

fun Int?.positionColor()  = Color(
    when (this) {
        1 ->"#D4AF37"
        2 -> "#C0C0C0"
        3 -> "#C49C48"
        4 -> "#F1B04C"
        5 -> "#EE9F27"
        6,7 -> "#EC9006"
        8 -> "#E88504"
        9,10 -> "#E27602"
        11 -> "#DC6601"
        12 -> "#D24E01"
        else -> "#000000"
    }.toColorInt())

fun Int?.positionToPoints() = when (this) {
    1 -> 15
    2 -> 12
    3 -> 10
    4 -> 9
    5 -> 8
    6 -> 7
    7 -> 6
    8 -> 5
    9 -> 4
    10 -> 3
    11 -> 2
    12 -> 1
    else -> 0
}