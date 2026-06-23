package com.example

import android.app.Application
import com.example.data.FinanceDatabase

class FinanceApplication : Application() {
    val database by lazy { FinanceDatabase.getDatabase(this) }
}
