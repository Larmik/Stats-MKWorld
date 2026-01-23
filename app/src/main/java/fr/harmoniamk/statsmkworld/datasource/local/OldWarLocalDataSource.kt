package fr.harmoniamk.statsmkworld.datasource.local

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.database.MKDatabase
import fr.harmoniamk.statsmkworld.database.entities.OldWarEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Deprecated("24 players")
interface OldWarLocalDataSourceInterface {
    fun getAll(): Flow<List<OldWarEntity>>
    fun getById(id: String): Flow<OldWarEntity>
    fun insert(wars: List<OldWarEntity>): Flow<Unit>
    fun insert(war: OldWarEntity): Flow<Unit>
    fun delete(war: OldWarEntity): Flow<Unit>
    fun clear(): Flow<Unit>
}

@FlowPreview
@ExperimentalCoroutinesApi
@Module
@InstallIn(SingletonComponent::class)
interface OldWarLocalDataSourceModule {
    @Binds
    @Singleton
    fun bind(impl: OldWarLocalDataSource): OldWarLocalDataSourceInterface
}

@FlowPreview
@ExperimentalCoroutinesApi
@Deprecated("24 players")
class OldWarLocalDataSource @Inject constructor(@ApplicationContext private val context: Context) : OldWarLocalDataSourceInterface {

    private val dao = MKDatabase.getInstance(context).warDao()

    override fun getAll(): Flow<List<OldWarEntity>> = dao.getAll()

    override fun getById(id: String): Flow<OldWarEntity> = dao.getById(id)

    override fun insert(wars: List<OldWarEntity>): Flow<Unit> = flow { emit(dao.bulkInsert(wars)) }

    override fun insert(war: OldWarEntity): Flow<Unit> = flow {
        emit(dao.insert(war))
    }

    override fun delete(war: OldWarEntity): Flow<Unit> = flow {
        emit(dao.delete(war))
    }

    override fun clear(): Flow<Unit> = flow { emit(dao.clear()) }

}