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
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

interface WarLocalDataSourceInterface {
    fun getAll(): Flow<List<WarEntity>>
    fun getById(id: String): Flow<WarEntity>
    fun insert(wars: List<WarEntity>): Flow<Unit>
    fun insert(war: WarEntity): Flow<Unit>
    fun delete(war: WarEntity): Flow<Unit>
    fun clear(): Flow<Unit>
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

    override fun getById(id: String): Flow<WarEntity> = dao.getById(id)

    override fun insert(wars: List<WarEntity>): Flow<Unit> = flow { emit(dao.bulkInsert(wars)) }

    override fun insert(war: WarEntity): Flow<Unit> = flow {
        emit(dao.insert(war))
    }

    override fun delete(war: WarEntity): Flow<Unit> = flow {
        emit(dao.delete(war))
    }

    override fun clear(): Flow<Unit> = flow { emit(dao.clear()) }

}