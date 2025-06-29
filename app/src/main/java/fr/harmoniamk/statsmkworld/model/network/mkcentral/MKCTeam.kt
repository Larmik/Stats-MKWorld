package fr.harmoniamk.statsmkworld.model.network.mkcentral

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import fr.harmoniamk.statsmkworld.debug.MKCTeamProto

@JsonClass(generateAdapter = true)
data class MKCTeam(
    val id: Long,
    val name: String,
    val tag: String,
    val description: String,
    @field:Json(name = "creation_date") val creationDate: Long,
    val language: String,
    val color: Long,
    val logo: String?,
    @field:Json(name = "approval_status") val approvalStatus: String,
    @field:Json(name = "rosters") val rosters: List<MKCTeamRoster> = listOf()
) {
    constructor(proto: MKCTeamProto) : this(
        id = proto.id,
        name = proto.name,
        tag = proto.tag,
        description = proto.description,
        creationDate = proto.creationDate,
        language = proto.language,
        color = proto.color,
        logo = proto.logo,
        approvalStatus = proto.approvalStatus,
        rosters = proto.rostersList.map { MKCTeamRoster(it) }

    )

    val proto: MKCTeamProto
        get()  {
            val builder = MKCTeamProto.newBuilder()
                .setId(id)
                .setName(name)
                .setTag(tag)
                .setDescription(description)
                .setCreationDate(creationDate)
                .setLanguage(language)
                .setColor(color)
                .setLogo(logo)
                .setApprovalStatus(approvalStatus)

            rosters.forEach {
                builder.addRosters(it.proto)
            }
            return builder.build()
        }
}

