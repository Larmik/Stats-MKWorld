package fr.harmoniamk.statsmkworld.model.local

import fr.harmoniamk.statsmkworld.debug.WarPenaltyProto
import fr.harmoniamk.statsmkworld.debug.WarPositionProto
import fr.harmoniamk.statsmkworld.model.firebase.WarPenalty
import fr.harmoniamk.statsmkworld.model.firebase.WarPosition

data class DatastoreWarPenalty(
    val teamId: String,
    val amount: Int
) {

    constructor(penalty: WarPenalty) : this(
        teamId = penalty.teamId,
        amount = penalty.amount
    )
    constructor(proto: WarPenaltyProto) : this(
        teamId = proto.teamId,
        amount = proto.amount
    )

    val proto: WarPenaltyProto
        get()  {
            val builder = WarPenaltyProto.newBuilder()
                .setTeamId(teamId)
                .setAmount(amount)
            return builder.build()
        }
}