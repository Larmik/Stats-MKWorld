package fr.harmoniamk.statsmkworld.screen.warList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.SeasonEntity
import fr.harmoniamk.statsmkworld.extension.filterBySeason
import fr.harmoniamk.statsmkworld.extension.format
import fr.harmoniamk.statsmkworld.extension.get
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.WarDetails
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
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

/**
 * ViewModel de l'historique des wars. [userId] `null`/`"me"` ⇒ toutes les wars de l'équipe
 * (filtre roster hôte) ; [userId] renseigné ⇒ wars où CE joueur a joué (#65). La war en cours
 * n'apparaît pas (écrite dans Room seulement à la validation) ; `State.currentWar` (listener
 * temps réel) ne sert qu'au gating du bouton « Créer une war ».
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = WarListViewModel.Factory::class)
class WarListViewModel @AssistedInject constructor(
    @Assisted val userId: String?,
    firebaseRepository: FirebaseRepositoryInterface,
    private val databaseRepository: DatabaseRepositoryInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(userId: String?): WarListViewModel
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
        val selectedSeasonNumber: Int? = null
    )

    /**
     * Sélection de saison (#70), même modèle que Stats/Classements. [Default] = saison en
     * cours (résolue après chargement) ; [AllTime] = tout l'historique ; [Specific] = passée.
     */
    sealed interface SeasonFilter {
        data object Default : SeasonFilter
        data object AllTime : SeasonFilter
        data class Specific(val number: Int) : SeasonFilter
    }

    private val currentRosterId = dataStoreRepository.mkcPlayer
        .mapNotNull { it.rosters?.firstOrNull { roster -> roster.game == "mkworld" }?.rosterID?.toString() }

    // Sélection de saison courante (#70) : recompute déclenché à chaque changement via combine.
    private val _seasonFilter = MutableStateFlow<SeasonFilter>(SeasonFilter.Default)

    val state = currentRosterId
        // `listenToCurrentWar` alimente `State.currentWar` (gating du bouton), pas le filtrage.
        .flatMapLatest { rosterId ->
            combine(
                databaseRepository.getWars(),
                firebaseRepository.listenToCurrentWar(rosterId),
                _seasonFilter,
                databaseRepository.getSeasons()
            ) { wars, currentWar, seasonFilter, seasons ->
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
                val activeSeason = when (seasonFilter) {
                    is SeasonFilter.AllTime -> null
                    is SeasonFilter.Specific -> seasons.firstOrNull { it.number == seasonFilter.number }
                    is SeasonFilter.Default -> seasons.lastOrNull { it.end == null }
                }
                // Filtre par saison (#70) + roster hôte + par joueur si demandé, tous modes 12/24.
                // Seul ce mapping/groupage CPU est déporté sur `Dispatchers.Default` (withContext,
                // pas flowOn — rule 21, #73) ; lectures de sources et `seasons` sur le collecteur.
                val (details, grouped) = withContext(Dispatchers.Default) {
                    val details = wars
                        .filterBySeason(activeSeason)
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
                    selectedSeasonNumber = activeSeason?.number
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())

    /** Sélection de saison depuis l'UI : `number` null = tout l'historique. */
    fun onSeasonSelected(number: Int?) {
        _seasonFilter.value = number?.let { SeasonFilter.Specific(it) } ?: SeasonFilter.AllTime
    }

}
