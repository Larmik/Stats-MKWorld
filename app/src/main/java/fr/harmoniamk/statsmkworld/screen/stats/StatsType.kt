package fr.harmoniamk.statsmkworld.screen.stats

import fr.harmoniamk.statsmkworld.R
import java.io.Serializable

/**
 * Véhicule de routage des statistiques : décrit la PORTÉE d'une fiche de stats
 * demandée depuis les Classements ([fr.harmoniamk.statsmkworld.screen.stats.ranking.StatsRankingScreen]).
 *
 * Émis par [fr.harmoniamk.statsmkworld.screen.stats.ranking.StatsRankingScreen] via le
 * callback `onStats`, puis routé par [fr.harmoniamk.statsmkworld.screen.RootScreen] vers
 * l'écran dédié correspondant (`Statsfull/{userId}`, `Opponent/…`, `Map/…`).
 *
 * `Serializable` car transporté via `savedStateHandle` de la navigation Compose.
 *
 * Note (#51) : l'ancien membre `TeamStats` et l'écran générique `StatsScreen`/`StatsViewModel`
 * ont été supprimés (aucun émetteur ne les atteignait plus, la fiche joueur passant
 * désormais par [fr.harmoniamk.statsmkworld.screen.stats.full.StatsFullScreen]). Seules les
 * 3 portées réellement émises subsistent.
 *
 * @property title Ressource de libellé de la portée (titre de la fiche).
 * @property is24PEnabled `true` si la portée cible les wars 24 joueurs (3 adversaires),
 *   `false` pour le mode 12 joueurs (1v1). Sert au filtrage des wars par mode.
 */
sealed class StatsType(val title: Int, val is24PEnabled: Boolean) : Serializable {

    /**
     * Statistiques individuelles d'un joueur donné.
     * @property userId Identifiant MKCentral du joueur ciblé.
     * @property is24p Portée 24 joueurs si `true`.
     */
    class PlayerStats(val userId: String, val is24p: Boolean) :
        StatsType(R.string.statistiques_du_joueur, is24p)

    /**
     * Statistiques face à un adversaire donné.
     * @property teamId Identifiant de l'opposant (rosterId, ou teamId legacy).
     * @property userId Optionnel : restreint au point de vue de ce joueur (`null` = équipe).
     * @property is24p Portée 24 joueurs si `true`.
     * @property seasonNumber Saison à l'origine de la navigation (#91 pt.5) : `null` = tout
     *   l'historique. Propagée à la fiche détail pour la filtrer comme le classement d'origine.
     */
    class OpponentStats(
        val teamId: String,
        val userId: String? = null,
        val is24p: Boolean,
        val seasonNumber: Int? = null
    ) : StatsType(R.string.statistiques_de_l_adversaire, is24p)

    /**
     * Statistiques sur un ou plusieurs circuits.
     * @property userId Optionnel : point de vue d'un joueur (`null` = équipe).
     * @property teamId Optionnel : restreint à un adversaire.
     * @property trackIndex Index(es) de circuit ([fr.harmoniamk.statsmkworld.model.local.Maps]).
     * @property is24p Portée 24 joueurs si `true`.
     * @property seasonNumber Saison à l'origine de la navigation (#91 pt.5) : `null` = tout l'historique.
     */
    class MapStats(
        val userId: String? = null,
        val teamId: String? = null,
        val trackIndex: List<Int>? = null,
        val is24p: Boolean,
        val seasonNumber: Int? = null
    ) : StatsType(R.string.statistiques_du_circuit, is24p)
}
