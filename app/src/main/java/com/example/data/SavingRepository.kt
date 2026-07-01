package com.example.data

import kotlinx.coroutines.flow.Flow

class SavingRepository(
    private val goalDao: SavingGoalDao,
    private val transactionDao: SavingTransactionDao
) {

    fun observeAllGoalsWithTotals(): Flow<List<SavingGoalWithTotals>> =
        goalDao.observeAllGoalsWithTotals()

    fun observeGoalWithTotals(goalId: Long): Flow<SavingGoalWithTotals?> =
        goalDao.observeGoalWithTotals(goalId)

    fun observeTransactions(goalId: Long): Flow<List<SavingTransaction>> =
        transactionDao.getTransactionsForGoal(goalId)

    suspend fun createGoal(
        title: String,
        icon: String,
        note: String,
        targetAmount: Double,
        initialAmount: Double,
        deadline: Long?,
        createdDate: Long
    ): Result<Long> {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) return Result.failure(IllegalArgumentException("Title cannot be blank"))
        if (targetAmount <= 0.0) return Result.failure(IllegalArgumentException("Target amount must be greater than 0"))
        if (initialAmount < 0.0) return Result.failure(IllegalArgumentException("Initial amount cannot be negative"))

        return try {
            val goalId = goalDao.insert(
                SavingGoal(
                    title = trimmedTitle,
                    icon = icon.ifBlank { "🎯" },
                    note = note.trim(),
                    targetAmount = targetAmount,
                    initialAmount = initialAmount,
                    deadline = deadline,
                    createdDate = createdDate
                )
            )

            if (initialAmount > 0.0) {
                transactionDao.insert(
                    SavingTransaction(
                        goalId = goalId,
                        amount = initialAmount,
                        type = SavingTransactionType.DEPOSIT,
                        note = "Initial Saving",
                        transactionDate = createdDate
                    )
                )
            }

            Result.success(goalId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGoal(
        goalId: Long,
        title: String,
        icon: String,
        note: String,
        targetAmount: Double,
        deadline: Long?
    ): Result<Unit> {
        val existing = goalDao.getGoalById(goalId)
            ?: return Result.failure(IllegalStateException("Goal not found"))
        if (title.isBlank()) return Result.failure(IllegalArgumentException("Title cannot be blank"))
        if (targetAmount <= 0.0) return Result.failure(IllegalArgumentException("Target amount must be greater than 0"))

        return try {
            goalDao.update(
                existing.copy(
                    title = title.trim(),
                    icon = icon.ifBlank { "🎯" },
                    note = note.trim(),
                    targetAmount = targetAmount,
                    deadline = deadline,
                    updatedAt = System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addDeposit(goalId: Long, amount: Double, note: String, date: Long): Result<Unit> {
        if (amount <= 0.0) return Result.failure(IllegalArgumentException("Amount must be greater than 0"))
        val existing = goalDao.getGoalById(goalId)
            ?: return Result.failure(IllegalStateException("Goal not found"))

        return try {
            transactionDao.insert(
                SavingTransaction(
                    goalId = existing.id,
                    amount = amount,
                    type = SavingTransactionType.DEPOSIT,
                    note = note.trim(),
                    transactionDate = date
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addWithdraw(goalId: Long, amount: Double, note: String, date: Long): Result<Unit> {
        if (amount <= 0.0) return Result.failure(IllegalArgumentException("Amount must be greater than 0"))
        val existing = goalDao.getGoalById(goalId)
            ?: return Result.failure(IllegalStateException("Goal not found"))

        val currentAmount = transactionDao.getCurrentAmount(goalId)
        if (amount > currentAmount) {
            return Result.failure(IllegalArgumentException("Withdraw amount exceeds current saving"))
        }

        return try {
            transactionDao.insert(
                SavingTransaction(
                    goalId = existing.id,
                    amount = amount,
                    type = SavingTransactionType.WITHDRAW,
                    note = note.trim(),
                    transactionDate = date
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGoal(goalId: Long): Result<Unit> {
        val existing = goalDao.getGoalById(goalId)
            ?: return Result.failure(IllegalStateException("Goal not found"))

        return try {
            goalDao.delete(existing)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAllSavingsData() {
        transactionDao.deleteAllTransactions()
        goalDao.deleteAllGoals()
    }
}
