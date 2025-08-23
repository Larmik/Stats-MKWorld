package fr.harmoniamk.statsmkworld.worker

import android.content.Context
import android.icu.util.Calendar
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.NotificationRepositoryInterface
import fr.harmoniamk.statsmkworld.usecase.FetchUseCaseInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import java.util.Date
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@HiltWorker
class UpdateDataWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val fetchUseCase: FetchUseCaseInterface,
    private val notificationRepository: NotificationRepositoryInterface,
    ): CoroutineWorker(appContext = context, params = workerParams), CoroutineScope {

        companion object {
            private val constraint = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val work : PeriodicWorkRequest
                get() {
                    val currentDate = Calendar.getInstance()
                    val dueDate = Calendar.getInstance()
                    dueDate.set(Calendar.HOUR_OF_DAY, 9)
                    dueDate.set(Calendar.MINUTE, 0)
                    dueDate.set(Calendar.SECOND, 0)
                    if (dueDate.before(currentDate)) {
                        dueDate.add(Calendar.HOUR_OF_DAY, 24)
                    }
                    val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

                    return PeriodicWorkRequestBuilder<UpdateDataWorker>(24, TimeUnit.HOURS)
                        .setConstraints(constraint)
                        .setInitialDelay(
                            duration = timeDiff,
                            timeUnit = TimeUnit.MILLISECONDS
                        ).build()
                }
        }

    override suspend fun doWork(): Result {
        dataStoreRepository.mkcPlayer.firstOrNull()?.id?.let {
           fetchUseCase.fetchData(it.toString())
        }
        return Result.success()

    }


}