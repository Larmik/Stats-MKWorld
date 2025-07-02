package fr.harmoniamk.statsmkworld.repository

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

interface WorkerRepositoryInterface {
    fun <T : ListenableWorker> launchBackgroundTask(workerClass: Class<T>, tag: String, data: Data?)
    fun cancelAllTask()
}

@Module
@InstallIn(SingletonComponent::class)
interface WorkerRepositoryModule {
    @Binds
    @Singleton
    fun bind(impl: WorkerRepository): WorkerRepositoryInterface
}


class WorkerRepository @Inject constructor(@ApplicationContext val context: Context) : WorkerRepositoryInterface {

    private val manager = WorkManager.getInstance(context)
    override fun <T : ListenableWorker> launchBackgroundTask(workerClass: Class<T>, tag: String, data: Data?) {
        val builder = OneTimeWorkRequest.Builder(workerClass)
            .addTag(tag)
        data?.let { builder.setInputData(it) }
        val request = builder.build()
        manager.cancelAllWorkByTag(tag)
        manager.enqueue(request)
    }

    override fun cancelAllTask() {
        manager.cancelAllWork()
    }

}