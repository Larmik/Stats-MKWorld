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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

interface TeamLocalDataSourceInterface {
    fun getAll(): Flow<List<TeamEntity>>
    fun getById(id: String) : Flow<TeamEntity>
    fun bulkInsert(teams: List<TeamEntity>): Flow<Unit>
    fun insert(team: TeamEntity): Flow<Unit>
    fun clear(): Flow<Unit>
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
    override fun bulkInsert(teams: List<TeamEntity>): Flow<Unit> = flow { emit(dao.bulkInsert(teams)) }
    override fun insert(team: TeamEntity): Flow<Unit> = flow { emit(dao.insert(team)) }
    override fun clear(): Flow<Unit> = flow { emit(dao.clear())}

}