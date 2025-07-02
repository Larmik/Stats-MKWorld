package fr.harmoniamk.statsmkworld.model.firebase

import fr.harmoniamk.statsmkworld.model.local.DatastoreWarPenalty

data class WarPenalty(val teamId: String, val amount: Int) {

    constructor(penalty: DatastoreWarPenalty) : this(
        teamId = penalty.teamId,
        amount = penalty.amount
    )
}