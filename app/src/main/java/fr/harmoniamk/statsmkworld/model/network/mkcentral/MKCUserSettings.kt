package fr.harmoniamk.statsmkworld.model.network.mkcentral

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import fr.harmoniamk.statsmkworld.debug.MKCUserSettingsProto


@JsonClass(generateAdapter = true)
data class MKCUserSettings (
    @field:Json(name = "user_id") val userID: Long,
    @field:Json(name = "avatar") val avatar: String?,
    @field:Json(name = "about_me") val aboutMe: String?,
    @field:Json(name = "language") val language: String,
    @field:Json(name = "color_scheme") val colorScheme: String,
    @field:Json(name = "timezone") val timezone: String,
) {
    constructor(proto: MKCUserSettingsProto) : this(
        userID = proto.userId,
        avatar = proto.avatar,
        aboutMe = proto.aboutMe,
        language = proto.language,
        colorScheme = proto.colorScheme,
        timezone = proto.timezone,
    )

    val proto: MKCUserSettingsProto
        get() {
            val builder = MKCUserSettingsProto.newBuilder()
                .setUserId(userID)
                .setAvatar(avatar)
                .setLanguage(language)
                .setColorScheme(colorScheme)
                .setTimezone(timezone)

            aboutMe?.let {
                builder.setAboutMe(it)
            }
            return builder.build()
        }
}
