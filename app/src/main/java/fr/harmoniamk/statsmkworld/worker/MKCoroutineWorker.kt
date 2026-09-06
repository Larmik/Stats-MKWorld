package fr.harmoniamk.statsmkworld.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.reflect.jvm.jvmName

object MKWorkerBuilder {

    inline fun <reified W : ListenableWorker> enqueueUniquePeriodicWork(context: Context) {

        val constraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(false)
            .setRequiresBatteryNotLow(true)
            .build()

        val delay = 24 + 4 - Calendar.getInstance().get(Calendar.HOUR_OF_DAY).toLong()
        val request = PeriodicWorkRequestBuilder<W>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraint)
            // Délai jusqu'au prochain passage à 4h (24h + 4h - heure actuelle).
            .setInitialDelay(
                duration = delay,
                timeUnit = TimeUnit.HOURS
            )
            .build()

        val uniqueWorkName = W::class.simpleName ?: W::class.qualifiedName ?: W::class.jvmName

        WorkManager.getInstance(context = context).enqueueUniquePeriodicWork(
            uniqueWorkName = uniqueWorkName,
            existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request = request
        )
    }
}

abstract class MKCoroutineWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext = context, params = workerParams) {

    abstract suspend fun task()

    override suspend fun doWork(): Result {
        task()
        return Result.success()
    }
}
