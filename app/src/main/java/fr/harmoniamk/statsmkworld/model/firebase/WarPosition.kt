package fr.harmoniamk.statsmkworld.model.firebase

import fr.harmoniamk.statsmkworld.model.local.DatastoreWarPosition

data class WarPosition(
    val id: Long,
    val playerId: String,
    val position: Int
) {
    constructor(position: DatastoreWarPosition) : this(
        id = position.id,
        playerId = position.playerId,
        position = position.position
    )

}