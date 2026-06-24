package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtTransactionDao {

    @Query("SELECT * FROM debt_transactions WHERE debtId = :debtId ORDER BY transactionDate DESC, createdAt DESC")
    fun getTransactionsForDebt(debtId: Long): Flow<List<DebtTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: DebtTransaction): Long

    @Delete
    suspend fun delete(tx: DebtTransaction)

    @Query("DELETE FROM debt_transactions WHERE debtId = :debtId")
    suspend fun deleteAllForDebt(debtId: Long)
}
