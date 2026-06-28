package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MoneySourceDao {
    @Query("SELECT * FROM money_sources ORDER BY id")
    fun getAllMoneySources(): Flow<List<MoneySource>>

    @Query("SELECT * FROM money_sources ORDER BY id")
    suspend fun getAllMoneySourcesOnce(): List<MoneySource>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoneySource(moneySource: MoneySource)

    @Query("DELETE FROM money_sources WHERE id = :id")
    suspend fun deleteMoneySource(id: Long)

    @Query("UPDATE money_sources SET balance = :newBalance WHERE id = :id")
    suspend fun updateMoneySourceBalance(id: Long, newBalance: Double)
}