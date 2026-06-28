package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MoneySourceType(
    val label: String,
    val iconName: String
) {
    CASH("Cash", "cash"),
    CHECKING("Bank Account", "bank"),
    SAVINGS("Savings Account", "savings"),
    INVESTMENT("Investment", "investment"),
    CREDIT_CARD("Credit Card", "credit_card"),
    E_WALLET("E-Wallet", "ewallet"),
    OTHER("Other", "other")
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
