package fr.harmoniamk.statsmkworld.extension

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.ContextWrapper
import androidx.core.app.NotificationCompat
import fr.harmoniamk.statsmkworld.R
import java.util.Date

fun Context.getActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

fun Context.sendDebugNotification(label: String) {
    val builder = NotificationCompat.Builder(this, "DEBUG")
        .setSmallIcon(R.mipmap.appicon_round)
        .setContentTitle("Stats MKWorld")
        .setContentText(label)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    val name = "Stats MKWorld"
    val descriptionText = label
    val importance = NotificationManager.IMPORTANCE_DEFAULT
    val channel = NotificationChannel("CHANNEL_ID", name, importance).apply {
        description = descriptionText
    }
    // Register the channel with the system.
    val notificationManager: NotificationManager = applicationContext.getSystemService(
        NOTIFICATION_SERVICE
    ) as NotificationManager
    notificationManager.createNotificationChannel(channel)
    builder.setChannelId("CHANNEL_ID")

    notificationManager.notify((Date().time / 1000L % Int.MAX_VALUE).toInt(), builder.build())
}