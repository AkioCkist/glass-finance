package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saving_transactions",
    foreignKeys = [
        ForeignKey(
            entity = SavingGoal::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("goalId")]
)
data class SavingTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val amount: Double,
    val type: SavingTransactionType,
    val note: String = "",
    val transactionDate: Long,
    val createdAt: Long = System.currentTimeMillis()
)
