package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MoneySourceType(
    val iconName: String
) {
    CASH("cash"),
    CHECKING("bank"),
    SAVINGS("savings"),
    INVESTMENT("investment"),
    CREDIT_CARD("credit_card"),
    E_WALLET("ewallet"),
    OTHER("other")
}

@Entity(tableName = "money_sources")
data class MoneySource(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: MoneySourceType,
    val name: String,
    val balance: Double = 0.0,
    val currency: String = "VND"
)
