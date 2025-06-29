
package fr.harmoniamk.statsmkworld.serializers

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import fr.harmoniamk.statsmkworld.debug.MKCPlayerProto
import fr.harmoniamk.statsmkworld.debug.MKCTeamProto
import java.io.InputStream
import java.io.OutputStream

object MKCTeamSerializer : Serializer<MKCTeamProto> {
    override val defaultValue: MKCTeamProto = MKCTeamProto.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): MKCTeamProto {
        try {
            return MKCTeamProto.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: MKCTeamProto,
        output: OutputStream
    ) = t.writeTo(output)
}

val Context.mkcTeamDataStore: DataStore<MKCTeamProto> by dataStore(
    fileName = "mkc_team.pb",
    serializer = MKCTeamSerializer
)