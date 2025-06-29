package fr.harmoniamk.statsmkworld.model.network.mkcentral

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import fr.harmoniamk.statsmkworld.debug.MKCPlayerProto

@JsonClass(generateAdapter = true)
data class MKCPlayer (
    @field:Json(name = "id") val id: Long,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "country_code") val countryCode: String,
    @field:Json(name = "join_date") val joinDate: Long,
    @field:Json(name = "discord") val discord: MKCDiscordInfo?,
    @field:Json(name = "friend_codes") val friendCodes: List<MKCFriendCode>?,
    @field:Json(name = "rosters") val rosters: List<MKCPlayerRoster>?,
    @field:Json(name = "user_settings") val userSettings: MKCUserSettings?,
) {

    constructor(proto: MKCPlayerProto) : this(
        id = proto.id,
        name = proto.name,
        countryCode = proto.countryCode,
        joinDate = proto.joinDate,
        discord = MKCDiscordInfo(proto.discord),
        friendCodes = proto.friendCodesList.map { MKCFriendCode(it) },
        rosters = proto.rostersList.map { MKCPlayerRoster(it) },
        userSettings = MKCUserSettings(proto.userSettings)
    )

    val proto: MKCPlayerProto
        get()  {
            val builder = MKCPlayerProto.newBuilder()
                .setId(id)
                .setName(name)
                .setCountryCode(countryCode)
                .setJoinDate(joinDate)

            discord?.proto?.let {
                builder.setDiscord(it)
            }
            userSettings?.proto?.let {
                builder.setUserSettings(it)
            }
            friendCodes?.forEach {
                builder.addFriendCodes(it.proto)
            }
            rosters?.forEach {
                builder.addRosters(it.proto)
            }
            return builder.build()
        }
}





