package fr.harmoniamk.statsmkworld.model.local

import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity

data class PlayerScore(val player: PlayerEntity?, val score: Int, val trackPlayed: Int, val shockCount: Int)

data class PlayerScoreForTab(val player: String, val score: Int, val shockCount: Int) {
    constructor(score: PlayerScore): this(
        player = score.player?.name.orEmpty(),
        score = score.score,
        shockCount = score.shockCount
    )
}