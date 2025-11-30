package fr.harmoniamk.statsmkworld.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.activity.MainActivity
import fr.harmoniamk.statsmkworld.application.MainApplication
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

enum class PermissionStatus {
    Granted,
    Denied,
    CanAsk
}

interface NotificationRepositoryInterface {
    val notificationsEnabled: Boolean
    suspend fun requestAuthorization(): Boolean
}

@Module
@InstallIn(SingletonComponent::class)
interface NotificationRepositoryModule {
    @Binds
    @Singleton
    fun bind(impl: NotificationRepository): NotificationRepositoryInterface
}


class NotificationRepository @Inject constructor(@ApplicationContext val context: Context, private val dataStoreRepository: DataStoreRepositoryInterface) : NotificationRepositoryInterface {

    override val notificationsEnabled: Boolean
        get() {
            val areNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            val isPermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            return areNotificationsEnabled && isPermissionGranted
        }

    override suspend fun requestAuthorization(): Boolean {
        val areNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && permissionCheck() == PermissionStatus.CanAsk) {
            dataStoreRepository.setNotifAlreadyRequested()
            ((context as? MainApplication)?.currentActivity as? MainActivity)?.notificationPermissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
             return false
        } else if (areNotificationsEnabled.not())
            return true
        else return false
    }

    private suspend fun permissionCheck() = ((context as? MainApplication)?.currentActivity as? MainActivity)?.let { activity ->
        when {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> PermissionStatus.Granted
            activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> PermissionStatus.CanAsk
            dataStoreRepository.notifAlreadyRequested.firstOrNull() != true -> PermissionStatus.CanAsk
            else -> PermissionStatus.Denied
        }
    } ?: PermissionStatus.Denied


}