package com.example.ui.screens

import androidx.navigation.NavController
import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalFocusManager
import kotlin.math.max
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.MoneySource
import com.example.data.MoneySourceType
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

// Chỉ dùng màu riêng cho dấu +/- (income/spend), giữ y hệt bản gốc.
// Mọi phần tử khác (pill, note, confirm...) giữ tone trắng/đen/xám trung tính, không đổi màu.
private val AmountIncomeColor = Color(0xFF4CAF50)
private val AmountSpendColor = Color.Red

// Gợi ý note hiển thị dạng chip ngay khi ô note được chọn (giống Zalopay)
private val NoteSuggestions = listOf(
    "Thanh toán", "Ăn uống", "Mua hàng", "Hàng tháng", "Học phí",
    "Hoá đơn", "Du lịch", "Đặt cọc", "Trả lương", "Trả nợ"
)

@Composable
fun SpendScreen(viewModel: FinanceViewModel,
                navController: NavController) {
    val focusManager = LocalFocusManager.current
    var amount by remember { mutableStateOf("0") }
    var isIncome by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf(TextFieldValue("")) }
    var isNoteFocused by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }

    val moneySources by viewModel.moneySources.collectAsState()
    var selectedSource by remember { mutableStateOf<MoneySource?>(null) }
    var showSourcePicker by remember { mutableStateOf(false) }

    // Auto-select Cash as default when sources load
    LaunchedEffect(moneySources) {
        if (selectedSource == null) {
            selectedSource = moneySources.find { it.type == MoneySourceType.CASH }
        }
    }

    val signColor by animateColorAsState(
        targetValue = if (isIncome) AmountIncomeColor else AmountSpendColor,
        animationSpec = tween(durationMillis = 250),
        label = "signColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(title = if (isIncome) "Income" else "Spend")
        Spacer(modifier = Modifier.height(24.dp))

        SpendIncomeToggle(
            isIncome = isIncome,
            onToggle = { isIncome = it }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Box(modifier = Modifier.offset(x = shakeOffset.value.dp)) {
            AmountEntry(
                amount = amount,
                isIncome = isIncome,
                signColor = signColor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── From Wallet — clickable source picker ──
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(GlassWhite)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showSourcePicker = true }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedSource != null) {
                        val accent = colorForType(selectedSource!!.type)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(accent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                iconForType(selectedSource!!.type),
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(GlassBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = selectedSource?.name ?: "From Wallet",
                        style = Typography.bodyLarge,
                        color = if (selectedSource != null) TextPrimary else TextSecondary,
                        fontWeight = if (selectedSource != null) FontWeight.Medium else FontWeight.Normal
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        NoteField(
            note = note,
            onNoteChange = { note = it },
            isFocused = isNoteFocused,
            onFocusChange = { isNoteFocused = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        val isConfirmEnabled = (amount.toDoubleOrNull() ?: 0.0) > 0.0
        val confirmAlpha by animateFloatAsState(
            targetValue = if (isConfirmEnabled) 1f else 0.4f,
            animationSpec = tween(200),
            label = "confirmAlpha"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(TextPrimary.copy(alpha = confirmAlpha))
                .clickable(enabled = isConfirmEnabled) {
                    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                    if (parsedAmount > 0) {
                        focusManager.clearFocus()
                        val finalNote = if (note.text.isNotBlank()) note.text else if (isIncome) "New Income" else "New Spend"
                        viewModel.addTransaction(
                            amount = parsedAmount,
                            note = finalNote,
                            isIncome = isIncome,
                            moneySourceId = selectedSource?.id
                        )
                        amount = "0"
                        note = TextFieldValue("")
                        isNoteFocused = false
                        selectedSource = null

                        navController.navigate("summary") {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text("Confirm", style = Typography.bodyLarge, color = Color.White)
        }

        Spacer(modifier = Modifier.weight(1f))

        NumericKeypad(
            onNumber = { num ->
                val currentDigits = if (amount == "0") 0 else amount.count { it.isDigit() }
                val newDigits = num.count { it.isDigit() }
                if (currentDigits + newDigits > 15) {
                    coroutineScope.launch {
                        repeat(5) {
                            shakeOffset.animateTo(4f, animationSpec = tween(15))
                            shakeOffset.animateTo(-6f, animationSpec = tween(15))
                            shakeOffset.animateTo(6f, animationSpec = tween(15))
                            shakeOffset.animateTo(-4f, animationSpec = tween(15))
                        }
                        shakeOffset.animateTo(0f, animationSpec = tween(15))
                    }
                    return@NumericKeypad
                }

                when {
                    num == "000" -> {
                        if (amount.contains(".")) return@NumericKeypad
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
        Spacer(modifier = Modifier.height(100.dp))
    }

    // ── Source picker dialog ──
    if (showSourcePicker) {
        SourcePickerDialog(
            sources = moneySources,
            onSelect = { source ->
                selectedSource = source
                showSourcePicker = false
            },
            onDismiss = { showSourcePicker = false }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  SOURCE PICKER DIALOG
// ══════════════════════════════════════════════════════════════════════════════

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
private fun SourcePickerDialog(
    sources: List<MoneySource>,
    onSelect: (MoneySource) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = { onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(24.dp)
        ) {
            Text(
                "Select Wallet",
                style = Typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (sources.isEmpty()) {
                Text(
                    "No money sources available",
                    style = Typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                sources.forEach { source ->
                    val accent = colorForType(source.type)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF7F7F7))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelect(source) }
                            .padding(vertical = 12.dp, horizontal = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Icon
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
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

                            // Name — takes remaining space, truncates if too long
                            Text(
                                source.name,
                                style = Typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            // Amount — fixed-priority, shrinks font if needed, never wraps
                            Text(
                                text = "${formatVnd(source.balance)} VND",
                                style = Typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Visible,
                                modifier = Modifier
                                    .widthIn(max = 140.dp)
                                    .basicMarquee(iterations = 0) // optional: remove if not desired
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cancel button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Cancel",
                    style = Typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// Helper: shortens long numbers (e.g. 1,234,567 -> 1.23M) to avoid layout break
private fun formatVnd(amount: Double): String {
    return when {
        amount >= 1_000_000_000 -> "%.2fB".format(amount / 1_000_000_000.0)
        amount >= 1_000_000 -> "%.2fM".format(amount / 1_000_000.0)
        else -> java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(amount)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ORIGINAL COMPONENTS (unchanged)
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Toggle Spend / Income dạng "viên thuốc" (pill) trượt mượt theo lựa chọn (spring bouncy),
 * thay cho việc chỉ đổi background tĩnh như bản cũ.
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun SpendIncomeToggle(
    isIncome: Boolean,
    onToggle: (Boolean) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.LightGray.copy(alpha = 0.2f))
    ) {
        val halfWidth = maxWidth / 2

        val pillOffset by animateDpAsState(
            targetValue = if (isIncome) halfWidth else 0.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "pillOffset"
        )
        Box(
            modifier = Modifier
                .offset(x = pillOffset)
                .width(halfWidth)
                .fillMaxHeight()
                .padding(4.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(TextPrimary)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            ToggleLabel(
                text = "Spend",
                selected = !isIncome,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggle(false) }
            )
            ToggleLabel(
                text = "Income",
                selected = isIncome,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggle(true) }
            )
        }
    }
}

@Composable
private fun ToggleLabel(text: String, selected: Boolean, modifier: Modifier = Modifier) {
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else TextSecondary,
        animationSpec = tween(250),
        label = "toggleTextColor"
    )
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Text(
            text = text,
            color = textColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/**
 * Khu vực hiển thị số tiền:
 * - Dấu +/- là phần tử DUY NHẤT đổi màu theo Income/Spend; số tiền và "VND" luôn giữ tone đen/xám trung tính.
 * - Khi gõ/xóa số, có animation "pop" nhẹ. Quan trọng: animation này chạy qua graphicsLayer (scaleX/scaleY),
 *   KHÔNG animate trực tiếp fontSize. Animate TextUnit buộc Compose remeasure lại text mỗi frame (rất tốn),
 *   đây chính là nguyên nhân lag khi spam — graphicsLayer chỉ ảnh hưởng render layer, không remeasure, nên mượt
 *   ngay cả khi bấm số liên tục.
 * - Sau khi vượt 6 chữ số, cỡ chữ giảm dần theo mỗi chữ số thêm vào, áp dụng đồng thời cho số tiền và "VND"
 *   — phần scale-theo-độ-dài này cũng đi qua graphicsLayer, không animate fontSize gốc.
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun AmountEntry(
    amount: String,
    isIncome: Boolean,
    signColor: Color
) {
    val formatted = formatAmount(amount)
    val digitCount = amount.count { it.isDigit() }

    val baseFontSize = 56f
    val minFontSize = 26f

    val overflowDigits = (digitCount - 5).coerceAtLeast(0)
    val targetFontSize = (baseFontSize - overflowDigits * 4f).coerceAtLeast(minFontSize)

    val animatedFontSize by animateFloatAsState(
        targetValue = targetFontSize,
        animationSpec = tween(durationMillis = 180),
        label = "amountFontSize"
    )
    val fontSize = animatedFontSize.sp
    val vndFontSize = (animatedFontSize * 0.55f).sp

    val popScale = remember { Animatable(1f) }
    LaunchedEffect(formatted) {
        popScale.snapTo(0.95f)
        popScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 140)
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = popScale.value
                    scaleY = popScale.value
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = isIncome,
                transitionSpec = {
                    (fadeIn(tween(200)) + scaleIn(initialScale = 0.6f, animationSpec = tween(200))) togetherWith
                            (fadeOut(tween(150)) + scaleOut(targetScale = 0.6f, animationSpec = tween(150)))
                },
                label = "signSwitch"
            ) { income ->
                Text(
                    text = if (income) "+" else "-",
                    fontSize = fontSize,
                    lineHeight = fontSize,
                    style = Typography.displayLarge,
                    color = signColor,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = formatted,
                fontSize = fontSize,
                lineHeight = fontSize,
                style = Typography.displayLarge,
                color = TextPrimary,
                maxLines = 1,
                softWrap = false
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "VND",
                fontSize = vndFontSize,
                lineHeight = vndFontSize,
                style = Typography.displayLarge,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}

/**
 * Note dạng đường kẻ đứt (dashed divider) — giống đúng ảnh mẫu Zalopay: không có ô/khung bo viền,
 * chỉ là 1 dòng kẻ đứt mỏng. Bấm vào dòng đó để focus và gõ trực tiếp lên trên đường kẻ.
 * Khi đang focus, các chip gợi ý hiện ra ngay dưới, xếp wrap nhiều dòng (không cuộn ngang).
 */
@Composable
private fun NoteField(
    note: TextFieldValue,
    onNoteChange: (TextFieldValue) -> Unit,
    isFocused: Boolean,
    onFocusChange: (Boolean) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val maxLength = 60

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicTextField(
            value = note,
            onValueChange = { if (it.text.length <= maxLength) onNoteChange(it) },
            modifier = Modifier
                .width(220.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { onFocusChange(it.isFocused) },
            singleLine = true,
            textStyle = Typography.bodyLarge.copy(color = TextPrimary, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(TextPrimary),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.Center) {
                    if (note.text.isEmpty()) {
                        Text("Add a note", style = Typography.bodyLarge, color = TextSecondary)
                    }
                    innerTextField()
                }
            }
        )

        Spacer(modifier = Modifier.height(6.dp))

        DashedDivider(
            modifier = Modifier
                .width(220.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { focusRequester.requestFocus() }
        )

        AnimatedVisibility(
            visible = isFocused,
            enter = fadeIn(tween(150)) + expandVertically(tween(150)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(120))
        ) {
            SimpleFlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalSpacing = 10.dp,
                verticalSpacing = 10.dp
            ) {
                NoteSuggestions.forEach { suggestion ->
                    NoteSuggestionChip(
                        text = suggestion,
                        onClick = {
                            onNoteChange(TextFieldValue(suggestion, TextRange(suggestion.length)))
                            focusManager.clearFocus()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SimpleFlowRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 0.dp,
    verticalSpacing: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val hSpacing = horizontalSpacing.roundToPx()
        val vSpacing = verticalSpacing.roundToPx()

        val rows = mutableListOf<List<Placeable>>()
        val rowHeights = mutableListOf<Int>()
        val rowWidths = mutableListOf<Int>()

        var currentRow = mutableListOf<Placeable>()
        var currentRowWidth = 0
        var currentRowHeight = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
            val widthWithSpacing = placeable.width + if (currentRow.isEmpty()) 0 else hSpacing

            if (currentRowWidth + widthWithSpacing > constraints.maxWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                rowHeights.add(currentRowHeight)
                rowWidths.add(currentRowWidth)

                currentRow = mutableListOf()
                currentRowWidth = 0
                currentRowHeight = 0
            }

            if (currentRow.isNotEmpty()) {
                currentRowWidth += hSpacing
            }
            currentRow.add(placeable)
            currentRowWidth += placeable.width
            currentRowHeight = max(currentRowHeight, placeable.height)
        }

        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
            rowHeights.add(currentRowHeight)
            rowWidths.add(currentRowWidth)
        }

        val totalHeight = rowHeights.sum() + (if (rows.size > 1) (rows.size - 1) * vSpacing else 0)

        layout(constraints.maxWidth, totalHeight) {
            var y = 0
            rows.forEachIndexed { index, row ->
                // Center horizontally
                var x = (constraints.maxWidth - rowWidths[index]) / 2
                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + hSpacing
                }
                y += rowHeights[index] + vSpacing
            }
        }
    }
}

@Composable
private fun DashedDivider(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.height(1.dp)) {
        val dashPx = 6.dp.toPx()
        val gapPx = 5.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = TextSecondary.copy(alpha = 0.5f),
                start = Offset(x, 0f),
                end = Offset((x + dashPx).coerceAtMost(size.width), 0f),
                strokeWidth = 2.dp.toPx()
            )
            x += dashPx + gapPx
        }
    }
}

@Composable
private fun NoteSuggestionChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(TextSecondary.copy(alpha = 0.08f))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(text, style = Typography.bodyMedium, color = TextPrimary)
    }
}

private fun formatAmount(rawAmount: String): String {
    if (rawAmount.isEmpty() || rawAmount == "0") return "0"
    val normalized = rawAmount.trimStart('0').ifEmpty { "0" }
    val parts = normalized.split(".")
    val intPart = parts[0]
    val decPart = if (parts.size > 1) ".${parts[1]}" else ""
    val formattedInt = intPart.reversed().chunked(3).joinToString(",").reversed()
    return formattedInt + decPart
}
