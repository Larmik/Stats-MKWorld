package fr.harmoniamk.statsmkworld.model.local

import fr.harmoniamk.statsmkworld.debug.ShockProto
import fr.harmoniamk.statsmkworld.model.firebase.Shock


data class DatastoreShock(
    val playerId: String,
    val count: Int
) {

    constructor(shock: Shock) : this(
        playerId = shock.playerId,
        count = shock.count
    )
    constructor(proto: ShockProto) : this(
        playerId = proto.playerId,
        count = proto.count
    )

    val proto: ShockProto
        get()  {
            val builder = ShockProto.newBuilder()
                .setPlayerId(playerId)
                .setCount(count)
            return builder.build()
        }
}