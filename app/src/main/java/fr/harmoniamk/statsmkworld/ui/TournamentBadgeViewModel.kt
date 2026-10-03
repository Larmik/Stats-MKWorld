package fr.harmoniamk.statsmkworld.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Logos officiels en cache (#152) pour [TournamentBadge] : une instance par écran (clé par défaut
 * de `hiltViewModel`), partagée par tous ses badges, quelle que soit la longueur de la liste.
 */
@HiltViewModel
class TournamentBadgeViewModel @Inject constructor(databaseRepository: DatabaseRepositoryInterface) : ViewModel() {
    /** Chemin relatif du logo par `Tournament.name` ; absent tant que la synchro n'a pas abouti. */
    val logos: StateFlow<Map<String, String?>> = databaseRepository.getTournaments()
        .map { tournaments -> tournaments.associate { it.id to it.logo } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}
