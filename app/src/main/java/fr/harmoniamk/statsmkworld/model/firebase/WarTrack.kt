package fr.harmoniamk.statsmkworld.model.firebase

import fr.harmoniamk.statsmkworld.model.local.DatastoreWarTrack

data class WarTrack(
    val id: Long,
    val index: Int,
    val positions: List<WarPosition>
) {
    constructor(track: DatastoreWarTrack) : this(
        id = track.id,
        index = track.index,
        positions = track.positions
    )

}