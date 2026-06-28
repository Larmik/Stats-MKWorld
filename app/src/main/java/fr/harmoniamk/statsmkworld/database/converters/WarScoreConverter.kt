package fr.harmoniamk.statsmkworld.database.converters

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import fr.harmoniamk.statsmkworld.model.firebase.WarScore

class WarScoreConverter {

    private val adapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter<List<WarScore>>(Types.newParameterizedType(List::class.java, WarScore::class.java))

    @TypeConverter
    fun fromWarScoreList(value: List<WarScore>?): String = adapter.toJson(value)

    @TypeConverter
    fun toWarScoreList(value: String?) =
        try {
            value?.let { adapter.fromJson(it) }
        } catch (e: Exception) {
            arrayListOf()
        }
}