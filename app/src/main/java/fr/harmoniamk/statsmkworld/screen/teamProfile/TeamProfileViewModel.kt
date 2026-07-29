package fr.harmoniamk.statsmkworld.screen.teamProfile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.datasource.network.MKCentralDataSourceInterface
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.model.firebase.User
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayer
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayerList
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeam
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = TeamProfileViewModel.Factory::class)
class TeamProfileViewModel @AssistedInject constructor(
    @Assisted val id: String,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val mkCentralDataSource: MKCentralDataSourceInterface,
    private val firebaseRepository: FirebaseRepositoryInterface,
    private val databaseRepository: DatabaseRepositoryInterface) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(id: String): TeamProfileViewModel
    }

    /**
     * Membre d'une équipe pour l'affichage (ligne `lrow` de la maquette) : identité,
     * **rattachement au roster** ([rosterId]/[rosterName], pour regrouper par roster
     * quand l'équipe en a plusieurs), couleur du roster (pastille), [role] **réel**
     * (valeur du nœud Firebase `users` : 2 = Leader, 1 = Admin, 0 = Membre — un allié
     * vaut toujours 0), et [avatarUrl] (photo MKCentral déjà préfixée, ou `null` →
     * fallback initiales).
     */
    data class MemberInfo(
        val playerId: String,
        val name: String,
        val rosterId: String,
        val rosterName: String,
        val rosterColor: Long,
        val role: Int,
        val avatarUrl: String?
    )

    data class State(
        val team: MKCTeam? = null,
        val members: List<MemberInfo> = listOf(),
        val allyList: List<PlayerEntity> = listOf(),
        val playerList: List<MKCPlayer> = listOf(),
        val addAllyVisible: Boolean = false
    )

    private val _state = MutableSharedFlow<State>()
    private val _onDismiss = MutableSharedFlow<Unit>()

    val onDismiss = _onDismiss.asSharedFlow()

    private suspend fun searchPlayers(page: Int, term: String): Pair<Int?, MKCPlayerList?> {
        val players = mkCentralDataSource.searchPlayers(page, term).successResponse
        return Pair(players?.pageCount, players?.playerList)
    }

    fun onSearchPlayers(term: String) {
        viewModelScope.launch {
            if (term.length >= 3) {
                var page = 1
                val playerList = mutableListOf<MKCPlayer>()
                _state.emit(state.value.copy(playerList = listOf()))
                var player: Pair<Int?, List<MKCPlayer>?>? = searchPlayers(page, term)

                playerList.addAll(player?.second.orEmpty())
                while (page < (player?.first ?: 0)) {
                    page++
                    player = searchPlayers(page, term)
                    playerList.addAll(player?.second?.filterNot { state.value.allyList.map { it.id }.contains(it.id.toString()) }.orEmpty())
                }
                _state.emit(state.value.copy(playerList = playerList))
            } else _state.emit(state.value.copy(playerList = listOf()))
        }

    }

    val state =  flowOf(Unit)
        .mapNotNull {
            when (id) {
                "me" -> dataStoreRepository.mkcTeam.firstOrNull()
                else -> mkCentralDataSource.getTeam(id).successResponse
            }
        }
        .map { team ->
            val allyList = when (id) {
                "me" -> databaseRepository.getPlayers().firstOrNull()?.filter { it.rosterId == "-1" }.orEmpty()
                else -> listOf()
            }
            val buttonVisible = (firebaseRepository
                .getUser(team.id.toString(), dataStoreRepository.mkcPlayer.firstOrNull()?.id.toString())
                ?.role ?: 0) > 0
            State(
                team = team,
                members = resolveMembers(team),
                addAllyVisible = buttonVisible,
                allyList = allyList
            )
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())

    /**
     * Membres de l'équipe (rosters mkworld) pour l'affichage : rôle **réel** + avatar.
     *
     * - **Rôle** : pour mon équipe (`id == "me"`), la valeur du nœud Firebase `users`
     *   (2 = Leader, 1 = Admin, 0 = Membre) — vraie source, comme la règle « changer
     *   le rôle ». Pour une équipe publique (pas de nœud `users` côté MKCentral), on
     *   retombe sur les indicateurs MKCentral leader/manager du roster.
     * - **Avatar** : photo MKCentral du joueur (`getPlayer`), récupérée en parallèle,
     *   préfixée par l'hôte MKCentral ; `null` si absente (fallback initiales).
     */
    private suspend fun resolveMembers(team: MKCTeam): List<MemberInfo> = coroutineScope {
        val rosters = team.rosters.filter { it.game == "mkworld" }
        val firebaseRoles = when (id) {
            "me" -> firebaseRepository.getUsers(team.id.toString()).associate { it.id to it.role }
            else -> emptyMap()
        }
        rosters.flatMap { roster -> roster.players.map { roster to it } }
            .map { (roster, player) ->
                async {
                    val role = firebaseRoles[player.playerId]
                        ?: when {
                            player.leader -> 2
                            player.manager -> 1
                            else -> 0
                        }
                    val avatar = mkCentralDataSource.getPlayer(player.playerId).successResponse
                        ?.userSettings?.avatar?.let { "https://mkcentral.com$it" }
                    MemberInfo(
                        playerId = player.playerId,
                        name = player.name,
                        rosterId = roster.id.toString(),
                        rosterName = roster.name,
                        rosterColor = roster.color,
                        role = role,
                        avatarUrl = avatar
                    )
                }
            }
            .awaitAll()
    }

    fun addAlly(player: MKCPlayer) {
        dataStoreRepository.mkcTeam
            .onEach { team ->
                val user = User(player)
                firebaseRepository.writeAlly(team.id.toString(), user)
            }
            .map { PlayerEntity(player = player, isAlly = true) }
            .onEach { databaseRepository.addAlly(it) }
            .onEach {
                val allyList = when (id) {
                    "me" -> databaseRepository.getPlayers().firstOrNull()?.filter { it.rosterId == "-1" }.orEmpty()
                    else -> listOf()
                }
                _state.emit(state.value.copy(allyList = allyList))
                _onDismiss.emit(Unit)
            }
            .launchIn(viewModelScope)

    }
}
