package fr.harmoniamk.statsmkworld.screen.warList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import fr.harmoniamk.statsmkworld.extension.format
import fr.harmoniamk.statsmkworld.extension.get
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.zip
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class WarListViewModel @Inject constructor(databaseRepository: DatabaseRepositoryInterface) :
    ViewModel() {

    data class State(
        val wars: List<Pair<String, List<WarDetails>>> = listOf()
    )

    val state = databaseRepository.getWars()
        .map {
            it.map { War(it) }
                .map { WarDetails(it) }
                .sortedByDescending { it.war.id }
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
        }
        .map { State(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())

}