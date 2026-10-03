package fr.harmoniamk.statsmkworld.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.harmoniamk.statsmkworld.model.local.Tournament
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTournament

/**
 * Cache local (Room) de la dernière saison MKCentral d'un tournoi officiel (#152). Clé = [Tournament.name]
 * (même valeur que `War.tournamentId`), pas l'id MKCentral qui change à chaque saison. Textes en
 * Markdown (anglais), dates en epoch s. Pas de logo : celui embarqué ([Tournament.logo]) suffit.
 */
@Entity
data class TournamentEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "mkcTournamentId") val mkcTournamentId: Int,
    @ColumnInfo(name = "seasonName") val seasonName: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "ruleset") val ruleset: String,
    @ColumnInfo(name = "dateStart") val dateStart: Long?,
    @ColumnInfo(name = "dateEnd") val dateEnd: Long?,
    @ColumnInfo(name = "organizer") val organizer: String?,
    @ColumnInfo(name = "mode") val mode: String?,
    // Traductions ML Kit (Markdown préservé) dans [translationLanguage] ; null = pas de traduction.
    @ColumnInfo(name = "descriptionTranslated") val descriptionTranslated: String? = null,
    @ColumnInfo(name = "rulesetTranslated") val rulesetTranslated: String? = null,
    @ColumnInfo(name = "translationLanguage") val translationLanguage: String? = null
) {
    // Textes affichés selon la logique du front MKCentral (`use_series_*`).
    constructor(tournament: Tournament, season: MKCTournament) : this(
        id = tournament.name,
        mkcTournamentId = season.id,
        seasonName = season.name.trim(),
        description = (if (season.useSeriesDescription) season.seriesDescription else season.description).orEmpty(),
        ruleset = (if (season.useSeriesRuleset) season.seriesRuleset else season.ruleset).orEmpty(),
        dateStart = season.dateStart,
        dateEnd = season.dateEnd,
        organizer = season.organizer,
        mode = season.mode
    )
}
