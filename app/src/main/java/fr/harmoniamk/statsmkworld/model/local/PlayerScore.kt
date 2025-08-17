package fr.harmoniamk.statsmkworld.model.local

import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity

data class PlayerScore(val player: PlayerEntity?, val score: Int, val trackPlayed: Int, val shockCount: Int)