package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Transaction::class,
        DebtPerson::class,
        Debt::class,
        DebtTransaction::class
    ],
    version = 3,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun debtPersonDao(): DebtPersonDao
    abstract fun debtDao(): DebtDao
    abstract fun debtTransactionDao(): DebtTransactionDao

    companion object {
        @Volatile
        private var Instance: FinanceDatabase? = null

        fun getDatabase(context: Context): FinanceDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, FinanceDatabase::class.java, "finance_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
