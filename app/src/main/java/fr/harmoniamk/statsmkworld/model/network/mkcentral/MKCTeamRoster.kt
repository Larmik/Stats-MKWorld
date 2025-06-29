package fr.harmoniamk.statsmkworld.model.network.mkcentral

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import fr.harmoniamk.statsmkworld.debug.MKCTeamProto
import fr.harmoniamk.statsmkworld.debug.MKCTeamRosterProto

@JsonClass(generateAdapter = true)
data class MKCTeamRoster (
    @field:Json(name = "id") val id: Long,
    @field:Json(name = "team_id") val teamId: Long,
    @field:Json(name = "creation_date") val creationDate: Long,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "tag") val tag: String,
    @field:Json(name = "color") val color: Long,
    @field:Json(name = "game") val game: String,
    @field:Json(name = "mode") val mode: String,
    @field:Json(name = "players") val players: List<MKCTeamPlayer>,

) {
    constructor(proto: MKCTeamRosterProto) : this(
        id = proto.id,
        teamId = proto.teamId,
        creationDate = proto.creationDate,
        name = proto.name,
        tag = proto.tag,
        color = proto.color.toLong(),
        game = proto.game,
        mode = proto.mode,
        players = proto.playersList.map {  MKCTeamPlayer(it)},
    )

    val proto: MKCTeamRosterProto
        get()  {
            val builder = MKCTeamRosterProto.newBuilder()
                .setId(id)
                .setTeamId(teamId)
                .setName(name)
                .setTag(tag)
                .setCreationDate(creationDate)
                .setColor(color.toInt())
                .setGame(game)
                .setMode(mode)

            players.forEach {
                builder.addPlayers(it.proto)
            }

            return builder.build()
        }
}