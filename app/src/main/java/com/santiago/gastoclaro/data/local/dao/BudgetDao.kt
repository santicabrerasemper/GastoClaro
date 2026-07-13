package com.santiago.gastoclaro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.santiago.gastoclaro.data.local.entity.MonthlyBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM monthly_budgets WHERE profileId = :profileId AND year = :year AND month = :month LIMIT 1")
    fun observe(profileId: Long, year: Int, month: Int): Flow<MonthlyBudgetEntity?>

    @Query("SELECT * FROM monthly_budgets WHERE profileId = :profileId AND year = :year AND month = :month LIMIT 1")
    suspend fun get(profileId: Long, year: Int, month: Int): MonthlyBudgetEntity?

    @Query("SELECT * FROM monthly_budgets ORDER BY year ASC, month ASC")
    suspend fun getAll(): List<MonthlyBudgetEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: MonthlyBudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MonthlyBudgetEntity): Long

    @Update
    suspend fun update(entity: MonthlyBudgetEntity)
}
