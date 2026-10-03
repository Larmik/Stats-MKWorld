package fr.harmoniamk.statsmkworld.model.firebase

import android.os.Parcelable
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import fr.harmoniamk.statsmkworld.model.local.DatastoreWar
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class War(
    val id: Long,
    val teamHost: String,
    val teamOpponent: List<String>,
    val tracks: List<WarTrack>,
    val penalties: List<WarPenalty>,
    val scores: List<WarScore>,
    /**
     * Id MKCentral du joueur créateur de la war. Vit uniquement sur Firebase
     * (nœud currentWars) et en mémoire : volontairement absent de war.proto
     * (DataStore) et de WarEntity (Room). Vaut 0L pour une war legacy ou pour
     * une war reconstruite depuis le DataStore / Room.
     */
    val playerHostId: Long = 0L,
    /** Tournoi officiel ([fr.harmoniamk.statsmkworld.model.local.Tournament.name]) ; `null` = war amicale (#103). Fixé à la création. */
    val tournamentId: String? = null
): Serializable, Parcelable {

    constructor(war: DatastoreWar) : this(
        id = war.id,
        teamHost = war.teamHost,
        teamOpponent = war.teamOpponent,
        tracks = war.tracks,
        penalties = war.penalties,
        scores = war.scores,
        tournamentId = war.tournamentId
    )

    constructor(entity: WarEntity): this(
        id = entity.id.toLong(),
        teamHost = entity.teamHost.orEmpty(),
        teamOpponent = entity.teamOpponent,
        tracks = entity.warTracks.orEmpty(),
        penalties = entity.penalties.orEmpty(),
        scores = entity.scores.orEmpty(),
        tournamentId = entity.tournamentId
    )

    fun hasPlayer(playerId: String?): Boolean {
        return tracks.size == tracks.filter { it.positions.any { pos -> pos.playerId == playerId } }.size
    }
    fun hasTeam(teamId: String?): Boolean {
        return teamHost == teamId || teamOpponent.contains(teamId)
    }

}