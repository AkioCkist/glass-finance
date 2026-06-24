package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debt_persons")
data class DebtPerson(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
