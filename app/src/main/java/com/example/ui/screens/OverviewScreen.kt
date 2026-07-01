package com.example.ui.screens

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.atan2
import kotlin.math.sqrt

enum class SortOrder {
    NAME_ASC,
    NAME_DESC,
    BALANCE_ASC,
    BALANCE_DESC
}

@Composable
fun OverviewScreen(
    viewModel: FinanceViewModel,
    onNavigateToSavings: () -> Unit = {}
) {
    val totalBalance by viewModel.totalBalance.collectAsState()
    val allSources by viewModel.moneySources.collectAsState()
    val debtSummary by viewModel.debtSummary.collectAsState()

    // State cho menu và filter
    var showMenu by remember { mutableStateOf(false) }
    var hideZeroBalance by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(SortOrder.NAME_ASC) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    // Filter và sort sources
    val filteredSources = remember(allSources, hideZeroBalance) {
        if (hideZeroBalance) {
            allSources.filter { it.balance > 0 }
        } else {
            allSources
        }
    }

    val sortedSources = remember(filteredSources, sortOrder) {
        when (sortOrder) {
            SortOrder.NAME_ASC -> filteredSources.sortedBy { it.name.lowercase() }
            SortOrder.NAME_DESC -> filteredSources.sortedByDescending { it.name.lowercase() }
            SortOrder.BALANCE_ASC -> filteredSources.sortedBy { it.balance }
            SortOrder.BALANCE_DESC -> filteredSources.sortedByDescending { it.balance }
        }
    }

    val fmt = NumberFormat.getNumberInstance(Locale.US)
    val formattedBalance = fmt.format(totalBalance)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header với menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Overview",
                    style = Typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // Sử dụng Box để menu xuất hiện đúng vị trí
                Box {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = TextPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(GlassBackground)
                            .clip(RoundedCornerShape(12.dp))
                            .width(260.dp)
                    ) {
                        // Sort options
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Name A-Z")
                                    }
                                    if (sortOrder == SortOrder.NAME_ASC) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            onClick = {
                                sortOrder = SortOrder.NAME_ASC
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Name Z-A")
                                    }
                                    if (sortOrder == SortOrder.NAME_DESC) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            onClick = {
                                sortOrder = SortOrder.NAME_DESC
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Balance ↑")
                                    }
                                    if (sortOrder == SortOrder.BALANCE_ASC) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            onClick = {
                                sortOrder = SortOrder.BALANCE_ASC
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.TrendingDown, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Balance ↓")
                                    }
                                    if (sortOrder == SortOrder.BALANCE_DESC) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            onClick = {
                                sortOrder = SortOrder.BALANCE_DESC
                                showMenu = false
                            }
                        )

                        HorizontalDivider(color = GlassBorder, thickness = 1.dp)

                        // Show/Hide zero balance
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (hideZeroBalance) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(if (hideZeroBalance) "Show empty" else "Hide empty")
                                    }
                                    if (hideZeroBalance) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            onClick = {
                                hideZeroBalance = !hideZeroBalance
                                showMenu = false
                            }
                        )

                        HorizontalDivider(color = GlassBorder, thickness = 1.dp)

                        // Export CSV
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Export CSV")
                                }
                            },
                            onClick = {
                                viewModel.exportCSV(sortedSources, totalBalance)
                                showMenu = false
                            }
                        )

                        HorizontalDivider(color = GlassBorder, thickness = 1.dp)

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Savings goals")
                                }
                            },
                            onClick = {
                                showMenu = false
                                onNavigateToSavings()
                            }
                        )

                        HorizontalDivider(color = GlassBorder, thickness = 1.dp)

                        // Reset all app data
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Clear all data")
                                }
                            },
                            onClick = {
                                showMenu = false
                                showClearDataDialog = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            BalanceSection(title = "Total Balance", value = formattedBalance)
            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // ── DISTRIBUTION BLEND CHART ──
                if (totalBalance > 0 && sortedSources.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularBlendChart(
                                sources = sortedSources,
                                total = totalBalance,
                                fmt = fmt
                            )
                        }
                    }
                    item {
                        BlendChartBreakdown(
                            sources = sortedSources,
                            total = totalBalance,
                            fmt = fmt
                        )
                    }
                }

                // ── SECTION HEADER ──
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "My Money Sources",
                            style = Typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            "${sortedSources.size} sources",
                            style = Typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                // Source cards — 2 per row
                val chunked = sortedSources.chunked(2)
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

                if (sortedSources.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (hideZeroBalance) "No money sources with balance" else "No money sources yet",
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

        if (showClearDataDialog) {
            AlertDialog(
                onDismissRequest = { showClearDataDialog = false },
                title = { Text("Clear all data?") },
                text = {
                    Text("This will delete all transactions, money sources, and debts. App data will be reset like a fresh install.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.resetAllData()
                            showClearDataDialog = false
                        }
                    ) {
                        Text("Clear")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDataDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
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
private const val BLEND_RING_WIDTH = 2.5f

@Composable
private fun CircularBlendChart(
    sources: List<MoneySource>,
    total: Double,
    fmt: NumberFormat
) {
    // Lưu index phân vùng đang được click chọn (-1 là trạng thái ban đầu)
    var selectedIndex by remember { mutableIntStateOf(-1) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            val sorted = remember(sources) { sources.sortedByDescending { it.balance } }
            val fractions = remember(sorted, total) {
                sorted.map { (it.balance / total).toFloat() }
            }

            val gapAngle = 8f

            Box(
                modifier = Modifier
                    .size(200.dp)
                    // pointerInput xử lý bắt tọa độ chạm tương tác phân vùng biểu đồ tròn
                    .pointerInput(sorted, total) {
                        detectTapGestures { offset ->
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val deltaX = offset.x - centerX
                            val deltaY = offset.y - centerY
                            val distance = sqrt(deltaX * deltaX + deltaY * deltaY)

                            val outerRadius = size.width / 2f
                            val strokeWidthPx = 24.dp.toPx()
                            val innerRadius = outerRadius - strokeWidthPx

                            // Kiểm tra xem vị trí click có nằm trên vành của biểu đồ tròn không
                            if (distance in innerRadius..outerRadius) {
                                var angle = Math.toDegrees(atan2(deltaY.toDouble(), deltaX.toDouble())).toFloat()
                                if (angle < 0) angle += 360f

                                // Đổi hệ trục tọa độ khớp với điểm vẽ xuất phát -90 độ (đỉnh trên cùng)
                                var shiftedAngle = angle + 90f
                                if (shiftedAngle >= 360f) shiftedAngle -= 360f

                                var currentAngle = 0f
                                var clickedIndex = -1

                                for (i in sorted.indices) {
                                    val sweepAngle = fractions[i] * 360f
                                    if (shiftedAngle >= currentAngle && shiftedAngle < currentAngle + sweepAngle) {
                                        clickedIndex = i
                                        break
                                    }
                                    currentAngle += sweepAngle
                                }
                                // Click lại vào cung đang chọn thì hủy hover, trả về mặc định
                                selectedIndex = if (selectedIndex == clickedIndex) -1 else clickedIndex
                            } else {
                                selectedIndex = -1
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 24.dp.toPx()
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val colors = sorted.map { colorForType(it.type) }

                    var currentAngle = -90f

                    sorted.forEachIndexed { index, source ->
                        val sweepAngle = fractions[index] * 360f - gapAngle
                        val isSelected = selectedIndex == index
                        val hasSelection = selectedIndex != -1

                        // Phân vùng không được chọn sẽ mờ đi (alpha = 0.2f), phân vùng được chọn giữ nguyên 1.0f
                        val alpha = if (hasSelection && !isSelected) 0.2f else 1f
                        // Phân vùng được chọn nhô dày lên nổi bật hơn (+6.dp)
                        val animatedStrokeWidth = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth

                        if (sweepAngle > 0) {
                            drawArc(
                                color = colors[index],
                                startAngle = currentAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(
                                    width = animatedStrokeWidth,
                                    cap = StrokeCap.Round
                                ),
                                alpha = alpha
                            )
                        }
                        currentAngle += fractions[index] * 360f
                    }

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

                // Nội dung text ở tâm vòng tròn cập nhật động theo trạng thái tương tác
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    if (selectedIndex != -1) {
                        val selectedSource = sorted[selectedIndex]
                        Text(
                            text = "${(fractions[selectedIndex] * 100).toInt()}%",
                            style = Typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorForType(selectedSource.type),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = selectedSource.name,
                            style = Typography.labelMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Text(
                            text = "${fmt.format(selectedSource.balance)} VND",
                            style = Typography.labelSmall,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    } else {
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
}

// Reads as near-white on the light theme's card background, giving the
// chart's center a soft glassy falloff without introducing a new surface
// color into the design system.
private val BackgroundColorForBlendCore: Color
    get() = AppBackground

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