package fr.harmoniamk.statsmkworld.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.database.entities.TournamentEntity
import fr.harmoniamk.statsmkworld.datasource.network.MKCentralDataSourceInterface
import fr.harmoniamk.statsmkworld.model.local.Tournament
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Données officielles des tournois de [Tournament] (#152) : synchro MKCentral → Room de leur
 * dernière saison mkworld. Repository dédié (réseau + Room) plutôt que `FetchUseCase` (rule 32).
 */
interface TournamentRepositoryInterface {
    /** Rafraîchit chaque tournoi de la liste blanche ; un tournoi en échec garde son cache. */
    suspend fun fetchTournaments()
}

@FlowPreview
@ExperimentalCoroutinesApi
@Module
@InstallIn(SingletonComponent::class)
interface TournamentRepositoryModule {
    @Binds
    @Singleton
    fun bindRepository(impl: TournamentRepository): TournamentRepositoryInterface
}

@FlowPreview
@ExperimentalCoroutinesApi
class TournamentRepository @Inject constructor(
    private val mkCentralDataSource: MKCentralDataSourceInterface,
    private val databaseRepository: DatabaseRepositoryInterface
) : TournamentRepositoryInterface {

    override suspend fun fetchTournaments() {
        // Séquentiel (API derrière Cloudflare, rule 30) : 2 appels par tournoi. La liste est triée
        // par date décroissante → 1ʳᵉ saison = dernière ; le filtre `name` étant un LIKE (il a
        // ramené « Halloween… » pour « Low Div »), le préfixe est revérifié côté client.
        Tournament.entries.forEach { tournament ->
            runCatching {
                mkCentralDataSource.getTournaments(seriesId = tournament.seriesId, name = tournament.namePrefix)
                    .successResponse?.tournaments
                    ?.firstOrNull { season -> tournament.namePrefix?.let { season.name.startsWith(it) } ?: true }
                    ?.let { mkCentralDataSource.getTournament(it.id).successResponse }
                    ?.let { databaseRepository.writeTournament(TournamentEntity(tournament, it)) }
            }
        }
    }
}
