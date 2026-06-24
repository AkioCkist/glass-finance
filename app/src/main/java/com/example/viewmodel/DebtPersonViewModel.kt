package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DebtPerson
import com.example.data.DebtPersonRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DebtPersonUiState(
    val persons: List<DebtPerson> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

class DebtPersonViewModel(private val repository: DebtPersonRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DebtPersonUiState())
    val uiState: StateFlow<DebtPersonUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(200)
                .flatMapLatest { query -> repository.searchPersons(query) }
                .catch { e -> emit(emptyList()) }
                .collect { persons ->
                    _uiState.update { state ->
                        state.copy(persons = persons, isLoading = false)
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun addPerson(name: String) {
        viewModelScope.launch {
            repository.addPerson(name)
                .onSuccess { _uiState.update { it.copy(successMessage = "Person added") } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun updatePerson(person: DebtPerson, newName: String) {
        viewModelScope.launch {
            repository.updatePerson(person, newName)
                .onSuccess { _uiState.update { it.copy(successMessage = "Person updated") } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun deletePerson(person: DebtPerson) {
        viewModelScope.launch {
            repository.deletePerson(person)
                .onSuccess { _uiState.update { it.copy(successMessage = "Person deleted") } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

class DebtPersonViewModelFactory(private val repository: DebtPersonRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DebtPersonViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DebtPersonViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
