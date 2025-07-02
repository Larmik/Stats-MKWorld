package fr.harmoniamk.statsmkworld.model.local

import fr.harmoniamk.statsmkworld.debug.WarPositionProto
import fr.harmoniamk.statsmkworld.model.firebase.WarPosition


data class DatastoreWarPosition(
    val id: Long,
    val playerId: String,
    val position: Int
) {

    constructor(position: WarPosition) : this(
        id = position.id,
        playerId = position.playerId,
        position = position.position
    )

    constructor(proto: WarPositionProto) : this(
        id = proto.id,
        playerId = proto.playerId,
        position = proto.position
    )

    val proto: WarPositionProto
        get()  {
            val builder = WarPositionProto.newBuilder()
                .setId(id)
                .setPlayerId(playerId)
                .setPosition(position)
            return builder.build()
        }
}