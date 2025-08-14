package fr.harmoniamk.statsmkworld.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayer
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeamPlayer

@Entity
class PlayerEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "country") val country: String,
    //si is_manager ou is_leader -> role = 2
    @ColumnInfo(name = "role") val role: Int,
    @ColumnInfo(name = "currentWar") val currentWar: String,
    @ColumnInfo(name = "isAlly") val isAlly: Boolean,
) {
    constructor(player: MKCPlayer, role: Int = 0, isAlly: Boolean = false) : this(
        id = player.id.toString(),
        name = player.name,
        country = player.countryCode,
        role = role,
        currentWar = "",
        isAlly = isAlly
    )

    constructor(player: MKCTeamPlayer, role: Int = 0, currentWar: String = "", isAlly: Boolean = false) : this(
        id = player.playerId,
        name = player.name,
        country = player.countryCode,
        role = role,
        currentWar = currentWar,
        isAlly = isAlly
    )
}