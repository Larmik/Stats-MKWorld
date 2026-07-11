package fr.harmoniamk.statsmkworld.model.local

/**
 * Un joueur **manquant** : son `playerId` apparaît dans au moins une war (via
 * `WarPosition.playerId`) mais ne correspond à **aucun**
 * [fr.harmoniamk.statsmkworld.database.entities.PlayerEntity] du cache local
 * (ni membre, ni allié) — il a probablement quitté l'équipe.
 *
 * Produit dédoublonné par `FetchUseCase.diagnoseMissingPlayers` : outil de debug
 * **non destructif** (lecture seule), miroir du diagnostic des adversaires
 * « Équipe inconnue ». Chaque entrée propose une action « Ajouter en ally »
 * (`addMissingPlayerAsAlly`). Le nom/pays sont résolus via MKCentral ; si l'id
 * n'y est pas résolu, on retombe sur une valeur dégradée (« Joueur inconnu »,
 * pays vide) sans faire disparaître l'entrée.
 */
data class MissingPlayer(
    /** Id MKCentral du joueur (contenu de `WarPosition.playerId`). */
    val playerId: String,
    /** Nom résolu via MKCentral, ou « Joueur inconnu » si non résolu. */
    val name: String,
    /** Code pays résolu via MKCentral, ou "" si non résolu. */
    val country: String,
    /** Nombre de wars (des rosters hôtes) où ce joueur apparaît — informatif. */
    val warCount: Int,
)
