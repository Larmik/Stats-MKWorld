package fr.harmoniamk.statsmkworld.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.harmoniamk.statsmkworld.database.entities.OldWarEntity
import kotlinx.coroutines.flow.Flow

@Dao
@Deprecated("24 players")
interface OldWarDao {

    @Query("SELECT * FROM OldWarEntity")
    fun getAll(): Flow<List<OldWarEntity>>

    @Query("SELECT * FROM OldWarEntity WHERE id=(:id) LIMIT 1")
    fun getById(id: String?): Flow<OldWarEntity>

    @Query("DELETE FROM OldWarEntity")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun bulkInsert(wars: List<OldWarEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(war: OldWarEntity)

    @Delete
    suspend fun delete(war: OldWarEntity)
}