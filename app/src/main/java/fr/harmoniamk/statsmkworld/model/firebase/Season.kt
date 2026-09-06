package fr.harmoniamk.statsmkworld.model.firebase

import android.os.Parcelable
import fr.harmoniamk.statsmkworld.database.entities.SeasonEntity
import kotlinx.parcelize.Parcelize
import java.io.Serializable

/**
 * Saison d'équipe (source de vérité RTDB `seasons/{teamId}`, cachée en Room via
 * [SeasonEntity]). Le nœud est un tableau indexé d'objets en ordre chronologique — la
 * dernière = la plus récente. [start]/[end] en epoch millis ; [end] `null` = en cours.
 */
@Parcelize
data class Season(
    val number: Int,
    val start: Long,
    val end: Long? = null
) : Serializable, Parcelable {

    constructor(entity: SeasonEntity) : this(
        number = entity.number,
        start = entity.start,
        end = entity.end
    )
}
