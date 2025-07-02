package fr.harmoniamk.statsmkworld.model.network.mkcentral

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import fr.harmoniamk.statsmkworld.debug.MKCTeamPlayerProto

@JsonClass(generateAdapter = true)
data class MKCTeamPlayer(
    @field:Json(name = "player_id") val playerId: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "country_code") val countryCode: String,
    @field:Json(name = "is_manager") val manager: Boolean,
    @field:Json(name = "is_leader") val leader: Boolean
) {
    constructor(proto: MKCTeamPlayerProto) : this(
        playerId = proto.playerId.toString(),
        name = proto.name,
        countryCode = proto.countryCode,
        manager = proto.isManager,
        leader = proto.isLeader,
    )

    val proto: MKCTeamPlayerProto
        get()  {
            val builder = MKCTeamPlayerProto.newBuilder()
                .setPlayerId(playerId.toInt())
                .setName(name)
                .setCountryCode(countryCode)
                .setIsManager(manager)
                .setIsLeader(leader)
            return builder.build()
        }
}