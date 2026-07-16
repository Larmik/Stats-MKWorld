package fr.harmoniamk.statsmkworld.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WarDao {

    // id = timestamp de création (war.id, epoch millis) stocké en TEXT : tri
    // chronologique croissant garanti EN AMONT de tous les calculs de stats
    // (séries, forme récente…). CAST en INTEGER pour un tri numérique et non
    // lexicographique. Source de vérité de l'ordre pour worker + ViewModels ;
    // Stats.kt re-trie par sécurité mais s'appuie sur cette garantie.
    @Query("SELECT * FROM WarEntity ORDER BY CAST(id AS INTEGER) ASC")
    fun getAll(): Flow<List<WarEntity>>

    @Query("SELECT * FROM WarEntity WHERE id=(:id) LIMIT 1")
    fun getById(id: String?): Flow<WarEntity?>

    @Query("DELETE FROM WarEntity")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun bulkInsert(wars: List<WarEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(war: WarEntity)

    @Delete
    suspend fun delete(war: WarEntity)
}