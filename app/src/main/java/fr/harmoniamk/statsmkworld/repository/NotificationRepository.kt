package fr.harmoniamk.statsmkworld.repository

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.activity.MainActivity
import fr.harmoniamk.statsmkworld.application.MainApplication
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

enum class PermissionStatus {
    Granted,
    Denied,
    CanAsk
}

interface NotificationRepositoryInterface {
    val notificationsEnabled: Boolean
    val requestAuthorization: Boolean
    val permissionStatus: PermissionStatus
    fun sendNotification(message: String)
}

@Module
@InstallIn(SingletonComponent::class)
interface NotificationRepositoryModule {

    @Binds
    @Singleton
    fun bind(impl: NotificationRepository): NotificationRepositoryInterface
}


class NotificationRepository @Inject constructor(@ApplicationContext val context: Context) : NotificationRepositoryInterface {


    override val notificationsEnabled: Boolean
        get() {
            val areNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            val isPermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || permissionStatus == PermissionStatus.Granted
            return areNotificationsEnabled && isPermissionGranted
        }

    override val requestAuthorization: Boolean
        get() {
                val areNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && permissionStatus == PermissionStatus.CanAsk) {
                    return false
                } else if (areNotificationsEnabled.not()) {
                    return true
                } else {
                    return false
                }

        }

    override val permissionStatus = ((context as? MainApplication)?.currentActivity as? MainActivity)?.let { activity ->
        when {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> PermissionStatus.Granted
            activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> PermissionStatus.CanAsk
            else -> PermissionStatus.Denied
        }
    } ?: PermissionStatus.Denied

    override fun sendNotification(message: String) {
        val builder = NotificationCompat.Builder(context, "DEBUG")
            .setSmallIcon(R.drawable.mkcentralpic)
            .setContentTitle("Stats MKWorld")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val name = "Stats MKWorld Debug"
        val descriptionText = "FindPlayerWorker"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("CHANNEL_ID", name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system.
        val notificationManager: NotificationManager = context.getSystemService(
            NOTIFICATION_SERVICE
        ) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        builder.setChannelId("CHANNEL_ID")

        notificationManager.notify((Date().time / 1000L % Int.MAX_VALUE).toInt(), builder.build())
    }
}