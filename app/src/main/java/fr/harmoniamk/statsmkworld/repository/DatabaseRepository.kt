package fr.harmoniamk.statsmkworld.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import fr.harmoniamk.statsmkworld.datasource.local.PlayerLocalDataSourceInterface
import fr.harmoniamk.statsmkworld.datasource.local.TeamLocalDataSourceInterface
import fr.harmoniamk.statsmkworld.datasource.local.WarLocalDataSourceInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

interface DatabaseRepositoryInterface {
    fun getPlayers(): Flow<List<PlayerEntity>>
    fun getPlayer(playerId: String): Flow<PlayerEntity>
    fun writePlayer(player: PlayerEntity): Flow<Unit>
    fun clearPlayers(): Flow<Unit>

    fun updateUser(id: String, currentWar: String): Flow<Unit>
    fun updateUser(id: String, role: Int): Flow<Unit>
    fun updateUser(id: String, isAlly: Boolean): Flow<Unit>

    fun addAlly(entity: PlayerEntity): Flow<Unit>

    fun getTeams(): Flow<List<TeamEntity>>
    fun getTeam(id: String): Flow<TeamEntity?>
    fun writeTeams(list: List<TeamEntity>): Flow<Unit>
    fun clearTeams(): Flow<Unit>

    fun getWars(): Flow<List<WarEntity>>
    fun getWar(id: String?): Flow<WarEntity?>
    fun writeWars(list: List<WarEntity>): Flow<Unit>
    fun writeWar(war: WarEntity): Flow<Unit>
    fun clearWars(): Flow<Unit>
}

@FlowPreview
@ExperimentalCoroutinesApi
@Module
@InstallIn(SingletonComponent::class)
interface DatabaseRepositoryModule {
    @Binds
    @Singleton
    fun bindRepository(impl: DatabaseRepository): DatabaseRepositoryInterface
}

@FlowPreview
@ExperimentalCoroutinesApi
class DatabaseRepository @Inject constructor(
    private val playerLocalDataSource: PlayerLocalDataSourceInterface,
    private val teamLocalDataSource: TeamLocalDataSourceInterface,
    private val warLocalDataSource: WarLocalDataSourceInterface,
) : DatabaseRepositoryInterface {

    override fun getPlayers(): Flow<List<PlayerEntity>> = playerLocalDataSource.getAll().flowOn(Dispatchers.IO)
    override fun getPlayer(playerId: String): Flow<PlayerEntity> = playerLocalDataSource.getById(playerId)
    override fun writePlayer(player: PlayerEntity): Flow<Unit> = playerLocalDataSource.insert(player).flowOn(Dispatchers.IO)
    override fun clearPlayers(): Flow<Unit> = playerLocalDataSource.clear()

    override fun updateUser(id: String, currentWar: String): Flow<Unit> = playerLocalDataSource.setCurrentWar(id, currentWar)
    override fun updateUser(id: String, role: Int): Flow<Unit> = playerLocalDataSource.setRole(id, role)
    override fun updateUser(id: String, isAlly: Boolean): Flow<Unit> = playerLocalDataSource.setAlly(id, isAlly)

    override fun addAlly(entity: PlayerEntity): Flow<Unit> = playerLocalDataSource.upsert(entity)

    override fun getTeams(): Flow<List<TeamEntity>> = teamLocalDataSource.getAll().flowOn(Dispatchers.IO)
    override fun getTeam(id: String): Flow<TeamEntity?> = teamLocalDataSource.getById(id).flowOn(Dispatchers.IO)
    override fun writeTeams(list: List<TeamEntity>): Flow<Unit> = teamLocalDataSource.bulkInsert(list).flowOn(Dispatchers.IO)
    override fun clearTeams(): Flow<Unit> = teamLocalDataSource.clear()

    override fun getWars(): Flow<List<WarEntity>> = warLocalDataSource.getAll().flowOn(Dispatchers.IO)
    override fun getWar(id: String?): Flow<WarEntity?> = id?.let { warLocalDataSource.getById(it).flowOn(Dispatchers.IO) } ?: flowOf(null)
    override fun writeWars(list: List<WarEntity>) = warLocalDataSource.insert(list).flowOn(Dispatchers.IO)
    override fun writeWar(war: WarEntity) = warLocalDataSource.insert(war).flowOn(Dispatchers.IO)
    override fun clearWars(): Flow<Unit> = warLocalDataSource.clear()

}