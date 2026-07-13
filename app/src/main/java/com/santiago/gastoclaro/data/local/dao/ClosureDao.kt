package com.santiago.gastoclaro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.santiago.gastoclaro.data.local.entity.MonthlyCategorySnapshotEntity
import com.santiago.gastoclaro.data.local.entity.MonthlyClosureEntity
import com.santiago.gastoclaro.data.local.model.ClosureWithSnapshots
import kotlinx.coroutines.flow.Flow

@Dao
interface ClosureDao {
    @Query("SELECT * FROM monthly_closures WHERE profileId = :profileId ORDER BY year DESC, month DESC")
    fun observeAll(profileId: Long): Flow<List<MonthlyClosureEntity>>

    @Transaction
    @Query("SELECT * FROM monthly_closures WHERE profileId = :profileId AND year = :year AND month = :month LIMIT 1")
    fun observeWithSnapshots(profileId: Long, year: Int, month: Int): Flow<ClosureWithSnapshots?>

    @Query("SELECT * FROM monthly_closures WHERE profileId = :profileId AND year = :year AND month = :month LIMIT 1")
    suspend fun get(profileId: Long, year: Int, month: Int): MonthlyClosureEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: MonthlyClosureEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshots(items: List<MonthlyCategorySnapshotEntity>)

    @Query("DELETE FROM monthly_closures WHERE profileId = :profileId AND year = :year AND month = :month")
    suspend fun delete(profileId: Long, year: Int, month: Int)
}
