package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DebtRepository
import com.example.data.DebtStatus
import com.example.data.DebtWithPerson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DebtListUiState(
    val debts: List<DebtWithPerson> = emptyList(),
    val filteredDebts: List<DebtWithPerson> = emptyList(),
    val selectedFilter: DebtStatusFilter = DebtStatusFilter.ALL,
    val isLoading: Boolean = true,
    val error: String? = null,
    val deleteSuccess: Boolean = false
)

enum class DebtStatusFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    OVERDUE("Overdue"),
    PAID("Paid")
}

class DebtListViewModel(private val repository: DebtRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DebtListUiState())
    val uiState: StateFlow<DebtListUiState> = _uiState.asStateFlow()

    private val _selectedFilter = MutableStateFlow(DebtStatusFilter.ALL)

    init {
        viewModelScope.launch {
            combine(
                repository.getAllDebtsWithPerson(),
                _selectedFilter
            ) { debts, filter ->
                val filtered = when (filter) {
                    DebtStatusFilter.ALL -> debts
                    DebtStatusFilter.ACTIVE -> debts.filter { it.debt.status == DebtStatus.ACTIVE }
                    DebtStatusFilter.OVERDUE -> debts.filter { it.debt.status == DebtStatus.OVERDUE }
                    DebtStatusFilter.PAID -> debts.filter { it.debt.status == DebtStatus.PAID }
                }
                DebtListUiState(
                    debts = debts,
                    filteredDebts = filtered,
                    selectedFilter = filter,
                    isLoading = false
                )
            }.catch { e ->
                emit(DebtListUiState(isLoading = false, error = e.message))
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setFilter(filter: DebtStatusFilter) {
        _selectedFilter.value = filter
    }

    fun deleteDebt(debtId: Long) {
        viewModelScope.launch {
            repository.deleteDebt(debtId).onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

class DebtListViewModelFactory(private val repository: DebtRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DebtListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DebtListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
