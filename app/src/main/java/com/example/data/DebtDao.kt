package com.example.data

import androidx.room.*
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Flat join of Debt + its associated DebtPerson. Used in list and detail queries. */
data class DebtWithPerson(
    @Embedded val debt: Debt,
    @Relation(
        parentColumn = "personId",
        entityColumn = "id"
    )
    val person: DebtPerson
)

@Dao
interface DebtDao {

    @Transaction
    @Query("SELECT * FROM debts ORDER BY createdAt DESC")
    fun getAllDebtsWithPerson(): Flow<List<DebtWithPerson>>

    @Transaction
    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getDebtWithPersonById(id: Long): DebtWithPerson?

    @Transaction
    @Query("SELECT * FROM debts WHERE id = :id")
    fun observeDebtWithPerson(id: Long): Flow<DebtWithPerson?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(debt: Debt): Long

    @Update
    suspend fun update(debt: Debt)

    @Delete
    suspend fun delete(debt: Debt)

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getDebtById(id: Long): Debt?

    /** Sum of all INCREASE transactions for a debt. Returns 0 if none. */
    @Query(
        """
        SELECT COALESCE(SUM(amount), 0.0)
        FROM debt_transactions
        WHERE debtId = :debtId AND type = 'INCREASE'
        """
    )
    suspend fun getTotalIncrease(debtId: Long): Double

    /** Sum of all PAYMENT transactions for a debt. Returns 0 if none. */
    @Query(
        """
        SELECT COALESCE(SUM(amount), 0.0)
        FROM debt_transactions
        WHERE debtId = :debtId AND type = 'PAYMENT'
        """
    )
    suspend fun getTotalPayments(debtId: Long): Double
}
