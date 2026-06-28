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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
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
            // ── DISTRIBUTION BLEND CHART ──
            if (totalBalance > 0 && activeSources.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularBlendChart(
                            sources = activeSources,
                            total = totalBalance,
                            fmt = fmt
                        )
                    }
                }
                item {
                    BlendChartBreakdown(
                        sources = activeSources,
                        total = totalBalance,
                        fmt = fmt
                    )
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
    val net = summary.owedToMe - summary.iOwe
    val total = summary.owedToMe + summary.iOwe
    val owedRatio = if (total > 0) (summary.owedToMe / total).toFloat() else 0.5f

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header with title and net badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TextPrimary.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        "Debt Overview",
                        style = Typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                // Net balance badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (net >= 0) GainGreen.copy(alpha = 0.12f)
                            else ExpenseRed.copy(alpha = 0.12f)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (net >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (net >= 0) GainGreen else ExpenseRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "${fmt.format(net)}",
                            style = Typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (net >= 0) GainGreen else ExpenseRed
                        )
                    }
                }
            }

            // Ratio bar - visual split between owed/owing
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(GlassBorder.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(owedRatio.coerceAtLeast(0.001f))
                            .fillMaxHeight()
                            .background(GainGreen)
                    )
                    Box(
                        modifier = Modifier
                            .weight((1f - owedRatio).coerceAtLeast(0.001f))
                            .fillMaxHeight()
                            .background(ExpenseRed)
                    )
                }
            }

            // Amounts section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DebtAmountItem(
                    label = "Owed to me",
                    amount = fmt.format(summary.owedToMe),
                    currency = "VND",
                    color = GainGreen,
                    icon = Icons.Default.CallReceived,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(16.dp))

                DebtAmountItem(
                    label = "I owe",
                    amount = fmt.format(summary.iOwe),
                    currency = "VND",
                    color = ExpenseRed,
                    icon = Icons.Default.CallMade,
                    modifier = Modifier.weight(1f),
                    alignEnd = true
                )
            }
        }
    }
}

@Composable
private fun DebtAmountItem(
    label: String,
    amount: String,
    currency: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                label,
                style = Typography.bodySmall,
                color = TextSecondary
            )
        }
        Text(
            amount,
            style = Typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            currency,
            style = Typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
    }
}

// ── Circular Blend Chart ─────────────────────────────────────────────────────
// A filled circle split into N proportional segments (one per money source),
// blended smoothly at each boundary via a multi-stop sweepGradient — no real
// blur needed, so it renders identically on every Android version.

private const val BLEND_RING_WIDTH = 2.5f // outer ring stroke, in dp

@Composable
private fun CircularBlendChart(
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
                fmt.format(total),
                style = Typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                "Total balance",
                style = Typography.labelMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(24.dp))

            val sorted = remember(sources) { sources.sortedByDescending { it.balance } }
            val fractions = remember(sorted, total) {
                sorted.map { (it.balance / total).toFloat() }
            }

            // Gap angle between segments (in degrees)
            val gapAngle = 8f

            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 24.dp.toPx()
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val colors = sorted.map { colorForType(it.type) }

                    var currentAngle = -90f // Start from top

                    sorted.forEachIndexed { index, source ->
                        val sweepAngle = fractions[index] * 360f - gapAngle

                        if (sweepAngle > 0) {
                            // Draw the arc segment
                            drawArc(
                                color = colors[index],
                                startAngle = currentAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            )
                        }

                        // Move to next segment position
                        currentAngle += fractions[index] * 360f
                    }

                    // Optional: Draw inner circle for glassy effect
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(BackgroundColorForBlendCore, Color.Transparent),
                            center = center,
                            radius = radius * 0.62f
                        ),
                        radius = radius - strokeWidth / 2,
                        center = center
                    )
                }

                // Center content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = fmt.format(total),
                        style = Typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Total balance",
                        style = Typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

// Reads as near-white on the light theme's card background, giving the
// chart's center a soft glassy falloff without introducing a new surface
// color into the design system.
private val BackgroundColorForBlendCore: Color
    get() = com.example.ui.theme.AppBackground

@Composable
private fun BlendChartBreakdown(
    sources: List<MoneySource>,
    total: Double,
    fmt: NumberFormat
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val sorted = sources.sortedByDescending { it.balance }
            sorted.forEachIndexed { index, source ->
                val accent = colorForType(source.type)
                val pct = (source.balance / total * 100).toInt()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            iconForType(source.type),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        source.name,
                        style = Typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${fmt.format(source.balance)} VND",
                            style = Typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1
                        )
                        Text(
                            "$pct%",
                            style = Typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }
                if (index != sorted.lastIndex) {
                    HorizontalDivider(
                        color = GlassBorder,
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
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