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
        val currentPlayer = dataStoreRepository.mkcPlayer.firstOrNull()
        val multiRosterEnabled = dataStoreRepository.multiRosterEnabled.firstOrNull() == true
        val rosterId = currentPlayer?.rosters?.firstOrNull { it.game == "mkworld" }?.rosterID?.toString()
        val is24PEnabled = dataStoreRepository.is24PEnabled.firstOrNull() == true

        // Pas de normalisation ici : withFullTeamStats rapproche les wars de chaque
        // équipe adverse par teamId OU rosterId (via TeamEntity.rosters), et le
        // regroupement mostPlayed/etc. résout le rosterId → équipe dans withFullStats.
        databaseRepository.getWars().firstOrNull()
            ?.filter { (!multiRosterEnabled && it.teamHost == rosterId) || multiRosterEnabled }
            ?.filter { (is24PEnabled && it.teamOpponent.size > 1 || (!is24PEnabled && it.teamOpponent.size == 1)) }
            ?.let { warList ->
            val currentTeam = dataStoreRepository.mkcTeam.firstOrNull()

            //Fetch tracks stats
            statsRepository.trackRankList = warList.withTrackStats().map { RankingItem.TrackRanking(it) }
            statsRepository.playerTrackRankList = warList.withTrackStats(currentPlayer?.id.toString()).map { RankingItem.TrackRanking(it) }

            //Fetch players stats
            databaseRepository.getPlayers()
                .mapNotNull { it.sortedBy { it.name } }
                .onEach { userList ->
                    val rosters = dataStoreRepository.mkcTeam.firstOrNull()?.rosters
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
                        .mapNotNull { it as? RankingItem.PlayerRanking }
                        .filter { it.stats.warStats.warsPlayed > 0 }
                        .groupBy { ranking ->
                            val pair = when (val name =  rosters?.firstOrNull { it.id.toString() == ranking.player.rosterId }?.name) {
                                null -> Pair(1, "Allies")
                                else -> Pair(0, name)
                             }
                            pair
                        }
                }.launchIn(this)

            //Fetch opponent stats
            val teams = databaseRepository.getTeams()
                .map { it.filterNot { team -> team.id == currentTeam?.id.toString() } }
                .mapNotNull { it.sortedBy { it.name } }
                .shareIn(this, SharingStarted.WhileSubscribed(5000))

            teams
                .flatMapLatest { it.withFullTeamStats(wars = warList, databaseRepository = databaseRepository, is24p = is24PEnabled) }
                .map { it
                    .sortedByDescending { it.second.warStats.warsPlayed }
                    .map { RankingItem.OpponentRanking(it.first, it.second) } }
                .onEach { statsRepository.opponentRankList = it }
                .launchIn(this)
            teams
                .flatMapLatest { it.withFullTeamStats(wars = warList, databaseRepository = databaseRepository, userId = currentPlayer?.id.toString(), is24p = is24PEnabled) }
                .map { it
                    .sortedByDescending { it.second.warStats.warsPlayed }
                    .map { RankingItem.OpponentRanking(it.first, it.second) } }
                .onEach { statsRepository.playerOpponentRankList = it }
                .launchIn(this)
        }
        return Result.success()
    }
}