package fr.harmoniamk.statsmkworld.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import fr.harmoniamk.statsmkworld.database.converters.RosterInfoConverter
import fr.harmoniamk.statsmkworld.database.converters.StringConverter
import fr.harmoniamk.statsmkworld.database.converters.WarPenaltyConverter
import fr.harmoniamk.statsmkworld.database.converters.WarPositionConverter
import fr.harmoniamk.statsmkworld.database.converters.WarScoreConverter
import fr.harmoniamk.statsmkworld.database.converters.WarTrackConverter
import fr.harmoniamk.statsmkworld.database.dao.PlayerDao
import fr.harmoniamk.statsmkworld.database.dao.TeamDao
import fr.harmoniamk.statsmkworld.database.dao.WarDao
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import kotlinx.coroutines.FlowPreview

@TypeConverters(value = [WarTrackConverter::class, WarPositionConverter::class, WarPenaltyConverter::class, StringConverter::class, WarScoreConverter::class, RosterInfoConverter::class])
// v7 : ajout de PlayerEntity.avatar (photo de profil MKCentral). fallbackToDestructiveMigration
// en place → perte des données locales acceptée (re-synchro Firebase/MKCentral). #50 pt.4.
@Database(entities = [WarEntity::class, PlayerEntity::class, TeamEntity::class], version = 7)
abstract class MKDatabase : RoomDatabase() {

    abstract fun playerDao(): PlayerDao
    abstract fun teamDao(): TeamDao
    abstract fun warDao(): WarDao

    @FlowPreview
    companion object {

        @Volatile
        private var instance: MKDatabase? = null

        @Synchronized
        fun getInstance(context: Context): MKDatabase {
            return instance ?: synchronized(this) {
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    MKDatabase::class.java,
                    "mk_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                instance = newInstance
                newInstance
            }
        }


    }
}