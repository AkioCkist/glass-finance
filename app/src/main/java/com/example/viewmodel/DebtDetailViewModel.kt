package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DebtRepository
import com.example.data.DebtTransaction
import com.example.data.DebtTransactionType
import com.example.data.DebtWithPerson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

data class DebtStats(
    val totalAdded: Double = 0.0,
    val totalPaid: Double = 0.0,
    val remaining: Double = 0.0,
    val progressPercent: Float = 0f  // 0–100
)

/** How many days until (positive) or past (negative) the due date. Null if no due date. */
data class DueDateStatus(
    val daysUntilDue: Long?,      // null = no due date
    val displayText: String        // e.g. "Due in 3 days", "Overdue by 5 days", "Due today"
)

data class DebtDetailUiState(
    val debtWithPerson: DebtWithPerson? = null,
    val transactions: List<DebtTransaction> = emptyList(),
    val stats: DebtStats = DebtStats(),
    val dueDateStatus: DueDateStatus = DueDateStatus(null, ""),
    val isLoading: Boolean = true,
    val error: String? = null,
    val actionSuccess: String? = null   // brief success message for snackbar
)

class DebtDetailViewModel(
    private val debtId: Long,
    private val repository: DebtRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebtDetailUiState())
    val uiState: StateFlow<DebtDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeDebtWithPerson(debtId).filterNotNull(),
                repository.getTransactionsForDebt(debtId)
            ) { debtWithPerson, transactions ->
                val totalAdded = transactions.filter { it.type == DebtTransactionType.INCREASE }.sumOf { it.amount }
                val totalPaid = transactions.filter { it.type == DebtTransactionType.PAYMENT }.sumOf { it.amount }
                val remaining = (totalAdded - totalPaid).coerceAtLeast(0.0)
                val progress = if (totalAdded > 0) ((totalPaid / totalAdded) * 100).toFloat().coerceIn(0f, 100f) else 0f

                val stats = DebtStats(totalAdded, totalPaid, remaining, progress)
                val dueDateStatus = buildDueDateStatus(debtWithPerson.debt.dueDate)

                DebtDetailUiState(
                    debtWithPerson = debtWithPerson,
                    transactions = transactions,
                    stats = stats,
                    dueDateStatus = dueDateStatus,
                    isLoading = false
                )
            }.catch { e ->
                emit(DebtDetailUiState(isLoading = false, error = e.message))
            }.collect { state ->
                _uiState.value = state.copy(
                    error = _uiState.value.error,
                    actionSuccess = _uiState.value.actionSuccess
                )
            }
        }
    }

    fun addDebtAmount(amount: Double, note: String, date: Long) {
        viewModelScope.launch {
            repository.addDebtAmount(debtId, amount, note, date)
                .onSuccess { _uiState.update { it.copy(actionSuccess = "Debt amount added") } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun addPayment(amount: Double, note: String, date: Long) {
        viewModelScope.launch {
            repository.addPayment(debtId, amount, note, date)
                .onSuccess { _uiState.update { it.copy(actionSuccess = "Payment recorded") } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearActionSuccess() {
        _uiState.update { it.copy(actionSuccess = null) }
    }

    // ── Due date helpers ──────────────────────────────────────────────────────

    private fun buildDueDateStatus(dueDate: Long?): DueDateStatus {
        if (dueDate == null) return DueDateStatus(null, "")

        val todayStart = startOfToday()
        val dueDayStart = startOfDay(dueDate)
        val diffMs = dueDayStart - todayStart
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)

        val text = when {
            diffDays > 0L -> "Due in $diffDays ${if (diffDays == 1L) "day" else "days"}"
            diffDays == 0L -> "Due today"
            else -> "Overdue by ${-diffDays} ${if (-diffDays == 1L) "day" else "days"}"
        }
        return DueDateStatus(diffDays, text)
    }

    private fun startOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun startOfDay(ts: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ts
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

class DebtDetailViewModelFactory(
    private val debtId: Long,
    private val repository: DebtRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DebtDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DebtDetailViewModel(debtId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
