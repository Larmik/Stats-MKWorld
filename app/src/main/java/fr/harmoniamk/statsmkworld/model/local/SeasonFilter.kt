package fr.harmoniamk.statsmkworld.model.local

import fr.harmoniamk.statsmkworld.database.entities.SeasonEntity

/**
 * Sélection de saison (#70) partagée par Accueil / Wars / Stats / Classements. [Default] = saison
 * en cours (numéro inconnu avant chargement) ; [AllTime] = tout l'historique ; [Specific] = passée.
 */
sealed interface SeasonFilter {
    data object Default : SeasonFilter
    data object AllTime : SeasonFilter
    data class Specific(val number: Int) : SeasonFilter

    /** Saison effective parmi [seasons] ; `null` = tout l'historique. */
    fun resolve(seasons: List<SeasonEntity>): SeasonEntity? = when (this) {
        is AllTime -> null
        is Specific -> seasons.firstOrNull { it.number == number }
        is Default -> seasons.lastOrNull { it.end == null }
    }

    companion object {
        /** Choix de l'UI : `number` null = tout l'historique. */
        fun of(number: Int?): SeasonFilter = number?.let { Specific(it) } ?: AllTime
    }
}
