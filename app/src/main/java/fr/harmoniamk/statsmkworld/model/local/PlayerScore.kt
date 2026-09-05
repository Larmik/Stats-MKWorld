package fr.harmoniamk.statsmkworld.model.local

import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.extension.displayName

data class PlayerScore(val player: PlayerEntity?, val score: Int, val trackPlayed: Int, val shockCount: Int)

data class PlayerScoreForTab(val player: String, val score: Int, val shockCount: Int) {
    constructor(score: PlayerScore): this(
        // Nom affiché sur le tab (image générée) : premier pseudo si concaténation MKCentral (#79).
        player = score.player?.name.orEmpty().displayName,
        score = score.score,
        shockCount = score.shockCount
    )
}