package fr.harmoniamk.statsmkworld.model.local

import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.model.firebase.War
import java.time.ZonedDateTime

/**
 * War amicale candidate à la migration rétroactive vers un tournoi officiel (#156), produite par
 * `DiagnosticRepository.findOfficialWarCandidates` (debug, lecture seule).
 */
data class OfficialWarCandidate(
    /** Nœud hôte Firebase `wars/{hostRosterId}` de la war. */
    val hostRosterId: String,
    /** War telle que lue sur Firebase (aucune conversion Room : tous les champs préservés). */
    val war: War,
    val tournament: Tournament,
    /** Création de la war (`War.id` = epoch ms) en heure de Paris. */
    val createdAt: ZonedDateTime,
    /** Adversaires résolus via `War.opponentTeams` (nom/tag du roster, dégradé si inconnu — rule 12). */
    val opponents: List<TeamEntity>,
)
