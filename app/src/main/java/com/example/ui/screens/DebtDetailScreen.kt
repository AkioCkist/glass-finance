package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DebtDirection
import com.example.data.DebtStatus
import com.example.data.DebtTransaction
import com.example.data.DebtTransactionType
import com.example.ui.components.DirectionLabel
import com.example.ui.components.GlassCard
import com.example.ui.components.KeypadDialog
import com.example.ui.theme.*
import com.example.viewmodel.DebtDetailViewModel
import com.example.viewmodel.DebtStats
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DebtDetailScreen(
    viewModel: DebtDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val fmt = remember { NumberFormat.getNumberInstance(Locale("vi", "VN")) }
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.actionSuccess) {
        uiState.actionSuccess?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionSuccess()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        if (uiState.isLoading || uiState.debtWithPerson == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TextPrimary)
            }
            return@Scaffold
        }

        val debt = uiState.debtWithPerson!!.debt
        val person = uiState.debtWithPerson!!.person

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Top bar ──────────────────────────────────────────────────────
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
                        text = "Debt Detail",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GlassWhite)
                            .border(1.dp, GlassBorder, CircleShape)
                            .clickable { onNavigateToEdit(debt.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // ── Basic info card ───────────────────────────────────────────────
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Person row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
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
                            Column {
                                Text(
                                    text = person.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Person",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                DirectionLabel(direction = debt.direction)
                                DebtStatusBadge(status = debt.status)
                            }
                        }

                        HorizontalDivider(color = GlassBorder)

                        // Title
                        Column {
                            Text("Title", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Text(
                                text = debt.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        }

                        // Note (if present)
                        if (debt.note.isNotBlank()) {
                            Column {
                                Text("Note", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                Text(
                                    text = debt.note,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // ── Debt information card ─────────────────────────────────────────
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Debt Information",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DebtInfoItem(
                                label = "Original Amount",
                                value = "VND ${fmt.format(debt.originalAmount)}"
                            )
                            DebtInfoItem(
                                label = "Remaining",
                                value = "VND ${fmt.format(uiState.stats.remaining)}",
                                valueColor = if (uiState.stats.remaining <= 0) GainGreen else TextPrimary
                            )
                        }

                        HorizontalDivider(color = GlassBorder)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val createdStr = remember(debt.createdDate) {
                                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(debt.createdDate))
                            }
                            DebtInfoItem(label = "Created", value = createdStr)

                            debt.dueDate?.let { dueDate ->
                                val dueStr = remember(dueDate) {
                                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dueDate))
                                }
                                DebtInfoItem(
                                    label = "Due Date",
                                    value = dueStr,
                                    valueColor = if (debt.status == DebtStatus.OVERDUE) ExpenseRed else TextPrimary
                                )
                            }
                        }

                        // Due date reminder
                        if (uiState.dueDateStatus.displayText.isNotBlank()) {
                            val daysUntilDue = uiState.dueDateStatus.daysUntilDue
                            val reminderColor = when {
                                daysUntilDue == null -> TextSecondary
                                daysUntilDue < 0L -> ExpenseRed
                                daysUntilDue <= 3L -> Color(0xFFF59E0B)
                                else -> GainGreen
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(reminderColor.copy(alpha = 0.08f))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = reminderColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = uiState.dueDateStatus.displayText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = reminderColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Statistics card ───────────────────────────────────────────────
            item {
                DebtStatsCard(stats = uiState.stats, fmt = fmt)
            }

            // ── Quick actions ─────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionButton(
                        label = "Add Debt",
                        icon = Icons.Default.Add,
                        modifier = Modifier.weight(1f),
                        enabled = debt.status != DebtStatus.PAID
                    ) { showAddDebtDialog = true }

                    ActionButton(
                        label = "Add Payment",
                        icon = Icons.Default.Payments,
                        modifier = Modifier.weight(1f),
                        enabled = debt.status != DebtStatus.PAID
                    ) { showAddPaymentDialog = true }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ExpenseRed.copy(alpha = 0.08f))
                        .border(1.dp, ExpenseRed.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .clickable { showDeleteDialog = true }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                        Text("Delete Debt", style = MaterialTheme.typography.bodyMedium, color = ExpenseRed, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // ── Transaction history ───────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Transaction History",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        "${uiState.transactions.size} records",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }

            if (uiState.transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No transactions yet", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            } else {
                items(uiState.transactions, key = { it.id }) { tx ->
                    TransactionRow(tx = tx, fmt = fmt)
                }
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────
    if (showAddDebtDialog) {
        KeypadDialog(
            title = "Add Debt Amount",
            onConfirm = { amountRaw ->
                val amount = amountRaw.toDoubleOrNull() ?: 0.0
                if (amount > 0) {
                    viewModel.addDebtAmount(amount, "", System.currentTimeMillis())
                }
                showAddDebtDialog = false
            },
            onDismiss = { showAddDebtDialog = false }
        )
    }

    if (showAddPaymentDialog) {
        KeypadDialog(
            title = "Record Payment",
            onConfirm = { amountRaw ->
                val amount = amountRaw.toDoubleOrNull() ?: 0.0
                if (amount > 0) {
                    viewModel.addPayment(amount, "", System.currentTimeMillis())
                }
                showAddPaymentDialog = false
            },
            onDismiss = { showAddPaymentDialog = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = AppBackground,
            title = {
                Text("Delete Debt", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            },
            text = {
                Text(
                    "This will permanently delete the debt and all its transaction history. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteDebt { onNavigateBack() }
                }) {
                    Text("Delete", color = ExpenseRed, fontWeight = FontWeight.SemiBold)
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

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun DebtInfoItem(label: String, value: String, valueColor: Color = TextPrimary) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = valueColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DebtStatsCard(stats: DebtStats, fmt: NumberFormat) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Statistics", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatPill(label = "Total Added", value = "VND ${fmt.format(stats.totalAdded)}", color = SecondaryVibrant)
                StatPill(label = "Total Paid", value = "VND ${fmt.format(stats.totalPaid)}", color = GainGreen)
            }

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Progress", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(
                        "${stats.progressPercent.toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                val animatedProgress by animateFloatAsState(
                    targetValue = stats.progressPercent / 100f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "debtProgress"
                )
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = GainGreen,
                    trackColor = GlassBorder,
                    strokeCap = StrokeCap.Round
                )
            }

            // Remaining
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Remaining", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(
                    "VND ${fmt.format(stats.remaining)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (stats.remaining <= 0) GainGreen else TextPrimary
                )
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) TextPrimary else GlassBorder)
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = if (enabled) Color.White else TextSecondary, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = if (enabled) Color.White else TextSecondary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun TransactionRow(tx: DebtTransaction, fmt: NumberFormat) {
    val isIncrease = tx.type == DebtTransactionType.INCREASE
    val amountColor = if (isIncrease) ExpenseRed else GainGreen
    val amountPrefix = if (isIncrease) "+" else "-"
    val dateStr = remember(tx.transactionDate) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(tx.transactionDate))
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(amountColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isIncrease) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = amountColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = if (tx.note.isNotBlank()) tx.note else if (isIncrease) "Debt added" else "Payment",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(dateStr, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                }
            }
            Text(
                text = "$amountPrefixVND ${fmt.format(tx.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

// ── Shared amount input dialog ────────────────────────────────────────────────

@Composable
fun AmountInputDialog(
    title: String,
    confirmLabel: String,
    confirmColor: Color,
    onConfirm: (amount: Double, note: String, date: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val isValid = amountText.toDoubleOrNull()?.let { it > 0 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppBackground,
        title = {
            Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (VND)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TextPrimary,
                        unfocusedBorderColor = GlassBorder,
                        focusedLabelColor = TextPrimary,
                        cursorColor = TextPrimary
                    )
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TextPrimary,
                        unfocusedBorderColor = GlassBorder,
                        focusedLabelColor = TextPrimary,
                        cursorColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: return@TextButton
                    onConfirm(amount, note, System.currentTimeMillis())
                },
                enabled = isValid
            ) {
                Text(confirmLabel, color = if (isValid) confirmColor else TextSecondary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
