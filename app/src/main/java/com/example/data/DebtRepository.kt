package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class DebtRepository(
    private val debtDao: DebtDao,
    private val txDao: DebtTransactionDao
) {

    fun getAllDebtsWithPerson(): Flow<List<DebtWithPerson>> = debtDao.getAllDebtsWithPerson()

    fun observeDebtWithPerson(debtId: Long): Flow<DebtWithPerson?> = debtDao.observeDebtWithPerson(debtId)

    fun getTransactionsForDebt(debtId: Long): Flow<List<DebtTransaction>> =
        txDao.getTransactionsForDebt(debtId)

    /**
     * Creates a new debt and automatically inserts the initial INCREASE transaction.
     */
    suspend fun createDebt(
        personId: Long,
        title: String,
        note: String,
        originalAmount: Double,
        createdDate: Long,
        dueDate: Long?,
        direction: DebtDirection = DebtDirection.OWED_TO_ME
    ): Result<Long> {
        if (title.isBlank()) return Result.failure(IllegalArgumentException("Title cannot be blank"))
        if (originalAmount <= 0) return Result.failure(IllegalArgumentException("Amount must be greater than 0"))

        return try {
            val debt = Debt(
                personId = personId,
                title = title.trim(),
                note = note.trim(),
                originalAmount = originalAmount,
                createdDate = createdDate,
                dueDate = dueDate,
                status = DebtStatus.ACTIVE,
                direction = direction
            )
            val debtId = debtDao.insert(debt)

            // Auto-create the initial INCREASE transaction
            txDao.insert(
                DebtTransaction(
                    debtId = debtId,
                    amount = originalAmount,
                    type = DebtTransactionType.INCREASE,
                    note = "Initial debt",
                    transactionDate = createdDate
                )
            )
            Result.success(debtId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates debt metadata (title, note, dueDate, personId) and refreshes status.
     */
    suspend fun updateDebt(
        debtId: Long,
        personId: Long,
        title: String,
        note: String,
        dueDate: Long?,
        direction: DebtDirection? = null
    ): Result<Unit> {
        if (title.isBlank()) return Result.failure(IllegalArgumentException("Title cannot be blank"))
        val existing = debtDao.getDebtById(debtId)
            ?: return Result.failure(IllegalStateException("Debt not found"))
        return try {
            val updated = existing.copy(
                personId = personId,
                title = title.trim(),
                note = note.trim(),
                dueDate = dueDate,
                direction = direction ?: existing.direction,
                updatedAt = System.currentTimeMillis()
            )
            val refreshed = updated.copy(status = computeStatus(debtId, updated.dueDate))
            debtDao.update(refreshed)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Adds an additional debt amount (INCREASE transaction).
     */
    suspend fun addDebtAmount(debtId: Long, amount: Double, note: String, date: Long): Result<Unit> {
        if (amount <= 0) return Result.failure(IllegalArgumentException("Amount must be greater than 0"))
        val existing = debtDao.getDebtById(debtId)
            ?: return Result.failure(IllegalStateException("Debt not found"))
        return try {
            txDao.insert(
                DebtTransaction(
                    debtId = debtId,
                    amount = amount,
                    type = DebtTransactionType.INCREASE,
                    note = note.trim(),
                    transactionDate = date
                )
            )
            refreshStatus(existing)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Records a payment (PAYMENT transaction).
     */
    suspend fun addPayment(debtId: Long, amount: Double, note: String, date: Long): Result<Unit> {
        if (amount <= 0) return Result.failure(IllegalArgumentException("Amount must be greater than 0"))
        val existing = debtDao.getDebtById(debtId)
            ?: return Result.failure(IllegalStateException("Debt not found"))
        return try {
            txDao.insert(
                DebtTransaction(
                    debtId = debtId,
                    amount = amount,
                    type = DebtTransactionType.PAYMENT,
                    note = note.trim(),
                    transactionDate = date
                )
            )
            refreshStatus(existing)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a debt. Transactions are cascade-deleted via FK.
     */
    suspend fun deleteDebt(debtId: Long): Result<Unit> {
        val existing = debtDao.getDebtById(debtId)
            ?: return Result.failure(IllegalStateException("Debt not found"))
        return try {
            debtDao.delete(existing)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private suspend fun refreshStatus(debt: Debt) {
        val newStatus = computeStatus(debt.id, debt.dueDate)
        if (newStatus != debt.status) {
            debtDao.update(debt.copy(status = newStatus, updatedAt = System.currentTimeMillis()))
        }
    }

    /**
     * Calculates the correct status based on transactions and due date.
     *
     * Rules:
     *  - remaining <= 0  → PAID
     *  - dueDate exists AND dueDate < today AND remaining > 0 → OVERDUE
     *  - otherwise → ACTIVE
     */
    suspend fun computeStatus(debtId: Long, dueDate: Long?): DebtStatus {
        val totalIncrease = debtDao.getTotalIncrease(debtId)
        val totalPayments = debtDao.getTotalPayments(debtId)
        val remaining = totalIncrease - totalPayments

        return when {
            remaining <= 0.0 -> DebtStatus.PAID
            dueDate != null && dueDate < startOfToday() -> DebtStatus.OVERDUE
            else -> DebtStatus.ACTIVE
        }
    }

    suspend fun getRemainingAmount(debtId: Long): Double {
        val inc = debtDao.getTotalIncrease(debtId)
        val paid = debtDao.getTotalPayments(debtId)
        return (inc - paid).coerceAtLeast(0.0)
    }

    private fun startOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
