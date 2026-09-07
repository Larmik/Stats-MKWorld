package fr.harmoniamk.statsmkworld.model.local

import com.squareup.moshi.JsonClass
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeamRoster

/**
 * Métadonnées locales d'un roster mkworld, stockées sur
 * [fr.harmoniamk.statsmkworld.database.entities.TeamEntity.rosters]. Résout un `rosterId`
 * (`War.teamOpponent`/`teamHost`) → nom/tag du roster pour l'affichage (avatar hérité de
 * l'équipe parente). Alimenté au fetch depuis [MKCTeamRoster], sans appel réseau.
 */
@JsonClass(generateAdapter = true)
data class RosterInfo(
    val id: String,
    val name: String,
    val tag: String,
) {
    constructor(roster: MKCTeamRoster) : this(
        id = roster.id.toString(),
        name = roster.name,
        tag = roster.tag,
    )
}
