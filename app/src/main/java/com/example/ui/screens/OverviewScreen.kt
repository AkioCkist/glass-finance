package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MoneySource
import com.example.data.MoneySourceType
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.DebtSummary
import com.example.viewmodel.FinanceViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun OverviewScreen(viewModel: FinanceViewModel) {
    val totalBalance by viewModel.totalBalance.collectAsState()
    val activeSources by viewModel.activeMoneySources.collectAsState()
    val debtSummary by viewModel.debtSummary.collectAsState()

    val fmt = NumberFormat.getNumberInstance(Locale.US)
    val formattedBalance = fmt.format(totalBalance)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(title = "Overview")
        Spacer(modifier = Modifier.height(32.dp))
        BalanceSection(title = "Total Balance", value = formattedBalance)
        Spacer(modifier = Modifier.height(40.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // ── DISTRIBUTION DONUT CHART ──
            if (totalBalance > 0 && activeSources.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DistributionDonutChart(
                            sources = activeSources,
                            total = totalBalance,
                            fmt = fmt
                        )
                    }
                }
            }

            // ── SECTION HEADER ──
            item {
                Text(
                    "My Money Sources",
                    style = Typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            // Source cards — 2 per row
            val chunked = activeSources.chunked(2)
            chunked.forEach { rowSources ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowSources.forEach { source ->
                            OverviewSourceCard(
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

            if (activeSources.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No money sources yet",
                            style = Typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            // ── DEBT SUMMARY ──
            if (debtSummary.owedToMe > 0 || debtSummary.iOwe > 0) {
                item {
                    Text(
                        "Debt Summary",
                        style = Typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
                item {
                    DebtSummaryCard(summary = debtSummary, fmt = fmt)
                }
            }
        }
    }
}

@Composable
private fun DebtSummaryCard(
    summary: DebtSummary,
    fmt: NumberFormat
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(GainGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = GainGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text("Owed to me", style = Typography.bodyMedium, color = TextSecondary)
                        Text("${fmt.format(summary.owedToMe)} VND", style = Typography.bodyLarge, fontWeight = FontWeight.Bold, color = GainGreen)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(ExpenseRed.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = ExpenseRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("I owe", style = Typography.bodyMedium, color = TextSecondary)
                        Text("${fmt.format(summary.iOwe)} VND", style = Typography.bodyLarge, fontWeight = FontWeight.Bold, color = ExpenseRed)
                    }
                }
            }
            HorizontalDivider(color = GlassBorder, thickness = 1.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Net", style = Typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                val net = summary.owedToMe - summary.iOwe
                Text(
                    "${fmt.format(net)} VND",
                    style = Typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (net >= 0) GainGreen else ExpenseRed
                )
            }
        }
    }
}

// ── Distribution Donut Chart ────────────────────────────────────────────────

private const val DONUT_THICKNESS = 0.28f // fraction of radius

@Composable
private fun DistributionDonutChart(
    sources: List<MoneySource>,
    total: Double,
    fmt: NumberFormat
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Asset Distribution",
                style = Typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Donut
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = size.minDimension * DONUT_THICKNESS
                    val arcSize = Size(
                        size.width - strokeWidth,
                        size.height - strokeWidth
                    )
                    val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                    var startAngle = -90f

                    sources.forEach { source ->
                        val sweep = (source.balance / total * 360f).toFloat()
                        drawArc(
                            color = colorForType(source.type),
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Butt)
                        )
                        startAngle += sweep
                    }
                }
                // Center text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        fmt.format(total),
                        style = Typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "VND",
                        style = Typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val leftCol = sources.take((sources.size + 1) / 2)
                val rightCol = sources.drop((sources.size + 1) / 2)

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    leftCol.forEach { source ->
                        val pct = (source.balance / total * 100).toInt()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(colorForType(source.type))
                            )
                            Text(
                                "${source.name} $pct%",
                                style = Typography.labelMedium,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rightCol.forEach { source ->
                        val pct = (source.balance / total * 100).toInt()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(colorForType(source.type))
                            )
                            Text(
                                "${source.name} $pct%",
                                style = Typography.labelMedium,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

private fun iconForType(type: MoneySourceType): ImageVector = when (type) {
    MoneySourceType.CASH        -> Icons.Default.AccountBalanceWallet
    MoneySourceType.CHECKING    -> Icons.Default.AccountBalance
    MoneySourceType.SAVINGS     -> Icons.Default.Savings
    MoneySourceType.INVESTMENT  -> Icons.Default.TrendingUp
    MoneySourceType.CREDIT_CARD -> Icons.Default.CreditCard
    MoneySourceType.E_WALLET    -> Icons.Default.Phone
    MoneySourceType.OTHER       -> Icons.Default.MoreHoriz
}

private fun colorForType(type: MoneySourceType): Color = when (type) {
    MoneySourceType.CASH        -> Color(0xFF34C759)
    MoneySourceType.CHECKING    -> Color(0xFF007AFF)
    MoneySourceType.SAVINGS     -> Color(0xFFAF52DE)
    MoneySourceType.INVESTMENT  -> Color(0xFFFF9500)
    MoneySourceType.CREDIT_CARD -> Color(0xFFFF3B30)
    MoneySourceType.E_WALLET    -> Color(0xFF5AC8FA)
    MoneySourceType.OTHER       -> TextSecondary
}

@Composable
private fun OverviewSourceCard(
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
                text = "${fmt.format(source.balance)} VND",
                style = Typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1
            )
        }
    }
}
