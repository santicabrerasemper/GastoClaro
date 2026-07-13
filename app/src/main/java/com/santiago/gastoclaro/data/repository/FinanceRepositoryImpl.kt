package com.santiago.gastoclaro.data.repository

import androidx.room.withTransaction
import com.santiago.gastoclaro.data.local.AppDatabase
import com.santiago.gastoclaro.data.local.entity.CategoryEntity
import com.santiago.gastoclaro.data.local.entity.MonthlyBudgetEntity
import com.santiago.gastoclaro.data.local.entity.MonthlyCategorySnapshotEntity
import com.santiago.gastoclaro.data.local.entity.MonthlyClosureEntity
import com.santiago.gastoclaro.data.local.entity.MovementEntity
import com.santiago.gastoclaro.data.local.entity.MovementType
import com.santiago.gastoclaro.data.local.entity.PaymentMethodEntity
import com.santiago.gastoclaro.data.local.model.ClosureWithSnapshots
import com.santiago.gastoclaro.data.local.model.MovementRow
import com.santiago.gastoclaro.domain.model.DomainException
import com.santiago.gastoclaro.domain.model.MovementDraft
import com.santiago.gastoclaro.domain.model.endEpochDay
import com.santiago.gastoclaro.domain.model.startEpochDay
import com.santiago.gastoclaro.domain.model.toYearMonth
import com.santiago.gastoclaro.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val RECURRING_MONTHS_TO_CREATE = 36

