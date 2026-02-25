package fr.harmoniamk.statsmkworld.model.firebase

import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayer

data class User(
    val id: String,
    var currentWar: String? = null,
    var role: Int = 0,
    val discordId: String = "",
    val name: String = ""
) {
    constructor(player: MKCPlayer?, role: Int? = null, currentWar: String? = null) : this(
        id = player?.id.toString(),
        currentWar = currentWar,
        role = role ?: 0,
        discordId = player?.discord?.discordID.orEmpty(),
        name = player?.name.orEmpty()
    )
    constructor(entity: PlayerEntity) : this(
        id = entity.id,
        currentWar = entity.currentWar,
        role = entity.role,
        discordId = entity.discordId,
        name = entity.name
    )
}