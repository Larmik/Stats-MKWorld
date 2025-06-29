package fr.harmoniamk.statsmkworld.database.converters

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import fr.harmoniamk.statsmkworld.model.firebase.WarPosition


class WarPositionConverter {

    private val adapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter<List<WarPosition>>(Types.newParameterizedType(List::class.java, WarPosition::class.java))

    @TypeConverter
    fun fromWarPositionList(value: List<WarPosition>?): String = adapter.toJson(value)

    @TypeConverter
    fun toWarPositionList(value: String?) =
        try {
            value?.let { adapter.fromJson(it) }
        } catch (e: Exception) {
            arrayListOf()
        }

}