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

    // Saison en cours = celle sans date de fin (end IS NULL). Il ne peut y en avoir qu'une.
    @Query("SELECT * FROM SeasonEntity WHERE `end` IS NULL ORDER BY number DESC LIMIT 1")
    suspend fun getCurrent(): SeasonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun bulkInsert(seasons: List<SeasonEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(season: SeasonEntity)

    @Query("DELETE FROM SeasonEntity")
    suspend fun clear()
}
