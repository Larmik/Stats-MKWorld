package fr.harmoniamk.statsmkworld.model.firebase


data class User(
    val id: String,
    var currentWar: String? = null,
    var role: Int = 0
)