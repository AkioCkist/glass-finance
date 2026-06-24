package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "debt_transactions",
    foreignKeys = [
        ForeignKey(
            entity = Debt::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("debtId")]
)
data class DebtTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val amount: Double,
    val type: DebtTransactionType,
    val note: String = "",
    val transactionDate: Long,
    val createdAt: Long = System.currentTimeMillis()
)
