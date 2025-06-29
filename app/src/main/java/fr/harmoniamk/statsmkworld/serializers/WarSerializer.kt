package fr.harmoniamk.statsmkworld.serializers

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import fr.harmoniamk.statsmkworld.debug.WarProto
import java.io.InputStream
import java.io.OutputStream

object WarSerializer : Serializer<WarProto> {
    override val defaultValue: WarProto = WarProto.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): WarProto {
        try {
            return WarProto.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: WarProto,
        output: OutputStream
    ) = t.writeTo(output)
}

val Context.warDataStore: DataStore<WarProto> by dataStore(
    fileName = "war.pb",
    serializer = WarSerializer
)