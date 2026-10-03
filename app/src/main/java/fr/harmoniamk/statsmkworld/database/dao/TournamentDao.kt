package fr.harmoniamk.statsmkworld.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.harmoniamk.statsmkworld.database.entities.TournamentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TournamentDao {

    @Query("SELECT * FROM TournamentEntity")
    fun getTournaments(): Flow<List<TournamentEntity>>

    @Query("SELECT * FROM TournamentEntity WHERE id = :id")
    fun getTournament(id: String): Flow<TournamentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tournament: TournamentEntity)
}
