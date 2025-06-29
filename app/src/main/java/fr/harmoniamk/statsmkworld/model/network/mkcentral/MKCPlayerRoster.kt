package fr.harmoniamk.statsmkworld.model.network.mkcentral

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import fr.harmoniamk.statsmkworld.debug.MKCRosterProto

@JsonClass(generateAdapter = true)
data class MKCPlayerRoster (
    @field:Json(name = "roster_id") val rosterID: Long,
    @field:Json(name = "join_date") val joinDate: Long,
    @field:Json(name = "team_id") val teamID: Long,
    @field:Json(name = "team_name") val teamName: String,
    @field:Json(name = "team_tag") val teamTag: String,
    @field:Json(name = "team_color") val teamColor: Long,
    @field:Json(name = "roster_name") val rosterName: String,
    @field:Json(name = "roster_tag") val rosterTag: String,
    @field:Json(name = "game") val game: String,
    @field:Json(name = "mode") val mode: String,
) {
    constructor(proto: MKCRosterProto) : this(
       rosterID = proto.rosterId,
        joinDate = proto.joinDate,
        teamID = proto.teamId,
        teamName = proto.teamName,
        teamTag = proto.teamTag,
        teamColor = proto.teamColor,
        rosterName = proto.rosterName,
        rosterTag = proto.rosterTag,
        game = proto.game,
        mode = proto.mode,
    )

    val proto: MKCRosterProto
        get() = MKCRosterProto.newBuilder()
            .setRosterId(rosterID)
            .setJoinDate(joinDate)
            .setTeamId(teamID)
            .setTeamName(teamName)
            .setTeamTag(teamTag)
            .setTeamColor(teamColor)
            .setRosterName(rosterName)
            .setRosterTag(rosterTag)
            .setGame(game)
            .setMode(mode)
            .build()
}
