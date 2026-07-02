package fr.harmoniamk.statsmkworld.extension

import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.PlayerPosition
import fr.harmoniamk.statsmkworld.model.local.PlayerScore
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import kotlinx.coroutines.flow.firstOrNull

suspend fun War.withPlayersList(databaseRepository: DatabaseRepositoryInterface, firebaseRepository: FirebaseRepositoryInterface, dataStoreRepository: DataStoreRepositoryInterface): List<PlayerScore> {
    val localPlayers = databaseRepository.getPlayers().firstOrNull()

    // Ensemble des ids de joueurs ayant une position sur cette war (hissé hors des boucles).
    val playerIdsInWar = this.tracks.flatMap { track -> track.positions.map { it.playerId } }.toHashSet()

    val currentLocalPlayers = localPlayers
        ?.filter { player -> playerIdsInWar.contains(player.id) || player.currentWar == this.id.toString() }
        ?.map { PlayerScore(it, 0, 0, 0) }
        .orEmpty()


    val players = when (currentLocalPlayers.isEmpty()) {
        true -> {
            val team = dataStoreRepository.mkcTeam.firstOrNull()
            firebaseRepository.getUsers(team?.id.toString())
                .filter { player -> playerIdsInWar.contains(player.id) || player.currentWar == this.id.toString() }
                .map { user -> localPlayers?.firstOrNull { it.id == user.id } }
                .map { PlayerScore(it, 0, 0, 0) }
        }

        else -> currentLocalPlayers
    }

    val trackList = this.tracks
    val finalList = mutableListOf<PlayerScore>()
    val positions = mutableListOf<Pair<PlayerEntity?, Int>>()
    val shocks =  trackList.flatMap { it.shocks.orEmpty() }
    val is24p = this.teamOpponent.size > 1
    trackList.forEach {
        it.positions.takeIf { it.isNotEmpty() }?.let { warPositions ->
            val trackPositions = mutableListOf<PlayerPosition>()
            warPositions.forEach { position ->
                trackPositions.add(
                    PlayerPosition(
                        position = position,
                        player = players.map { it.player }.singleOrNull { it?.id == position.playerId }
                    )
                )
            }
            trackPositions.groupBy { it.player }.entries.forEach { entry ->
                positions.add(
                    Pair(
                        entry.key,
                        entry.value.sumOf { playerPos -> playerPos.position.position.positionToPoints(is24p) }
                    )
                )
            }
        }
    }
    val temp = positions.groupBy { it.first }
        .map { Pair(it.key, it.value.sumOf { it.second }) }
        .sortedByDescending { it.second }
    temp.forEach { pair ->
        finalList.add(PlayerScore(
            player = pair.first,
            score = pair.second,
            trackPlayed = trackList.filter { it.positions.any { it.playerId == pair.first?.id } }.size,
            shockCount = shocks.filter { it.playerId == pair.first?.id }.sumOf { it.count }
        ))
    }
    val scoredPlayerIds = finalList.mapTo(HashSet()) { it.player?.id }
    players
        .filter { !scoredPlayerIds.contains(it.player?.id) }
        .forEach { finalList.add(it) }
    return finalList
}