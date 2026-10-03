package fr.harmoniamk.statsmkworld.model.local

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import fr.harmoniamk.statsmkworld.R

/**
 * Répertoire des tournois officiels (#103), liste blanche commune à toutes les équipes : ajouter
 * un tournoi demande une release. `War.tournamentId` stocke le [name] (stable si l'ordre change).
 *
 * [seriesId] / [namePrefix] : clé de résolution MKCentral (série, ou préfixe de nom sans série) de la
 * dernière saison, lue par `TournamentRepository` (#152) ; [fallbackLogo] = repli du logo officiel.
 */
enum class Tournament(
    @StringRes val label: Int,
    @DrawableRes val fallbackLogo: Int,
    val seriesId: Int?,
    val namePrefix: String?
) {
    ATLAS_LEAGUE(R.string.tournament_atlas_league, R.drawable.tournament_atlas_league, seriesId = 65, namePrefix = null),
    LOW_DIV_CUP(R.string.tournament_low_div_cup, R.drawable.tournament_low_div_cup, seriesId = null, namePrefix = "Low Div Cup"),
    MKC_FRONTIER(R.string.tournament_mkc_frontier, R.drawable.tournament_mkc_frontier, seriesId = 12, namePrefix = null),
    EUROLEAGUE(R.string.tournament_euroleague, R.drawable.tournament_euroleague, seriesId = 27, namePrefix = null);

    companion object {
        /** Tournoi d'une war ; `null` si amicale ou id inconnu de cette version de l'app. */
        fun fromId(tournamentId: String?): Tournament? = entries.find { it.name == tournamentId }
    }
}
