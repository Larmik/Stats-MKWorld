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
 * ViewModel de l'historique des wars.
 *
 * - [userId] `null` (ou `"me"`) ⇒ historique **du joueur courant** au sens habituel :
 *   toutes les wars de l'équipe (filtre roster hôte), sans filtre de participation ;
 * - [userId] renseigné (autre joueur, ex. lien « Résultats → » depuis `Statsfull/{userId}`,
 *   #65) ⇒ historique **filtré sur les wars où CE joueur a joué** (`War.hasPlayer`).
 *
 * **War en cours** : elle n'apparaît PAS dans l'historique — non par un filtre, mais
 * parce qu'elle n'est écrite dans Room qu'à la **validation** (`CurrentWarViewModel.onValidateWar`) ;
 * tant qu'elle est « en cours », elle n'est pas dans `getWars()`. La bannière « Reprendre »
 * a par ailleurs été retirée de l'écran (#65). `State.currentWar` (issu du listener temps réel
 * `listenToCurrentWar(rosterId)`) ne sert plus qu'au **gating du bouton « Créer une war »**
 * (masqué tant qu'une war est en cours) — pas à un filtrage de la liste.
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
        // Filtre par saison (#70) : liste des saisons + saison sélectionnée (null = tout
        // l'historique, défaut = saison en cours). Filtre la liste des wars affichées.
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
        // `listenToCurrentWar` alimente `State.currentWar` (gating du bouton « Créer une war ») ;
        // il n'est PAS utilisé pour filtrer la liste (la war en cours n'est pas dans Room).
        .flatMapLatest { rosterId ->
            combine(
                databaseRepository.getWars(),
                firebaseRepository.listenToCurrentWar(rosterId),
                _seasonFilter,
                databaseRepository.getSeasons()
            ) { wars, currentWar, seasonFilter, seasons ->
                val multiRosterEnabled = dataStoreRepository.multiRosterEnabled.firstOrNull() == true
                // "me"/null = joueur courant ; sinon le joueur passé. Filtre de participation
                // uniquement pour un joueur donné (autre que la vue « toute l'équipe »).
                val currentPlayerId = dataStoreRepository.mkcPlayer.firstOrNull()?.id?.toString()
                val targetUserId = userId?.takeIf { it != "me" } ?: currentPlayerId
                val filterByPlayer = userId != null
                val playerName = targetUserId
                    ?.takeIf { filterByPlayer }
                    ?.let { databaseRepository.getPlayer(it).firstOrNull()?.name }
                // Saisons (cache Room) observées en Flow réactif (#73) : le dropdown apparaît
                // dès que l'hydratation eager écrit les saisons. Liste pour le dropdown +
                // résolution de la saison effective (défaut = saison en cours ; null = tout).
                val activeSeason = when (seasonFilter) {
                    is SeasonFilter.AllTime -> null
                    is SeasonFilter.Specific -> seasons.firstOrNull { it.number == seasonFilter.number }
                    is SeasonFilter.Default -> seasons.lastOrNull { it.end == null }
                }
                // Aucun filtre par mode (12/24) : l'historique mélange tous les modes.
                // Filtre par saison (#70) + roster hôte + par joueur si demandé. Pas de filtre
                // sur la war en cours : elle n'est pas dans Room tant qu'elle n'est pas validée.
                // SEUL ce mapping/groupage CPU (construction WarDetails + tri + groupBy) est
                // déporté sur `Dispatchers.Default` via `withContext` (et NON `flowOn` — cf.
                // rule 21, #73) ; les lectures de sources et `seasons` restent sur le collecteur.
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
