package fr.harmoniamk.statsmkworld.model.local

import fr.harmoniamk.statsmkworld.debug.WarProto
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.firebase.WarPenalty
import fr.harmoniamk.statsmkworld.model.firebase.WarTrack

data class DatastoreWar(
    val id: Long,
    val teamHost: String,
    val teamOpponent: String,
    val tracks: List<WarTrack>,
    val penalties: List<WarPenalty>
) {
    var name: String? = null

    constructor(war: War) : this(
        id = war.id,
        teamHost = war.teamHost,
        teamOpponent = war.teamOpponent,
        tracks = war.tracks,
        penalties = war.penalties
    )

    constructor(proto: WarProto) : this(
        id = proto.id,
        teamHost = proto.teamHost,
        teamOpponent = proto.teamOpponent,
        tracks = proto.tracksList
            .map { DatastoreWarTrack(it) }
            .map { WarTrack(it) },
        penalties = proto.penaltiesList
            .map { DatastoreWarPenalty(it) }
            .map { WarPenalty(it) }

    )

    val proto: WarProto
        get()  {
            val builder = WarProto.newBuilder()
                .setId(id)
                .setTeamHost(teamHost)
                .setTeamOpponent(teamOpponent)

            tracks.forEach {
                builder.addTracks(DatastoreWarTrack(it).proto)
            }
            penalties.forEach {
                builder.addPenalties(DatastoreWarPenalty(it).proto)
            }

            return builder.build()
        }
}