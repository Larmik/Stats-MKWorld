package fr.harmoniamk.statsmkworld.model.local

import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.model.firebase.WarPosition

data class PlayerPosition(val player: PlayerEntity?, val position: WarPosition)