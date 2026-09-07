package fr.harmoniamk.statsmkworld.datasource.local

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.database.MKDatabase
import fr.harmoniamk.statsmkworld.database.entities.SeasonEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface SeasonLocalDataSourceInterface {
    fun getAll(): Flow<List<SeasonEntity>>
    suspend fun insert(seasons: List<SeasonEntity>)
    suspend fun clear()
}

@FlowPreview
@ExperimentalCoroutinesApi
@Module
@InstallIn(SingletonComponent::class)
interface SeasonLocalDataSourceModule {
    @Binds
    @Singleton
    fun bind(impl: SeasonLocalDataSource): SeasonLocalDataSourceInterface
}

@FlowPreview
@ExperimentalCoroutinesApi
class SeasonLocalDataSource @Inject constructor(@ApplicationContext private val context: Context) : SeasonLocalDataSourceInterface {

    private val dao = MKDatabase.getInstance(context).seasonDao()

    override fun getAll(): Flow<List<SeasonEntity>> = dao.getAll()

    override suspend fun insert(seasons: List<SeasonEntity>) = dao.bulkInsert(seasons)

    override suspend fun clear() = dao.clear()

}
