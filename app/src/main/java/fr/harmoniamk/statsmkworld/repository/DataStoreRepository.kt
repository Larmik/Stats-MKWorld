package fr.harmoniamk.statsmkworld.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.debug.MKCPlayerProto
import fr.harmoniamk.statsmkworld.debug.MKCTeamProto
import fr.harmoniamk.statsmkworld.debug.WarProto
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.DatastoreWar
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayer
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeam
import fr.harmoniamk.statsmkworld.serializers.mkcPlayerDataStore
import fr.harmoniamk.statsmkworld.serializers.mkcTeamDataStore
import fr.harmoniamk.statsmkworld.serializers.warDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "datastore")


interface DataStoreRepositoryInterface {
    suspend fun setAccessToken(token: String)
    suspend fun setPage(page: Int)
    suspend fun setMKCPlayer(player: MKCPlayer)
    suspend fun setMKCTeam(team: MKCTeam)
    suspend fun setCurrentWar(war: War)
    suspend fun deleteCurrentWar()
    suspend fun clearPlayer()
    suspend fun clearTeam()
    suspend fun setLastUpdate(lastUpdate: Long)

    val accessToken: Flow<String>
    val page: Flow<Int>
    val mkcPlayer: Flow<MKCPlayer>
    val mkcTeam: Flow<MKCTeam>
    val war: Flow<War?>
    val lastUpdate: Flow<Long>
}

@Module
@InstallIn(SingletonComponent::class)
interface DataStoreRepositoryModule {
    @Singleton
    @Binds
    fun bind(impl: DataStoreRepository): DataStoreRepositoryInterface
}

class DataStoreRepository @Inject constructor(@ApplicationContext val context: Context): DataStoreRepositoryInterface {
    override suspend fun setAccessToken(token: String) {
        val key = stringPreferencesKey("access_token")
        context.dataStore.edit {
            it[key] = token
        }
    }

    override suspend fun setPage(page: Int) {
        val key = intPreferencesKey("page")
        context.dataStore.edit {
            it[key] = page
        }
    }

    override suspend fun setMKCPlayer(player: MKCPlayer) {
        context.mkcPlayerDataStore.updateData {
           player.proto
        }
    }

    override suspend fun setMKCTeam(team: MKCTeam) {
        context.mkcTeamDataStore.updateData {
            team.proto
        }
    }


    override suspend fun setCurrentWar(war: War) {
        context.warDataStore.updateData {
            DatastoreWar(war).proto
        }


    }

    override suspend fun deleteCurrentWar() {
        context.warDataStore.updateData {
            WarProto.newBuilder().build()
        }
    }

    override suspend fun clearPlayer() {
        context.mkcPlayerDataStore.updateData {
            MKCPlayerProto.newBuilder().build()
        }
    }
    override suspend fun clearTeam() {
        context.mkcTeamDataStore.updateData {
            MKCTeamProto.newBuilder().build()
        }
    }

    override suspend fun setLastUpdate(lastUpdate: Long) {
        val key = longPreferencesKey("lastUpdate")
        context.dataStore.edit {
            it[key] = lastUpdate
        }
    }

    override val accessToken: Flow<String>
        get() {
            val key = stringPreferencesKey("access_token")
            return context.dataStore.data.map { it[key].orEmpty() }
        }
    override val page: Flow<Int>
        get() {
            val key = intPreferencesKey("page")
            return context.dataStore.data.map { it[key]?.toInt() ?: 0 }
        }
    override val mkcPlayer: Flow<MKCPlayer>
        get() = context.mkcPlayerDataStore.data
            .map { MKCPlayer(it) }

    override val mkcTeam: Flow<MKCTeam>
        get() = context.mkcTeamDataStore.data
            .map { MKCTeam(it) }

    override val war: Flow<War?>
        get() = context.warDataStore.data
            .map { DatastoreWar(it) }
            .map { War(it).takeIf { it.id != 0L } }

    override val lastUpdate: Flow<Long>
        get() {
            val key = longPreferencesKey("lastUpdate")
            return context.dataStore.data.map { it[key] ?: 0 }
        }

}