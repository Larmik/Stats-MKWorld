package fr.harmoniamk.statsmkworld.model.selectors

import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity

data class PlayerSelector(val player: PlayerEntity, var isSelected: Boolean = false)