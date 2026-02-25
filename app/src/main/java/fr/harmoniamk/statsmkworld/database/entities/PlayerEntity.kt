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
    @ColumnInfo(name = "rosterId") val rosterId: String,
    @ColumnInfo(name = "discordId") val discordId: String,
) {
    constructor(player: MKCPlayer, role: Int = 0, isAlly: Boolean) : this(
        id = player.id.toString(),
        name = player.name,
        country = player.countryCode,
        role = role,
        currentWar = "",
        rosterId = when (isAlly) {
            true -> "-1"
            else -> player.rosters?.firstOrNull { it.game == "mkworld" }?.rosterID?.toString().orEmpty()
        },
        discordId = player.discord?.discordID.orEmpty()
    )

    constructor(player: MKCTeamPlayer, role: Int = 0, currentWar: String = "", rosterId: String, discordId: String = "") : this(
        id = player.playerId,
        name = player.name,
        country = player.countryCode,
        role = when (player.leader || player.manager) {
            true -> 2
            else -> role
        },
        currentWar = currentWar,
        rosterId = rosterId,
        discordId = discordId
    )
}