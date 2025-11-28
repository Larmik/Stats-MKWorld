package fr.harmoniamk.statsmkworld.model.network

import kotlinx.serialization.Serializable

@Serializable
data class SplitsDto(
    val laps: List<String>,
    val coinsPerLap: List<Int>,
    val shroomsPerLap: List<Int>
)

@Serializable
data class RecordDto(
    val date: String,
    val track: String,
    val time: String,
    val player: String,
    val nation: String,
    val durationDays: Int?,
    val character: String,
    val vehicle: String,
    val splits: SplitsDto? = null
)