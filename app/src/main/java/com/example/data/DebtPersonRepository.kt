package com.example.data

import kotlinx.coroutines.flow.Flow

class DebtPersonRepository(private val dao: DebtPersonDao) {

    fun getAllPersons(): Flow<List<DebtPerson>> = dao.getAllPersons()

    fun searchPersons(query: String): Flow<List<DebtPerson>> =
        if (query.isBlank()) dao.getAllPersons() else dao.searchPersons(query)

    suspend fun getPersonById(id: Long): DebtPerson? = dao.getPersonById(id)

    /**
     * Adds a new person. Returns failure if the name already exists (case-insensitive trim).
     */
    suspend fun addPerson(name: String): Result<Long> {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return Result.failure(IllegalArgumentException("Name cannot be blank"))
        if (dao.nameExists(trimmed)) return Result.failure(IllegalArgumentException("A person named \"$trimmed\" already exists"))
        return try {
            val id = dao.insert(DebtPerson(name = trimmed))
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates an existing person. Returns failure if the new name conflicts with another person.
     */
    suspend fun updatePerson(person: DebtPerson, newName: String): Result<Unit> {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return Result.failure(IllegalArgumentException("Name cannot be blank"))
        if (dao.nameExists(trimmed, excludeId = person.id)) {
            return Result.failure(IllegalArgumentException("A person named \"$trimmed\" already exists"))
        }
        return try {
            dao.update(person.copy(name = trimmed, updatedAt = System.currentTimeMillis()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a person only if they have no associated debts.
     */
    suspend fun deletePerson(person: DebtPerson): Result<Unit> {
        return if (dao.hasDebts(person.id)) {
            Result.failure(IllegalStateException("Cannot delete \"${person.name}\" — they have existing debts. Remove those debts first."))
        } else {
            try {
                dao.delete(person)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
