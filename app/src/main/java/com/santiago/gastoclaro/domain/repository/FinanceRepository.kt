package com.santiago.gastoclaro.domain.repository

import com.santiago.gastoclaro.data.local.entity.CategoryEntity
import com.santiago.gastoclaro.data.local.entity.MonthlyBudgetEntity
import com.santiago.gastoclaro.data.local.entity.MonthlyClosureEntity
import com.santiago.gastoclaro.data.local.entity.MovementType
import com.santiago.gastoclaro.data.local.entity.PaymentMethodEntity
import com.santiago.gastoclaro.data.local.entity.SavingGoalEntity
import com.santiago.gastoclaro.data.local.model.ClosureWithSnapshots
import com.santiago.gastoclaro.data.local.model.MovementRow
import com.santiago.gastoclaro.domain.model.MovementDraft
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

interface FinanceRepository {
    fun observeBudget(profileId: Long, period: YearMonth): Flow<MonthlyBudgetEntity?>
    fun observeCategories(profileId: Long): Flow<List<CategoryEntity>>
    fun observeCategories(profileId: Long, type: MovementType): Flow<List<CategoryEntity>>
    fun observePaymentMethods(profileId: Long): Flow<List<PaymentMethodEntity>>
    fun observeMovements(profileId: Long, period: YearMonth): Flow<List<MovementRow>>
    fun observeMovementsBetween(profileId: Long, start: LocalDate, end: LocalDate): Flow<List<MovementRow>>
    fun observeClosures(profileId: Long): Flow<List<MonthlyClosureEntity>>
    fun observeClosure(profileId: Long, period: YearMonth): Flow<ClosureWithSnapshots?>
    fun observeSavingGoals(profileId: Long): Flow<List<SavingGoalEntity>>

    suspend fun getMovement(profileId: Long, movementId: Long): MovementRow?
    suspend fun saveMovement(draft: MovementDraft): Long
    suspend fun savePaymentMethod(entity: PaymentMethodEntity): Long
    suspend fun saveSavingGoal(entity: SavingGoalEntity): Long
    suspend fun archiveSavingGoal(profileId: Long, goalId: Long)
    suspend fun archivePaymentMethod(profileId: Long, paymentMethodId: Long)
    suspend fun deleteMovement(profileId: Long, movementId: Long)
    suspend fun updateBudget(profileId: Long, period: YearMonth, amountCents: Long)
    suspend fun ensureClosedMonths(today: LocalDate, origin: String = "APP_START")
    suspend fun closeMonth(profileId: Long, period: YearMonth, origin: String = "MANUAL")
    suspend fun reopenMonth(profileId: Long, period: YearMonth)
}
