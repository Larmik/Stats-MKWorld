package fr.harmoniamk.statsmkworld.datasource.local

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.database.MKDatabase
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface WarLocalDataSourceInterface {
    fun getAll(): Flow<List<WarEntity>>
    fun getById(id: String): Flow<WarEntity?>
    suspend fun insert(wars: List<WarEntity>)
    suspend fun insert(war: WarEntity)
    suspend fun delete(war: WarEntity)
    suspend fun clear()
}

@FlowPreview
@ExperimentalCoroutinesApi
@Module
@InstallIn(SingletonComponent::class)
interface WarLocalDataSourceModule {
    @Binds
    @Singleton
    fun bind(impl: WarLocalDataSource): WarLocalDataSourceInterface
}

@FlowPreview
@ExperimentalCoroutinesApi
class WarLocalDataSource @Inject constructor(@ApplicationContext private val context: Context) : WarLocalDataSourceInterface {

    private val dao = MKDatabase.getInstance(context).warDao()

    override fun getAll(): Flow<List<WarEntity>> = dao.getAll()

    override fun getById(id: String): Flow<WarEntity?> = dao.getById(id)

    override suspend fun insert(wars: List<WarEntity>) = dao.bulkInsert(wars)

    override suspend fun insert(war: WarEntity) = dao.insert(war)

    override suspend fun delete(war: WarEntity) = dao.delete(war)

    override suspend fun clear() = dao.clear()

}