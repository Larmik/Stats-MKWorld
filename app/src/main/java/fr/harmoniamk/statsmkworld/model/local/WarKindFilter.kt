package fr.harmoniamk.statsmkworld.model.local

import java.io.Serializable

/**
 * Filtre Amicaux / Officiels (#103), les deux cochés par défaut. Non mémorisé : chaque écran
 * racine repart du défaut, un écran enfant le reçoit par sa route ([routeSegment]).
 * Invariant : au moins une case cochée (les toggles refusent de décocher la dernière).
 */
data class WarKindFilter(val friendly: Boolean = true, val official: Boolean = true) : Serializable {

    fun toggleFriendly(): WarKindFilter = when {
        friendly && !official -> this
        else -> copy(friendly = !friendly)
    }

    fun toggleOfficial(): WarKindFilter = when {
        official && !friendly -> this
        else -> copy(official = !official)
    }

    /** Segment de route (`all` / `friendly` / `official`), relu par [fromRouteSegment]. */
    val routeSegment: String
        get() = when {
            !official -> FRIENDLY_SEGMENT
            !friendly -> OFFICIAL_SEGMENT
            else -> ALL_SEGMENT
        }

    companion object {
        private const val ALL_SEGMENT = "all"
        private const val FRIENDLY_SEGMENT = "friendly"
        private const val OFFICIAL_SEGMENT = "official"

        /** Segment absent ou inconnu → les deux cochés. */
        fun fromRouteSegment(segment: String?): WarKindFilter = when (segment) {
            FRIENDLY_SEGMENT -> WarKindFilter(official = false)
            OFFICIAL_SEGMENT -> WarKindFilter(friendly = false)
            else -> WarKindFilter()
        }
    }
}
