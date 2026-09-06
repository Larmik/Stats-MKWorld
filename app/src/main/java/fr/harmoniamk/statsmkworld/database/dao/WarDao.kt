package fr.harmoniamk.statsmkworld.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WarDao {

    // id = timestamp epoch millis stocké en TEXT ; CAST INTEGER pour un tri numérique
    // (et non lexicographique). Ordre chronologique croissant = source de vérité pour
    // tous les calculs de stats (séries, forme récente…).
    @Query("SELECT * FROM WarEntity ORDER BY CAST(id AS INTEGER) ASC")
    fun getAll(): Flow<List<WarEntity>>

    @Query("DELETE FROM WarEntity")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun bulkInsert(wars: List<WarEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(war: WarEntity)
}