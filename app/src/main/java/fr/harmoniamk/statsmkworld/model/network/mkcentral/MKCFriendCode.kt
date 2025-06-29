package fr.harmoniamk.statsmkworld.model.network.mkcentral

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import fr.harmoniamk.statsmkworld.debug.MKCDiscordInfoProto
import fr.harmoniamk.statsmkworld.debug.MKCFriendCodeProto

@JsonClass(generateAdapter = true)
data class MKCFriendCode (
    @field:Json(name = "id") val id: Long,
    @field:Json(name = "fc") val fc: String,
    @field:Json(name = "type") val type: String,
    @field:Json(name = "player_id") val playerID: Long,
    @field:Json(name = "is_verified") val isVerified: Boolean,
    @field:Json(name = "is_primary") val isPrimary: Boolean,
    @field:Json(name = "creation_date") val creationDate: Long,
    @field:Json(name = "description") val description: String?,
    @field:Json(name = "is_active") val isActive: Boolean
) {
    constructor(proto: MKCFriendCodeProto) : this(
        id = proto.id,
        fc = proto.fc,
        type = proto.type,
        playerID = proto.playerId,
        isVerified = proto.isVerified,
        isPrimary = proto.isPrimary,
        creationDate = proto.creationDate,
        description = proto.description,
        isActive = proto.isActive
    )

    val proto: MKCFriendCodeProto
        get() = MKCFriendCodeProto.newBuilder()
            .setId(id)
            .setFc(fc)
            .setType(type)
            .setPlayerId(playerID)
            .setIsVerified(isVerified)
            .setIsPrimary(isPrimary)
            .setIsActive(isActive)
            .setCreationDate(creationDate)
            .setDescription(description.orEmpty())
            .build()
}