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
import javax.inject.Inject
import javax.inject.Singleton

interface PlayerLocalDataSourceInterface {
    fun getAll(): Flow<List<PlayerEntity>>
    fun getById(id: String): Flow<PlayerEntity?>
    suspend fun insert(player: PlayerEntity)
    suspend fun upsert(player: PlayerEntity)
    suspend fun setCurrentWar(id: String, currentWar: String)
    suspend fun setRole(id: String, role: Int)
    suspend fun setRosterId(id: String, rosterId: String)
    suspend fun clear()
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
    override fun getById(id: String): Flow<PlayerEntity?> = dao.getById(id)
    override suspend fun insert(player: PlayerEntity) = dao.insert(player)
    override suspend fun upsert(player: PlayerEntity) = dao.upsert(player)
    override suspend fun setCurrentWar(id: String, currentWar: String) = dao.setCurrentWar(id, currentWar)
    override suspend fun setRole(id: String, role: Int) = dao.setRole(id, role)
    override suspend fun setRosterId(id: String, rosterId: String) = dao.setRosterId(id, rosterId)

    override suspend fun clear() = dao.clear()

}