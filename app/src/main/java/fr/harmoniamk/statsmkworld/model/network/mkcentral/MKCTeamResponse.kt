package fr.harmoniamk.statsmkworld.model.network.mkcentral

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

typealias MKCTeamList = List<MKCTeam>

@JsonClass(generateAdapter = true)
data class MKCTeamResponse(
    @field:Json(name = "teams") val teamList: MKCTeamList,
    @field:Json(name = "page_count") val pageCount: Int
)