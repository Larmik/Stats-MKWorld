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
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Données officielles des tournois de [Tournament] (#152) : synchro MKCentral → Room de leur
 * dernière saison mkworld. Repository dédié (réseau + Room) plutôt que `FetchUseCase`.
 */
interface TournamentRepositoryInterface {
    /** Rafraîchit chaque tournoi de la liste blanche ; un tournoi en échec garde son cache. */
    suspend fun fetchTournaments()

    /** Traduit les tournois en cache non encore traduits dans la langue du téléphone (aucun appel MKCentral). */
    suspend fun translateTournaments()
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
    private val databaseRepository: DatabaseRepositoryInterface,
    private val translationRepository: TranslationRepositoryInterface
) : TournamentRepositoryInterface {

    override suspend fun fetchTournaments() {
        // Séquentiel (API derrière Cloudflare) : 2 appels par tournoi. La liste est triée
        // par date décroissante → 1ʳᵉ saison = dernière ; le filtre `name` étant un LIKE (il a
        // ramené « Halloween… » pour « Low Div »), le préfixe est revérifié côté client.
        Tournament.entries.forEach { tournament ->
            runCatching {
                mkCentralDataSource.getTournaments(seriesId = tournament.seriesId, name = tournament.namePrefix)
                    .successResponse?.tournaments
                    ?.firstOrNull { season -> tournament.namePrefix?.let { season.name.startsWith(it) } ?: true }
                    ?.let { mkCentralDataSource.getTournament(it.id).successResponse }
                    ?.let { season ->
                        val fetched = TournamentEntity(tournament, season)
                        // Textes inchangés → traduction conservée (pas de retraduction).
                        val translated = databaseRepository.getTournament(tournament.name).firstOrNull()
                            ?.takeIf { it.description == fetched.description && it.ruleset == fetched.ruleset }
                        databaseRepository.writeTournament(
                            fetched.copy(
                                descriptionTranslated = translated?.descriptionTranslated,
                                rulesetTranslated = translated?.rulesetTranslated,
                                translationLanguage = translated?.translationLanguage
                            )
                        )
                    }
            }
        }
        translateTournaments()
    }

    override suspend fun translateTournaments() {
        // Un seul traducteur (donc un seul téléchargement de modèle) pour tous les tournois. Échec
        // (modèle absent hors Wi-Fi) → originaux conservés, retenté au prochain démarrage.
        translationRepository.targetLanguage?.let { language ->
            val pending = databaseRepository.getTournaments().firstOrNull().orEmpty().filter { it.translationLanguage != language }
            if (pending.isNotEmpty()) {
                val translations = runCatching {
                    translationRepository.translateMarkdown(pending.flatMap { listOf(it.description, it.ruleset) }, language)
                }.getOrNull()
                translations?.chunked(2)?.zip(pending)?.forEach { (texts, tournament) ->
                    databaseRepository.writeTournament(
                        tournament.copy(descriptionTranslated = texts[0], rulesetTranslated = texts[1], translationLanguage = language)
                    )
                }
            }
        }
    }
}
