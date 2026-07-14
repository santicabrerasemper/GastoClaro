package com.santiago.gastoclaro.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.santiago.gastoclaro.data.local.dao.BudgetDao
import com.santiago.gastoclaro.data.local.dao.CategoryDao
import com.santiago.gastoclaro.data.local.dao.ClosureDao
import com.santiago.gastoclaro.data.local.dao.MovementDao
import com.santiago.gastoclaro.data.local.dao.PaymentMethodDao
import com.santiago.gastoclaro.data.local.dao.ProfileDao
import com.santiago.gastoclaro.data.local.dao.SavingGoalDao
import com.santiago.gastoclaro.data.local.entity.CategoryEntity
import com.santiago.gastoclaro.data.local.entity.MonthlyBudgetEntity
import com.santiago.gastoclaro.data.local.entity.MonthlyCategorySnapshotEntity
import com.santiago.gastoclaro.data.local.entity.MonthlyClosureEntity
import com.santiago.gastoclaro.data.local.entity.MovementEntity
import com.santiago.gastoclaro.data.local.entity.PaymentMethodEntity
import com.santiago.gastoclaro.data.local.entity.ProfileEntity
import com.santiago.gastoclaro.data.local.entity.SavingGoalEntity

@Database(
    entities = [
        ProfileEntity::class,
        MonthlyBudgetEntity::class,
        CategoryEntity::class,
        MovementEntity::class,
        PaymentMethodEntity::class,
        MonthlyClosureEntity::class,
        MonthlyCategorySnapshotEntity::class,
        SavingGoalEntity::class
    ],
    version = 14,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryDao(): CategoryDao
    abstract fun movementDao(): MovementDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun closureDao(): ClosureDao
    abstract fun savingGoalDao(): SavingGoalDao
}
