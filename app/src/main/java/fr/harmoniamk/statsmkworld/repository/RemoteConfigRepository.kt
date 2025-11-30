package fr.harmoniamk.statsmkworld.repository

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton

interface RemoteConfigRepositoryInterface {
    suspend fun minimumVersion(): Int
}

@Module
@InstallIn(SingletonComponent::class)
interface RemoteConfigRepositoryModule {
    @Singleton
    @Binds
    fun bind(impl: RemoteConfigRepository): RemoteConfigRepositoryInterface
}

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteConfigRepository @Inject constructor() : RemoteConfigRepositoryInterface {

    private val remoteConfig = FirebaseRemoteConfig.getInstance()

    private suspend fun loadConfig() = suspendCancellableCoroutine { continuation ->
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        val config = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(0)
            .build()
        remoteConfig.setConfigSettingsAsync(config)
        remoteConfig.fetch(0)
            .continueWith { remoteConfig.activate() }
            .addOnCompleteListener {
                continuation.resume(value = Unit) {  }
            }
    }

    override suspend fun minimumVersion(): Int {
        loadConfig()
        return remoteConfig.getString("minimumVersion").toIntOrNull() ?: 0
    }

}