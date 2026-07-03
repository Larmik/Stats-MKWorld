package fr.harmoniamk.statsmkworld.model.local

import com.squareup.moshi.JsonClass
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeamRoster

/**
 * Métadonnées locales d'un roster mkworld d'une équipe, stockées sur
 * [fr.harmoniamk.statsmkworld.database.entities.TeamEntity.rosters].
 *
 * Permet de résoudre un `rosterId` (contenu de `War.teamOpponent`/`teamHost`
 * depuis le passage à la granularité roster) vers le **nom et le tag du roster**
 * pour l'affichage, tout en conservant l'avatar de l'équipe parente. Alimenté au
 * fetch / à la sélection (données déjà présentes dans [MKCTeamRoster]), sans
 * appel réseau supplémentaire.
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
