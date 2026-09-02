package fr.harmoniamk.statsmkworld.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.SeasonEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import fr.harmoniamk.statsmkworld.datasource.local.PlayerLocalDataSourceInterface
import fr.harmoniamk.statsmkworld.datasource.local.SeasonLocalDataSourceInterface
import fr.harmoniamk.statsmkworld.datasource.local.TeamLocalDataSourceInterface
import fr.harmoniamk.statsmkworld.datasource.local.WarLocalDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface DatabaseRepositoryInterface {
    fun getPlayers(): Flow<List<PlayerEntity>>
    fun getPlayer(playerId: String): Flow<PlayerEntity?>
    suspend fun writePlayer(player: PlayerEntity)
    suspend fun clearPlayers()

    suspend fun updateUser(id: String, currentWar: String)
    suspend fun updateUser(id: String, role: Int)
    suspend fun updateUserRoster(id: String, rosterId: String)

    suspend fun addAlly(entity: PlayerEntity)

    fun getTeams(): Flow<List<TeamEntity>>
    suspend fun getTeam(id: String): TeamEntity?
    suspend fun writeTeams(list: List<TeamEntity>)
    suspend fun clearTeams()

    fun getWars(): Flow<List<WarEntity>>
    fun getWar(id: String?): Flow<WarEntity?>
    suspend fun writeWars(list: List<WarEntity>)
    suspend fun writeWar(war: WarEntity)
    suspend fun clearWars()

    fun getSeasons(): Flow<List<SeasonEntity>>
    suspend fun getCurrentSeason(): SeasonEntity?
    suspend fun writeSeasons(list: List<SeasonEntity>)
    suspend fun writeSeason(season: SeasonEntity)
    suspend fun clearSeasons()
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
    private val warLocalDataSource: WarLocalDataSource,
    private val seasonLocalDataSource: SeasonLocalDataSourceInterface,
) : DatabaseRepositoryInterface {

    override fun getPlayers(): Flow<List<PlayerEntity>> = playerLocalDataSource.getAll().flowOn(Dispatchers.IO)
    override fun getPlayer(playerId: String): Flow<PlayerEntity?> = playerLocalDataSource.getById(playerId)
    override suspend fun writePlayer(player: PlayerEntity) = withContext(Dispatchers.IO) { playerLocalDataSource.insert(player) }
    override suspend fun clearPlayers() = withContext(Dispatchers.IO) { playerLocalDataSource.clear() }

    override suspend fun updateUser(id: String, currentWar: String) = withContext(Dispatchers.IO) { playerLocalDataSource.setCurrentWar(id, currentWar) }
    override suspend fun updateUser(id: String, role: Int) = withContext(Dispatchers.IO) { playerLocalDataSource.setRole(id, role) }
    override suspend fun updateUserRoster(id: String, rosterId: String) = withContext(Dispatchers.IO) { playerLocalDataSource.setRosterId(id, rosterId) }

    override suspend fun addAlly(entity: PlayerEntity) = withContext(Dispatchers.IO) { playerLocalDataSource.upsert(entity) }

    override fun getTeams(): Flow<List<TeamEntity>> = teamLocalDataSource.getAll().flowOn(Dispatchers.IO)

    // Résout un identifiant d'équipe adverse : d'abord par teamId (clé primaire),
    // à défaut par le rosterId (War.teamOpponent contient un rosterId depuis le
    // passage à la granularité roster) en cherchant l'équipe dont l'un des rosters
    // porte cet id → on remonte à l'équipe parente pour l'affichage/regroupement.
    override suspend fun getTeam(id: String): TeamEntity? = withContext(Dispatchers.IO) {
        val teams = teamLocalDataSource.getAll().firstOrNull().orEmpty()
        teams.firstOrNull { it.id == id } ?: teams.firstOrNull { team -> team.rosters.any { it.id == id } }
    }
    override suspend fun writeTeams(list: List<TeamEntity>) = withContext(Dispatchers.IO) { teamLocalDataSource.bulkInsert(list) }
    override suspend fun clearTeams() = withContext(Dispatchers.IO) { teamLocalDataSource.clear() }

    override fun getWars(): Flow<List<WarEntity>> = warLocalDataSource.getAll().flowOn(Dispatchers.IO)
    override fun getWar(id: String?): Flow<WarEntity?> = id?.let { warLocalDataSource.getById(it).flowOn(Dispatchers.IO) } ?: flowOf(null)
    override suspend fun writeWars(list: List<WarEntity>) = withContext(Dispatchers.IO) { warLocalDataSource.insert(list) }
    override suspend fun writeWar(war: WarEntity) = withContext(Dispatchers.IO) { warLocalDataSource.insert(war) }
    override suspend fun clearWars() = withContext(Dispatchers.IO) { warLocalDataSource.clear() }

    override fun getSeasons(): Flow<List<SeasonEntity>> = seasonLocalDataSource.getAll().flowOn(Dispatchers.IO)
    override suspend fun getCurrentSeason(): SeasonEntity? = withContext(Dispatchers.IO) { seasonLocalDataSource.getCurrent() }
    override suspend fun writeSeasons(list: List<SeasonEntity>) = withContext(Dispatchers.IO) { seasonLocalDataSource.insert(list) }
    override suspend fun writeSeason(season: SeasonEntity) = withContext(Dispatchers.IO) { seasonLocalDataSource.insert(season) }
    override suspend fun clearSeasons() = withContext(Dispatchers.IO) { seasonLocalDataSource.clear() }

}