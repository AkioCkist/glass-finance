package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingTransactionDao {

    @Query("SELECT * FROM saving_transactions WHERE goalId = :goalId ORDER BY transactionDate DESC, createdAt DESC")
    fun getTransactionsForGoal(goalId: Long): Flow<List<SavingTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: SavingTransaction): Long

    @Query(
        """
        SELECT COALESCE(SUM(
            CASE
                WHEN type = 'DEPOSIT' THEN amount
                WHEN type = 'WITHDRAW' THEN -amount
                ELSE 0
            END
        ), 0.0)
        FROM saving_transactions
        WHERE goalId = :goalId
        """
    )
    suspend fun getCurrentAmount(goalId: Long): Double

    @Query("DELETE FROM saving_transactions WHERE goalId = :goalId")
    suspend fun deleteAllForGoal(goalId: Long)

    @Query("DELETE FROM saving_transactions")
    suspend fun deleteAllTransactions()
}
