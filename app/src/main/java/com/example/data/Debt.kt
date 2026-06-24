package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "debts",
    foreignKeys = [
        ForeignKey(
            entity = DebtPerson::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("personId")]
)
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val title: String,
    val note: String = "",
    val originalAmount: Double,
    val createdDate: Long,
    val dueDate: Long? = null,
    val status: DebtStatus = DebtStatus.ACTIVE,
    val direction: DebtDirection = DebtDirection.OWED_TO_ME,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
