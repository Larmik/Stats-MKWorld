package fr.harmoniamk.statsmkworld.usecase

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import fr.harmoniamk.statsmkworld.datasource.network.MKCentralDataSourceInterface
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

interface FetchUseCaseInterface {
    fun fetchPlayer(playerId: String): Flow<MKCPlayer>
    fun fetchTeam(teamId: String): Flow<MKCTeam>
    fun fetchAllies(teamId: String): Flow<Unit>
    fun fetchTeams(): Flow<String>
    fun fetchWars(teamId: String): Flow<Unit>
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

    override fun fetchPlayer(playerId: String): Flow<MKCPlayer> =
        mkCentralDataSource.getPlayer(playerId)
            .mapNotNull { it.successResponse }
            .onEach { dataStoreRepository.setMKCPlayer(it) }

    override fun fetchTeam(teamId: String): Flow<MKCTeam> = mkCentralDataSource.getTeam(teamId)
        .filterNotNull()
        .onEach {
            dataStoreRepository.setMKCTeam(it)
            databaseRepository.clearPlayers().firstOrNull()
            it.rosters.firstOrNull { it.game == "mkworld" }?.players?.let {
                databaseRepository.writePlayers(it.map {
                    val role = when (it.leader || it.manager) {
                        true -> 2
                        else -> 0
                    }
                    return@map PlayerEntity(player = it, role = role)
                }).firstOrNull()
                databaseRepository.getPlayers().firstOrNull()?.forEach { player ->
                    firebaseRepository.getUser(teamId, player.id).firstOrNull()?.let {
                        databaseRepository.updateUser(player.id, role = it.role).firstOrNull()
                        databaseRepository.updateUser(
                            player.id,
                            currentWar = it.currentWar.orEmpty()
                        ).firstOrNull()
                    }
                }
            }
        }

    override fun fetchAllies(teamId: String): Flow<Unit> = dataStoreRepository.mkcTeam
        .flatMapLatest { firebaseRepository.getAllies(it.id.toString()) }
        .map { allies ->
            val players = databaseRepository.getPlayers().firstOrNull().orEmpty()
            allies.forEach { allyId ->
                when (players.map { it.id }.contains(allyId)) {
                    true -> {
                        databaseRepository.getPlayer(allyId).firstOrNull()?.let { player ->
                            firebaseRepository.deleteAlly(teamId, allyId).firstOrNull()
                            databaseRepository.updateUser(allyId, false).firstOrNull()
                        }
                    }

                    else -> {
                        mkCentralDataSource.getPlayer(allyId).firstOrNull()?.let { response ->
                            response.successResponse?.let {
                                databaseRepository.addAlly(PlayerEntity(it, isAlly = true))
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
        val firstResponseMK8 = getMK8Teams(teamPage).firstOrNull()
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
        emit(dataStoreRepository.mkcTeam.firstOrNull()?.id.toString())
    }

    override fun fetchWars(teamId: String): Flow<Unit> = firebaseRepository.getWars(teamId)
        .map {
            val existingWars = databaseRepository.getWars().firstOrNull().orEmpty()
            it.forEach { war ->
                if (!existingWars.map { it.id }.contains(war.id.toString()))
                    databaseRepository.writeWar(WarEntity(war)).firstOrNull()
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