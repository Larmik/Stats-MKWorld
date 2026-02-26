package fr.harmoniamk.statsmkworld.model.firebase

import android.os.Parcelable
import fr.harmoniamk.statsmkworld.model.local.DatastoreWarPenalty
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class WarPenalty(val teamId: String, val amount: Int): Serializable, Parcelable {

    constructor(penalty: DatastoreWarPenalty) : this(
        teamId = penalty.teamId,
        amount = penalty.amount
    )
}