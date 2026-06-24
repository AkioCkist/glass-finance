package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.data.DebtPerson
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.viewmodel.DebtPersonViewModel

@Composable
fun DebtPersonScreen(
    viewModel: DebtPersonViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var personToEdit by remember { mutableStateOf<DebtPerson?>(null) }
    var personToDelete by remember { mutableStateOf<DebtPerson?>(null) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Header ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GlassWhite)
                            .border(1.dp, GlassBorder, CircleShape)
                            .clickable { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Text(
                        text = "Debt People",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(TextPrimary)
                        .clickable { showAddDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Person", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Search ───────────────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search people…", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                trailingIcon = {
                    AnimatedVisibility(uiState.searchQuery.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TextPrimary,
                    unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = GlassWhite,
                    unfocusedContainerColor = GlassWhite,
                    cursorColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── List ──────────────────────────────────────────────────────────
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(32.dp))
                }
            } else if (uiState.persons.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            if (uiState.searchQuery.isNotBlank()) "No results for \"${uiState.searchQuery}\"" else "No people yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                        if (uiState.searchQuery.isBlank()) {
                            TextButton(onClick = { showAddDialog = true }) {
                                Text("Add first person", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(uiState.persons, key = { it.id }) { person ->
                        PersonRow(
                            person = person,
                            onEdit = { personToEdit = person },
                            onDelete = { personToDelete = person }
                        )
                    }
                }
            }
        }
    }

    // ── Add dialog ────────────────────────────────────────────────────────────
    if (showAddDialog) {
        PersonNameDialog(
            title = "Add Person",
            initialName = "",
            confirmLabel = "Add",
            onConfirm = { name ->
                viewModel.addPerson(name)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // ── Edit dialog ───────────────────────────────────────────────────────────
    personToEdit?.let { person ->
        PersonNameDialog(
            title = "Edit Person",
            initialName = person.name,
            confirmLabel = "Save",
            onConfirm = { newName ->
                viewModel.updatePerson(person, newName)
                personToEdit = null
            },
            onDismiss = { personToEdit = null }
        )
    }

    // ── Delete confirm ────────────────────────────────────────────────────────
    personToDelete?.let { person ->
        AlertDialog(
            onDismissRequest = { personToDelete = null },
            containerColor = AppBackground,
            title = { Text("Delete \"${person.name}\"?", color = TextPrimary) },
            text = {
                Text(
                    "If this person has debts associated with them, deletion will be blocked.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePerson(person)
                    personToDelete = null
                }) {
                    Text("Delete", color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { personToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun PersonRow(
    person: DebtPerson,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GlassBorder),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = person.name.take(1).uppercase(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun PersonNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    val isValid = name.trim().isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppBackground,
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TextPrimary,
                    unfocusedBorderColor = GlassBorder,
                    focusedLabelColor = TextPrimary,
                    cursorColor = TextPrimary
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (isValid) onConfirm(name) },
                enabled = isValid
            ) {
                Text(confirmLabel, color = if (isValid) TextPrimary else TextSecondary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
