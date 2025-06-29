package fr.harmoniamk.statsmkworld.database.converters

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import fr.harmoniamk.statsmkworld.model.firebase.WarTrack

class WarTrackConverter {

    private val adapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter<List<WarTrack>>(Types.newParameterizedType(List::class.java, WarTrack::class.java))

    @TypeConverter
    fun fromWarTrackList(value: List<WarTrack>?): String = adapter.toJson(value)

    @TypeConverter
    fun toWarTrackList(value: String?) =
        try {
            value?.let { adapter.fromJson(it) }
        } catch (e: Exception) {
            arrayListOf()
        }

}