@Singleton
class FinanceRepositoryImpl @Inject constructor(
    private val database: AppDatabase
) : FinanceRepository {
    private val budgetDao = database.budgetDao()
    private val categoryDao = database.categoryDao()
    private val movementDao = database.movementDao()
    private val paymentMethodDao = database.paymentMethodDao()
    private val closureDao = database.closureDao()

    override fun observeBudget(profileId: Long, period: YearMonth): Flow<MonthlyBudgetEntity?> =
        budgetDao.observe(profileId, period.year, period.monthValue)

    override fun observeCategories(profileId: Long): Flow<List<CategoryEntity>> =
        categoryDao.observeActive(profileId)

    override fun observeCategories(profileId: Long, type: MovementType): Flow<List<CategoryEntity>> =
        categoryDao.observeByType(profileId, type)

    override fun observePaymentMethods(profileId: Long): Flow<List<PaymentMethodEntity>> =
        paymentMethodDao.observeActive(profileId)

    override fun observeMovements(profileId: Long, period: YearMonth): Flow<List<MovementRow>> =
        movementDao.observeForPeriod(profileId, period.startEpochDay(), period.endEpochDay())

    override fun observeMovementsBetween(profileId: Long, start: LocalDate, end: LocalDate): Flow<List<MovementRow>> =
        movementDao.observeBetween(profileId, start.toEpochDay(), end.toEpochDay())

    override fun observeClosures(profileId: Long): Flow<List<MonthlyClosureEntity>> =
        closureDao.observeAll(profileId)

    override fun observeClosure(profileId: Long, period: YearMonth): Flow<ClosureWithSnapshots?> =
        closureDao.observeWithSnapshots(profileId, period.year, period.monthValue)

    override suspend fun getMovement(profileId: Long, movementId: Long): MovementRow? =
        movementDao.getRow(profileId, movementId)

    override suspend fun saveMovement(draft: MovementDraft): Long = database.withTransaction {
        validateDraft(draft)
        val nowDate = LocalDate.now()
        if (draft.occurredOn.isAfter(nowDate)) throw DomainException("La fecha no puede ser futura")

        val targetPeriod = draft.occurredOn.toYearMonth()
        if (closureDao.get(draft.profileId, targetPeriod.year, targetPeriod.monthValue) != null) {
            throw DomainException("Ese mes está cerrado. Reabrilo desde Historial para modificarlo")
        }

        val category = categoryDao.getById(draft.profileId, draft.categoryId)
            ?: throw DomainException("La categoría seleccionada ya no existe")
        if (category.type != draft.type || category.isArchived) {
            throw DomainException("Elegí una categoría compatible con el movimiento")
        }

        val paymentMethod = draft.paymentMethodId?.let {
            paymentMethodDao.getById(draft.profileId, it)
                ?: throw DomainException("El medio de pago seleccionado ya no existe")
        }
        if (draft.installmentCount > 1 && paymentMethod?.kind != "CREDIT_CARD") {
            throw DomainException("Las cuotas requieren una tarjeta de crédito")
        }
        if (draft.isRecurringMonthly && (draft.installmentCount > 1 || draft.annualizedMonths > 1)) {
            throw DomainException("El pago mensual no se puede combinar con cuotas o anualización")
        }
        val normalizedCurrency = draft.currency.uppercase().takeIf { it == "ARS" || it == "USD" }
            ?: throw DomainException("Elegí una moneda válida")
        if (normalizedCurrency == "USD" && (draft.exchangeRateCents == null || draft.exchangeRateCents <= 0)) {
            throw DomainException("Ingresá la cotización para convertir USD a pesos")
        }

        val now = System.currentTimeMillis()
        budgetDao.insertIgnore(
            MonthlyBudgetEntity(
                profileId = draft.profileId,
                year = targetPeriod.year,
                month = targetPeriod.monthValue,
                initialAmountCents = 0,
                updatedAt = now
            )
        )

        if (draft.id == null) {
            if (draft.type == MovementType.EXPENSE && draft.isRecurringMonthly) {
                val groupId = UUID.randomUUID().toString()
                var firstId = 0L
                repeat(RECURRING_MONTHS_TO_CREATE) { index ->
                    val recurringDate = draft.occurredOn.plusMonths(index.toLong())
                    val period = recurringDate.toYearMonth()
                    if (closureDao.get(draft.profileId, period.year, period.monthValue) != null) {
                        throw DomainException("Un pago mensual cae en un mes cerrado")
                    }
                    budgetDao.insertIgnore(
                        MonthlyBudgetEntity(
                            profileId = draft.profileId,
                            year = period.year,
                            month = period.monthValue,
                            initialAmountCents = 0,
                            updatedAt = now
                        )
                    )
                    val inserted = movementDao.insert(
                        draft.toEntity(
                            amountCents = draft.amountCents,
                            monthlyImpactCents = draft.amountCents,
                            annualizedMonths = 1,
                            occurredOn = recurringDate,
                            now = now,
                            installmentGroupId = null,
                            installmentIndex = 1,
                            installmentCount = 1,
                            isRecurringMonthly = true,
                            recurringGroupId = groupId,
                            currencyAmountCents = draft.currencyAmountCents,
                            originalAmountCents = draft.amountCents
                        )
                    )
                    if (index == 0) firstId = inserted
                }
                firstId
            } else if (draft.type == MovementType.EXPENSE && draft.installmentCount > 1) {
                val groupId = UUID.randomUUID().toString()
                val base = draft.amountCents / draft.installmentCount
                val remainder = draft.amountCents % draft.installmentCount
                val currencyBase = draft.currencyAmountCents / draft.installmentCount
                val currencyRemainder = draft.currencyAmountCents % draft.installmentCount
                var firstId = 0L
                repeat(draft.installmentCount) { index ->
                    val installmentAmount = base + if (index < remainder) 1 else 0
                    val installmentCurrencyAmount = currencyBase + if (index < currencyRemainder) 1 else 0
                    val installmentDate = draft.occurredOn.plusMonths(index.toLong())
                    val period = installmentDate.toYearMonth()
                    if (closureDao.get(draft.profileId, period.year, period.monthValue) != null) {
                        throw DomainException("Una cuota cae en un mes cerrado")
                    }
                    budgetDao.insertIgnore(
                        MonthlyBudgetEntity(
                            profileId = draft.profileId,
                            year = period.year,
                            month = period.monthValue,
                            initialAmountCents = 0,
                            updatedAt = now
                        )
                    )
                    val inserted = movementDao.insert(
                        draft.toEntity(
                            amountCents = installmentAmount,
                            monthlyImpactCents = installmentAmount,
                            annualizedMonths = 1,
                            occurredOn = installmentDate,
                            now = now,
                            installmentGroupId = groupId,
                            installmentIndex = index + 1,
                            installmentCount = draft.installmentCount,
                            isRecurringMonthly = false,
                            recurringGroupId = null,
                            currencyAmountCents = installmentCurrencyAmount,
                            originalAmountCents = draft.amountCents
                        )
                    )
                    if (index == 0) firstId = inserted
                }
                firstId
            } else {
                movementDao.insert(
                    draft.toEntity(
                        amountCents = draft.amountCents,
                        monthlyImpactCents = draft.monthlyImpactCents,
                        annualizedMonths = draft.annualizedMonths,
                        occurredOn = draft.occurredOn,
                        now = now,
                        installmentGroupId = null,
                        installmentIndex = 1,
                        installmentCount = 1,
                        isRecurringMonthly = false,
                        recurringGroupId = null,
                        currencyAmountCents = draft.currencyAmountCents,
                        originalAmountCents = draft.amountCents
                    )
                )
            }
        } else {
            val previous = movementDao.getEntity(draft.profileId, draft.id)
                ?: throw DomainException("El movimiento ya no existe")
            val originalPeriod = LocalDate.ofEpochDay(previous.occurredEpochDay).toYearMonth()
            if (closureDao.get(draft.profileId, originalPeriod.year, originalPeriod.monthValue) != null) {
                throw DomainException("El mes original está cerrado")
            }
            val updated = previous.copy(
                categoryId = draft.categoryId,
                subcategoryName = draft.subcategoryName.trim(),
                paymentMethodId = draft.paymentMethodId,
                currency = normalizedCurrency,
                currencyAmountCents = draft.currencyAmountCents,
                exchangeRateCents = draft.exchangeRateCents.takeIf { normalizedCurrency == "USD" },
                type = draft.type,
                amountCents = draft.amountCents,
                monthlyImpactCents = draft.monthlyImpactCents,
                annualizedMonths = draft.annualizedMonths,
                installmentGroupId = previous.installmentGroupId,
                installmentIndex = previous.installmentIndex,
                installmentCount = previous.installmentCount,
                isRecurringMonthly = previous.isRecurringMonthly,
                recurringGroupId = previous.recurringGroupId,
                originalAmountCents = previous.originalAmountCents,
                note = draft.note.trim(),
                occurredEpochDay = draft.occurredOn.toEpochDay(),
                updatedAt = now
            )
            if (movementDao.update(updated) == 0) throw DomainException("No se pudo actualizar el movimiento")
            updated.id
        }
    }

    override suspend fun deleteMovement(profileId: Long, movementId: Long) = database.withTransaction {
        val entity = movementDao.getEntity(profileId, movementId) ?: return@withTransaction
        val period = LocalDate.ofEpochDay(entity.occurredEpochDay).toYearMonth()
        if (closureDao.get(profileId, period.year, period.monthValue) != null) {
            throw DomainException("No se puede eliminar un movimiento de un mes cerrado")
        }
        movementDao.delete(entity)
    }

    override suspend fun savePaymentMethod(entity: PaymentMethodEntity): Long = database.withTransaction {
        val normalizedKind = entity.kind.ifBlank { "OTHER" }
        val cleanName = if (normalizedKind == "CASH") "Efectivo" else entity.name.trim()
        if (entity.profileId <= 0) throw DomainException("Selecciona un perfil")
        if (cleanName.length < 2) throw DomainException("Ingresa un nombre de al menos 2 caracteres")
        val cleanDigits = if (normalizedKind == "CASH") {
            ""
        } else {
            entity.lastDigits.filter { it.isDigit() }.takeLast(4)
        }
        if (cleanDigits.isNotBlank() && cleanDigits.length < 2) throw DomainException("Usa entre 2 y 4 digitos")
        val now = System.currentTimeMillis()
        val normalized = entity.copy(
            name = cleanName,
            kind = normalizedKind,
            lastDigits = cleanDigits,
            closingDay = entity.closingDay?.takeIf { normalizedKind == "CREDIT_CARD" && it in 1..31 },
            dueDay = entity.dueDay?.takeIf { normalizedKind == "CREDIT_CARD" && it in 1..31 },
            createdAt = if (entity.id == 0L) now else entity.createdAt,
            updatedAt = now
        )
        if (normalized.id == 0L) {
            if (normalizedKind == "CASH") {
                val existingCash = paymentMethodDao.getFirstByKind(normalized.profileId, "CASH")
                if (existingCash != null) {
                    paymentMethodDao.update(
                        existingCash.copy(
                            name = "Efectivo",
                            lastDigits = "",
                            closingDay = null,
                            dueDay = null,
                            isArchived = false,
                            updatedAt = now
                        )
                    )
                    return@withTransaction existingCash.id
                }
            }
            paymentMethodDao.insert(normalized)
        } else {
            val previous = paymentMethodDao.getById(normalized.profileId, normalized.id)
                ?: throw DomainException("El medio de pago ya no existe")
            paymentMethodDao.update(normalized.copy(createdAt = previous.createdAt))
            normalized.id
        }
    }

    override suspend fun archivePaymentMethod(profileId: Long, paymentMethodId: Long) = database.withTransaction {
        val current = paymentMethodDao.getById(profileId, paymentMethodId) ?: return@withTransaction
        paymentMethodDao.update(current.copy(isArchived = true, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun updateBudget(profileId: Long, period: YearMonth, amountCents: Long) = database.withTransaction {
        if (amountCents < 0) throw DomainException("El presupuesto no puede ser negativo")
        if (closureDao.get(profileId, period.year, period.monthValue) != null) {
            throw DomainException("No se puede editar el presupuesto de un mes cerrado")
        }
        val current = budgetDao.get(profileId, period.year, period.monthValue)
        val entity = MonthlyBudgetEntity(
            id = current?.id ?: 0,
            profileId = profileId,
            year = period.year,
            month = period.monthValue,
            initialAmountCents = amountCents,
            isReopened = current?.isReopened ?: false,
            updatedAt = System.currentTimeMillis()
        )
        budgetDao.upsert(entity)
        Unit
    }

    override suspend fun ensureClosedMonths(today: LocalDate, origin: String) = database.withTransaction {
        val current = today.toYearMonth()
        budgetDao.getAll()
            .filter { !it.isReopened && YearMonth.of(it.year, it.month).isBefore(current) }
            .forEach { budget ->
                val period = YearMonth.of(budget.year, budget.month)
                if (closureDao.get(budget.profileId, budget.year, budget.month) == null) {
                    closeMonthInternal(budget.profileId, period, origin)
                }
            }
    }

    override suspend fun closeMonth(profileId: Long, period: YearMonth, origin: String) =
        database.withTransaction { closeMonthInternal(profileId, period, origin) }

    override suspend fun reopenMonth(profileId: Long, period: YearMonth) = database.withTransaction {
        val budget = budgetDao.get(profileId, period.year, period.monthValue)
            ?: throw DomainException("No existe un presupuesto para ese mes")
        closureDao.delete(profileId, period.year, period.monthValue)
        budgetDao.update(budget.copy(isReopened = true, updatedAt = System.currentTimeMillis()))
    }

    private suspend fun closeMonthInternal(profileId: Long, period: YearMonth, origin: String) {
        if (closureDao.get(profileId, period.year, period.monthValue) != null) return
        val budget = budgetDao.get(profileId, period.year, period.monthValue)
            ?: MonthlyBudgetEntity(
                profileId = profileId,
                year = period.year,
                month = period.monthValue,
                initialAmountCents = 0,
                updatedAt = System.currentTimeMillis()
            )
        val start = period.startEpochDay()
        val end = period.endEpochDay()
        val income = movementDao.sumForPeriod(profileId, MovementType.INCOME, start, end)
        val expenseRows = movementDao
            .getRowsBetween(profileId, period.minusMonths(59).startEpochDay(), end)
            .filter { it.type == MovementType.EXPENSE && it.impacts(period) }
        val expense = expenseRows.sumOf { it.monthlyImpactCents }
        val count = movementDao.countForPeriod(profileId, start, end)
        val closureId = closureDao.insertIgnore(
            MonthlyClosureEntity(
                profileId = profileId,
                year = period.year,
                month = period.monthValue,
                initialAmountCents = budget.initialAmountCents,
                incomeCents = income,
                expenseCents = expense,
                balanceCents = budget.initialAmountCents + income - expense,
                movementCount = count,
                closedAt = System.currentTimeMillis(),
                closureOrigin = origin
            )
        )
        if (closureId <= 0) return
        val snapshots = expenseRows
            .groupBy { it.categoryId }
            .map { (_, rows) ->
                val first = rows.first()
                com.santiago.gastoclaro.data.local.model.CategoryTotalRow(
                    categoryId = first.categoryId,
                    categoryName = first.categoryName,
                    categoryEmoji = first.categoryEmoji,
                    colorArgb = first.categoryColorArgb,
                    amountCents = rows.sumOf { it.monthlyImpactCents }
                )
            }
            .filter { it.amountCents > 0 }
            .map { row ->
            MonthlyCategorySnapshotEntity(
                closureId = closureId,
                categoryName = row.categoryName,
                categoryEmoji = row.categoryEmoji,
                colorArgb = row.colorArgb,
                amountCents = row.amountCents
            )
        }
        if (snapshots.isNotEmpty()) closureDao.insertSnapshots(snapshots)
        val persisted = budgetDao.get(profileId, period.year, period.monthValue)
        if (persisted == null) budgetDao.insertIgnore(budget.copy(isReopened = false))
        else if (persisted.isReopened) budgetDao.update(persisted.copy(isReopened = false, updatedAt = System.currentTimeMillis()))
    }

    private fun validateDraft(draft: MovementDraft) {
        if (draft.profileId <= 0) throw DomainException("Seleccioná un perfil")
        if (draft.amountCents <= 0) throw DomainException("Ingresá un monto mayor a cero")
        if (draft.monthlyImpactCents <= 0) throw DomainException("El impacto mensual debe ser mayor a cero")
        if (draft.annualizedMonths !in 1..60) throw DomainException("La cantidad de meses debe estar entre 1 y 60")
        if (draft.installmentCount !in 1..60) throw DomainException("La cantidad de cuotas debe estar entre 1 y 60")
        if (draft.installmentCount > 1 && draft.annualizedMonths > 1) {
            throw DomainException("Elegí cuotas o anualización, no ambas")
        }
        if (draft.isRecurringMonthly && draft.type != MovementType.EXPENSE) throw DomainException("El pago mensual solo aplica a gastos")
        if (draft.currencyAmountCents <= 0) throw DomainException("Ingresá un monto mayor a cero")
        if (draft.subcategoryName.length > 60) throw DomainException("La subcategoría puede tener hasta 60 caracteres")
        if (draft.note.length > 120) throw DomainException("La nota puede tener hasta 120 caracteres")
    }

    private fun MovementDraft.toEntity(
        amountCents: Long,
        monthlyImpactCents: Long,
        annualizedMonths: Int,
        occurredOn: LocalDate,
        now: Long,
        installmentGroupId: String?,
        installmentIndex: Int,
        installmentCount: Int,
        isRecurringMonthly: Boolean,
        recurringGroupId: String?,
        currencyAmountCents: Long,
        originalAmountCents: Long
    ): MovementEntity = MovementEntity(
        profileId = profileId,
        categoryId = categoryId,
        subcategoryName = subcategoryName.trim(),
        paymentMethodId = paymentMethodId,
        currency = currency.uppercase(),
        currencyAmountCents = currencyAmountCents,
        exchangeRateCents = exchangeRateCents.takeIf { currency.uppercase() == "USD" },
        type = type,
        amountCents = amountCents,
        monthlyImpactCents = monthlyImpactCents,
        annualizedMonths = annualizedMonths,
        installmentGroupId = installmentGroupId,
        installmentIndex = installmentIndex,
        installmentCount = installmentCount,
        isRecurringMonthly = isRecurringMonthly,
        recurringGroupId = recurringGroupId,
        originalAmountCents = originalAmountCents,
        note = note.trim(),
        occurredEpochDay = occurredOn.toEpochDay(),
        createdAt = now,
        updatedAt = now
    )

    private fun MovementRow.impacts(period: YearMonth): Boolean {
        val occurredPeriod = LocalDate.ofEpochDay(occurredEpochDay).toYearMonth()
        return if (annualizedMonths > 1) {
            !period.isBefore(occurredPeriod) && period.isBefore(occurredPeriod.plusMonths(annualizedMonths.toLong()))
        } else {
            occurredPeriod == period
        }
    }
}
