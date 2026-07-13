package com.santiago.gastoclaro.data.repository

import androidx.room.withTransaction
import com.santiago.gastoclaro.data.local.AppDatabase
import com.santiago.gastoclaro.data.local.entity.CategoryEntity
import com.santiago.gastoclaro.data.local.entity.MonthlyBudgetEntity
import com.santiago.gastoclaro.data.local.entity.MovementType
import com.santiago.gastoclaro.data.local.entity.PaymentMethodEntity
import com.santiago.gastoclaro.data.local.entity.ProfileEntity
import com.santiago.gastoclaro.domain.model.DomainException
import com.santiago.gastoclaro.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val database: AppDatabase
) : ProfileRepository {
    private val profileDao = database.profileDao()
    private val categoryDao = database.categoryDao()
    private val budgetDao = database.budgetDao()
    private val paymentMethodDao = database.paymentMethodDao()

    override fun observeProfiles(): Flow<List<ProfileEntity>> = profileDao.observeAll()

    override suspend fun getProfile(id: Long): ProfileEntity? = profileDao.getById(id)

    override suspend fun createProfile(
        name: String,
        emoji: String,
        colorArgb: Int,
        initialAmountCents: Long
    ): Long = database.withTransaction {
        val cleanName = name.trim()
        if (cleanName.length < 2) throw DomainException("Ingresá un nombre de al menos 2 caracteres")
        if (initialAmountCents < 0) throw DomainException("El presupuesto no puede ser negativo")

        val now = System.currentTimeMillis()
        val id = profileDao.insert(
            ProfileEntity(
                name = cleanName,
                emoji = emoji.ifBlank { "👤" },
                colorArgb = colorArgb,
                createdAt = now
            )
        )
        categoryDao.insertAll(defaultCategories(id))
        paymentMethodDao.insertAll(defaultPaymentMethods(id, now))
        val current = YearMonth.now()
        budgetDao.insertIgnore(
            MonthlyBudgetEntity(
                profileId = id,
                year = current.year,
                month = current.monthValue,
                initialAmountCents = initialAmountCents,
                updatedAt = now
            )
        )
        id
    }

    override suspend fun updateProfile(profile: ProfileEntity) {
        if (profile.name.trim().length < 2) throw DomainException("El nombre es demasiado corto")
        profileDao.update(profile.copy(name = profile.name.trim()))
    }

    override suspend fun deleteProfile(profileId: Long) {
        val profile = profileDao.getById(profileId) ?: return
        profileDao.delete(profile)
    }

    private fun defaultCategories(profileId: Long): List<CategoryEntity> = listOf(
        CategoryEntity(profileId = profileId, name = "Comida", emoji = "🍔", colorArgb = 0xFFE57373.toInt(), type = MovementType.EXPENSE),
        CategoryEntity(profileId = profileId, name = "Transporte", emoji = "🚗", colorArgb = 0xFF64B5F6.toInt(), type = MovementType.EXPENSE),
        CategoryEntity(profileId = profileId, name = "Hogar", emoji = "🏠", colorArgb = 0xFFFFB74D.toInt(), type = MovementType.EXPENSE),
        CategoryEntity(profileId = profileId, name = "Salud", emoji = "💊", colorArgb = 0xFF81C784.toInt(), type = MovementType.EXPENSE),
        CategoryEntity(profileId = profileId, name = "Ocio", emoji = "🎮", colorArgb = 0xFFBA68C8.toInt(), type = MovementType.EXPENSE),
        CategoryEntity(profileId = profileId, name = "Otros", emoji = "🧾", colorArgb = 0xFF90A4AE.toInt(), type = MovementType.EXPENSE),
        CategoryEntity(profileId = profileId, name = "Sueldo", emoji = "💼", colorArgb = 0xFF4DB6AC.toInt(), type = MovementType.INCOME),
        CategoryEntity(profileId = profileId, name = "Extra", emoji = "✨", colorArgb = 0xFF7986CB.toInt(), type = MovementType.INCOME),
        CategoryEntity(profileId = profileId, name = "Reintegro", emoji = "↩️", colorArgb = 0xFFAED581.toInt(), type = MovementType.INCOME)
    )
    private fun defaultPaymentMethods(profileId: Long, now: Long): List<PaymentMethodEntity> = listOf(
        PaymentMethodEntity(profileId = profileId, name = "Efectivo", kind = "CASH", createdAt = now, updatedAt = now),
        PaymentMethodEntity(profileId = profileId, name = "Cuenta bancaria", kind = "BANK", createdAt = now, updatedAt = now),
        PaymentMethodEntity(profileId = profileId, name = "Debito", kind = "DEBIT_CARD", createdAt = now, updatedAt = now),
        PaymentMethodEntity(profileId = profileId, name = "Visa", kind = "CREDIT_CARD", createdAt = now, updatedAt = now)
    )
}
