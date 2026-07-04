package fr.harmoniamk.statsmkworld.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.harmoniamk.statsmkworld.model.local.RosterInfo
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeam
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeamRoster

@Entity
data class TeamEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "tag") val tag: String,
    @ColumnInfo(name = "color") val color: Int?,
    @ColumnInfo(name = "logo") val logo: String?,
    // Métadonnées {id, nom, tag} des rosters mkworld de cette équipe. La clé
    // primaire reste le teamId ; cette liste permet de résoudre un rosterId
    // (contenu de War.teamOpponent/teamHost depuis le passage à la granularité
    // roster) vers le nom/tag du roster (avatar hérité de l'équipe parente) pour
    // l'affichage, et vers l'équipe parente pour le regroupement des stats.
    @ColumnInfo(name = "rosters") val rosters: List<RosterInfo> = listOf(),
) {
    constructor(team: MKCTeam): this(
        id = team.id.toString(),
        name = team.name,
        tag = team.tag,
        color = team.color.toInt(),
        logo = team.logo,
        rosters = team.rosters.filter { it.game == "mkworld" }.map { RosterInfo(it) }
    )

    constructor(roster: MKCTeamRoster): this(
        id = roster.teamId.toString(),
        name = roster.name,
        tag = roster.tag,
        color = roster.color.toInt(),
        logo = null
    )
}