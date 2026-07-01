package com.example.ui.components

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.InsertChartOutlined
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.DebtDirection
import com.example.ui.theme.*

import androidx.compose.ui.tooling.preview.Preview
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
fun TopAppBar(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = TextPrimary)
        Text(
            text = title,
            style = Typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(GlassWhite)
                .border(1.dp, GlassBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary)
        }
    }
}

// Hàm format tiền tệ với superscript
fun formatCurrencyWithSuperscript(
    amount: String,
    currency: String = "VND",
    amountSize: TextUnit = 32.sp,
    currencySize: TextUnit = 16.sp
): AnnotatedString {
    return buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                fontSize = amountSize,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        ) {
            append(amount)
        }
        withStyle(
            style = SpanStyle(
                fontSize = currencySize,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                baselineShift = BaselineShift.Superscript
            )
        ) {
            append(" $currency")
        }
    }
}

@Composable
fun CurrencyText(
    amount: String,
    currency: String = "VND",
    amountSize: TextUnit = 32.sp,
    currencySize: TextUnit = 16.sp,
    color: Color = TextPrimary,
    modifier: Modifier = Modifier
) {
    Text(
        text = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    fontSize = amountSize,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            ) {
                append(amount)
            }
            withStyle(
                style = SpanStyle(
                    fontSize = currencySize,
                    fontWeight = FontWeight.Medium,
                    color = color,
                    baselineShift = BaselineShift.Superscript
                )
            ) {
                append(" $currency")
            }
        },
        modifier = modifier
    )
}


@Composable
fun BalanceSection(
    title: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = Typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                ) {
                    append(value)
                }
                withStyle(
                    style = SpanStyle(
                        fontSize = 24.sp, // Nhỏ hơn
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        baselineShift = BaselineShift.Superscript // 👈 Đẩy lên trên
                    )
                ) {
                    append(" VND")
                }
            },
            style = Typography.headlineLarge
        )
    }
}


@Composable
fun ChartSection(mainColor: Color) {
    val heights = listOf(30, 40, 35, 50, 70, 45, 90, 60, 100, 120, 50, 70, 80, 40, 20, 45, 30, 50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEachIndexed { index, height ->
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(height.dp)
                    .clip(CircleShape)
                    .background(if (index == 9) mainColor else GlassBorder)
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val times = listOf("1D", "1W", "1M", "6M", "1Y")
        times.forEach { time ->
            Text(
                text = time,
                style = Typography.labelMedium,
                color = if (time == "1W") mainColor else TextSecondary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun WarningsBanner() {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PrimaryVibrant.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Savings, contentDescription = null, tint = PrimaryVibrant)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Savings goal on track",
                    style = Typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}

@Composable
fun BentoGridSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BentoCard(title = "Cash", amount = "VND 1.2M", icon = Icons.Default.AccountBalanceWallet)
            BentoCard(title = "Earnings", amount = "VND 500K", icon = Icons.Default.TrendingUp)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BentoCard(title = "Investments", amount = "VND 8.5M", icon = Icons.Default.PieChart)
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp),
                borderDashed = true
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GlassWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "New Card", style = Typography.labelMedium, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun BentoCard(title: String, amount: String, icon: ImageVector) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(text = title, style = Typography.labelMedium, color = TextSecondary)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(AppBackground), // subtle contrast
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                }
            }
            Text(text = amount, style = Typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderDashed: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(GlassWhite)
            .border(
                1.dp,
                if (borderDashed) GlassBorder else GlassBorder.copy(alpha = 0.5f),
                RoundedCornerShape(24.dp)
            )
    ) {
        content()
    }
}

@Composable
fun FloatingBottomNav(
    currentRouteIndex: Int,
    backdropState: Backdrop,
    onNavigate: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .width(300.dp)
            .height(64.dp)
    ) {
        LiquidBottomTabs(
            selectedTabIndex = { currentRouteIndex },
            onTabSelected = { index -> onNavigate(index) },
            backdropState = backdropState,
            tabsCount = 5,
            modifier = Modifier.fillMaxSize()
        ) {
            NavIcon(
                icon = Icons.Default.Home,
                isActive = currentRouteIndex == 0,
                onClick = { onNavigate(0) }
            )
            NavIcon(
                icon = Icons.Outlined.AttachMoney,
                isActive = currentRouteIndex == 1,
                onClick = { onNavigate(1) }
            )
            NavIcon(
                icon = Icons.Outlined.InsertChartOutlined,
                isActive = currentRouteIndex == 2,
                onClick = { onNavigate(2) }
            )
            NavIcon(
                icon = Icons.Default.AccountBalanceWallet,
                isActive = currentRouteIndex == 3,
                onClick = { onNavigate(3) }
            )
            NavIcon(
                icon = Icons.Default.Savings,
                isActive = currentRouteIndex == 4,
                onClick = { onNavigate(4) }
            )
        }
    }
}

