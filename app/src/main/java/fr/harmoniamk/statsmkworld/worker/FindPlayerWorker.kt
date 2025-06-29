package fr.harmoniamk.statsmkworld.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.datasource.network.MKCentralDataSourceInterface
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayer
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.NotificationRepositoryInterface
import fr.harmoniamk.statsmkworld.usecase.FetchUseCaseInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
@HiltWorker
class FindPlayerWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val mkCentralDataSource: MKCentralDataSourceInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val notificationRepository: NotificationRepositoryInterface,
    private val fetchUseCase: FetchUseCaseInterface,
    private val firebaseRepository: FirebaseRepositoryInterface
): CoroutineWorker(appContext = context, params = workerParams), CoroutineScope {

    private fun getPlayer(page: Int, id: String, country: String) = mkCentralDataSource.getPlayers(page, country)
        .map { Pair(it?.pageCount, it?.playerList?.singleOrNull { it.discord?.discordID == id }) }
        .shareIn(this, SharingStarted.Eagerly)


    override suspend fun doWork(): Result {
        var page = 1
        when (val id = inputData.getString("discord_id")) {
            null -> {
                firebaseRepository.log("Discord id doesn't exist", "FindPlayerWorker").firstOrNull()
                dataStoreRepository.setPage(7)
                notificationRepository.sendNotification("Erreur lors du lien MKC")
            }
            else -> {
                val country = inputData.getString("country").orEmpty()
                firebaseRepository.log("Discord id found: $id, search player with country $country", "FindPlayerWorker").firstOrNull()
                var player: Pair<Int?, MKCPlayer?>? = getPlayer(page, id, country).firstOrNull()
                while (player?.second == null && page < (player?.first ?: 0)) {
                    page++
                    Log.d("====>>>>", "doWork: $page")
                    player = getPlayer(page, id, country).firstOrNull()
                }
                firebaseRepository.log("End searching players: Player found - name: ${player?.second?.name}, id: ${player?.second?.name}", "FindPlayerWorker").firstOrNull()
                val fullPlayer = mkCentralDataSource.getPlayer(player?.second?.id.toString()).firstOrNull()
                firebaseRepository.log("Full player found: ${fullPlayer?.successResponse?.name}", "FindPlayerWorker").firstOrNull()
                when {
                    fullPlayer?.errorResponse != null -> {
                        firebaseRepository.log(fullPlayer.errorResponse ?: "Erreur inconnue", "FindPlayerWorker").firstOrNull()
                        dataStoreRepository.setPage(7)
                        notificationRepository.sendNotification("Erreur lors du lien MKC")
                    }
                    else -> fullPlayer?.successResponse?.let { player ->
                        player.rosters?.firstOrNull { it.game == "mkworld" }?.teamID?.let { teamId ->
                            firebaseRepository.log("TeamId for player is $teamId", "FindPlayerWorker").firstOrNull()
                            dataStoreRepository.setMKCPlayer(player)
                            firebaseRepository.log("MKCPlayer set", "FindPlayerWorker").firstOrNull()
                            fetchUseCase.fetchTeam(teamId.toString())
                                .onEach { firebaseRepository.log("Team fetched : ${it.name}", "FindPlayerWorker").firstOrNull() }
                                .flatMapLatest { fetchUseCase.fetchAllies(it.id.toString()) }
                                .onEach { firebaseRepository.log("Allies fetched ", "FindPlayerWorker").firstOrNull() }
                                .flatMapLatest { fetchUseCase.fetchTeams() }
                                .onEach { firebaseRepository.log("Opponents fetched ", "FindPlayerWorker").firstOrNull() }
                                .flatMapLatest { fetchUseCase.fetchWars(teamId.toString()) }
                                .onEach { firebaseRepository.log("Wars fetched ", "FindPlayerWorker").firstOrNull() }
                                .onEach { dataStoreRepository.setLastUpdate(Date().time) }
                                .launchIn(this)
                            firebaseRepository.log("Tout est good !", "FindPlayerWorker").firstOrNull()
                            dataStoreRepository.setPage(6)
                            notificationRepository.sendNotification("Bienvenue ${player.name} ! Ton compte MKCentral est correctement lié à l'application.")
                        } ?: run {
                            firebaseRepository.log("Tout est good ! (pas de roster MKWorld trouvé)", "FindPlayerWorker").firstOrNull()
                            dataStoreRepository.setPage(6)
                            notificationRepository.sendNotification("Bienvenue ${player.name} ! Ton compte MKCentral est correctement lié à l'application.")
                        }
                    } ?: run {
                        firebaseRepository.log("Erreur inconnue", "FindPlayerWorker").firstOrNull()
                        dataStoreRepository.setPage(7)
                        notificationRepository.sendNotification("Erreur lors du lien MKC")
                    }
                }
            }
        }
        return Result.success()
    }


}