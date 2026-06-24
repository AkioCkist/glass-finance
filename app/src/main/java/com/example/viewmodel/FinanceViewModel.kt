package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Transaction
import com.example.data.TransactionDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ChartPeriod { DAY, WEEK, MONTH, YEAR }

class FinanceViewModel(private val transactionDao: TransactionDao) : ViewModel() {

    val transactions: StateFlow<List<Transaction>> = transactionDao.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalBalance: StateFlow<Double> = transactions.map { list ->
        list.sumOf { if (it.isIncome) it.amount else -it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

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
                    ChartBar(label, txs.filter { it.isIncome }.sumOf { it.amount }, txs.filter { !it.isIncome }.sumOf { it.amount })
                }
            }
            ChartPeriod.WEEK -> {
                // Last 7 days, each bar = 1 day
                val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                (6 downTo 0).reversed().map { daysAgo ->
                    val day = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -(6 - daysAgo)) }
                    val label = dayNames[day.get(Calendar.DAY_OF_WEEK) - 1]
                    val txs = allTransactions.filter {
                        val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        c.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR) &&
                            c.get(Calendar.YEAR) == day.get(Calendar.YEAR)
                    }
                    ChartBar(label, txs.filter { it.isIncome }.sumOf { it.amount }, txs.filter { !it.isIncome }.sumOf { it.amount })
                }
            }
            ChartPeriod.MONTH -> {
                // Last 5 weeks, each bar = 1 week
                (4 downTo 0).reversed().map { weeksAgo ->
                    val weekCal = Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, -(4 - weeksAgo)) }
                    val label = "W${weeksAgo + 1}"
                    val txs = allTransactions.filter {
                        val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        c.get(Calendar.WEEK_OF_YEAR) == weekCal.get(Calendar.WEEK_OF_YEAR) &&
                            c.get(Calendar.YEAR) == weekCal.get(Calendar.YEAR)
                    }
                    ChartBar(label, txs.filter { it.isIncome }.sumOf { it.amount }, txs.filter { !it.isIncome }.sumOf { it.amount })
                }
            }
            ChartPeriod.YEAR -> {
                // Last 12 months, each bar = 1 month
                val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                (11 downTo 0).reversed().map { monthsAgo ->
                    val monthCal = Calendar.getInstance().apply { add(Calendar.MONTH, -(11 - monthsAgo)) }
                    val m = monthCal.get(Calendar.MONTH)
                    val y = monthCal.get(Calendar.YEAR)
                    val label = monthNames[m]
                    val txs = allTransactions.filter {
                        val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        c.get(Calendar.MONTH) == m && c.get(Calendar.YEAR) == y
                    }
                    ChartBar(label, txs.filter { it.isIncome }.sumOf { it.amount }, txs.filter { !it.isIncome }.sumOf { it.amount })
                }
            }
        }
    }

    fun addTransaction(amount: Double, note: String, isIncome: Boolean) {
        viewModelScope.launch {
            transactionDao.insertTransaction(
                Transaction(
                    amount = amount,
                    note = note,
                    isIncome = isIncome,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}

data class ChartBar(val label: String, val income: Double, val spend: Double) {
    val total get() = income + spend
}

class FinanceViewModelFactory(private val transactionDao: TransactionDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(transactionDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
