package com.santiago.gastoclaro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.santiago.gastoclaro.data.local.entity.SavingGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingGoalDao {
    @Query("SELECT * FROM saving_goals WHERE profileId = :profileId AND isArchived = 0 ORDER BY createdAt")
    fun observeActive(profileId: Long): Flow<List<SavingGoalEntity>>

    @Query("SELECT * FROM saving_goals WHERE id = :id AND profileId = :profileId LIMIT 1")
    suspend fun getById(profileId: Long, id: Long): SavingGoalEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(goal: SavingGoalEntity): Long

    @Update
    suspend fun update(goal: SavingGoalEntity): Int
}
