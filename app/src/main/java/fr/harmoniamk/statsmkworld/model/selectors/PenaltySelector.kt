package fr.harmoniamk.statsmkworld.model.selectors

data class PenaltySelector(val penalty: PenaltyType, var isSelected: Boolean = false)

enum class PenaltyType{
    REPICK_HOST, INTERMISSION_HOST, REPICK_OPPONENT, INTERMISSION_OPPONENT
}