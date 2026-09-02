package fr.harmoniamk.statsmkworld.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.database.entities.SeasonEntity
import fr.harmoniamk.statsmkworld.model.firebase.Season
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository dédié à la notion de **saison** (#30). Agrège deux sources sans
 * repository naturel unique — RTDB (`FirebaseRepository`, source de vérité
 * `seasons/{teamId}`) et Room (`DatabaseRepository`, cache local) — d'où un
 * repository propre plutôt qu'une extension d'un UseCase partagé (rule 32).
 *
 * Deux responsabilités :
 * - [fetchSeasons] : synchro RTDB → Room, avec **seeding** de l'historique réel si le
 *   nœud est vide (appelée par la chaîne `FetchUseCase.fetchData`) ;
 * - [startNewSeason] : action leader « démarrer une nouvelle saison » (clôt la courante,
 *   en ouvre une nouvelle), écrite **en RTDB ET en Room**.
 */
interface SeasonRepositoryInterface {
    suspend fun fetchSeasons(teamId: String)
    suspend fun startNewSeason(teamId: String)
}

@FlowPreview
@ExperimentalCoroutinesApi
@Module
@InstallIn(SingletonComponent::class)
interface SeasonRepositoryModule {
    @Binds
    @Singleton
    fun bindRepository(impl: SeasonRepository): SeasonRepositoryInterface
}

@FlowPreview
@ExperimentalCoroutinesApi
class SeasonRepository @Inject constructor(
    private val firebaseRepository: FirebaseRepositoryInterface,
    private val databaseRepository: DatabaseRepositoryInterface
) : SeasonRepositoryInterface {

    override suspend fun fetchSeasons(teamId: String) {
        // Seeding-si-vide : si aucune saison n'existe en RTDB pour l'équipe, on écrit
        // l'historique réel (l'app est déjà en saison 3, S3 laissée ouverte : end=null).
        // Les 6 dates sont un littéral de seeding mono-site → inline (rule 61).
        // Timestamps 00:00 UTC (ms). Une saison commence le lendemain de la fin de la
        // précédente. La fin prévue de la S3 (~01/11/2026) N'est PAS préremplie :
        // l'end réel sera le timestamp du clic « Démarrer une nouvelle saison ».
        val remoteSeasons = firebaseRepository.getSeasons(teamId).ifEmpty {
            val seeded = listOf(
                Season(number = 1, start = 1749081600000, end = 1766275200000), // 05/06/2025 → 21/12/2025
                Season(number = 2, start = 1766361600000, end = 1777766400000), // 22/12/2025 → 03/05/2026
                Season(number = 3, start = 1777852800000, end = null)           // 04/05/2026 → en cours
            )
            firebaseRepository.writeSeasons(teamId, seeded)
            seeded
        }
        databaseRepository.clearSeasons()
        databaseRepository.writeSeasons(remoteSeasons.map { SeasonEntity(teamId, it) })
    }

    override suspend fun startNewSeason(teamId: String) {
        val now = System.currentTimeMillis()
        val seasons = firebaseRepository.getSeasons(teamId)
        // Clôt la dernière saison en cours (end == null) au timestamp actuel, puis
        // ouvre une nouvelle saison (numéro incrémenté, start = maintenant, end = null).
        val currentNumber = seasons.maxOfOrNull { it.number } ?: 0
        val updated = seasons.map { season ->
            if (season.end == null) season.copy(end = now) else season
        } + Season(number = currentNumber + 1, start = now, end = null)
        firebaseRepository.writeSeasons(teamId, updated)
        databaseRepository.clearSeasons()
        databaseRepository.writeSeasons(updated.map { SeasonEntity(teamId, it) })
    }
}
