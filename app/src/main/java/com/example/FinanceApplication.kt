package com.example

import android.app.Application
import com.example.data.FinanceDatabase

class FinanceApplication : Application() {
    val database by lazy { FinanceDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        // Pre-warm Room DB on background thread during splash screen
        // Ensures DB is ready before first frame needs it
        Thread {
            database
        }.start()
    }
}
