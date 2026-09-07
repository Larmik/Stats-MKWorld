package fr.harmoniamk.statsmkworld.model.selectors

data class PenaltySelector(val penalty: PenaltyType, var isSelected: Boolean = false)



sealed class PenaltyType(val teamId: String, val amount: Int) {
    data class Minus10(val team: String): PenaltyType(team, 10)
    data class Minus15(val team: String): PenaltyType(team, 15)
    data class Minus20(val team: String): PenaltyType(team, 20)
}