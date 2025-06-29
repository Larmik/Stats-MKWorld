package fr.harmoniamk.statsmkworld.model.firebase

import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import fr.harmoniamk.statsmkworld.model.local.DatastoreWar


data class War(
    val id: Long,
    val teamHost: String,
    val teamOpponent: String,
    val tracks: List<WarTrack>,
    val penalties: List<WarPenalty>
) {
    var name: String? = null

    constructor(war: DatastoreWar) : this(
        id = war.id,
        teamHost = war.teamHost,
        teamOpponent = war.teamOpponent,
        tracks = war.tracks,
        penalties = war.penalties
    )

    constructor(entity: WarEntity): this(
        id = entity.id.toLong(),
        teamHost = entity.teamHost.orEmpty(),
        teamOpponent = entity.teamOpponent.orEmpty(),
        tracks = entity.warTracks.orEmpty(),
        penalties = entity.penalties.orEmpty()
    )

}