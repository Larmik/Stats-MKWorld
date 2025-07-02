package fr.harmoniamk.statsmkworld.database.converters

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import fr.harmoniamk.statsmkworld.model.firebase.WarPenalty

class WarPenaltyConverter {

    private val adapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter<List<WarPenalty>>(Types.newParameterizedType(List::class.java, WarPenalty::class.java))

    @TypeConverter
    fun fromWarPenaltyList(value: List<WarPenalty>?): String = adapter.toJson(value)

    @TypeConverter
    fun toWarPenaltyList(value: String?) =
        try {
            value?.let { adapter.fromJson(it) }
        } catch (e: Exception) {
            arrayListOf()
        }

}