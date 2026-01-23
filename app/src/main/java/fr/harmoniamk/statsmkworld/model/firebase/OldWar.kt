package fr.harmoniamk.statsmkworld.model.firebase

import fr.harmoniamk.statsmkworld.database.entities.OldWarEntity
import fr.harmoniamk.statsmkworld.model.local.DatastoreOldWar

@Deprecated("24 players")
data class OldWar(
    val id: Long,
    val teamHost: String,
    val teamOpponent: String,
    val tracks: List<OldWarTrack>,
    val penalties: List<WarPenalty>
) {
    var name: String? = null

    @Deprecated("24 players")
    constructor(war: DatastoreOldWar) : this(
        id = war.id,
        teamHost = war.teamHost,
        teamOpponent = war.teamOpponent,
        tracks = war.tracks,
        penalties = war.penalties
    )

    constructor(entity: OldWarEntity): this(
        id = entity.id.toLong(),
        teamHost = entity.teamHost.orEmpty(),
        teamOpponent = entity.teamOpponent.orEmpty(),
        tracks = entity.warTracks.orEmpty(),
        penalties = entity.penalties.orEmpty()
    )

    fun hasPlayer(playerId: String?): Boolean {
        return tracks.size == tracks.filter { it.positions.any { pos -> pos.playerId == playerId } }.size
    }
    fun hasTeam(teamId: String?): Boolean {
        return teamHost == teamId || teamOpponent == teamId
    }

}