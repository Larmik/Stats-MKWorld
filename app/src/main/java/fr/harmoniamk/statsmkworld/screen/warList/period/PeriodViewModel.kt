package fr.harmoniamk.statsmkworld.screen.warList.period

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.extension.withPlayersList
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel de « Voir par période » (#80). Filtre les wars de l'équipe (roster hôte, 12p) dont
 * le timestamp (`War.id`, epoch ms) tombe dans `[dateA, dateB]`, produit l'historique et le
 * classement des joueurs de la période. Logique mono-consommateur → dans le VM (rule 32).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PeriodViewModel @Inject constructor(
    private val databaseRepository: DatabaseRepositoryInterface,
    private val firebaseRepository: FirebaseRepositoryInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface
) : ViewModel() {

    /**
     * Agrégat par joueur sur la période : [warsPlayed] (wars jouées), [participationRate]
     * (`warsPlayed × 100 / nb wars équipe`, 0 % si dénominateur nul), [averageScore] (points /
     * warsPlayed, moyenne par war, #80), [shockCount] (cumul des shocks).
     */
    data class PlayerPeriodStats(
        val player: PlayerEntity,
        val warsPlayed: Int,
        val participationRate: Int,
        val averageScore: Int,
        val shockCount: Int
    )

    data class State(
        // Plage sélectionnée (epoch ms). Semée sur la saison en cours au chargement.
        val dateA: Long? = null,
        val dateB: Long? = null,
        // Wars de la plage (12p, roster hôte), triées récentes → anciennes.
        val wars: List<WarDetails> = listOf(),
        // Classement des joueurs de la période, trié par nb de wars jouées (décroissant).
        val players: List<PlayerPeriodStats> = listOf()
    )

    // Sélection de plage : null tant que la saison n'est pas résolue (sème dateA/dateB une fois).
    private val _range = MutableStateFlow<Pair<Long, Long>?>(null)
    private val _state = MutableStateFlow(State())

    val state = combine(databaseRepository.getWars(), _range) { warEntities, range ->
        val multiRosterEnabled = dataStoreRepository.multiRosterEnabled.firstOrNull() == true
        val rosterId = dataStoreRepository.mkcPlayer.firstOrNull()
            ?.rosters?.firstOrNull { it.game == "mkworld" }?.rosterID?.toString()

        // Bornes par défaut = saison en cours (borne haute plafonnée à aujourd'hui), puis
        // sélection utilisateur une fois la plage semée.
        val effectiveRange = range ?: run {
            val seasons = databaseRepository.getSeasons().firstOrNull().orEmpty()
            val currentSeason = seasons.lastOrNull { it.end == null } ?: seasons.maxByOrNull { it.number }
            val now = System.currentTimeMillis()
            val start = currentSeason?.start ?: now
            val end = currentSeason?.end?.coerceAtMost(now) ?: now
            (start to end).also { _range.value = it }
        }
        val (dateA, dateB) = effectiveRange

        // 12p only + roster hôte + plage de dates (sur le `war.id` brut, epoch ms).
        val periodWars = warEntities
            .filter { it.teamOpponent.size == 1 }
            .filter { (!multiRosterEnabled && it.teamHost == rosterId) || multiRosterEnabled }
            .filter { it.id.toLongOrNull()?.let { id -> id in dateA..dateB } == true }
            .map { War(it) }
            .sortedByDescending { it.id }

        val warDetails = periodWars.map { WarDetails(it) }

        // Agrégats par joueur via withPlayersList (score/shocks/présence). trackPlayed > 0 = « a
        // joué » la war. Dénominateur participation = nb wars équipe de la période.
        val teamWarsCount = periodWars.size
        val scoreSum = HashMap<String, Int>()
        val shockSum = HashMap<String, Int>()
        val warsPlayed = HashMap<String, Int>()
        val playersById = HashMap<String, PlayerEntity>()
        periodWars.forEach { war ->
            war.withPlayersList(databaseRepository, firebaseRepository, dataStoreRepository)
                .filter { it.trackPlayed > 0 && it.player != null }
                .forEach { playerScore ->
                    val player = playerScore.player!!
                    playersById[player.id] = player
                    scoreSum[player.id] = (scoreSum[player.id] ?: 0) + playerScore.score
                    shockSum[player.id] = (shockSum[player.id] ?: 0) + playerScore.shockCount
                    warsPlayed[player.id] = (warsPlayed[player.id] ?: 0) + 1
                }
        }
        val players = playersById.values.map { player ->
            val played = warsPlayed[player.id] ?: 0
            PlayerPeriodStats(
                player = player,
                warsPlayed = played,
                participationRate = when (teamWarsCount) {
                    0 -> 0
                    else -> played * 100 / teamWarsCount
                },
                averageScore = when (played) {
                    0 -> 0
                    else -> (scoreSum[player.id] ?: 0) / played
                },
                shockCount = shockSum[player.id] ?: 0
            )
        }.sortedByDescending { it.warsPlayed }

        _state.value.copy(
            dateA = dateA,
            dateB = dateB,
            wars = warDetails,
            players = players
        )
    }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    /** Sélection utilisateur d'une plage (dateA ≤ dateB garanti par l'appelant / borné ici). */
    fun onRangeSelected(dateA: Long, dateB: Long) {
        _range.value = minOf(dateA, dateB) to maxOf(dateA, dateB)
    }
}
