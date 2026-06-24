package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DebtDirection
import com.example.data.DebtStatus
import com.example.data.DebtWithPerson
import com.example.ui.components.DirectionLabel
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.viewmodel.DebtListUiState
import com.example.viewmodel.DebtListViewModel
import com.example.viewmodel.DebtStatusFilter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DebtListScreen(
    viewModel: DebtListViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToPeople: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val fmt = remember { NumberFormat.getNumberInstance(Locale("vi", "VN")) }

    // Error snackbar
    if (uiState.error != null) {
        LaunchedEffect(uiState.error) {
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Debts",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // People management button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GlassWhite)
                        .border(1.dp, GlassBorder, CircleShape)
                        .clickable { onNavigateToPeople() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.People,
                        contentDescription = "Manage People",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // ── Summary card ─────────────────────────────────────────────────────
        if (uiState.debts.isNotEmpty()) {
            DebtSummaryCard(debts = uiState.debts, fmt = fmt)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Filter chips ──────────────────────────────────────────────────────
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(DebtStatusFilter.entries) { filter ->
                DebtFilterChip(
                    filter = filter,
                    isSelected = uiState.selectedFilter == filter,
                    count = countForFilter(uiState.debts, filter),
                    onClick = { viewModel.setFilter(filter) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // ── List ──────────────────────────────────────────────────────────────
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(32.dp))
            }
        } else if (uiState.filteredDebts.isEmpty()) {
            DebtEmptyState(
                filter = uiState.selectedFilter,
                onAddClick = onNavigateToAdd
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(uiState.filteredDebts, key = { it.debt.id }) { item ->
                    DebtCard(
                        item = item,
                        fmt = fmt,
                        onClick = { onNavigateToDetail(item.debt.id) }
                    )
                }
            }
        }
    }

    // ── FAB ───────────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .padding(end = 24.dp, bottom = 88.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(TextPrimary)
                .clickable { onNavigateToAdd() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Debt", tint = Color.White)
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun DebtSummaryCard(debts: List<DebtWithPerson>, fmt: NumberFormat) {
    val activeCount = debts.count { it.debt.status == DebtStatus.ACTIVE }
    val overdueCount = debts.count { it.debt.status == DebtStatus.OVERDUE }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryStatItem(
                label = "Total Debts",
                value = "${debts.size}",
                color = TextPrimary
            )
            VerticalDivider()
            SummaryStatItem(
                label = "Active",
                value = "$activeCount",
                color = SecondaryVibrant
            )
            VerticalDivider()
            SummaryStatItem(
                label = "Overdue",
                value = "$overdueCount",
                color = ExpenseRed
            )
        }
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(GlassBorder)
    )
}

@Composable
private fun SummaryStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun DebtFilterChip(
    filter: DebtStatusFilter,
    isSelected: Boolean,
    count: Int,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) TextPrimary else GlassWhite,
        animationSpec = tween(200),
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else TextSecondary,
        animationSpec = tween(200),
        label = "chipText"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "${filter.label} ($count)",
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun DebtCard(
    item: DebtWithPerson,
    fmt: NumberFormat,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left: person + title
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(GlassBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.person.name.take(1).uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = item.person.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.debt.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Right: direction + status badge
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DirectionLabel(direction = item.debt.direction)
                    DebtStatusBadge(status = item.debt.status)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Original",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "₫ ${fmt.format(item.debt.originalAmount)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                // Due date
                item.debt.dueDate?.let { dueDate ->
                    val dateStr = remember(dueDate) {
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dueDate))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Due",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (item.debt.status == DebtStatus.OVERDUE) ExpenseRed else TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DebtStatusBadge(status: DebtStatus) {
    val (bgColor, textColor, label) = when (status) {
        DebtStatus.ACTIVE -> Triple(SecondaryVibrant.copy(alpha = 0.1f), SecondaryVibrant, "Active")
        DebtStatus.PAID -> Triple(GainGreen.copy(alpha = 0.1f), GainGreen, "Paid")
        DebtStatus.OVERDUE -> Triple(ExpenseRed.copy(alpha = 0.1f), ExpenseRed, "Overdue")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DebtEmptyState(filter: DebtStatusFilter, onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(GlassWhite)
                .border(1.dp, GlassBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(
            text = if (filter == DebtStatusFilter.ALL) "No debts yet" else "No ${filter.label.lowercase()} debts",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
        Text(
            text = if (filter == DebtStatusFilter.ALL) "Tap + to record a new debt" else "Switch filters to see other debts",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

private fun countForFilter(debts: List<DebtWithPerson>, filter: DebtStatusFilter): Int = when (filter) {
    DebtStatusFilter.ALL -> debts.size
    DebtStatusFilter.ACTIVE -> debts.count { it.debt.status == DebtStatus.ACTIVE }
    DebtStatusFilter.OVERDUE -> debts.count { it.debt.status == DebtStatus.OVERDUE }
    DebtStatusFilter.PAID -> debts.count { it.debt.status == DebtStatus.PAID }
}

