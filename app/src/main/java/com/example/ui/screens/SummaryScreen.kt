package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.MoneySource
import com.example.data.MoneySourceType
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.ChartBar
import com.example.viewmodel.ChartPeriod
import com.example.viewmodel.FinanceViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SummaryScreen(viewModel: FinanceViewModel) {
    val totalBalance by viewModel.totalBalance.collectAsState()
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlySpend by viewModel.monthlySpend.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val moneySources by viewModel.moneySources.collectAsState()
    var selectedPeriod by remember { mutableStateOf(ChartPeriod.MONTH) }

    val chartData = remember(selectedPeriod, transactions) {
        viewModel.getChartData(selectedPeriod, transactions)
    }

    val fmt = NumberFormat.getNumberInstance(Locale.US)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(title = "Summary")
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {

            // ── INCOME / SPEND STATS ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Income",
                        value = fmt.format(monthlyIncome),
                        isIncome = true
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Spend",
                        value = fmt.format(monthlySpend),
                        isIncome = false
                    )
                }
            }

            // ── CHART ──
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "History Chart",
                                style = Typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(
                                    ChartPeriod.DAY to "Day",
                                    ChartPeriod.WEEK to "Week",
                                    ChartPeriod.MONTH to "Month",
                                    ChartPeriod.YEAR to "Year"
                                ).forEach { (period, label) ->
                                    PeriodChip(
                                        label = label,
                                        isSelected = selectedPeriod == period,
                                        onClick = { selectedPeriod = period }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            ChartLegendDot("Income", GainGreen)
                            ChartLegendDot("Spend", ExpenseRed)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        LiveBarChart(bars = chartData)
                    }
                }
            }

            // ── MONEY SOURCES ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Money Sources",
                        style = Typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        "VND ${fmt.format(moneySources.sumOf { it.balance })}",
                        style = Typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
            }

            // source cards — 2 per row
            val chunked = moneySources.chunked(2)
            chunked.forEach { rowSources ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowSources.forEach { source ->
                            SourceCard(
                                modifier = Modifier.weight(1f),
                                source = source,
                                fmt = fmt
                            )
                        }
                        if (rowSources.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // ── RECENT TRANSACTIONS ──
            item {
                Text(
                    "Recent Transactions",
                    style = Typography.bodyLarge,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(transactions) { tx ->
                val amountStr = (if (tx.isIncome) "+" else "-") + fmt.format(tx.amount)
                val dateStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(tx.timestamp))
                SummaryTransactionRow(
                    isIncome = tx.isIncome,
                    amount = amountStr,
                    title = tx.note,
                    dateStr = dateStr
                )
            }
            if (transactions.isEmpty()) {
                item {
                    Text(
                        "No transactions yet.",
                        style = Typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  COMPONENTS
// ══════════════════════════════════════════════════════════════════════════════

// ── Source Card ──────────────────────────────────────────────────────────────

private fun iconForType(type: MoneySourceType): ImageVector = when (type) {
    MoneySourceType.CASH       -> Icons.Default.AccountBalanceWallet
    MoneySourceType.CHECKING   -> Icons.Default.AccountBalance
    MoneySourceType.SAVINGS    -> Icons.Default.Savings
    MoneySourceType.INVESTMENT -> Icons.Default.TrendingUp
    MoneySourceType.CREDIT_CARD -> Icons.Default.CreditCard
    MoneySourceType.E_WALLET   -> Icons.Default.Phone
    MoneySourceType.OTHER      -> Icons.Default.MoreHoriz
}

private fun colorForType(type: MoneySourceType): Color = when (type) {
    MoneySourceType.CASH       -> Color(0xFF34C759) // green
    MoneySourceType.CHECKING   -> Color(0xFF007AFF) // blue
    MoneySourceType.SAVINGS    -> Color(0xFFAF52DE) // purple
    MoneySourceType.INVESTMENT -> Color(0xFFFF9500) // orange
    MoneySourceType.CREDIT_CARD -> Color(0xFFFF3B30) // red
    MoneySourceType.E_WALLET   -> Color(0xFF5AC8FA) // cyan
    MoneySourceType.OTHER      -> TextSecondary
}

@Composable
private fun SourceCard(
    modifier: Modifier = Modifier,
    source: MoneySource,
    fmt: NumberFormat
) {
    val accent = colorForType(source.type)
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        iconForType(source.type),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = source.name,
                style = Typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "VND ${fmt.format(source.balance)}",
                style = Typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1
            )
        }
    }
}

// ── StatCard ─────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(modifier: Modifier = Modifier, label: String, value: String, isIncome: Boolean) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isIncome) GainGreen.copy(alpha = 0.12f) else ExpenseRed.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isIncome) GainGreen else ExpenseRed,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(label, style = Typography.labelMedium, color = TextSecondary)
            }
            Text(
                text = "VND $value",
                style = Typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) GainGreen else ExpenseRed,
                maxLines = 1
            )
        }
    }
}

// ── PeriodChip ───────────────────────────────────────────────────────────────

@Composable
private fun PeriodChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chipBg"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PrimaryVibrant.copy(alpha = bgAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = Typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 10.sp
        )
    }
}

// ── ChartLegendDot ───────────────────────────────────────────────────────────

@Composable
private fun ChartLegendDot(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = Typography.labelSmall, color = TextSecondary)
    }
}

// ── LiveBarChart ─────────────────────────────────────────────────────────────

@Composable
private fun LiveBarChart(bars: List<ChartBar>) {
    if (bars.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No data", style = Typography.bodyMedium, color = TextSecondary)
        }
        return
    }

    val maxValue = bars.maxOf { maxOf(it.income, it.spend) }.coerceAtLeast(1.0)
    val chartHeight = 120.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(chartHeight),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { bar ->
            val incomeH by animateFloatAsState(
                targetValue = (bar.income / maxValue).toFloat(),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "incomeH"
            )
            val spendH by animateFloatAsState(
                targetValue = (bar.spend / maxValue).toFloat(),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "spendH"
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .fillMaxHeight(incomeH)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(GainGreen.copy(alpha = 0.8f))
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .fillMaxHeight(spendH)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(ExpenseRed.copy(alpha = 0.8f))
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    val step = when {
        bars.size > 18 -> 4
        bars.size > 12 -> 3
        bars.size > 7 -> 2
        else -> 1
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        bars.forEachIndexed { i, bar ->
            Text(
                text = if (i % step == 0) bar.label else "",
                style = Typography.labelSmall,
                color = TextSecondary,
                fontSize = 8.sp,
                modifier = Modifier.weight(2f),
                maxLines = 1
            )
        }
    }
}

// ── Transaction Row ──────────────────────────────────────────────────────────

@Composable
private fun SummaryTransactionRow(isIncome: Boolean, amount: String, title: String, dateStr: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isIncome) GainGreen.copy(alpha = 0.1f)
                            else ExpenseRed.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isIncome) "↓" else "↑",
                        color = if (isIncome) GainGreen else ExpenseRed,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        title.ifEmpty { if (isIncome) "Income" else "Expense" },
                        style = Typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(dateStr, style = Typography.labelMedium, color = TextSecondary)
                }
            }
            Text(
                amount,
                style = Typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) GainGreen else ExpenseRed
            )
        }
    }
}
