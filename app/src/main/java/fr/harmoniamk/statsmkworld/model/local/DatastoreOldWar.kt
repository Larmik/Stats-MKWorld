package fr.harmoniamk.statsmkworld.model.local

import fr.harmoniamk.statsmkworld.debug.OldWarProto
import fr.harmoniamk.statsmkworld.model.firebase.OldWar
import fr.harmoniamk.statsmkworld.model.firebase.WarPenalty
import fr.harmoniamk.statsmkworld.model.firebase.OldWarTrack

@Deprecated("24 players")
data class DatastoreOldWar(
    val id: Long,
    val teamHost: String,
    val teamOpponent: String,
    val tracks: List<OldWarTrack>,
    val penalties: List<WarPenalty>
) {
    var name: String? = null

    @Deprecated("24 players")
    constructor(war: OldWar) : this(
        id = war.id,
        teamHost = war.teamHost,
        teamOpponent = war.teamOpponent,
        tracks = war.tracks,
        penalties = war.penalties
    )

    @Deprecated("24 players")
    constructor(proto: OldWarProto) : this(
        id = proto.id,
        teamHost = proto.teamHost,
        teamOpponent = proto.teamOpponent,
        tracks = proto.tracksList
            .map { DatastoreOldWarTrack(it) }
            .map { OldWarTrack(it) },
        penalties = proto.penaltiesList
            .map { DatastoreWarPenalty(it) }
            .map { WarPenalty(it) }
    )

    @Deprecated("24 players")
    val proto: OldWarProto
        get()  {
            val builder = OldWarProto.newBuilder()
                .setId(id)
                .setTeamHost(teamHost)
                .setTeamOpponent(teamOpponent)
            tracks.forEach {
                builder.addTracks(DatastoreOldWarTrack(it).proto)
            }
            penalties.forEach {
                builder.addPenalties(DatastoreWarPenalty(it).proto)
            }
            return builder.build()
        }
}