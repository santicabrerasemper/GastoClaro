package com.santiago.gastoclaro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.santiago.gastoclaro.data.local.entity.PaymentMethodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentMethodDao {
    @Query("SELECT * FROM payment_methods WHERE profileId = :profileId AND isArchived = 0 ORDER BY kind, name")
    fun observeActive(profileId: Long): Flow<List<PaymentMethodEntity>>

    @Query("SELECT * FROM payment_methods WHERE profileId = :profileId AND id = :id LIMIT 1")
    suspend fun getById(profileId: Long, id: Long): PaymentMethodEntity?

    @Query("SELECT * FROM payment_methods WHERE profileId = :profileId AND kind = :kind ORDER BY id LIMIT 1")
    suspend fun getFirstByKind(profileId: Long, kind: String): PaymentMethodEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: PaymentMethodEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<PaymentMethodEntity>)

    @Update
    suspend fun update(entity: PaymentMethodEntity)
}
