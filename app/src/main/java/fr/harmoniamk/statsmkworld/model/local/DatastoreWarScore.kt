package fr.harmoniamk.statsmkworld.model.local

import fr.harmoniamk.statsmkworld.debug.WarScoreProto
import fr.harmoniamk.statsmkworld.model.firebase.WarScore

data class DatastoreWarScore(
    val teamId: String,
    val score: Int
) {

    constructor(score: WarScore) : this(
        teamId = score.teamId,
        score = score.score
    )
    constructor(proto: WarScoreProto) : this(
        teamId = proto.teamId,
        score = proto.score
    )

    val proto: WarScoreProto
        get()  {
            val builder = WarScoreProto.newBuilder()
                .setTeamId(teamId)
                .setScore(score)
            return builder.build()
        }
}