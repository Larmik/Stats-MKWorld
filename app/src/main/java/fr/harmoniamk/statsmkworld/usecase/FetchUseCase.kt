package fr.harmoniamk.statsmkworld.usecase

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import fr.harmoniamk.statsmkworld.datasource.network.MKCentralDataSourceInterface
import fr.harmoniamk.statsmkworld.model.firebase.Tag
import fr.harmoniamk.statsmkworld.model.firebase.User
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayer
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeam
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.zip
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.firstOrNull
import kotlin.coroutines.CoroutineContext

interface FetchUseCaseInterface {
    fun fetchData(playerId: String): Flow<Unit>
    fun fetchPlayer(playerId: String): Flow<MKCPlayer>
    fun fetchTeam(teamId: String): Flow<MKCTeam>
    fun fetchAllies(teamId: String): Flow<Unit>
    fun fetchTeams(): Flow<String>
    fun fetchWars(teamId: String): Flow<Unit>
    fun fetchTags(): Flow<Unit>
    fun manageTransferts(): Flow<Unit>
}

@FlowPreview
@ExperimentalCoroutinesApi
@Module
@InstallIn(SingletonComponent::class)
interface FetchUseCaseModule {
    @Binds
    @Singleton
    fun bindRepository(impl: FetchUseCase): FetchUseCaseInterface
}

