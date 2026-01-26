package fr.harmoniamk.statsmkworld.model.firebase

import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import fr.harmoniamk.statsmkworld.model.local.DatastoreWar

data class War(
    val id: Long,
    val teamHost: String,
    val teamOpponent: List<String>,
    val tracks: List<WarTrack>,
    val penalties: List<WarPenalty>,
    val scores: List<WarScore>
) {

    @Deprecated("24 players")
    constructor(war: DatastoreWar) : this(
        id = war.id,
        teamHost = war.teamHost,
        teamOpponent = war.teamOpponent,
        tracks = war.tracks,
        penalties = war.penalties,
        scores = war.scores
    )

    constructor(entity: WarEntity): this(
        id = entity.id.toLong(),
        teamHost = entity.teamHost.orEmpty(),
        teamOpponent = entity.teamOpponent,
        tracks = entity.warTracks.orEmpty(),
        penalties = entity.penalties.orEmpty(),
        scores = entity.scores.orEmpty()
    )

    fun hasPlayer(playerId: String?): Boolean {
        return tracks.size == tracks.filter { it.positions.any { pos -> pos.playerId == playerId } }.size
    }
    fun hasTeam(teamId: String?): Boolean {
        return teamHost == teamId || teamOpponent.contains(teamId)
    }

}