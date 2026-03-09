package fr.harmoniamk.statsmkworld.database.converters

import androidx.room.TypeConverter
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import fr.harmoniamk.statsmkworld.model.firebase.WarTrack
import java.lang.reflect.Type

class WarTrackConverter {

    private val moshi = Moshi.Builder()
        .add(NumberToIntAdapterFactory())
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter<List<WarTrack>>(
        Types.newParameterizedType(List::class.java, WarTrack::class.java)
    )

    @TypeConverter
    fun fromWarTrackList(value: List<WarTrack>?): String = adapter.toJson(value)

    @TypeConverter
    fun toWarTrackList(value: String?) =
        try {
            value?.let { adapter.fromJson(it) }
        } catch (e: Exception) {
            arrayListOf()
        }

    // Factory qui convertit automatiquement tous les nombres en Int
    class NumberToIntAdapterFactory : JsonAdapter.Factory {
        override fun create(
            type: Type,
            annotations: MutableSet<out Annotation>,
            moshi: Moshi
        ): JsonAdapter<*>? {
            if (type != Int::class.java && type != Integer::class.java) {
                return null
            }

            return object : JsonAdapter<Int>() {
                @FromJson
                override fun fromJson(reader: JsonReader): Int {
                    return when (reader.peek()) {
                        JsonReader.Token.NUMBER -> reader.nextInt()
                        else -> 0
                    }
                }

                @ToJson
                override fun toJson(writer: JsonWriter, value: Int?) {
                    writer.value(value)
                }
            }
        }
    }
}