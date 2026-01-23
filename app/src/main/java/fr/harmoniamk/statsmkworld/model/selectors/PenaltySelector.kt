package fr.harmoniamk.statsmkworld.model.selectors

data class PenaltySelector(val penalty: PenaltyType, var isSelected: Boolean = false)



sealed class PenaltyType(teamId: String) {
    data class Minus10(val teamId: String): PenaltyType(teamId)
    data class Minus15(val teamId: String): PenaltyType(teamId)
    data class Minus20(val teamId: String): PenaltyType(teamId)
}