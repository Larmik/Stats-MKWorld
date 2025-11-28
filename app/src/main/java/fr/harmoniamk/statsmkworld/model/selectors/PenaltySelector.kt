package fr.harmoniamk.statsmkworld.model.selectors

data class PenaltySelector(val penalty: PenaltyType, var isSelected: Boolean = false)

enum class PenaltyType{
    HOST_MINUS_10,OPPONENT_MINUS_10, HOST_MINUS_15, OPPONENT_MINUS_15, HOST_MINUS_20, OPPONENT_MINUS_20
}