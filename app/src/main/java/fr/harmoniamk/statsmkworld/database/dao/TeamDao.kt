package fr.harmoniamk.statsmkworld.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {

    @Query("SELECT * FROM TeamEntity")
    fun getAll(): Flow<List<TeamEntity>>

    @Query("SELECT * FROM TeamEntity WHERE id=(:id) LIMIT 1")
    fun getById(id: String): Flow<TeamEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun bulkInsert(teams: List<TeamEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(team: TeamEntity)

    @Delete
    suspend fun delete(team: TeamEntity)

    @Query("DELETE FROM TeamEntity")
    suspend fun clear()
}