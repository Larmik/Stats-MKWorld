package fr.harmoniamk.statsmkworld.model.firebase

import fr.harmoniamk.statsmkworld.model.local.DatastoreShock
import java.io.Serializable

data class Shock(var playerId: String, var count: Int): Serializable {
    constructor(shock: DatastoreShock) : this(
        playerId = shock.playerId,
        count = shock.count
    )
}