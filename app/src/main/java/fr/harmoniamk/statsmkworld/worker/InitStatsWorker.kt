package fr.harmoniamk.statsmkworld.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.SeasonRepositoryInterface
import kotlinx.coroutines.flow.firstOrNull

/**
 * Worker one-shot enfilé à chaque démarrage (`MainViewModel`) et lors de la connexion
 * (`DataStoreRepository`). Rôle : **hydratation eager des saisons (#73)** — synchro RTDB → Room
 * sans attendre le worker périodique.
 *
 * Historique : ce worker peuplait aussi un cache de classements (`StatsRepository`) ; ce cache
 * n'était plus lu par aucun écran (les VM stats recalculent à la demande) et a été retiré comme
 * code mort (#51). Le nom `InitStatsWorker` est conservé car référencé par WorkManager.
 */
@HiltWorker
class InitStatsWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val seasonRepository: SeasonRepositoryInterface
) : CoroutineWorker(appContext = context, params = workerParams) {

    companion object {
        /** Requête one-shot du worker (enfilée par les points de démarrage/connexion). */
        val work: OneTimeWorkRequest
            get() = OneTimeWorkRequestBuilder<InitStatsWorker>().build()
    }

    override suspend fun doWork(): Result {
        // Hydratation eager des saisons (#73) : synchro RTDB → Room à chaque onCreate, sans
        // attendre le worker périodique. Idempotent, rattachée à l'équipe (pas au roster).
        dataStoreRepository.mkcTeam.firstOrNull()?.id?.let { seasonRepository.fetchSeasons(it.toString()) }
        return Result.success()
    }
}
