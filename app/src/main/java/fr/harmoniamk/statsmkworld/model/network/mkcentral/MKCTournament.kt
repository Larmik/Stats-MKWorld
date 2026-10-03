package fr.harmoniamk.statsmkworld.model.network.mkcentral

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Tournoi MKCentral (une saison), `tournaments/{id}` ; l'endpoint liste n'en porte qu'un
 * sous-ensemble (pas de textes), d'où les champs nullables. Textes en Markdown, dates en epoch s,
 * [logo] chemin relatif (déjà celui de la série si `use_series_logo`).
 */
@JsonClass(generateAdapter = true)
data class MKCTournament(
    @field:Json(name = "id") val id: Int,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "mode") val mode: String? = null,
    @field:Json(name = "organizer") val organizer: String? = null,
    @field:Json(name = "logo") val logo: String? = null,
    @field:Json(name = "date_start") val dateStart: Long? = null,
    @field:Json(name = "date_end") val dateEnd: Long? = null,
    @field:Json(name = "description") val description: String? = null,
    @field:Json(name = "ruleset") val ruleset: String? = null,
    @field:Json(name = "series_description") val seriesDescription: String? = null,
    @field:Json(name = "series_ruleset") val seriesRuleset: String? = null,
    @field:Json(name = "use_series_description") val useSeriesDescription: Boolean = false,
    @field:Json(name = "use_series_ruleset") val useSeriesRuleset: Boolean = false
)
