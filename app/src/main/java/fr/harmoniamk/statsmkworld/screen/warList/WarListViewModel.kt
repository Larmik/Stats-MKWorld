package fr.harmoniamk.statsmkworld.screen.warList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.SeasonEntity
import fr.harmoniamk.statsmkworld.extension.filterByKind
import fr.harmoniamk.statsmkworld.extension.filterBySeason
import fr.harmoniamk.statsmkworld.extension.format
import fr.harmoniamk.statsmkworld.extension.get
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.SeasonFilter
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.model.local.WarKindFilter
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

/**
 * ViewModel de l'historique des wars. [userId] `null`/`"me"` ⇒ toutes les wars de l'équipe
 * (filtre roster hôte) ; [userId] renseigné ⇒ wars où CE joueur a joué (#65). La war en cours
 * n'apparaît pas (écrite dans Room seulement à la validation) ; `State.currentWar` (listener
 * temps réel) ne sert qu'au gating du bouton « Créer une war ». [initialKindFilter] : filtre
 * Amicaux/Officiels hérité de l'écran parent (#103), défaut au pôle.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = WarListViewModel.Factory::class)
class WarListViewModel @AssistedInject constructor(
    @Assisted val userId: String?,
    @Assisted initialKindFilter: WarKindFilter,
    firebaseRepository: FirebaseRepositoryInterface,
    private val databaseRepository: DatabaseRepositoryInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(userId: String?, initialKindFilter: WarKindFilter): WarListViewModel
    }

    data class State(
        // Historique groupé par mois (sticky headers) — TOUS les modes (12j ET 24j).
        val wars: List<Pair<String, List<WarDetails>>> = listOf(),
        // Nombre total de wars affichées (sous-titre « N wars »).
        val warCount: Int = 0,
        // War en cours (bannière « En direct ») ; null → CTA « Nouvelle war ».
        val currentWar: War? = null,
        // Nom du joueur filtré (sous-titre « wars de … ») ; null = pas de filtre joueur.
        val playerName: String? = null,
        // Filtre par saison (#70) : liste + sélection (null = tout, défaut = saison en cours).
        val seasons: List<SeasonEntity> = listOf(),
        val selectedSeasonNumber: Int? = null,
        // Filtre Amicaux / Officiels (#103).
        val kindFilter: WarKindFilter = WarKindFilter()
    )

    private val currentRosterId = dataStoreRepository.mkcPlayer
        .mapNotNull { it.rosters?.firstOrNull { roster -> roster.game == "mkworld" }?.rosterID?.toString() }

    // Sélection de saison courante (#70) : recompute déclenché à chaque changement via combine.
    private val _seasonFilter = MutableStateFlow<SeasonFilter>(SeasonFilter.Default)
    private val _kindFilter = MutableStateFlow(initialKindFilter)

    /** Sources légères combinées avant le calcul (`mapLatest` annule un calcul devenu obsolète). */
    private data class Sources(
        val wars: List<WarEntity>,
        val currentWar: War?,
        val seasonFilter: SeasonFilter,
        val seasons: List<SeasonEntity>,
        val kindFilter: WarKindFilter
    )

    val state = currentRosterId
        // `listenToCurrentWar` alimente `State.currentWar` (gating du bouton), pas le filtrage.
        .flatMapLatest { rosterId ->
            combine(
                databaseRepository.getWars(),
                firebaseRepository.listenToCurrentWar(rosterId),
                _seasonFilter,
                databaseRepository.getSeasons(),
                _kindFilter,
                ::Sources
            ).mapLatest { (wars, currentWar, seasonFilter, seasons, kindFilter) ->
                val multiRosterEnabled = dataStoreRepository.multiRosterEnabled.firstOrNull() == true
                // "me"/null = joueur courant ; sinon le joueur passé (filtre de participation).
                val currentPlayerId = dataStoreRepository.mkcPlayer.firstOrNull()?.id?.toString()
                val targetUserId = userId?.takeIf { it != "me" } ?: currentPlayerId
                val filterByPlayer = userId != null
                val playerName = targetUserId
                    ?.takeIf { filterByPlayer }
                    ?.let { databaseRepository.getPlayer(it).firstOrNull()?.name }
                // Saisons observées en Flow réactif (#73) ; résolution de la saison effective
                // (défaut = saison en cours ; null = tout).
                val activeSeason = seasonFilter.resolve(seasons)
                // Filtres saison (#70) + Amicaux/Officiels (#103) + roster hôte + par joueur si
                // demandé, tous modes 12/24.
                // Seul ce mapping/groupage CPU est déporté sur `Dispatchers.Default` (withContext,
                // pas flowOn, #73) ; lectures de sources et `seasons` sur le collecteur.
                val (details, grouped) = withContext(Dispatchers.Default) {
                    val details = wars
                        .filterBySeason(activeSeason)
                        .filterByKind(kindFilter)
                        .filter { (!multiRosterEnabled && it.teamHost == rosterId) || multiRosterEnabled }
                        .filter { !filterByPlayer || it.hasPlayer(targetUserId) }
                        .map { War(it) }
                        .map { WarDetails(it) }
                        .sortedByDescending { it.war.id }
                    val grouped = details
                        .groupBy { war ->
                            val date = Date(war.war.id)
                            val month = date.get(Calendar.MONTH)
                            val year = date.get(Calendar.YEAR)
                            month.toString() + year.toString()
                        }.mapNotNull {
                            it.value.firstOrNull()?.war?.id?.let { id ->
                                val date = Date(id)
                                Pair(date.format("MMMM yyyy"), it.value)
                            }
                        }
                    details to grouped
                }
                State(
                    wars = grouped,
                    warCount = details.size,
                    currentWar = currentWar,
                    playerName = playerName,
                    seasons = seasons,
                    selectedSeasonNumber = activeSeason?.number,
                    kindFilter = kindFilter
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State(kindFilter = initialKindFilter))

    /** Sélection de saison depuis l'UI : `number` null = tout l'historique. */
    fun onSeasonSelected(number: Int?) {
        _seasonFilter.value = SeasonFilter.of(number)
    }

    /** Filtre Amicaux / Officiels (#103). */
    fun onKindFilterChange(filter: WarKindFilter) {
        _kindFilter.value = filter
    }

}
