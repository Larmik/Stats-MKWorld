package fr.harmoniamk.statsmkworld.datasource.local

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.database.MKDatabase
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

interface PlayerLocalDataSourceInterface {
    fun getAll(): Flow<List<PlayerEntity>>
    fun getById(id: String): Flow<PlayerEntity>
    fun bulkInsert(players: List<PlayerEntity>): Flow<Unit>
    fun upsert(player: PlayerEntity): Flow<Unit>
    fun setCurrentWar(id: String, currentWar: String): Flow<Unit>
    fun setRole(id: String, role: Int): Flow<Unit>
    fun setAlly(id: String, isAlly: Boolean): Flow<Unit>
    fun clear(): Flow<Unit>
}

@FlowPreview
@ExperimentalCoroutinesApi
@Module
@InstallIn(SingletonComponent::class)
interface PlayerLocalDataSourceModule {
    @Singleton
    @Binds
    fun bind(impl: PlayerLocalDataSource): PlayerLocalDataSourceInterface
}

@FlowPreview
@ExperimentalCoroutinesApi
class PlayerLocalDataSource @Inject constructor(@ApplicationContext private val context: Context) :
    PlayerLocalDataSourceInterface {

    private val dao = MKDatabase.getInstance(context).playerDao()

    override fun getAll(): Flow<List<PlayerEntity>> = dao.getAll()
    override fun getById(id: String): Flow<PlayerEntity> = dao.getById(id)
    override fun bulkInsert(players: List<PlayerEntity>): Flow<Unit> = flow { emit(dao.bulkInsert(players)) }
    override fun upsert(player: PlayerEntity): Flow<Unit> = flow { emit(dao.upsert(player)) }
    override fun setCurrentWar(id: String, currentWar: String): Flow<Unit> = flow { emit(dao.setCurrentWar(id, currentWar)) }
    override fun setRole(id: String, role: Int): Flow<Unit> = flow { emit(dao.setRole(id, role)) }
    override fun setAlly(id: String, isAlly: Boolean): Flow<Unit> = flow { emit(dao.setAlly(id, isAlly)) }

    override fun clear(): Flow<Unit> = flow { emit(dao.clear())}


}