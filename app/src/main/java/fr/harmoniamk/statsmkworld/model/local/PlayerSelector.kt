package fr.harmoniamk.statsmkworld.model.local

import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity

data class PlayerSelector(val player: PlayerEntity, var isSelected: Boolean = false)

data class PenaltySelector(val penalty: PenaltyType, var isSelected: Boolean = false)

enum class PenaltyType{
    REPICK_HOST, INTERMISSION_HOST, REPICK_OPPONENT, INTERMISSION_OPPONENT
}
