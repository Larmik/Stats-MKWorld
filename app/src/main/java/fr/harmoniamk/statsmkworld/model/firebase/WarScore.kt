package fr.harmoniamk.statsmkworld.model.firebase

import fr.harmoniamk.statsmkworld.model.local.DatastoreWarScore
import java.io.Serializable

data class WarScore(var teamId: String, var score: Int): Serializable {
    constructor(score: DatastoreWarScore) : this(
        teamId = score.teamId,
        score = score.score
    )
}