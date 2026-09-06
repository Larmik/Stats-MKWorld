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

/**
 * Abstraction d'accès à WorkManager pour enfiler des tâches d'arrière-plan ponctuelles
 * (ex. `InitStatsWorker` de recalcul des stats). Interface + module Hilt (convention DI).
 */
interface WorkerRepositoryInterface {
    /**
     * Enfile une tâche unique par `tag` : annule toute tâche existante portant ce même tag
     * avant d'enfiler la nouvelle (une seule instance active par tag).
     *
     * @param workerClass Classe du `ListenableWorker` à exécuter.
     * @param tag Étiquette d'unicité de la tâche.
     * @param data Données d'entrée optionnelles passées au worker.
     */
    fun <T : ListenableWorker> launchBackgroundTask(workerClass: Class<T>, tag: String, data: Data?)
}

@Module
@InstallIn(SingletonComponent::class)
interface WorkerRepositoryModule {
    @Binds
    @Singleton
    fun bind(impl: WorkerRepository): WorkerRepositoryInterface
}


/** Implémentation WorkManager de [WorkerRepositoryInterface]. */
class WorkerRepository @Inject constructor(@ApplicationContext val context: Context) : WorkerRepositoryInterface {

    private val manager = WorkManager.getInstance(context)

    override fun <T : ListenableWorker> launchBackgroundTask(workerClass: Class<T>, tag: String, data: Data?) {
        val builder = OneTimeWorkRequest.Builder(workerClass)
            .addTag(tag)
        data?.let { builder.setInputData(it) }
        val request = builder.build()
        // Unicité par tag : on annule l'éventuelle tâche précédente de même tag avant d'enfiler.
        manager.cancelAllWorkByTag(tag)
        manager.enqueue(request)
    }

}