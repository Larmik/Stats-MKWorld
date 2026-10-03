package fr.harmoniamk.statsmkworld.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import fr.harmoniamk.statsmkworld.model.local.Tournament
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.SeasonRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.TournamentRepositoryInterface
import kotlinx.coroutines.flow.firstOrNull

/**
 * Worker one-shot enfilé à chaque démarrage (`MainViewModel`) et lors de la connexion
 * (`DataStoreRepository`). Rôle : **hydratation eager des saisons (#73)** — synchro RTDB → Room
 * sans attendre le worker périodique — et des tournois officiels tant que leur cache est incomplet
 * ou non traduit dans la langue du téléphone (#152).
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
    private val seasonRepository: SeasonRepositoryInterface,
    private val databaseRepository: DatabaseRepositoryInterface,
    private val tournamentRepository: TournamentRepositoryInterface
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
        // Le worker périodique ne passe qu'à 4 h : sans cela, badges et fiches resteraient vides
        // jusqu'à la nuit suivante. Indépendant du joueur (données publiques).
        // Cache complet : seule la traduction est rattrapée (langue changée, modèle absent la veille).
        when (databaseRepository.getTournaments().firstOrNull().orEmpty().size < Tournament.entries.size) {
            true -> tournamentRepository.fetchTournaments()
            else -> tournamentRepository.translateTournaments()
        }
        return Result.success()
    }
}