@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (Int) -> Unit,
    backdropState: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            // Dùng đoạn code drawBackdrop chính xác từ mẫu của bạn
            .drawBackdrop(
                backdrop = backdropState,
                shape = { CircleShape }, // Đổi sang CircleShape giống code của bạn
                effects = {
                    vibrancy()
                    blur(4f.dp.toPx())
                    lens(16f.dp.toPx(), 32f.dp.toPx())
                },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) }
            )
            // Vì dùng CircleShape ở trên nên border và clip cũng phải dùng CircleShape để đồng bộ viền
            .clip(CircleShape)
            .border(1.dp, GlassBorder, CircleShape)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            content = content
        )
    }
}

@Composable
fun DirectionLabel(direction: DebtDirection) {
    val (bg, fg, label) = when (direction) {
        DebtDirection.OWED_TO_ME -> Triple(GainGreen.copy(alpha = 0.1f), GainGreen, "They owe me")
        DebtDirection.I_OWE -> Triple(SecondaryVibrant.copy(alpha = 0.1f), SecondaryVibrant, "I owe them")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun NavIcon(icon: ImageVector, isActive: Boolean, onClick: () -> Unit) {
    // Animate background color smoothly instead of snapping instantly
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) PrimaryVibrant else Color.Transparent,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "navIconBg"
    )
    val tintColor by animateColorAsState(
        targetValue = if (isActive) Color.White else TextSecondary,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "navIconTint"
    )
    // Small "pop" scale when the tab becomes active, using a snappy spring (cheap, no lag)
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "navIconScale"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(50.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // avoid extra ripple layer competing with our own animation
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tintColor
        )
    }
}

// ── Numeric Keypad Components ─────────────────────────────────────────────────

fun formatAmountWithCommas(rawAmount: String): String {
    if (rawAmount.isEmpty() || rawAmount == "0") return "0"
    val normalized = rawAmount.trimStart('0').ifEmpty { "0" }
    return normalized.reversed().chunked(3).joinToString(",").reversed()
}

@Composable
fun NumericKeypad(
    onNumber: (String) -> Unit,
    onDelete: () -> Unit,
    onDeleteAll: () -> Unit
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("000", "0", "delete")
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    when (key) {
                        "delete" -> DeleteKey(onDelete = onDelete, onDeleteAll = onDeleteAll)
                        "000"    -> TripleZeroKey(onClick = { onNumber(key) })
                        else     -> KeypadKey(key = key, onClick = { onNumber(key) })
                    }
                }
            }
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun TripleZeroKey(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "tripleZeroScale"
    )
    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(pressScale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "000",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

@Composable
private fun KeypadKey(key: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "keyScale"
    )
    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(pressScale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(key, style = Typography.headlineMedium, color = TextPrimary)
    }
}

@Composable
private fun DeleteKey(onDelete: () -> Unit, onDeleteAll: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "deleteKeyScale"
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(pressScale)
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(onDelete, onDeleteAll) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                        }
                    },
                    onTap = { onDelete() },
                    onLongPress = { onDeleteAll() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = TextPrimary)
    }
}

@Composable
fun KeypadDialog(
    title: String = "Enter Amount",
    initialAmount: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var amount by remember { mutableStateOf(initialAmount) }

    Dialog(onDismissRequest = { onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(AppBackground)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "${formatAmountWithCommas(amount)} VND",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            NumericKeypad(
                onNumber = { num ->
                    when {
                        num == "000" -> {
                            if (amount == "0") amount = "000"
                            else amount += "000"
                        }
                        else -> {
                            if (amount == "0") amount = num
                            else amount += num
                        }
                    }
                },
                onDelete = {
                    if (amount.length > 1) amount = amount.dropLast(1) else amount = "0"
                },
                onDeleteAll = {
                    amount = "0"
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassBorder)
                        .clickable { onDismiss() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cancel", color = TextSecondary, fontWeight = FontWeight.Medium)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(TextPrimary)
                        .clickable { onConfirm(amount) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Confirm", color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FloatingBottomNavPreview() {
    val backdropState = rememberLayerBackdrop {
        drawRect(Color.White)
        drawContent()
    }
    MyApplicationTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(AppBackground),
            contentAlignment = Alignment.Center
        ) {
            FloatingBottomNav(
                currentRouteIndex = 0,
                backdropState = backdropState,
                onNavigate = {}
            )
        }
    }
}