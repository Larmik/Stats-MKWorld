package fr.harmoniamk.statsmkworld.model.firebase

import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayer

data class User(
    val id: String,
    var currentWar: String? = null,
    var role: Int = 0,
    val discordId: String = "",
    val name: String = ""
) {
    constructor(player: MKCPlayer?) : this(
        id = player?.id.toString(),
        currentWar = null,
        role = 0,
        discordId = player?.discord?.discordID.orEmpty(),
        name = player?.name.orEmpty()
    )
}