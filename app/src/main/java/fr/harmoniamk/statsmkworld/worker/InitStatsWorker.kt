package fr.harmoniamk.statsmkworld.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import fr.harmoniamk.statsmkworld.extension.withFullStats
import fr.harmoniamk.statsmkworld.extension.withFullTeamStats
import fr.harmoniamk.statsmkworld.extension.withTrackStats
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.StatsRepositoryInterface
import fr.harmoniamk.statsmkworld.screen.stats.ranking.RankingItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltWorker
class InitStatsWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val databaseRepository: DatabaseRepositoryInterface,
    private val statsRepository: StatsRepositoryInterface
) : CoroutineWorker(appContext = context, params = workerParams), CoroutineScope {

    companion object {

        val work: OneTimeWorkRequest
            get() = OneTimeWorkRequestBuilder<InitStatsWorker>().build()

    }

    override suspend fun doWork(): Result {
        databaseRepository.getWars().firstOrNull()?.let { warList ->
            val currentTeam = dataStoreRepository.mkcTeam.firstOrNull()
            val currentPlayer = dataStoreRepository.mkcPlayer.firstOrNull()

            //Fetch tracks stats
            statsRepository.trackRankList = warList.withTrackStats().map { RankingItem.TrackRanking(it) }
            statsRepository.playerTrackRankList = warList.withTrackStats(currentPlayer?.id.toString()).map { RankingItem.TrackRanking(it) }

            //Fetch players stats
            databaseRepository.getPlayers()
                .mapNotNull { it.sortedBy { it.name } }
                .onEach { userList ->
                    val players = mutableListOf<RankingItem>()
                    userList.forEach { user ->
                        warList
                            .filter { war -> war.hasPlayer(user.id) }
                            .map { WarDetails(War(it)) }
                            .withFullStats(databaseRepository, userId = user.id)
                            .map { players.add(RankingItem.PlayerRanking(user, it)) }
                            .firstOrNull()
                    }
                    statsRepository.playersRankList = players
                }.launchIn(this)

            //Fetch opponent stats
            val teams = databaseRepository.getTeams()
                .map { it.filterNot { team -> team.id == currentTeam?.id.toString() } }
                .mapNotNull { it.sortedBy { it.name } }
                .shareIn(this, SharingStarted.WhileSubscribed(5000))

            teams
                .flatMapLatest { it.withFullTeamStats(wars = warList, databaseRepository = databaseRepository) }
                .map { it.map { RankingItem.OpponentRanking(it.first, it.second) } }
                .onEach { statsRepository.opponentRankList = it }
                .launchIn(this)
            teams
                .flatMapLatest { it.withFullTeamStats(wars = warList, databaseRepository = databaseRepository, userId = currentPlayer?.id.toString()) }
                .map { it.map { RankingItem.OpponentRanking(it.first, it.second) } }
                .onEach { statsRepository.playerOpponentRankList = it }
                .launchIn(this)
        }
        return Result.success()
    }
}