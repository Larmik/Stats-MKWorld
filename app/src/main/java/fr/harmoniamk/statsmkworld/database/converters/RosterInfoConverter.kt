package fr.harmoniamk.statsmkworld.database.converters

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import fr.harmoniamk.statsmkworld.model.local.RosterInfo

class RosterInfoConverter {

    private val adapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter<List<RosterInfo>>(Types.newParameterizedType(List::class.java, RosterInfo::class.java))

    @TypeConverter
    fun fromRosterInfoList(value: List<RosterInfo>?): String = adapter.toJson(value)

    @TypeConverter
    fun toRosterInfoList(value: String?) =
        try {
            value?.let { adapter.fromJson(it) }
        } catch (e: Exception) {
            arrayListOf()
        }

}
