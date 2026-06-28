package fr.harmoniamk.statsmkworld.datasource.local

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.database.MKDatabase
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface TeamLocalDataSourceInterface {
    fun getAll(): Flow<List<TeamEntity>>
    fun getById(id: String) : Flow<TeamEntity?>
    suspend fun bulkInsert(teams: List<TeamEntity>)
    suspend fun insert(team: TeamEntity)
    suspend fun clear()
}

@FlowPreview
@ExperimentalCoroutinesApi
@Module
@InstallIn(SingletonComponent::class)
interface TeamLocalDataSourceModule {
    @Singleton
    @Binds
    fun bind(impl: TeamLocalDataSource): TeamLocalDataSourceInterface
}

@FlowPreview
@ExperimentalCoroutinesApi
class TeamLocalDataSource @Inject constructor(@ApplicationContext private val context: Context) : TeamLocalDataSourceInterface {

    private val dao = MKDatabase.getInstance(context).teamDao()

    override fun getAll(): Flow<List<TeamEntity>> = dao.getAll()
    override fun getById(id: String) = dao.getById(id)
    override suspend fun bulkInsert(teams: List<TeamEntity>) = dao.bulkInsert(teams)
    override suspend fun insert(team: TeamEntity) = dao.insert(team)
    override suspend fun clear() = dao.clear()

}