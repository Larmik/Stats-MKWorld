package fr.harmoniamk.statsmkworld.model.network.mkcentral

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

typealias MKCPlayerList = List<MKCPlayer>

@JsonClass(generateAdapter = true)
data class MKCPlayerResponse(
    @field:Json(name = "player_list") val playerList: MKCPlayerList,
    @field:Json(name = "page_count") val pageCount: Int
)