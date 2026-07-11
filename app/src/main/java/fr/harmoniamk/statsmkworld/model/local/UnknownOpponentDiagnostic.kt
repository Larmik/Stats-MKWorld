package fr.harmoniamk.statsmkworld.model.local

/**
 * Résultat de diagnostic d'une war dont un adversaire (`War.teamOpponent`) ne se
 * résout à aucune [fr.harmoniamk.statsmkworld.database.entities.TeamEntity] du
 * cache local (affichée « Équipe inconnue » / tag « ??? » par
 * [fr.harmoniamk.statsmkworld.extension.opponentTeams]).
 *
 * Produit par `FetchUseCase.diagnoseUnknownOpponents` : outil de debug **non
 * destructif** (lecture seule) listant les wars concernées et, pour chaque id
 * d'adversaire non résolu, une tentative de résolution MKCentral parmi les
 * **équipes mkworld actives, non historiques et à effectif ≥ 6** (miroir du
 * filtre par défaut du site MKCentral). Sert à arbitrer war par war entre
 * réattribution (paquet A) et suppression (paquet B) — décision humaine.
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
    /**
     * Réattribuable (paquet A) si l'équipe source a été retrouvée en ligne **et**
     * qu'au moins un candidat mkworld a été proposé (par override manuel ou par
     * nom/tag) : c'est un roster mkworld candidat qui fournit l'id à réécrire.
     */
    val isReattributable: Boolean
        get() = resolution is OpponentResolution.Found && resolution.mkworldCandidates.isNotEmpty()
}

sealed class OpponentResolution {

    /**
     * Équipe « source » mkworld retrouvée via le balayage des équipes mkworld
     * actives 6+ joueurs (domaine exclusivement mkworld — cf. rule
     * 31-mkworld-only). Son id ne sert PAS
     * directement à la réattribution : on rebondit sur son [teamName] / [teamTag]
     * pour proposer des [mkworldCandidates] — les équipes **mkworld** dont le nom
     * ou le tag matche (l'adversaire a souvent recréé une équipe avec un nom/tag
     * proche), ou l'équipe cible d'un override manuel. La réattribution écrit
     * alors le **rosterId d'un roster mkworld candidat** choisi par l'humain.
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
 * Une équipe **mkworld** candidate à la réattribution, retrouvée par
 * correspondance de nom/tag avec l'équipe source. Chaque roster mkworld de
 * l'équipe est une cible potentielle : son [CandidateRoster.rosterId] est l'id à
 * écrire dans `War.teamOpponent`.
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
