package com.example.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.text.NumberFormat
import java.util.Locale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.MoneySource
import com.example.data.MoneySourceDao
import com.example.data.DebtDirection
import com.example.data.DebtRepository
import com.example.data.DebtStatus
import com.example.data.MoneySourceType
import com.example.data.Transaction
import com.example.data.TransactionDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

enum class ChartPeriod { DAY, WEEK, MONTH, YEAR }

class FinanceViewModel(
    application: Application,
    private val transactionDao: TransactionDao,
    private val moneySourceDao: MoneySourceDao,
    private val debtRepository: DebtRepository
) : AndroidViewModel(application) {

    // Seed default money sources on first launch
    init {
        viewModelScope.launch {
            seedDefaultMoneySourcesIfEmpty()
        }
    }

    val transactions: StateFlow<List<Transaction>> = transactionDao.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ── Money Sources ────────────────────────────────────────────────────────
    val moneySources: StateFlow<List<MoneySource>> = moneySourceDao.getAllMoneySources()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalBalance: StateFlow<Double> = moneySources.map { list ->
        list.sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /** Money sources with balance > 0 (for OverviewScreen) */
    val activeMoneySources: StateFlow<List<MoneySource>> = moneySources.map { list ->
        list.filter { it.balance > 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Debt Summary ─────────────────────────────────────────────────────────
    val debtSummary: StateFlow<DebtSummary> = debtRepository.getAllDebtsWithPerson().map { debts ->
        val active =
            debts.filter { it.debt.status == DebtStatus.ACTIVE || it.debt.status == DebtStatus.OVERDUE }
        val owedToMe = active.filter { it.debt.direction == DebtDirection.OWED_TO_ME }
            .sumOf { it.debt.originalAmount }
        val iOwe = active.filter { it.debt.direction == DebtDirection.I_OWE }
            .sumOf { it.debt.originalAmount }
        DebtSummary(owedToMe = owedToMe, iOwe = iOwe)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DebtSummary())

    val monthlyIncome: StateFlow<Double> = transactions.map { list ->
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        list.filter {
            val txCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            it.isIncome &&
                    txCal.get(Calendar.MONTH) == currentMonth &&
                    txCal.get(Calendar.YEAR) == currentYear
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlySpend: StateFlow<Double> = transactions.map { list ->
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        list.filter {
            val txCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            !it.isIncome &&
                    txCal.get(Calendar.MONTH) == currentMonth &&
                    txCal.get(Calendar.YEAR) == currentYear
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /**
     * Returns a list of ChartBar objects grouped by the given period.
     * Each bar holds income + spend totals for that time bucket.
     */
    fun getChartData(period: ChartPeriod, allTransactions: List<Transaction>): List<ChartBar> {
        val now = Calendar.getInstance()
        return when (period) {
            ChartPeriod.DAY -> {
                // 24 hours of today, each bar = 1 hour
                (0..23).map { hour ->
                    val label = "${hour}h"
                    val txs = allTransactions.filter {
                        val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        c.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) &&
                                c.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                                c.get(Calendar.HOUR_OF_DAY) == hour
                    }
                    ChartBar(
                        label,
                        txs.filter { it.isIncome }.sumOf { it.amount },
                        txs.filter { !it.isIncome }.sumOf { it.amount })
                }
            }

            ChartPeriod.WEEK -> {
                // Last 7 days, each bar = 1 day
                val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                (6 downTo 0).reversed().map { daysAgo ->
                    val day =
                        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -(6 - daysAgo)) }
                    val label = dayNames[day.get(Calendar.DAY_OF_WEEK) - 1]
                    val txs = allTransactions.filter {
                        val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        c.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR) &&
                                c.get(Calendar.YEAR) == day.get(Calendar.YEAR)
                    }
                    ChartBar(
                        label,
                        txs.filter { it.isIncome }.sumOf { it.amount },
                        txs.filter { !it.isIncome }.sumOf { it.amount })
                }
            }

            ChartPeriod.MONTH -> {
                // Last 5 weeks, each bar = 1 week
                (4 downTo 0).reversed().map { weeksAgo ->
                    val weekCal =
                        Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, -(4 - weeksAgo)) }
                    val label = "W${weeksAgo + 1}"
                    val txs = allTransactions.filter {
                        val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        c.get(Calendar.WEEK_OF_YEAR) == weekCal.get(Calendar.WEEK_OF_YEAR) &&
                                c.get(Calendar.YEAR) == weekCal.get(Calendar.YEAR)
                    }
                    ChartBar(
                        label,
                        txs.filter { it.isIncome }.sumOf { it.amount },
                        txs.filter { !it.isIncome }.sumOf { it.amount })
                }
            }

            ChartPeriod.YEAR -> {
                // Last 12 months, each bar = 1 month
                val monthNames = listOf(
                    "Jan",
                    "Feb",
                    "Mar",
                    "Apr",
                    "May",
                    "Jun",
                    "Jul",
                    "Aug",
                    "Sep",
                    "Oct",
                    "Nov",
                    "Dec"
                )
                (11 downTo 0).reversed().map { monthsAgo ->
                    val monthCal =
                        Calendar.getInstance().apply { add(Calendar.MONTH, -(11 - monthsAgo)) }
                    val m = monthCal.get(Calendar.MONTH)
                    val y = monthCal.get(Calendar.YEAR)
                    val label = monthNames[m]
                    val txs = allTransactions.filter {
                        val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        c.get(Calendar.MONTH) == m && c.get(Calendar.YEAR) == y
                    }
                    ChartBar(
                        label,
                        txs.filter { it.isIncome }.sumOf { it.amount },
                        txs.filter { !it.isIncome }.sumOf { it.amount })
                }
            }
        }
    }

    fun addMoneySource(type: MoneySourceType, name: String, balance: Double) {
        viewModelScope.launch {
            moneySourceDao.insertMoneySource(
                MoneySource(
                    type = type,
                    name = name,
                    balance = balance
                )
            )
        }
    }

    fun removeMoneySource(id: Long) {
        viewModelScope.launch {
            moneySourceDao.deleteMoneySource(id)
        }
    }

    fun updateMoneySourceBalance(id: Long, newBalance: Double) {
        viewModelScope.launch {
            moneySourceDao.updateMoneySourceBalance(id, newBalance)
        }
    }

    fun addTransaction(
        amount: Double,
        note: String,
        isIncome: Boolean,
        moneySourceId: Long? = null
    ) {
        viewModelScope.launch {
            transactionDao.insertTransaction(
                Transaction(
                    amount = amount,
                    note = note,
                    isIncome = isIncome,
                    timestamp = System.currentTimeMillis(),
                    moneySourceId = moneySourceId
                )
            )
            // Update source balance
            if (moneySourceId != null) {
                val source = moneySourceDao.getAllMoneySourcesOnce().find { it.id == moneySourceId }
                if (source != null) {
                    val delta = if (isIncome) amount else -amount
                    moneySourceDao.updateMoneySourceBalance(moneySourceId, source.balance + delta)
                }
            }
        }
    }

    fun exportCSV(sources: List<MoneySource>, totalBalance: Double) {
        viewModelScope.launch {
            try {
                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(java.util.Date())
                val fileName = "finance_report_$timestamp.csv"

                val csvContent = buildString {
                    appendLine("Money Sources Report")
                    appendLine("Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(java.util.Date())}")
                    appendLine("Total Balance: ${NumberFormat.getNumberInstance(Locale.US).format(totalBalance)} VND")
                    appendLine()
                    appendLine("Source Name,Type,Balance (VND),Percentage")

                    sources.forEach { source ->
                        val pct = if (totalBalance > 0) {
                            (source.balance / totalBalance * 100).toInt()
                        } else 0
                        appendLine("${source.name},${source.type.name},${NumberFormat.getNumberInstance(Locale.US).format(source.balance)},$pct%")
                    }

                    appendLine()
                    appendLine("Summary:")
                    appendLine("Total Sources: ${sources.size}")
                    appendLine("Active Sources: ${sources.count { it.balance > 0 }}")
                    appendLine("Zero Balance Sources: ${sources.count { it.balance == 0.0 }}")
                }

                // Lưu file
                val context = getApplication<Application>().applicationContext
                saveCSVToStorage(context, fileName, csvContent)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            try {
                debtRepository.clearAllDebtData()
                transactionDao.deleteAllTransactions()
                moneySourceDao.deleteAllMoneySources()
                seedDefaultMoneySourcesIfEmpty()
                showToast(getApplication(), "All data has been reset")
            } catch (e: Exception) {
                e.printStackTrace()
                showToast(getApplication(), "Failed to reset data: ${e.message}")
            }
        }
    }

    private suspend fun seedDefaultMoneySourcesIfEmpty() {
        val existing = moneySourceDao.getAllMoneySourcesOnce()
        if (existing.isEmpty()) {
            val defaults = listOf(
                MoneySource(type = MoneySourceType.CASH, name = "Cash", balance = 0.0),
                MoneySource(
                    type = MoneySourceType.CHECKING,
                    name = "Bank Account",
                    balance = 0.0
                ),
                MoneySource(
                    type = MoneySourceType.SAVINGS,
                    name = "Savings Account",
                    balance = 0.0
                ),
                MoneySource(type = MoneySourceType.E_WALLET, name = "E-Wallet", balance = 0.0),
                MoneySource(
                    type = MoneySourceType.CREDIT_CARD,
                    name = "Credit Card",
                    balance = 0.0
                ),
                MoneySource(
                    type = MoneySourceType.INVESTMENT,
                    name = "Investment",
                    balance = 0.0
                ),
                MoneySource(type = MoneySourceType.OTHER, name = "Other", balance = 0.0)
            )
            defaults.forEach { moneySourceDao.insertMoneySource(it) }
        }
    }

    private fun saveCSVToStorage(context: Context, fileName: String, content: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ sử dụng MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(content.toByteArray())
                    }
                    // Hiển thị thông báo thành công
                    showToast(context, "CSV exported to Downloads folder")
                }
            } else {
                // Android 9 và thấp hơn
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)

                FileOutputStream(file).use { outputStream ->
                    outputStream.write(content.toByteArray())
                }

                showToast(context, "CSV exported to Downloads folder")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(context, "Failed to export CSV: ${e.message}")
        }
    }

    private fun showToast(context: Context, message: String) {
        android.os.Handler(context.mainLooper).post {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
data class ChartBar(val label: String, val income: Double, val spend: Double) {
    val total get() = income + spend
}

data class DebtSummary(
    val owedToMe: Double = 0.0,
    val iOwe: Double = 0.0
)

class FinanceViewModelFactory(
    private val application: Application,
    private val transactionDao: TransactionDao,
    private val moneySourceDao: MoneySourceDao,
    private val debtRepository: DebtRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(application, transactionDao, moneySourceDao, debtRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
