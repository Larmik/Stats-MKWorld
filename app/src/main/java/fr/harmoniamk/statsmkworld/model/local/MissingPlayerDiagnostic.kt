package fr.harmoniamk.statsmkworld.model.local

/**
 * Joueur manquant : son `playerId` figure dans une war mais aucun `PlayerEntity` local ne
 * le porte (membre ou allié parti). Produit par `DiagnosticRepository` (debug, lecture
 * seule), chaque entrée propose « Ajouter en ally ». Nom/pays résolus via MKCentral, sinon
 * dégradés (« Joueur inconnu ») sans effacer l'entrée.
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
