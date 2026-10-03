package fr.harmoniamk.statsmkworld.screen.editTab

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.displayName
import fr.harmoniamk.statsmkworld.extension.opponentTeams
import fr.harmoniamk.statsmkworld.extension.withPlayersList
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.PlayerScoreForTab
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.model.network.lorenzi.LorenziStylePreset
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.LorenziRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.PDFRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URL
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = EditTabViewModel.Factory::class)
class EditTabViewModel @AssistedInject constructor(
    @Assisted val details: WarDetails?,
    private val databaseRepository: DatabaseRepositoryInterface,
    private val firebaseRepository: FirebaseRepositoryInterface,
    private val pdfRepository: PDFRepositoryInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val lorenziRepository: LorenziRepositoryInterface
): ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(@Assisted details: WarDetails?): EditTabViewModel
    }

    @Suppress("ArrayInDataClass")
    data class State(
        val rows: Int = 6,
        val preset: LorenziStylePreset = LorenziStylePreset.ATLAS_LEAGUE,
        val isGenerating: Boolean = false,
        /** PNG HLorenzi affiché en aperçu, partagé (et écrit dans Pictures) seulement à la demande. */
        val lorenziTab: ByteArray? = null,
    )

    data class SharedTab(val uri: Uri, val mimeType: String)

    data class TabMessage(@StringRes val text: Int, val count: Int? = null)

    private val _state = MutableStateFlow(State())
    private val _share = MutableSharedFlow<SharedTab>()
    private val _toast = MutableSharedFlow<TabMessage>()
    private var lorenziJob: Job? = null

    val state = _state.asStateFlow()
    val share = _share.asSharedFlow()
    val toast = _toast.asSharedFlow()

    fun onManageRows(isAdding: Boolean) {
        _state.update {
            it.copy(rows = when (isAdding) {
                true -> it.rows + 1
                else -> it.rows - 1
            })
        }
    }

    fun onPresetChange(preset: LorenziStylePreset) {
        lorenziJob?.cancel()
        _state.update { it.copy(preset = preset, isGenerating = false, lorenziTab = null) }
    }

    /** Tab via gb2.hlorenzi.com (#105) ; repli sur le tab classique si le service échoue. */
    fun generateLorenziTab(players: List<String>, scores: List<String>) {
        details?.let { warDetails ->
            lorenziJob?.cancel()
            lorenziJob = viewModelScope.launch {
                opponentScores(warDetails, players, scores)?.let { opponentScores ->
                    _state.update { it.copy(isGenerating = true, lorenziTab = null) }
                    val hostTeam = hostTeam(warDetails.war)
                    val opponentTeam = warDetails.war.opponentTeams(databaseRepository).firstOrNull()
                    val tab = when (hostTeam != null && opponentTeam != null) {
                        true -> lorenziRepository.generateTab(
                            details = warDetails,
                            hostTeam = hostTeam,
                            opponentTeam = opponentTeam,
                            hostScores = hostScores(warDetails.war),
                            opponentScores = opponentScores,
                            preset = _state.value.preset
                        )
                        else -> null
                    }
                    _state.update { it.copy(isGenerating = false, lorenziTab = tab) }
                    if (tab == null) {
                        _toast.emit(TabMessage(R.string.tab_lorenzi_error))
                        generateClassicPdf(players, scores)
                    }
                }
            }
        }
    }

    fun shareLorenziTab() {
        _state.value.lorenziTab?.let { png ->
            viewModelScope.launch {
                pdfRepository.writeImage(png, tabFileName(), PDFRepositoryInterface.MIME_PNG)
                    ?.let { _share.emit(SharedTab(it, PDFRepositoryInterface.MIME_PNG)) }
                    ?: _toast.emit(TabMessage(R.string.tab_write_error))
            }
        }
    }

    fun generateClassicPdf(players: List<String>, scores: List<String>) {
        val filename = tabFileName()
        flowOf(details)
            .mapNotNull { it }
            .mapNotNull { warDetails ->
                opponentScores(warDetails, players, scores)?.let { opponentScores ->
                    val war = warDetails.war
                    val teamHost = hostTeam(war)
                    // Résout chaque adversaire en conservant le rosterId comme id, pour que les
                    // comparaisons de score du PDF (war.teamOpponent.contains(team.id)) matchent.
                    val teamOpponent = war.teamOpponent.mapNotNull { opponentId ->
                        databaseRepository.getTeam(opponentId)?.copy(id = opponentId)
                    }.firstOrNull()
                    val (teamWin, teamLose) = when (warDetails.scoreHostWithPenalties >= warDetails.scoreOpponentWithPenalties) {
                        true -> teamHost to teamOpponent
                        else -> teamOpponent to teamHost
                    }
                    pdfRepository.generatePdf(warDetails, teamWin, teamLose, hostScores(war), opponentScores)
                }
            }
            .flatMapLatest { pdfRepository.write(it, filename) }
            .onEach { uri ->
                uri?.let { _share.emit(SharedTab(it, PDFRepositoryInterface.MIME_JPEG)) }
                    ?: _toast.emit(TabMessage(R.string.tab_write_error))
            }
            .launchIn(scope = viewModelScope)
    }

    /** Scores adverses saisis si leur somme vaut le score adverse de la war ; sinon toast de l'écart et `null`. */
    private suspend fun opponentScores(details: WarDetails, players: List<String>, scores: List<String>): List<PlayerScoreForTab>? {
        val values = scores.map { it.toIntOrNull() ?: 0 }
        val diff = values.sum() - details.scoreOpponent
        return when {
            diff == 0 -> players.mapIndexed { index, player ->
                PlayerScoreForTab(player.displayName, values.getOrElse(index) { 0 }, 0)
            }
            diff > 0 -> {
                _toast.emit(TabMessage(R.string.tab_scores_too_many, diff))
                null
            }
            else -> {
                _toast.emit(TabMessage(R.string.tab_scores_missing, -diff))
                null
            }
        }
    }

    private suspend fun hostScores(war: War): List<PlayerScoreForTab> =
        war.withPlayersList(databaseRepository, firebaseRepository, dataStoreRepository)
            .map { PlayerScoreForTab(it, war.tracks.size) }

    /** Équipe hôte au nom/tag du roster de la war (rule 12) ; id = rosterId pour l'appariement des pénalités. */
    private suspend fun hostTeam(war: War): TeamEntity? =
        dataStoreRepository.mkcTeam.firstOrNull()?.let { team ->
            val roster = team.rosters.singleOrNull { it.id.toString() == war.teamHost }
            TeamEntity(team).copy(id = war.teamHost, name = roster?.name ?: team.name, tag = roster?.tag ?: team.tag)
        }

    private fun tabFileName() = "war_" + Date().time

    /*
    fun generateDetailedPdf(players: List<String>, scores: List<String>) {
        val filename = "war_" + Date().time.toString()

        flowOf(details)
            .mapNotNull { it?.war }
            .mapNotNull {
                val details = WarDetails(it)
                if (scores.mapNotNull { it.toIntOrNull() }.sum() == details.scoreOpponent) {
                    val playerScores = it.withPlayersList(databaseRepository, firebaseRepository).map { PlayerScoreForTab(it) }
                    val opponentScores = players.mapIndexed { index, player -> PlayerScoreForTab(player.displayName, scores[index].toInt(), 0) }
                    val teamHost = dataStoreRepository.mkcTeam.map { TeamEntity(it) }.firstOrNull()
                    val teamOpponent = databaseRepository.getTeam(it.teamOpponent).firstOrNull()

                    val policy = ThreadPolicy.Builder().permitAll().build()

                    StrictMode.setThreadPolicy(policy)
                    val teamHostLogo = try {
                        BitmapFactory.decodeStream(URL("https://mkcentral.com${teamHost?.logo}").openConnection().getInputStream())
                    } catch (e: Exception) {
                        null
                    }
                    val teamOpponentLogo = try {
                        BitmapFactory.decodeStream(URL("https://mkcentral.com${teamOpponent?.logo}").openConnection().getInputStream())
                    } catch (e: Exception) {
                        null
                    }
                    pdfRepository.generateDetailedPdf(details, teamHost, teamOpponent, playerScores, opponentScores, teamHostLogo, teamOpponentLogo)
                } else {
                    val diff = scores.mapNotNull { it.toIntOrNull() }.sum() - details.scoreOpponent
                    val secondaryLabel = when  {
                        diff > 0 -> "$diff points en trop"
                        else -> "${0-diff} points manquants"
                    }
                    _toast.emit("Les scores des joueurs sont incorrects ($secondaryLabel)")
                    null
                }
            }
            .flatMapLatest { pdfRepository.write(it, filename) }
            .onEach { uri -> _uri.emit(uri) }
            .launchIn(scope = viewModelScope)
    }

     */

}