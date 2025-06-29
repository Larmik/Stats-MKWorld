package fr.harmoniamk.statsmkworld.model.network.mkcentral

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import fr.harmoniamk.statsmkworld.debug.MKCDiscordInfoProto

@JsonClass(generateAdapter = true)
data class MKCDiscordInfo (
    @field:Json(name = "discord_id") val discordID: String,
    @field:Json(name = "username") val username: String,
    @field:Json(name = "discriminator") val discriminator: String,
    @field:Json(name = "global_name") val globalName: String?,
    @field:Json(name = "avatar") val avatar: String?
) {
    constructor(proto: MKCDiscordInfoProto) : this(
        discordID = proto.discordId,
        username = proto.username,
        discriminator = proto.discriminator,
        globalName = proto.globalName,
        avatar = proto.avatar
    )

    val proto: MKCDiscordInfoProto
        get() = MKCDiscordInfoProto.newBuilder()
            .setDiscordId(discordID)
            .setUsername(username)
            .setDiscriminator(discriminator)
            .setGlobalName(globalName)
            .setAvatar(avatar)
            .build()
}