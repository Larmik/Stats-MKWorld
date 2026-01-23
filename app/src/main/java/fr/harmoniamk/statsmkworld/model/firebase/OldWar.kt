package fr.harmoniamk.statsmkworld.model.firebase

@Deprecated("24 players")
data class OldWar(
    val id: Long,
    val teamHost: String,
    val teamOpponent: String,
    val tracks: List<OldWarTrack>,
    val penalties: List<WarPenalty>
) {
    var name: String? = null



    fun hasPlayer(playerId: String?): Boolean {
        return tracks.size == tracks.filter { it.positions.any { pos -> pos.playerId == playerId } }.size
    }
    fun hasTeam(teamId: String?): Boolean {
        return teamHost == teamId || teamOpponent == teamId
    }

}