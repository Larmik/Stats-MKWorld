package fr.harmoniamk.statsmkworld.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeam

@Entity
class TeamEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "tag") val tag: String,
    @ColumnInfo(name = "color") val color: Int?,
    @ColumnInfo(name = "logo") val logo: String?,
) {
    constructor(team: MKCTeam): this(
        id = team.id.toString(),
        name = team.name,
        tag = team.tag,
        color = team.color.toInt(),
        logo = team.logo
    )
}