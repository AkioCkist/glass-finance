package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SavingGoal
import com.example.data.SavingRepository
import com.example.data.SavingStatus
import com.example.data.SavingTransaction
import com.example.data.SavingTransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

data class SavingDetailUiState(
    val goal: SavingGoal? = null,
    val currentAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val progressPercent: Int = 0,
    val status: SavingStatus = SavingStatus.ACTIVE,
    val deadlineText: String = "No deadline",
    val transactions: List<SavingTransaction> = emptyList(),
    val totalDeposits: Double = 0.0,
    val totalWithdrawals: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val actionSuccess: String? = null
)

class SavingDetailViewModel(
    private val goalId: Long,
    private val repository: SavingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavingDetailUiState())
    val uiState: StateFlow<SavingDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeGoalWithTotals(goalId),
                repository.observeTransactions(goalId)
            ) { goalWithTotals, transactions ->
                if (goalWithTotals == null) {
                    SavingDetailUiState(
                        isLoading = false,
                        error = "Saving goal not found"
                    )
                } else {
                    val goal = goalWithTotals.goal
                    val current = (goalWithTotals.totalDeposits - goalWithTotals.totalWithdrawals).coerceAtLeast(0.0)
                    val remaining = (goal.targetAmount - current).coerceAtLeast(0.0)
                    val progress = if (goal.targetAmount > 0) {
                        ((current / goal.targetAmount) * 100).roundToInt().coerceIn(0, 999)
                    } else {
                        0
                    }

                    SavingDetailUiState(
                        goal = goal,
                        currentAmount = current,
                        remainingAmount = remaining,
                        progressPercent = progress,
                        status = computeSavingStatus(goal, current),
                        deadlineText = buildDeadlineDetailText(goal.deadline),
                        transactions = transactions,
                        totalDeposits = goalWithTotals.totalDeposits,
                        totalWithdrawals = goalWithTotals.totalWithdrawals,
                        isLoading = false
                    )
                }
            }.catch { e ->
                emit(SavingDetailUiState(isLoading = false, error = e.message))
            }.collect { newState ->
                _uiState.value = newState.copy(
                    error = _uiState.value.error ?: newState.error,
                    actionSuccess = _uiState.value.actionSuccess
                )
            }
        }
    }

    fun addDeposit(amount: Double, note: String, date: Long) {
        viewModelScope.launch {
            repository.addDeposit(goalId, amount, note, date)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(actionSuccess = "Saving added")
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
        }
    }

    fun withdraw(amount: Double, note: String, date: Long) {
        viewModelScope.launch {
            repository.addWithdraw(goalId, amount, note, date)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(actionSuccess = "Withdraw recorded")
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
        }
    }

    fun deleteGoal(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteGoal(goalId)
                .onSuccess { onSuccess() }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearActionSuccess() {
        _uiState.value = _uiState.value.copy(actionSuccess = null)
    }
}

class SavingDetailViewModelFactory(
    private val goalId: Long,
    private val repository: SavingRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavingDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SavingDetailViewModel(goalId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

private fun computeSavingStatus(goal: SavingGoal, current: Double): SavingStatus {
    if (current >= goal.targetAmount) return SavingStatus.COMPLETED
    val deadline = goal.deadline ?: return SavingStatus.ACTIVE
    return if (startOfDay(deadline) < startOfDay(System.currentTimeMillis())) SavingStatus.OVERDUE else SavingStatus.ACTIVE
}

private fun buildDeadlineDetailText(deadline: Long?): String {
    if (deadline == null) return "No deadline"

    val diffDays = ((startOfDay(deadline) - startOfDay(System.currentTimeMillis())) / ONE_DAY_MILLIS).toInt()
    return when {
        diffDays < 0 -> "Overdue by ${-diffDays} days"
        diffDays == 0 -> "Today"
        diffDays == 1 -> "Tomorrow"
        else -> "$diffDays days remaining"
    }
}

private fun startOfDay(timestamp: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L
