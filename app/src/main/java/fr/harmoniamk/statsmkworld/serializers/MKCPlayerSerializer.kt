package fr.harmoniamk.statsmkworld.serializers

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import fr.harmoniamk.statsmkworld.debug.MKCPlayerProto
import java.io.InputStream
import java.io.OutputStream

object MKCPlayerSerializer : Serializer<MKCPlayerProto> {
    override val defaultValue: MKCPlayerProto = MKCPlayerProto.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): MKCPlayerProto {
        try {
            return MKCPlayerProto.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: MKCPlayerProto,
        output: OutputStream
    ) = t.writeTo(output)
}

val Context.mkcPlayerDataStore: DataStore<MKCPlayerProto> by dataStore(
    fileName = "mkc_player.pb",
    serializer = MKCPlayerSerializer
)