@FlowPreview
@ExperimentalCoroutinesApi
class FetchUseCase @Inject constructor(
    private val firebaseRepository: FirebaseRepositoryInterface,
    private val mkCentralDataSource: MKCentralDataSourceInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val databaseRepository: DatabaseRepositoryInterface
) : FetchUseCaseInterface, CoroutineScope {
    override fun fetchData(playerId: String): Flow<Unit> = fetchPlayer(playerId)
            .mapNotNull { it.rosters?.firstOrNull { it.game == "mkworld" } }
            .flatMapLatest { fetchTeam(it.teamID.toString()) }
            .flatMapLatest { fetchAllies(it.id.toString()) }
            .flatMapLatest { fetchTeams() }
            .flatMapLatest { dataStoreRepository.mkcTeam }
            .mapNotNull { it.rosters.filter { it.game == "mkworld" }.map { it.id.toString() } }
            .flatMapLatest { ids ->
                val flows = ids.map { fetchWars(it) }
                merge(*flows.toTypedArray())
            }
            .onEach { dataStoreRepository.setLastUpdate(Date().time) }

    override fun fetchPlayer(playerId: String): Flow<MKCPlayer> =
        mkCentralDataSource.getPlayer(playerId)
            .mapNotNull { it.successResponse }
            .onEach { dataStoreRepository.setMKCPlayer(it) }

    override fun fetchTeam(teamId: String): Flow<MKCTeam> = mkCentralDataSource.getTeam(teamId)
        .filterNotNull()
        .onEach {
            dataStoreRepository.setMKCTeam(it)
            databaseRepository.clearPlayers().firstOrNull()
            it.rosters.filter { it.game == "mkworld" }.forEach { roster ->
                roster.players.forEach { player ->
                    val user = firebaseRepository.getUser(teamId, player.playerId).firstOrNull()
                    val playerEntity = PlayerEntity(player = player, role = user?.role ?: 0, currentWar = user?.currentWar.orEmpty(), discordId = user?.discordId.orEmpty(), rosterId = roster.id.toString())
                    databaseRepository.writePlayer(playerEntity).firstOrNull()
                }
            }
        }

    override fun fetchAllies(teamId: String): Flow<Unit> = dataStoreRepository.mkcTeam
        .flatMapLatest { firebaseRepository.getAllies(it.id.toString()) }
        .map { allies ->
            val players = databaseRepository.getPlayers().firstOrNull().orEmpty()
            allies.forEach { ally ->
                when (players.map { it.id }.contains(ally.id)) {
                    true -> {
                        databaseRepository.getPlayer(ally.id).firstOrNull()?.let { player ->
                            firebaseRepository.deleteAlly(teamId, ally.id).firstOrNull()
                            databaseRepository.updateUserRoster(ally.id, player.rosterId).firstOrNull()
                        }
                    }

                    else -> {
                        mkCentralDataSource.getPlayer(ally.id).firstOrNull()?.let { response ->
                            response.successResponse?.let {
                                databaseRepository.addAlly(PlayerEntity(player = it, isAlly = true))
                                    .firstOrNull()
                            }
                        }
                    }
                }
            }
        }

    override fun fetchTeams(): Flow<String> = flow {
        val teams = mutableListOf<TeamEntity>()
        var teamPage = 1
        var teamPageMK8 = 1
        val firstResponse = getTeams(teamPage).firstOrNull()
        val firstResponseMK8 = getMK8Teams(teamPageMK8).firstOrNull()
        teams.addAll(firstResponse?.second?.map {
            TeamEntity(
                id = it.id.toString(),
                name = it.name,
                tag = it.tag,
                color = it.color.toInt(),
                logo = it.logo
            )
        }.orEmpty())
        teams.addAll(firstResponseMK8?.second?.map {
            TeamEntity(
                id = it.id.toString(),
                name = it.name,
                tag = it.tag,
                color = it.color.toInt(),
                logo = it.logo
            )
        }.orEmpty())
        while (teamPage < (firstResponse?.first ?: 1)) {
            teamPage++
            val teamsToAdd = getTeams(teamPage).firstOrNull()
            teams.addAll(teamsToAdd?.second?.map {
                TeamEntity(
                    id = it.id.toString(),
                    name = it.name,
                    tag = it.tag,
                    color = it.color.toInt(),
                    logo = it.logo
                )
            }.orEmpty())
        }
        while (teamPageMK8 < (firstResponseMK8?.first ?: 1)) {
            teamPageMK8++
            val teamsToAdd = getMK8Teams(teamPageMK8).firstOrNull()
            teams.addAll(teamsToAdd?.second?.map {
                TeamEntity(
                    id = it.id.toString(),
                    name = it.name,
                    tag = it.tag,
                    color = it.color.toInt(),
                    logo = it.logo
                )
            }.orEmpty())
        }
        databaseRepository.writeTeams(teams).firstOrNull()
        databaseRepository.writeTeams(listOf(
            TeamEntity(
                name = "6v6 Squad",
                tag = "SQ",
                id = "123456789",
                color = null,
                logo = null
            )
        )).firstOrNull()
        emit(dataStoreRepository.mkcTeam.firstOrNull()?.id.toString())
    }

    override fun fetchWars(teamId: String): Flow<Unit> = firebaseRepository.getWars(teamId)
        .map {
            databaseRepository.clearWars().firstOrNull()
            databaseRepository.writeWars(it.map { WarEntity(it) }).firstOrNull()
        }
    override fun fetchTags(): Flow<Unit> = databaseRepository.getTeams()
        .map { it.map { Tag(it.tag, it.id) } }
        .flatMapLatest { firebaseRepository.writeTags(it) }

    override fun manageTransferts(): Flow<Unit> = dataStoreRepository.mkcTeam
        .flatMapLatest { mkCentralDataSource.getTeam(it.id.toString()) }
        .zip(databaseRepository.getPlayers()) { team, players ->
            players.forEach { player ->
                if (team?.rosters?.firstOrNull { it.game == "mkworld" }?.players?.none { it.playerId == player.id } == true) {
                    mkCentralDataSource.getPlayer(player.id).firstOrNull()?.successResponse?.let { mkcPlayer ->
                        val fbUser = firebaseRepository.getUser(team.id.toString(), player.id).firstOrNull()
                        fbUser?.let {
                            firebaseRepository.writeUser(mkcPlayer.rosters?.firstOrNull { it.game == "mkworld" }?.teamID.toString(), it).firstOrNull()
                            firebaseRepository.writeAlly(team.id.toString(), it).firstOrNull()
                            databaseRepository.updateUserRoster(it.id, rosterId = "-1").firstOrNull()
                            firebaseRepository.deleteUser(team.id.toString(), it.id).firstOrNull()
                        }
                    }
                }
                if (team?.rosters?.filter { it.game == "mkworld" }?.flatMap { it.players }?.any { it.playerId == player.id } == true) {
                    mkCentralDataSource.getPlayer(player.id).firstOrNull()?.successResponse?.let { mkcPlayer ->
                        val fbUser = User(mkcPlayer)
                            firebaseRepository.writeUser(team.id.toString(), fbUser).firstOrNull()
                            firebaseRepository.deleteAlly(team.id.toString(), fbUser.id).firstOrNull()
                            databaseRepository.updateUserRoster(fbUser.id, rosterId = team.rosters.firstOrNull { it.game == "mkworld" && it.players.map { it.playerId }.contains(mkcPlayer.id.toString()) }?.id.toString()).firstOrNull()
                    }
                }
            }
        }


    private fun getTeams(page: Int) = mkCentralDataSource.getTeams(page)
        .map { Pair(it?.pageCount, it?.teamList) }
        .shareIn(this, SharingStarted.Eagerly)

    private fun getMK8Teams(page: Int) = mkCentralDataSource.getMK8Teams(page)
        .map { Pair(it?.pageCount, it?.teamList) }
        .shareIn(this, SharingStarted.Eagerly)

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

}