package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlySpend by viewModel.monthlySpend.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    var selectedPeriod by remember { mutableStateOf(ChartPeriod.MONTH) }

    val chartData = remember(selectedPeriod, transactions) {
        viewModel.getChartData(selectedPeriod, transactions)
    }

    val fmt = NumberFormat.getNumberInstance(Locale("vi", "VN"))

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
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {

            // ── SECTION: INCOME / SPEND STATS ──
            item {
                Spacer(modifier = Modifier.height(16.dp))
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

            // ── SECTION: LIVE CHART ──
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {

                        // Header + period toggle
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
                            // Period toggle buttons
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

                        // Legend
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            ChartLegendDot("Income", GainGreen)
                            ChartLegendDot("Spend", ExpenseRed)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Real chart
                        LiveBarChart(bars = chartData)
                    }
                }
            }

            // ── SECTION: INCOME SOURCES ──
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                "Income Sources",
                                style = Typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Icon(Icons.Default.PieChart, contentDescription = null, tint = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(GlassBorder)
                        ) {
                            Box(modifier = Modifier.weight(0.6f).fillMaxHeight().background(PrimaryVibrant))
                            Box(modifier = Modifier.weight(0.25f).fillMaxHeight().background(TextSecondary))
                            Box(modifier = Modifier.weight(0.15f).fillMaxHeight().background(GlassBorder))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SummaryLegendItem("Salary", PrimaryVibrant)
                            SummaryLegendItem("Freelance", TextSecondary)
                            SummaryLegendItem("Other", GlassBorder)
                        }
                    }
                }
            }

            // ── SECTION: RECENT TRANSACTIONS ──
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
                    Text("No transactions yet.", style = Typography.bodyMedium, color = TextSecondary)
                }
            }
        }
    }
}

// ── COMPONENTS ──

@Composable
private fun StatCard(modifier: Modifier = Modifier, label: String, value: String, isIncome: Boolean) {
    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                text = "₫ $value",
                style = Typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) GainGreen else ExpenseRed,
                maxLines = 1
            )
        }
    }
}

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

@Composable
private fun ChartLegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = Typography.labelSmall, color = TextSecondary)
    }
}

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

    // Bars
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
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "incomeH"
            )
            val spendH by animateFloatAsState(
                targetValue = (bar.spend / maxValue).toFloat(),
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "spendH"
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // Income bar
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
                // Spend bar
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

    // X-axis labels (show only every Nth label so they don't overlap)
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
                modifier = Modifier.weight(2f),  // weight=2 because income+spend share same pair
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SummaryLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = Typography.labelMedium, color = TextSecondary)
    }
}

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
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(if (isIncome) GainGreen.copy(alpha = 0.1f) else ExpenseRed.copy(alpha = 0.1f)),
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
                    Text(title, style = Typography.bodyLarge, fontWeight = FontWeight.Medium, color = TextPrimary)
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
