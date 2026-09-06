package fr.harmoniamk.statsmkworld.model.local

/**
 * Diagnostic (debug, lecture seule) d'une war dont un adversaire (`War.teamOpponent`) ne se
 * résout à aucune `TeamEntity` locale (affichée « Équipe inconnue »). Produit par
 * `DiagnosticRepository` : pour chaque id non résolu, tente une résolution MKCentral parmi
 * les équipes mkworld actives/non historiques/≥ 6 joueurs. Aide à arbitrer réattribution vs
 * suppression, war par war (décision humaine).
 */
data class UnknownOpponentDiagnostic(
    /** Nœud hôte Firebase (`wars/{hostRosterId}`) sous lequel la war est stockée. */
    val hostRosterId: String,
    /** `War.id` (timestamp de création). */
    val warId: Long,
    /** `War.teamHost` (rosterId hôte estampillé dans la war). */
    val teamHost: String,
    /** Date lisible dérivée de `War.id`. */
    val date: String,
    /** Score affiché (host - adversaire), 12p ou 24p selon le mode. */
    val displayedScore: String,
    /** Ids bruts d'adversaire NON résolus localement, avec leur résolution MKCentral. */
    val unresolvedOpponents: List<UnresolvedOpponent>,
)

/**
 * Un id d'adversaire brut (rosterId ou teamId legacy) non résolu localement, et
 * le résultat de sa résolution parmi les équipes mkworld actives 6+ joueurs.
 */
data class UnresolvedOpponent(
    /** Id brut stocké dans `War.teamOpponent`. */
    val rawId: String,
    /** Issue de la résolution. */
    val resolution: OpponentResolution,
) {
    /** Réattribuable si l'équipe source est retrouvée en ligne ET qu'au moins un candidat mkworld existe. */
    val isReattributable: Boolean
        get() = resolution is OpponentResolution.Found && resolution.mkworldCandidates.isNotEmpty()
}

sealed class OpponentResolution {

    /**
     * Équipe source mkworld retrouvée (rule 31). Son id ne sert PAS à réattribuer : on
     * rebondit sur [teamName]/[teamTag] pour proposer des [mkworldCandidates] (équipes
     * mkworld au nom/tag proche, ou override manuel). La réattribution écrit le rosterId
     * d'un candidat choisi par l'humain.
     */
    data class Found(
        val teamId: String,
        val teamName: String,
        val teamTag: String,
        /** Rosters mkworld candidats retrouvés par nom/tag (0, 1 ou plusieurs). */
        val mkworldCandidates: List<MkworldCandidate>,
    ) : OpponentResolution()

    /** Aucune équipe/roster mkworld actif 6+ joueurs ne porte cet id (hors override). */
    data object NotFound : OpponentResolution()

    /** La résolution a échoué (réseau/parsing) — à réessayer. */
    data object Error : OpponentResolution()
}

/**
 * Équipe mkworld candidate à la réattribution (retrouvée par nom/tag). Chaque roster est une
 * cible : son [CandidateRoster.rosterId] est l'id à écrire dans `War.teamOpponent`.
 */
data class MkworldCandidate(
    val teamId: String,
    val teamName: String,
    val teamTag: String,
    val rosters: List<CandidateRoster>,
)

data class CandidateRoster(
    /** rosterId mkworld — id à réécrire dans `War.teamOpponent`. */
    val rosterId: String,
    val name: String,
    val tag: String,
)
