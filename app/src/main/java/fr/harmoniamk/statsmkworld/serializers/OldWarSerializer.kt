package fr.harmoniamk.statsmkworld.serializers

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import fr.harmoniamk.statsmkworld.debug.OldWarProto
import java.io.InputStream
import java.io.OutputStream

@Deprecated("24 players")
object OldWarSerializer : Serializer<OldWarProto> {
    override val defaultValue: OldWarProto = OldWarProto.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): OldWarProto {
        try {
            return OldWarProto.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: OldWarProto,
        output: OutputStream
    ) = t.writeTo(output)
}

val Context.warDataStore: DataStore<OldWarProto> by dataStore(
    fileName = "oldwar.pb",
    serializer = OldWarSerializer
)