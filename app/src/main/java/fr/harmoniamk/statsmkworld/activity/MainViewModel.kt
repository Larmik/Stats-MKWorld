package fr.harmoniamk.statsmkworld.activity

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.BuildConfig
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.RemoteConfigRepositoryInterface
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(dataStoreRepository: DataStoreRepositoryInterface, remoteConfigRepository: RemoteConfigRepositoryInterface) : ViewModel() {

    data class State(
        val startDestination: String? = null,
        val code: String = "",
        val currentPage: Int? = null,
        val needUpdate: Boolean = false
    )

    private val _state = MutableStateFlow(State())

    val state = flowOf(Unit)
        .map {
            val player = dataStoreRepository.mkcPlayer.firstOrNull()
            val currentPage = dataStoreRepository.page.firstOrNull()
            delay(1000)
            when {
                remoteConfigRepository.minimumVersion > BuildConfig.VERSION_CODE -> _state.value.copy(needUpdate = true)
                player?.id != 0L  -> _state.value.copy(startDestination = "Home")
                else -> _state.value.copy(currentPage = currentPage, startDestination = "Signup")
            }
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.Lazily, _state.value)

    fun processIntent(intent: Intent) {
        intent.dataString?.split("?")?.lastOrNull()?.split("=")?.lastOrNull()?.let { code ->
            _state.value = _state.value.copy(code = code, startDestination = "Signup")
        }
    }
}

