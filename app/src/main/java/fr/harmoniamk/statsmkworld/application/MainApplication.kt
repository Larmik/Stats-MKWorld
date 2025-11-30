package fr.harmoniamk.statsmkworld.application

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import java.lang.ref.WeakReference
import javax.inject.Inject


@HiltAndroidApp
class MainApplication : Application(), Application.ActivityLifecycleCallbacks, Configuration.Provider {

    companion object {
        var instance: MainApplication? = null
    }

    init {
        instance = this
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private var activityReference = WeakReference<Activity?>(null)

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
        activityReference = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        activityReference.clear()
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