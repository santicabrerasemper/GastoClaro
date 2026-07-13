package com.santiago.gastoclaro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.santiago.gastoclaro.data.local.entity.CategoryEntity
import com.santiago.gastoclaro.data.local.entity.MovementType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE profileId = :profileId AND isArchived = 0 ORDER BY type, name")
    fun observeActive(profileId: Long): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE profileId = :profileId AND type = :type AND isArchived = 0 ORDER BY name")
    fun observeByType(profileId: Long, type: MovementType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id AND profileId = :profileId LIMIT 1")
    suspend fun getById(profileId: Long, id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)
}
