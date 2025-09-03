package fr.harmoniamk.statsmkworld.screen.editTab

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.withPlayersList
import fr.harmoniamk.statsmkworld.model.local.PlayerScoreForTab
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.PDFRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import java.net.URL
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = EditTabViewModel.Factory::class)
class EditTabViewModel @AssistedInject constructor(
    @Assisted val details: WarDetails?,
    private val databaseRepository: DatabaseRepositoryInterface,
    private val firebaseRepository: FirebaseRepositoryInterface,
    private val pdfRepository: PDFRepositoryInterface
): ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(@Assisted details: WarDetails?): EditTabViewModel
    }


    private val _rows = MutableStateFlow(6)
    private val _uri = MutableSharedFlow<Uri?>()
    private val _toast = MutableSharedFlow<String?>()

    val rows = _rows.asStateFlow()
    val uri = _uri.asSharedFlow()
    val toast = _toast.asSharedFlow()

    fun onManageRows(isAdding: Boolean) {
        when (isAdding) {
            true -> _rows.value += 1
            else -> _rows.value -= 1
        }
    }

    fun generateClassicPdf(players: List<String>, scores: List<String>) {
        val filename = "war_" + Date().time.toString()
        flowOf(details)
            .mapNotNull { it?.war }
            .mapNotNull {
                if (scores.mapNotNull { it.toIntOrNull() }.sum() == details?.scoreOpponent) {
                    var teamWin: TeamEntity? = null
                    var teamLose: TeamEntity? = null
                    val playerScores = it.withPlayersList(databaseRepository, firebaseRepository).map { PlayerScoreForTab(it) }
                    val opponentScores = players.mapIndexed { index, player -> PlayerScoreForTab(player, scores[index].toInt(), 0) }
                    val teamHost = databaseRepository.getTeam(it.teamHost).firstOrNull()
                    val teamOpponent = databaseRepository.getTeam(it.teamOpponent).firstOrNull()
                    if (details.scoreHostWithPenalties >= details.scoreOpponentWithPenalties) {
                        teamWin = teamHost
                        teamLose = teamOpponent
                    }
                    else {
                        teamWin = teamOpponent
                        teamLose = teamHost
                    }
                    pdfRepository.generatePdf(details, teamWin, teamLose, playerScores, opponentScores)
                } else {
                    val diff = scores.mapNotNull { it.toIntOrNull() }.sum() - (details?.scoreOpponent ?: 0)
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

    fun generateDetailedPdf(players: List<String>, scores: List<String>) {
        val filename = "war_" + Date().time.toString()

        flowOf(details)
            .mapNotNull { it?.war }
            .mapNotNull {
                val details = WarDetails(it)
                if (scores.mapNotNull { it.toIntOrNull() }.sum() == details.scoreOpponent) {
                    val playerScores = it.withPlayersList(databaseRepository, firebaseRepository).map { PlayerScoreForTab(it) }
                    val opponentScores = players.mapIndexed { index, player -> PlayerScoreForTab(player, scores[index].toInt(), 0) }
                    val teamHost = databaseRepository.getTeam(it.teamHost).firstOrNull()
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

}