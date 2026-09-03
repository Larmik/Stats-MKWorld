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
 * Repository dédié à la notion de **saison** (#30). Agrège deux sources sans
 * repository naturel unique — RTDB (`FirebaseRepository`, source de vérité
 * `seasons/{teamId}`) et Room (`DatabaseRepository`, cache local) — d'où un
 * repository propre plutôt qu'une extension d'un UseCase partagé (rule 32).
 *
 * Responsabilités :
 * - [fetchSeasons] : synchro RTDB → Room, avec **seeding** de l'historique réel si le
 *   nœud est vide (appelée par la chaîne `FetchUseCase.fetchData`) ;
 * - [seedInitialSeasons] : (ré)inscrit l'historique réel des 3 saisons dans RTDB + Room,
 *   **inconditionnellement** — outil de maintenance appelé par l'écran Debug (#30) ;
 * - [startNewSeason] : action leader « démarrer une nouvelle saison » (clôt la courante,
 *   en ouvre une nouvelle), écrite **en RTDB ET en Room**.
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
        // Seeding-si-vide : si aucune saison n'existe en RTDB pour l'équipe, on écrit
        // l'historique réel via seedInitialSeasons (qui persiste RTDB + Room et renvoie
        // la liste). Sinon on rafraîchit simplement le cache Room depuis RTDB.
        val remoteSeasons = firebaseRepository.getSeasons(teamId).ifEmpty { return seedInitialSeasons(teamId) }
        databaseRepository.clearSeasons()
        databaseRepository.writeSeasons(remoteSeasons.map { SeasonEntity(teamId, it) })
    }

    override suspend fun seedInitialSeasons(teamId: String) {
        // Historique réel des 3 saisons (l'app est déjà en saison 3, S3 laissée ouverte :
        // end=null). Écriture INCONDITIONNELLE (RTDB + Room) — seuls appelants : le
        // seeding-si-vide de fetchSeasons et l'outil de maintenance Debug (2 sites → le
        // littéral partagé est légitime ici, il empêche la divergence entre les deux, rule 61).
        // Timestamps 00:00 UTC (ms). Une saison commence le lendemain de la fin de la
        // précédente. La fin prévue de la S3 (~01/11/2026) N'est PAS préremplie :
        // l'end réel sera le timestamp du clic « Démarrer une nouvelle saison ».
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
        // Bornes « propres » autour de minuit, calculées dans le fuseau horaire de
        // l'APPAREIL (ZoneId.systemDefault()) : le jour est celui du clic (now → LocalDate
        // local), la saison en cours se termine ce jour-là à 23:59, la nouvelle commence
        // le lendemain à 00:01 (précision minute, comme demandé). Le seeding (dates figées
        // à 00:00 UTC) reste inchangé — seul startNewSeason est en local. java.time (minSdk 28 ≥ 26).
        val zone = ZoneId.systemDefault()
        val clickDay = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(zone).toLocalDate()
        val closeEnd = clickDay.atTime(23, 59).atZone(zone).toInstant().toEpochMilli()
        val newStart = clickDay.plusDays(1).atTime(0, 1).atZone(zone).toInstant().toEpochMilli()
        val seasons = firebaseRepository.getSeasons(teamId)
        // Clôt la dernière saison en cours (end == null), puis ouvre une nouvelle saison
        // (numéro incrémenté, end = null) commençant le lendemain (règle des bornes).
        val currentNumber = seasons.maxOfOrNull { it.number } ?: 0
        val updated = seasons.map { season ->
            if (season.end == null) season.copy(end = closeEnd) else season
        } + Season(number = currentNumber + 1, start = newStart, end = null)
        firebaseRepository.writeSeasons(teamId, updated)
        databaseRepository.clearSeasons()
        databaseRepository.writeSeasons(updated.map { SeasonEntity(teamId, it) })
    }
}
