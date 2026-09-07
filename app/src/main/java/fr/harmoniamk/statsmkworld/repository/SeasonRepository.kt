package fr.harmoniamk.statsmkworld.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.database.entities.SeasonEntity
import fr.harmoniamk.statsmkworld.model.firebase.Season
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository dédié aux saisons (#30) : agrège RTDB (source de vérité `seasons/{teamId}`)
 * et Room (cache), d'où un repository propre plutôt qu'un UseCase partagé (rule 32).
 * - [fetchSeasons] : synchro RTDB → Room, seeding si le nœud est vide ;
 * - [seedInitialSeasons] : (ré)inscrit l'historique inconditionnellement (outil Debug) ;
 * - [startNewSeason] : clôt la saison courante et en ouvre une nouvelle (RTDB + Room).
 */
interface SeasonRepositoryInterface {
    suspend fun fetchSeasons(teamId: String)
    suspend fun seedInitialSeasons(teamId: String)
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
        // Nœud RTDB vide → seed l'historique ; sinon rafraîchit le cache Room depuis RTDB.
        val remoteSeasons = firebaseRepository.getSeasons(teamId).ifEmpty { return seedInitialSeasons(teamId) }
        databaseRepository.clearSeasons()
        databaseRepository.writeSeasons(remoteSeasons.map { SeasonEntity(teamId, it) })
    }

    override suspend fun seedInitialSeasons(teamId: String) {
        // Historique réel des 3 saisons (S3 laissée ouverte, end=null), écrit
        // inconditionnellement en RTDB + Room. Timestamps 00:00 UTC (ms) ; une saison
        // commence le lendemain de la fin de la précédente. La fin de S3 sera fixée au
        // clic « Démarrer une nouvelle saison ».
        val seeded = listOf(
            Season(number = 1, start = 1749081600000, end = 1766275200000), // 05/06/2025 → 21/12/2025
            Season(number = 2, start = 1766361600000, end = 1777766400000), // 22/12/2025 → 03/05/2026
            Season(number = 3, start = 1777852800000, end = null)           // 04/05/2026 → en cours
        )
        firebaseRepository.writeSeasons(teamId, seeded)
        databaseRepository.clearSeasons()
        databaseRepository.writeSeasons(seeded.map { SeasonEntity(teamId, it) })
    }

    override suspend fun startNewSeason(teamId: String) {
        // Bornes autour de minuit dans le fuseau de l'APPAREIL : la saison courante se
        // termine le jour du clic à 23:59, la nouvelle commence le lendemain à 00:01.
        val zone = ZoneId.systemDefault()
        val clickDay = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(zone).toLocalDate()
        val closeEnd = clickDay.atTime(23, 59).atZone(zone).toInstant().toEpochMilli()
        val newStart = clickDay.plusDays(1).atTime(0, 1).atZone(zone).toInstant().toEpochMilli()
        val seasons = firebaseRepository.getSeasons(teamId)
        // Clôt la saison en cours (end == null), ouvre une nouvelle (numéro incrémenté).
        val currentNumber = seasons.maxOfOrNull { it.number } ?: 0
        val updated = seasons.map { season ->
            if (season.end == null) season.copy(end = closeEnd) else season
        } + Season(number = currentNumber + 1, start = newStart, end = null)
        firebaseRepository.writeSeasons(teamId, updated)
        databaseRepository.clearSeasons()
        databaseRepository.writeSeasons(updated.map { SeasonEntity(teamId, it) })
    }
}
