package fr.harmoniamk.statsmkworld.model.local

import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.extension.displayName

data class PlayerScore(val player: PlayerEntity?, val score: Int, val trackPlayed: Int, val shockCount: Int)

data class PlayerScoreForTab(
    val player: String,
    val score: Int,
    val shockCount: Int,
    // Courses réellement jouées par ce joueur / total de courses de la war. Un joueur
    // remplacé n'a joué qu'une partie des courses (trackPlayed < totalTracks).
    val trackPlayed: Int = 0,
    val totalTracks: Int = 0
) {
    constructor(score: PlayerScore, totalTracks: Int): this(
        // Nom affiché sur le tab (image générée) : premier pseudo si concaténation MKCentral (#79).
        player = score.player?.name.orEmpty().displayName,
        score = score.score,
        shockCount = score.shockCount,
        trackPlayed = score.trackPlayed,
        totalTracks = totalTracks
    )

    // Nom affiché sur le tab, suffixé du nombre de courses jouées « (N) » uniquement quand
    // le joueur a joué moins que le total (remplacement). Total dérivé de la war, jamais codé en dur.
    val displayedName: String
        get() = when (trackPlayed in 1 until totalTracks) {
            true -> "$player ($trackPlayed)"
            else -> player
        }
}