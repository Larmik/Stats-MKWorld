package fr.harmoniamk.statsmkworld.database.converters

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import fr.harmoniamk.statsmkworld.model.firebase.OldWarTrack

@Deprecated("24 players")
class OldWarTrackConverter {

    private val adapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter<List<OldWarTrack>>(Types.newParameterizedType(List::class.java, OldWarTrack::class.java))

    @TypeConverter
    fun fromWarTrackList(value: List<OldWarTrack>?): String = adapter.toJson(value)

    @TypeConverter
    fun toWarTrackList(value: String?) =
        try {
            value?.let { adapter.fromJson(it) }
        } catch (e: Exception) {
            arrayListOf()
        }

}