package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class SavingGoalWithTotals(
    @Embedded val goal: SavingGoal,
    val totalDeposits: Double,
    val totalWithdrawals: Double
)

@Dao
interface SavingGoalDao {

    @Query(
        """
        SELECT g.*,
               COALESCE((
                   SELECT SUM(t.amount)
                   FROM saving_transactions t
                   WHERE t.goalId = g.id AND t.type = 'DEPOSIT'
               ), 0.0) AS totalDeposits,
               COALESCE((
                   SELECT SUM(t.amount)
                   FROM saving_transactions t
                   WHERE t.goalId = g.id AND t.type = 'WITHDRAW'
               ), 0.0) AS totalWithdrawals
        FROM saving_goals g
        ORDER BY g.createdAt DESC
        """
    )
    fun observeAllGoalsWithTotals(): Flow<List<SavingGoalWithTotals>>

    @Query(
        """
        SELECT g.*,
               COALESCE((
                   SELECT SUM(t.amount)
                   FROM saving_transactions t
                   WHERE t.goalId = g.id AND t.type = 'DEPOSIT'
               ), 0.0) AS totalDeposits,
               COALESCE((
                   SELECT SUM(t.amount)
                   FROM saving_transactions t
                   WHERE t.goalId = g.id AND t.type = 'WITHDRAW'
               ), 0.0) AS totalWithdrawals
        FROM saving_goals g
        WHERE g.id = :goalId
        """
    )
    fun observeGoalWithTotals(goalId: Long): Flow<SavingGoalWithTotals?>

    @Query("SELECT * FROM saving_goals WHERE id = :goalId")
    suspend fun getGoalById(goalId: Long): SavingGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: SavingGoal): Long

    @Update
    suspend fun update(goal: SavingGoal)

    @Delete
    suspend fun delete(goal: SavingGoal)

    @Query("DELETE FROM saving_goals")
    suspend fun deleteAllGoals()
}
