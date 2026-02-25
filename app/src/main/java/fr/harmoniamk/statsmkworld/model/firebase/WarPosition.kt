package fr.harmoniamk.statsmkworld.model.firebase

import android.os.Parcelable
import fr.harmoniamk.statsmkworld.model.local.DatastoreWarPosition
import kotlinx.parcelize.Parcelize

@Parcelize
data class WarPosition(
    val id: Long,
    val playerId: String,
    val position: Int
): Parcelable {
    constructor(position: DatastoreWarPosition) : this(
        id = position.id,
        playerId = position.playerId,
        position = position.position
    )
}