package fr.harmoniamk.statsmkworld.model.firebase

import android.os.Parcelable
import fr.harmoniamk.statsmkworld.database.entities.SeasonEntity
import kotlinx.parcelize.Parcelize
import java.io.Serializable

/**
 * Saison d'équipe (source de vérité RTDB `seasons/{teamId}`, cachée en Room via
 * [SeasonEntity]). Le nœud `seasons/{teamId}` est un **tableau indexé** (`0,1,2…`)
 * d'objets `Season`, en ordre chronologique — la dernière = la plus récente.
 *
 * - [number] : numéro de saison (non null) ;
 * - [start]  : timestamp de début, epoch millis (non null) ;
 * - [end]    : timestamp de fin, epoch millis, **nullable** — `null` = saison en cours.
 *
 * Modèle « couche Firebase » (cf. `CLAUDE.md`, architecture 3 couches) : la conversion
 * vers/depuis Room passe par les constructeurs dédiés ([Season] ⇄ [SeasonEntity]).
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
