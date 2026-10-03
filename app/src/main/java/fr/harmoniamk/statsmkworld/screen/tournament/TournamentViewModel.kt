package fr.harmoniamk.statsmkworld.screen.tournament

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.TournamentEntity
import fr.harmoniamk.statsmkworld.extension.displayedString
import fr.harmoniamk.statsmkworld.extension.mkcentralUrl
import fr.harmoniamk.statsmkworld.model.local.Tournament
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.TranslationRepositoryInterface
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Date

/** Fiche d'un tournoi officiel (#152), lue dans le cache Room (aucun appel réseau à l'ouverture). */
@HiltViewModel(assistedFactory = TournamentViewModel.Factory::class)
class TournamentViewModel @AssistedInject constructor(
    @Assisted id: String,
    databaseRepository: DatabaseRepositoryInterface,
    translationRepository: TranslationRepositoryInterface
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(id: String): TournamentViewModel
    }

    /**
     * [details] `null` tant qu'aucune synchro n'a abouti (état vide). [ruleset] `null` s'il est vide
     * ou identique à la description (Low Div Cup) : pas de section Règles en doublon.
     * [descriptionTranslated]/[rulesetTranslated] : traduction dans la langue courante du téléphone,
     * `null` si absente ou faite dans une autre langue (originaux affichés, sans bascule).
     */
    data class State(
        val tournament: Tournament? = null,
        val details: TournamentEntity? = null,
        val dateStart: String? = null,
        val dateEnd: String? = null,
        val ruleset: String? = null,
        val descriptionTranslated: String? = null,
        val rulesetTranslated: String? = null,
        val pageUrl: String? = null,
        val isLoaded: Boolean = false
    )

    val state: StateFlow<State> = databaseRepository.getTournament(id)
        .map { details ->
            val ruleset = details?.ruleset?.takeIf { it.isNotBlank() && it.trim() != details.description.trim() }
            val translation = details?.takeIf { it.translationLanguage != null && it.translationLanguage == translationRepository.targetLanguage }
            State(
                tournament = Tournament.fromId(id),
                details = details,
                dateStart = details?.dateStart?.let { Date(it * 1000).displayedString("dd/MM/yyyy") },
                dateEnd = details?.dateEnd?.let { Date(it * 1000).displayedString("dd/MM/yyyy") },
                ruleset = ruleset,
                descriptionTranslated = translation?.descriptionTranslated,
                rulesetTranslated = ruleset?.let { translation?.rulesetTranslated },
                pageUrl = details?.let { "/en-us/tournaments/details?id=${it.mkcTournamentId}".mkcentralUrl },
                isLoaded = true
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State(tournament = Tournament.fromId(id)))
}
