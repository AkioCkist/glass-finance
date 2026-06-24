package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtPersonDao {

    @Query("SELECT * FROM debt_persons ORDER BY name ASC")
    fun getAllPersons(): Flow<List<DebtPerson>>

    @Query("SELECT * FROM debt_persons WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchPersons(query: String): Flow<List<DebtPerson>>

    @Query("SELECT * FROM debt_persons WHERE id = :id")
    suspend fun getPersonById(id: Long): DebtPerson?

    @Query("SELECT COUNT(*) > 0 FROM debts WHERE personId = :personId")
    suspend fun hasDebts(personId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(person: DebtPerson): Long

    @Update
    suspend fun update(person: DebtPerson)

    @Delete
    suspend fun delete(person: DebtPerson)

    @Query("SELECT COUNT(*) > 0 FROM debt_persons WHERE name = :name AND id != :excludeId")
    suspend fun nameExists(name: String, excludeId: Long = -1L): Boolean
}
