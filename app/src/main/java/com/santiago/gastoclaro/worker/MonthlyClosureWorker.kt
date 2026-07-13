package com.santiago.gastoclaro.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.santiago.gastoclaro.domain.repository.FinanceRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

@HiltWorker
class MonthlyClosureWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val financeRepository: FinanceRepository
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = runCatching {
        financeRepository.ensureClosedMonths(LocalDate.now(), origin = "WORK_MANAGER")
    }.fold(
        onSuccess = { Result.success() },
        onFailure = { Result.retry() }
    )
}
