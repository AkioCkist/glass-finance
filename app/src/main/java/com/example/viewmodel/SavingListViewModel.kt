package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SavingGoal
import com.example.data.SavingGoalWithTotals
import com.example.data.SavingRepository
import com.example.data.SavingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

data class SavingGoalItem(
    val id: Long,
    val title: String,
    val icon: String,
    val note: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val remainingAmount: Double,
    val progressPercent: Int,
    val deadline: Long?,
    val status: SavingStatus,
    val deadlineText: String,
    val totalDeposits: Double,
    val totalWithdrawals: Double
)

data class SavingSummaryStats(
    val totalGoal: Double = 0.0,
    val totalCurrent: Double = 0.0,
    val totalRemaining: Double = 0.0,
    val progressPercent: Int = 0,
    val totalDeposits: Double = 0.0,
    val totalWithdrawals: Double = 0.0
)

data class SavingListUiState(
    val goals: List<SavingGoalItem> = emptyList(),
    val stats: SavingSummaryStats = SavingSummaryStats(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class SavingListViewModel(
    private val repository: SavingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavingListUiState())
    val uiState: StateFlow<SavingListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAllGoalsWithTotals()
                .map { rows ->
                    val items = rows.map { row -> row.toSavingGoalItem() }
                    val totalGoal = items.sumOf { it.targetAmount }
                    val totalCurrent = items.sumOf { it.currentAmount }
                    val totalRemaining = (totalGoal - totalCurrent).coerceAtLeast(0.0)
                    val progress = if (totalGoal > 0.0) {
                        ((totalCurrent / totalGoal) * 100).roundToInt().coerceIn(0, 100)
                    } else {
                        0
                    }

                    SavingListUiState(
                        goals = items,
                        stats = SavingSummaryStats(
                            totalGoal = totalGoal,
                            totalCurrent = totalCurrent,
                            totalRemaining = totalRemaining,
                            progressPercent = progress,
                            totalDeposits = items.sumOf { it.totalDeposits },
                            totalWithdrawals = items.sumOf { it.totalWithdrawals }
                        ),
                        isLoading = false
                    )
                }
                .catch { e ->
                    emit(SavingListUiState(isLoading = false, error = e.message))
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

class SavingListViewModelFactory(
    private val repository: SavingRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavingListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SavingListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

private fun SavingGoalWithTotals.toSavingGoalItem(nowMillis: Long = System.currentTimeMillis()): SavingGoalItem {
    val current = (totalDeposits - totalWithdrawals).coerceAtLeast(0.0)
    val remaining = (goal.targetAmount - current).coerceAtLeast(0.0)
    val progress = if (goal.targetAmount > 0.0) {
        ((current / goal.targetAmount) * 100).roundToInt().coerceIn(0, 999)
    } else {
        0
    }
    val status = computeSavingStatus(goal, current, nowMillis)

    return SavingGoalItem(
        id = goal.id,
        title = goal.title,
        icon = goal.icon,
        note = goal.note,
        targetAmount = goal.targetAmount,
        currentAmount = current,
        remainingAmount = remaining,
        progressPercent = progress,
        deadline = goal.deadline,
        status = status,
        deadlineText = buildDeadlineHint(goal.deadline, nowMillis),
        totalDeposits = totalDeposits,
        totalWithdrawals = totalWithdrawals
    )
}

private fun computeSavingStatus(goal: SavingGoal, current: Double, nowMillis: Long): SavingStatus {
    if (current >= goal.targetAmount) return SavingStatus.COMPLETED
    val deadline = goal.deadline ?: return SavingStatus.ACTIVE
    return if (startOfDay(deadline) < startOfDay(nowMillis)) SavingStatus.OVERDUE else SavingStatus.ACTIVE
}

private fun buildDeadlineHint(deadline: Long?, nowMillis: Long): String {
    if (deadline == null) return "No deadline"

    val diffDays = ((startOfDay(deadline) - startOfDay(nowMillis)) / ONE_DAY_MILLIS).toInt()
    return when {
        diffDays < 0 -> "Overdue"
        diffDays == 0 -> "Today"
        diffDays == 1 -> "Tomorrow"
        else -> "$diffDays days left"
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
