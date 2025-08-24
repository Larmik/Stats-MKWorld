package fr.harmoniamk.statsmkworld.extension

import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.PlayerPosition
import fr.harmoniamk.statsmkworld.model.local.PlayerScore
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import kotlinx.coroutines.flow.firstOrNull
import kotlin.collections.filter
import kotlin.collections.forEach
import kotlin.collections.map
import kotlin.collections.orEmpty

suspend fun War.withPlayersList(databaseRepository: DatabaseRepositoryInterface, firebaseRepository: FirebaseRepositoryInterface): List<PlayerScore> {
    val localPlayers = databaseRepository.getPlayers().firstOrNull()

    val currentLocalPlayers = localPlayers
        ?.filter { player -> this.tracks.flatMap { it.positions }.any { it.playerId == player.id  } || player.currentWar == this.id.toString() }
        ?.map { PlayerScore(it, 0, 0, 0) }
        .orEmpty()


    val players = when (currentLocalPlayers.isEmpty()) {
        true -> firebaseRepository.getUsers(this.teamHost)
            .firstOrNull()
            ?.filter { player -> this.tracks.flatMap { it.positions }.any { it.playerId == player.id  } ||  player.currentWar == this.id.toString()}
            ?.map { user -> localPlayers?.firstOrNull { it.id == user.id } }
            ?.map { PlayerScore(it, 0, 0, 0) }
            .orEmpty()

        else -> currentLocalPlayers
    }

    val trackList = this.tracks
    val finalList = mutableListOf<PlayerScore>()
    val positions = mutableListOf<Pair<PlayerEntity?, Int>>()
    val shocks =  trackList.flatMap { it.shocks.orEmpty() }
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
                        entry.value.sumOf { playerPos -> playerPos.position.position.positionToPoints() }
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
    players
        .filter { !finalList.map { it.player?.id }.contains(it.player?.id) }
        .forEach { finalList.add(it) }
    return finalList
}