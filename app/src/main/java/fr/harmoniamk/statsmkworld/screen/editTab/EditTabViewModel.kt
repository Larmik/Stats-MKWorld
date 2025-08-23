package fr.harmoniamk.statsmkworld.screen.editTab

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.positionToPoints
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.PlayerPosition
import fr.harmoniamk.statsmkworld.model.local.PlayerScore
import fr.harmoniamk.statsmkworld.model.local.PlayerScoreForTab
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.PDFRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import java.util.Date
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EditTabViewModel @Inject constructor(private val dataStoreRepository: DataStoreRepositoryInterface, private val databaseRepository: DatabaseRepositoryInterface, private val firebaseRepository: FirebaseRepositoryInterface, private val pdfRepository: PDFRepositoryInterface): ViewModel() {

    private val _rows = MutableStateFlow(6)
    private val _uri = MutableSharedFlow<Uri?>()
    private val _toast = MutableSharedFlow<String?>()
    val rows = _rows.asStateFlow()
    val uri = _uri.asSharedFlow()
    val toast = _toast.asSharedFlow()

    fun onManageRows(isAdding: Boolean) {
        when (isAdding) {
            true -> _rows.value += 1
            else -> _rows.value -= 1
        }
    }

    fun onGenerate(players: List<String>, scores: List<String>) {
        val filename = "war_" + Date().time.toString()
        dataStoreRepository.war
            .filterNotNull()
            .mapNotNull {
                val details = WarDetails(it)
                if (scores.mapNotNull { it.toIntOrNull() }.sum() == details.scoreOpponent) {
                    var teamWin: TeamEntity? = null
                    var teamLose: TeamEntity? = null
                    val playerScores = initPlayersList(it)
                    val opponentScores = players.mapIndexed { index, player -> PlayerScoreForTab(player, scores[index].toInt()) }
                    if (details.scoreHostWithPenalties >= details.scoreOpponentWithPenalties) {
                        teamWin = databaseRepository.getTeam(it.teamHost).firstOrNull()
                        teamLose = databaseRepository.getTeam(it.teamOpponent).firstOrNull()
                    }
                    else {
                        teamWin = databaseRepository.getTeam(it.teamOpponent).firstOrNull()
                        teamLose = databaseRepository.getTeam(it.teamHost).firstOrNull()
                    }
                    pdfRepository.generatePdf(details, teamWin, teamLose, playerScores, opponentScores)
                } else {
                    _toast.emit("Les scores des joueurs sont incorrects.")
                    null
                }
            }
            .flatMapLatest { pdfRepository.write(it, filename) }
            .onEach { uri -> _uri.emit(uri) }
            .launchIn(scope = viewModelScope)
    }

    private suspend fun initPlayersList(war: War): List<PlayerScoreForTab> {

        val localPlayers = databaseRepository.getPlayers().firstOrNull()

        val currentLocalPlayers = localPlayers
            ?.filter { player -> war.tracks.flatMap { it.positions }.any { it.playerId == player.id  } || player.currentWar == war.id.toString() }
            ?.map { PlayerScore(it, 0, 0, 0) }
            .orEmpty()

        val players = when (currentLocalPlayers.isEmpty()) {
            true -> firebaseRepository.getUsers(war.teamHost)
                .firstOrNull()
                ?.filter { player -> war.tracks.flatMap { it.positions }.any { it.playerId == player.id  } ||  player.currentWar == war.id.toString()}
                ?.map { user -> localPlayers?.firstOrNull { it.id == user.id } }
                ?.map { PlayerScore(it, 0, 0, 0) }
                .orEmpty()

            else -> currentLocalPlayers
        }

        val trackList = war.tracks
        val finalList = mutableListOf<PlayerScoreForTab>()
        val positions = mutableListOf<Pair<PlayerEntity?, Int>>()
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
            finalList.add(PlayerScoreForTab(
                player = pair.first?.name.orEmpty(),
                score = pair.second
            ))
        }
        players
            .filter { !finalList.map { it.player }.contains(it.player?.name) }
            .forEach { finalList.add(PlayerScoreForTab(it.player?.name.orEmpty(), it.score)) }
        return finalList
    }
}