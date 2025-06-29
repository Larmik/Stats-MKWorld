package fr.harmoniamk.statsmkworld.application

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import fr.harmoniamk.statsmkworld.worker.UpdateDataWorker
import java.lang.ref.WeakReference
import javax.inject.Inject


@HiltAndroidApp
class MainApplication : Application(), Application.ActivityLifecycleCallbacks, Configuration.Provider {

    companion object {
        var instance: MainApplication? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            uniqueWorkName = "UpdateDataWorker",
            existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request = UpdateDataWorker.work
        )
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private var activityReference = WeakReference<Activity?>(null)

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        activityReference = WeakReference(activity)
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
        activityReference.clear()
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    override val workManagerConfiguration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory = workerFactory)
            .build()

    val currentActivity
        get() = activityReference.get()
}