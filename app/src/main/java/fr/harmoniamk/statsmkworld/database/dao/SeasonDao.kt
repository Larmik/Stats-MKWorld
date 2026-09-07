package fr.harmoniamk.statsmkworld.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.harmoniamk.statsmkworld.database.entities.SeasonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeasonDao {

    // Ordre chronologique croissant (par numéro de saison) : la dernière = la plus récente.
    @Query("SELECT * FROM SeasonEntity ORDER BY number ASC")
    fun getAll(): Flow<List<SeasonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun bulkInsert(seasons: List<SeasonEntity>)

    @Query("DELETE FROM SeasonEntity")
    suspend fun clear()
}
