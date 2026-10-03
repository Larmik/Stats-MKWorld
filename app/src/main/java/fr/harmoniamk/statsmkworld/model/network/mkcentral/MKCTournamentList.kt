package fr.harmoniamk.statsmkworld.model.network.mkcentral

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Page de `tournaments/list`, triée par `date_start` décroissant (1ʳᵉ entrée = dernière saison). */
@JsonClass(generateAdapter = true)
data class MKCTournamentList(
    @field:Json(name = "tournaments") val tournaments: List<MKCTournament>,
    @field:Json(name = "page_count") val pageCount: Int
)
