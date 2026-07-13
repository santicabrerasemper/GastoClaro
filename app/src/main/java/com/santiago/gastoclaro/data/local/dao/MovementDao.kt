package com.santiago.gastoclaro.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.santiago.gastoclaro.data.local.entity.MovementEntity
import com.santiago.gastoclaro.data.local.entity.MovementType
import com.santiago.gastoclaro.data.local.model.CategoryTotalRow
import com.santiago.gastoclaro.data.local.model.MovementRow
import kotlinx.coroutines.flow.Flow

@Dao
interface MovementDao {
    @Query("""
        SELECT m.*, c.name AS categoryName, c.emoji AS categoryEmoji, c.colorArgb AS categoryColorArgb,
               pm.name AS paymentMethodName, pm.kind AS paymentMethodKind, pm.lastDigits AS paymentMethodLastDigits
        FROM movements m
        INNER JOIN categories c ON c.id = m.categoryId
        LEFT JOIN payment_methods pm ON pm.id = m.paymentMethodId
        WHERE m.profileId = :profileId
          AND m.occurredEpochDay BETWEEN :startEpochDay AND :endEpochDay
        ORDER BY m.occurredEpochDay DESC, m.createdAt DESC
    """)
    fun observeForPeriod(
        profileId: Long,
        startEpochDay: Long,
        endEpochDay: Long
    ): Flow<List<MovementRow>>

    @Query("""
        SELECT m.*, c.name AS categoryName, c.emoji AS categoryEmoji, c.colorArgb AS categoryColorArgb,
               pm.name AS paymentMethodName, pm.kind AS paymentMethodKind, pm.lastDigits AS paymentMethodLastDigits
        FROM movements m
        INNER JOIN categories c ON c.id = m.categoryId
        LEFT JOIN payment_methods pm ON pm.id = m.paymentMethodId
        WHERE m.profileId = :profileId
          AND m.occurredEpochDay BETWEEN :startEpochDay AND :endEpochDay
        ORDER BY m.occurredEpochDay DESC, m.createdAt DESC
    """)
    fun observeBetween(
        profileId: Long,
        startEpochDay: Long,
        endEpochDay: Long
    ): Flow<List<MovementRow>>

    @Query("""
        SELECT m.*, c.name AS categoryName, c.emoji AS categoryEmoji, c.colorArgb AS categoryColorArgb,
               pm.name AS paymentMethodName, pm.kind AS paymentMethodKind, pm.lastDigits AS paymentMethodLastDigits
        FROM movements m
        INNER JOIN categories c ON c.id = m.categoryId
        LEFT JOIN payment_methods pm ON pm.id = m.paymentMethodId
        WHERE m.profileId = :profileId
          AND m.occurredEpochDay BETWEEN :startEpochDay AND :endEpochDay
        ORDER BY m.occurredEpochDay DESC, m.createdAt DESC
    """)
    suspend fun getRowsBetween(
        profileId: Long,
        startEpochDay: Long,
        endEpochDay: Long
    ): List<MovementRow>

    @Query("""
        SELECT m.*, c.name AS categoryName, c.emoji AS categoryEmoji, c.colorArgb AS categoryColorArgb,
               pm.name AS paymentMethodName, pm.kind AS paymentMethodKind, pm.lastDigits AS paymentMethodLastDigits
        FROM movements m
        INNER JOIN categories c ON c.id = m.categoryId
        LEFT JOIN payment_methods pm ON pm.id = m.paymentMethodId
        WHERE m.id = :movementId AND m.profileId = :profileId
        LIMIT 1
    """)
    suspend fun getRow(profileId: Long, movementId: Long): MovementRow?

    @Query("SELECT * FROM movements WHERE id = :movementId AND profileId = :profileId LIMIT 1")
    suspend fun getEntity(profileId: Long, movementId: Long): MovementEntity?

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN monthlyImpactCents ELSE amountCents END), 0) FROM movements
        WHERE profileId = :profileId
          AND type = :type
          AND occurredEpochDay BETWEEN :startEpochDay AND :endEpochDay
    """)
    suspend fun sumForPeriod(
        profileId: Long,
        type: MovementType,
        startEpochDay: Long,
        endEpochDay: Long
    ): Long

    @Query("""
        SELECT COUNT(*) FROM movements
        WHERE profileId = :profileId
          AND occurredEpochDay BETWEEN :startEpochDay AND :endEpochDay
    """)
    suspend fun countForPeriod(profileId: Long, startEpochDay: Long, endEpochDay: Long): Int

    @Query("""
        SELECT c.id AS categoryId, c.name AS categoryName, c.emoji AS categoryEmoji,
               c.colorArgb AS colorArgb, COALESCE(SUM(m.monthlyImpactCents), 0) AS amountCents
        FROM movements m
        INNER JOIN categories c ON c.id = m.categoryId
        WHERE m.profileId = :profileId
          AND m.type = 'EXPENSE'
          AND m.occurredEpochDay BETWEEN :startEpochDay AND :endEpochDay
        GROUP BY c.id, c.name, c.emoji, c.colorArgb
        HAVING amountCents > 0
        ORDER BY amountCents DESC
    """)
    suspend fun expenseTotalsForPeriod(
        profileId: Long,
        startEpochDay: Long,
        endEpochDay: Long
    ): List<CategoryTotalRow>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: MovementEntity): Long

    @Update
    suspend fun update(entity: MovementEntity): Int

    @Delete
    suspend fun delete(entity: MovementEntity)
}
