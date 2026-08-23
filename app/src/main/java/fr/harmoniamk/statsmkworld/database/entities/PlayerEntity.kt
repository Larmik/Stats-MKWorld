package fr.harmoniamk.statsmkworld.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayer
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeamPlayer

@Entity
data class PlayerEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "country") val country: String,
    //si is_manager ou is_leader -> role = 2
    @ColumnInfo(name = "role") val role: Int,
    @ColumnInfo(name = "currentWar") val currentWar: String,
    @ColumnInfo(name = "rosterId") val rosterId: String,
    @ColumnInfo(name = "discordId") val discordId: String,
    // Chemin RELATIF de la photo de profil MKCentral (userSettings.avatar), à préfixer
    // par https://mkcentral.com à l'affichage (cf. médaillon UI). Null si indisponible :
    // l'endpoint LISTE d'équipe (MKCTeamPlayer) ne fournit PAS l'avatar des membres —
    // seuls le joueur courant (DataStore) et les alliés (fetchés en MKCPlayer) l'ont.
    @ColumnInfo(name = "avatar") val avatar: String? = null,
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
        discordId = player.discord?.discordID.orEmpty(),
        avatar = player.userSettings?.avatar?.takeIf { it.isNotEmpty() }
    )

    constructor(player: MKCTeamPlayer, role: Int = 0, currentWar: String = "", rosterId: String, discordId: String = "", avatar: String? = null) : this(
        id = player.playerId,
        name = player.name,
        country = player.countryCode,
        role = when (player.leader || player.manager) {
            true -> 2
            else -> role
        },
        currentWar = currentWar,
        rosterId = rosterId,
        discordId = discordId,
        avatar = avatar
    )
}