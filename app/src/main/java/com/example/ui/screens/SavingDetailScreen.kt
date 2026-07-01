package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SavingStatus
import com.example.data.SavingTransaction
import com.example.data.SavingTransactionType
import com.example.ui.components.GlassCard
import com.example.ui.components.KeypadDialog
import com.example.ui.theme.AppBackground
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GainGreen
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.SavingDetailViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavingDetailScreen(
    viewModel: SavingDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val fmt = remember { NumberFormat.getNumberInstance(Locale("vi", "VN")) }
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(uiState.actionSuccess) {
        uiState.actionSuccess?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionSuccess()
        }
    }

    Scaffold(
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val goal = uiState.goal
        if (uiState.isLoading || goal == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading...", color = TextSecondary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        text = "Saving Detail",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GlassWhite)
                            .border(1.dp, GlassBorder, CircleShape)
                            .clickable { onNavigateToEdit(goal.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextPrimary)
                    }
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(goal.icon, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                goal.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            SavingStatusBadge(uiState.status)
                        }

                        if (goal.note.isNotBlank()) {
                            Text(goal.note, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }

                        Text(
                            uiState.deadlineText,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Statistics", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        StatRow("Goal", fmt.format(goal.targetAmount))
                        StatRow("Current", fmt.format(uiState.currentAmount), valueColor = GainGreen)
                        StatRow("Remaining", fmt.format(uiState.remainingAmount), valueColor = ExpenseRed)
                        StatRow("Progress", "${uiState.progressPercent}%")

                        LinearProgressIndicator(
                            progress = { (uiState.progressPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(100.dp)),
                            color = when (uiState.status) {
                                SavingStatus.COMPLETED -> GainGreen
                                SavingStatus.OVERDUE -> ExpenseRed
                                SavingStatus.ACTIVE -> TextPrimary
                            },
                            trackColor = GlassBorder
                        )

                        StatRow("Total Deposits", fmt.format(uiState.totalDeposits), valueColor = GainGreen)
                        StatRow("Total Withdrawals", fmt.format(uiState.totalWithdrawals), valueColor = ExpenseRed)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionBox(
                        label = "+ Add Saving",
                        icon = Icons.Default.Savings,
                        modifier = Modifier.weight(1f)
                    ) { showAddDialog = true }

                    ActionBox(
                        label = "Withdraw",
                        icon = Icons.Default.RemoveCircleOutline,
                        modifier = Modifier.weight(1f),
                        bgColor = ExpenseRed.copy(alpha = 0.1f),
                        fgColor = ExpenseRed
                    ) { showWithdrawDialog = true }
                }

                Spacer(modifier = Modifier.height(8.dp))

                ActionBox(
                    label = "Delete Goal",
                    icon = Icons.Default.Delete,
                    modifier = Modifier.fillMaxWidth(),
                    bgColor = ExpenseRed.copy(alpha = 0.1f),
                    fgColor = ExpenseRed
                ) { showDeleteDialog = true }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Transaction History", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("${uiState.transactions.size} records", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                }
            }

            if (uiState.transactions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                        Text("No transactions", color = TextSecondary)
                    }
                }
            } else {
                items(uiState.transactions, key = { it.id }) { tx ->
                    SavingTransactionRow(tx = tx, fmt = fmt)
                }
            }
        }
    }

    if (showAddDialog) {
        SavingTransactionDialog(
            title = "Add Saving",
            confirmText = "Add",
            noteLabel = "Note",
            onDismiss = { showAddDialog = false },
            onConfirm = { amount, note, date ->
                viewModel.addDeposit(amount, note, date)
                showAddDialog = false
            }
        )
    }

    if (showWithdrawDialog) {
        SavingTransactionDialog(
            title = "Withdraw",
            confirmText = "Withdraw",
            noteLabel = "Reason",
            onDismiss = { showWithdrawDialog = false },
            onConfirm = { amount, note, date ->
                viewModel.withdraw(amount, note, date)
                showWithdrawDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete goal?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGoal(onSuccess = onNavigateBack)
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = ExpenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SavingTransactionDialog(
    title: String,
    confirmText: String,
    noteLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, note: String, date: Long) -> Unit
) {
    var amountRaw by remember { mutableStateOf("0") }
    var note by remember { mutableStateOf("") }
    var transactionDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showKeypad by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showKeypad = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Amount", color = TextSecondary)
                        Text("${formatAmount(amountRaw)} VND", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(noteLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TextPrimary,
                        unfocusedBorderColor = GlassBorder,
                        cursorColor = TextPrimary
                    )
                )

                val dateText = remember(transactionDate) {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(transactionDate))
                }
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            transactionDate = System.currentTimeMillis()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Date", color = TextSecondary)
                        Text(dateText, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {
            val amount = amountRaw.toDoubleOrNull() ?: 0.0
            TextButton(onClick = {
                onConfirm(amount, note, transactionDate)
            }, enabled = amount > 0.0) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )

    if (showKeypad) {
        KeypadDialog(
            title = "Amount",
            initialAmount = amountRaw,
            onDismiss = { showKeypad = false },
            onConfirm = {
                amountRaw = it.ifBlank { "0" }
                showKeypad = false
            }
        )
    }
}

@Composable
private fun SavingTransactionRow(tx: SavingTransaction, fmt: NumberFormat) {
    val isDeposit = tx.type == SavingTransactionType.DEPOSIT
    val color = if (isDeposit) GainGreen else ExpenseRed
    val sign = if (isDeposit) "+" else "-"

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$sign${fmt.format(tx.amount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
                if (tx.note.isNotBlank()) {
                    Text(tx.note, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }

            val dateText = remember(tx.transactionDate) {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(tx.transactionDate))
            }
            Text(dateText, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun ActionBox(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    bgColor: Color = TextPrimary.copy(alpha = 0.1f),
    fgColor: Color = TextPrimary,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = fgColor, modifier = Modifier.size(16.dp))
            Text(label, color = fgColor, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SavingStatusBadge(status: SavingStatus) {
    val (bg, fg, text) = when (status) {
        SavingStatus.ACTIVE -> Triple(TextPrimary.copy(alpha = 0.1f), TextPrimary, "ACTIVE")
        SavingStatus.COMPLETED -> Triple(GainGreen.copy(alpha = 0.12f), GainGreen, "COMPLETED")
        SavingStatus.OVERDUE -> Triple(ExpenseRed.copy(alpha = 0.12f), ExpenseRed, "OVERDUE")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatAmount(raw: String): String {
    val normalized = raw.trim().ifBlank { "0" }
    val digits = normalized.filter { it.isDigit() }.ifBlank { "0" }
    return digits.reversed().chunked(3).joinToString(",").reversed()
}
