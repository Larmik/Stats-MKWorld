package fr.harmoniamk.statsmkworld.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import fr.harmoniamk.statsmkworld.extension.sendDebugNotification
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.TournamentRepositoryInterface
import fr.harmoniamk.statsmkworld.usecase.FetchUseCaseInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalCoroutinesApi::class)
@HiltWorker
class UpdateDataWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val fetchUseCase: FetchUseCaseInterface,
    private val tournamentRepository: TournamentRepositoryInterface
    ): MKCoroutineWorker(context = context, workerParams = workerParams), CoroutineScope {

    override suspend fun task() {
        dataStoreRepository.mkcPlayer.firstOrNull()?.id?.let {
           fetchUseCase.fetchData(it.toString())
            // Nouvelle saison d'un tournoi officiel prise en compte sans release (#152).
            tournamentRepository.fetchTournaments()
            if (dataStoreRepository.notifEnabled.firstOrNull() == true)
                context.sendDebugNotification("Données mises à jour")
        }
    }


}