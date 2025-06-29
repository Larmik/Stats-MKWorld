package fr.harmoniamk.statsmkworld.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {

    @Query("SELECT * FROM PlayerEntity")
    fun getAll(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM PlayerEntity WHERE id=(:id) LIMIT 1")
    fun getById(id: String): Flow<PlayerEntity>

    @Query("UPDATE PlayerEntity SET currentWar=(:currentWar) WHERE id=(:id)")
    suspend fun setCurrentWar(id: String, currentWar: String)

    @Query("UPDATE PlayerEntity SET role=(:role) WHERE id=(:id)")
    suspend fun setRole(id: String, role: Int)

    @Query("UPDATE PlayerEntity SET isAlly=(:ally) WHERE id=(:id)")
    suspend fun setAlly(id: String, ally: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun bulkInsert(teams: List<PlayerEntity>)

    @Upsert
    suspend fun upsert(player: PlayerEntity)

    @Delete
    suspend fun delete(team: PlayerEntity)

    @Query("DELETE FROM PlayerEntity")
    suspend fun clear()


}