package com.santiago.gastoclaro.domain.repository

import com.santiago.gastoclaro.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfiles(): Flow<List<ProfileEntity>>
    suspend fun getProfile(id: Long): ProfileEntity?
    suspend fun createProfile(name: String, emoji: String, colorArgb: Int, initialAmountCents: Long): Long
    suspend fun updateProfile(profile: ProfileEntity)
    suspend fun deleteProfile(profileId: Long)
}
