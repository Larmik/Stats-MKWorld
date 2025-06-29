package fr.harmoniamk.statsmkworld.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.harmoniamk.statsmkworld.extension.displayedString
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.firebase.WarPenalty
import fr.harmoniamk.statsmkworld.model.firebase.WarTrack
import java.util.Date

@Entity
data class WarEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "teamHost") val teamHost: String?,
    @ColumnInfo(name = "teamOpponent") val teamOpponent: String?,
    @ColumnInfo(name = "createdDate") val createdDate: String?,
    @ColumnInfo(name = "warTracks") val warTracks: List<WarTrack>?,
    @ColumnInfo(name = "penalties") val penalties: List<WarPenalty>?,
) {
    constructor(war: War): this(
        id = war.id.toString(),
        teamHost = war.teamHost,
        teamOpponent = war.teamOpponent,
        createdDate = Date(war.id).displayedString("dd/MM/yyyy"),
        warTracks = war.tracks,
        penalties = war.penalties
    )
}