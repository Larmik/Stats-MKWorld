package fr.harmoniamk.statsmkworld.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.harmoniamk.statsmkworld.extension.displayedString
import fr.harmoniamk.statsmkworld.model.firebase.OldWar
import fr.harmoniamk.statsmkworld.model.firebase.WarPenalty
import fr.harmoniamk.statsmkworld.model.firebase.OldWarTrack
import java.util.Date

@Entity
@Deprecated("24 players")
data class OldWarEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "teamHost") val teamHost: String?,
    @ColumnInfo(name = "teamOpponent") val teamOpponent: String?,
    @ColumnInfo(name = "createdDate") val createdDate: String?,
    @ColumnInfo(name = "warTracks") val warTracks: List<OldWarTrack>?,
    @ColumnInfo(name = "penalties") val penalties: List<WarPenalty>?,
) {
    @Deprecated("24 players")
    constructor(war: OldWar): this(
        id = war.id.toString(),
        teamHost = war.teamHost,
        teamOpponent = war.teamOpponent,
        createdDate = Date(war.id).displayedString("dd/MM/yyyy"),
        warTracks = war.tracks,
        penalties = war.penalties
    )

    fun hasPlayer(playerId: String?): Boolean {
        return warTracks?.size == warTracks?.filter { it.positions.any { pos -> pos.playerId == playerId } }?.size
    }
    fun hasTeam(teamId: String?): Boolean {
        return teamHost == teamId || teamOpponent == teamId
    }
}