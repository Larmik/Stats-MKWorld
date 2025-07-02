package fr.harmoniamk.statsmkworld.model.local

import fr.harmoniamk.statsmkworld.debug.WarTrackProto
import fr.harmoniamk.statsmkworld.model.firebase.Shock
import fr.harmoniamk.statsmkworld.model.firebase.WarPosition
import fr.harmoniamk.statsmkworld.model.firebase.WarTrack

data class DatastoreWarTrack(
    val id: Long,
    val index: Int,
    val positions: List<WarPosition>,
    var shocks: List<Shock>? = null
) {

    constructor(track: WarTrack) : this(
        id = track.id,
        index = track.index,
        positions = track.positions,
        shocks = track.shocks
    )

    constructor(proto: WarTrackProto) : this(
        id = proto.id,
        index = proto.index,
        positions = proto.positionsList
            .map { DatastoreWarPosition(it) }
            .map { WarPosition(it) },
        shocks = proto.shocksList
            .map { DatastoreShock(it) }
            .map { Shock(it) }
    )

    val proto: WarTrackProto
        get()  {
            val builder = WarTrackProto.newBuilder()
                .setId(id)
                .setIndex(index)
            positions.forEach {
                builder.addPositions(DatastoreWarPosition(it).proto)
            }
            shocks?.forEach {
                builder.addShocks(DatastoreShock(it).proto)
            }
            return builder.build()
        }
}