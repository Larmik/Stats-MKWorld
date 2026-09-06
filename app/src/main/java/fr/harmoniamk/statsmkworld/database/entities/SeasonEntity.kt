package fr.harmoniamk.statsmkworld.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import fr.harmoniamk.statsmkworld.model.firebase.Season

/**
 * Cache local (Room) d'une saison, miroir de [Season] (RTDB `seasons/{teamId}`). Clé
 * composite `teamId + number` ; [end] `null` = saison en cours. Ré-hydratée depuis RTDB
 * (fallbackToDestructiveMigration, pas de migration).
 */
@Entity(primaryKeys = ["teamId", "number"])
data class SeasonEntity(
    @ColumnInfo(name = "teamId") val teamId: String,
    @ColumnInfo(name = "number") val number: Int,
    @ColumnInfo(name = "start") val start: Long,
    @ColumnInfo(name = "end") val end: Long?
) {
    constructor(teamId: String, season: Season) : this(
        teamId = teamId,
        number = season.number,
        start = season.start,
        end = season.end
    )
}
