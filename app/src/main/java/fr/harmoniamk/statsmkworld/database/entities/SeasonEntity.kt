package fr.harmoniamk.statsmkworld.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import fr.harmoniamk.statsmkworld.model.firebase.Season

/**
 * Cache local (Room) d'une saison d'équipe. Miroir de [Season] (couche Firebase,
 * `seasons/{teamId}`). Clé primaire **composite** `teamId + number` : une saison est
 * unique pour une équipe donnée. [end] nullable → `null` = saison en cours.
 *
 * ⚠️ La DB est en `fallbackToDestructiveMigration()` : la table est ré-hydratée depuis
 * RTDB à la synchro, pas de migration.
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
