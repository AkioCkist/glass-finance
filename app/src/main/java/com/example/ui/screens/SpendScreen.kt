package com.example.ui.screens

import android.annotation.SuppressLint
import kotlinx.coroutines.delay
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
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
fun SpendScreen(viewModel: FinanceViewModel) {
    var amount by remember { mutableStateOf("0") }
    var isIncome by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var isNoteFocused by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }

    // Màu này CHỈ dùng cho dấu +/- — không áp dụng cho bất kỳ phần tử nào khác,
    // để giữ đúng tone trắng/đen/xám trung tính của toàn màn hình.
    val signColor by animateColorAsState(
        targetValue = if (isIncome) AmountIncomeColor else AmountSpendColor,
        animationSpec = tween(durationMillis = 250),
        label = "signColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(title = if (isIncome) "Income" else "Spend")
        Spacer(modifier = Modifier.height(24.dp))

        // 1. Toggle Spend / Income — pill trượt mượt, vẫn tone trung tính (đen/trắng)
        SpendIncomeToggle(
            isIncome = isIncome,
            onToggle = { isIncome = it }
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 2. Amount Entry — căn giữa, scale đồng bộ giữa số tiền và "VND"
        Box(modifier = Modifier.offset(x = shakeOffset.value.dp)) {
            AmountEntry(
                amount = amount,
                isIncome = isIncome,
                signColor = signColor
            )
        }


        Spacer(modifier = Modifier.height(8.dp))
        Text("From Wallet", style = Typography.bodyLarge, color = TextSecondary)

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Note — bấm vào là gõ luôn, kèm gợi ý chip phía dưới (giống Zalopay)
        NoteField(
            note = note,
            onNoteChange = { note = it },
            isFocused = isNoteFocused,
            onFocusChange = { isNoteFocused = it }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Confirm — tone trung tính, không phụ thuộc accent color
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
                        val finalNote = if (note.isNotBlank()) note else if (isIncome) "New Income" else "New Spend"
                        viewModel.addTransaction(parsedAmount, finalNote, isIncome = isIncome)
                        amount = "0"
                        note = ""
                        isNoteFocused = false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text("Confirm", style = Typography.bodyLarge, color = Color.White)
        }

        Spacer(modifier = Modifier.weight(1f))

        // Numeric Keypad
        NumericKeypad(
            onNumber = { num ->
                val currentDigits = if (amount == "0") 0 else amount.count { it.isDigit() }
                val newDigits = num.count { it.isDigit() }
                if (currentDigits + newDigits > 15) {
                    coroutineScope.launch {
                        // Rung biên độ nhỏ (4f-6f), tần suất cực nhanh (15ms)
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
                    // ✅ Xử lý riêng cho nút 000
                    num == "000" -> {
                        // Không cho thêm 000 nếu đã có dấu thập phân (5.5 + 000 = vô lý)
                        if (amount.contains(".")) return@NumericKeypad
                        if (amount == "0") amount = "000"
                        else amount += "000"
                    }
                    // Các số bình thường
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
        Spacer(modifier = Modifier.height(64.dp))
    }
}

// --- CÁC COMPONENT CON ---

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

        // Vị trí pill trượt mượt giữa 2 nửa, hơi nảy (bold) cho cảm giác rõ rệt
        val pillOffset by animateDpAsState(
            targetValue = if (isIncome) halfWidth else 0.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "pillOffset"
        )
        // Pill nền màu đen trung tính (giữ tone đen/trắng), chỉ trượt vị trí, không đổi màu theo Income/Spend
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

    // Đếm số chữ số thực (bỏ dấu phẩy, dấu chấm) để tính tỉ lệ scale theo độ dài.
    val digitCount = amount.count { it.isDigit() }

    // Cỡ chữ CỐ ĐỊNH (không animate trực tiếp) — phần "scale theo độ dài" và "pop khi gõ"
    // đều áp dụng bằng graphicsLayer scale, không đổi giá trị sp thật.
    val baseFontSize = 40.sp
    val minScale = 0.45f

    // Từ chữ số thứ 7 trở đi (>6 số), giảm dần 6% mỗi số thêm, có giới hạn dưới.
    val overflowDigits = (digitCount - 6).coerceAtLeast(0)
    val targetLengthScale = (1f - overflowDigits * 0.06f).coerceAtLeast(minScale)

    // Scale theo độ dài số (rẻ: chỉ ảnh hưởng layer, không remeasure)
    val lengthScale by animateFloatAsState(
        targetValue = targetLengthScale,
        animationSpec = tween(durationMillis = 180),
        label = "amountLengthScale"
    )

    // "Pop" nhẹ mỗi khi số tiền thay đổi — dùng Animatable + graphicsLayer (không AnimatedContent,
    // không animate fontSize), nên khi user spam bấm số/xóa liên tục, animation chỉ retarget giá trị
    // hiện có trên layer, không gây đo lại layout → không giật.
    val popScale = remember { Animatable(1f) }
    LaunchedEffect(formatted) {
        popScale.snapTo(0.95f)
        popScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 140)
        )
    }

    // Tổng hợp 2 hiệu ứng scale lại thành 1 giá trị duy nhất áp lên graphicsLayer
    val combinedScale = lengthScale * popScale.value

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .graphicsLayer {
                    scaleX = combinedScale
                    scaleY = combinedScale
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dấu +/- — phần tử duy nhất mang màu accent, đổi mượt theo Income/Spend.
            // Toggle Income/Spend không bị spam (chỉ 2 trạng thái, người dùng không bấm liên tục),
            // nên AnimatedContent ở đây an toàn, không phải nguồn lag.
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
                    fontSize = baseFontSize,
                    style = Typography.displayLarge,
                    color = signColor
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = formatted,
                fontSize = baseFontSize,
                lineHeight = baseFontSize,
                style = Typography.displayLarge,
                color = TextPrimary,
                maxLines = 1,
                softWrap = false
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "VND",
                fontSize = baseFontSize * 0.55f,
                lineHeight = baseFontSize * 0.55f,
                style = Typography.displayLarge,
                color = TextSecondary
            )
        }
    }
}

/**
 * Note dạng đường kẻ đứt (dashed divider) — giống đúng ảnh mẫu Zalopay: không có ô/khung bo viền,
 * chỉ là 1 dòng kẻ đứt mỏng. Bấm vào dòng đó để focus và gõ trực tiếp lên trên đường kẻ.
 * Khi đang focus, các chip gợi ý hiện ra ngay dưới, xếp wrap nhiều dòng (không cuộn ngang).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteField(
    note: String,
    onNoteChange: (String) -> Unit,
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
        // Text gõ trực tiếp, không icon, không khung — chỉ căn giữa phía trên đường kẻ đứt
        BasicTextField(
            value = note,
            onValueChange = { if (it.length <= maxLength) onNoteChange(it) },
            modifier = Modifier
                .widthIn(min = 80.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { onFocusChange(it.isFocused) },
            singleLine = true,
            textStyle = Typography.bodyLarge.copy(color = TextPrimary, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(TextPrimary),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.Center) {
                    if (note.isEmpty()) {
                        Text("Add a note", style = Typography.bodyLarge, color = TextSecondary)
                    }
                    innerTextField()
                }
            }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Đường kẻ đứt mỏng — bấm vào đây cũng mở bàn phím để gõ note, giống tap vào divider trong ảnh mẫu
        DashedDivider(
            modifier = Modifier
                .width(220.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { focusRequester.requestFocus() }
        )

        // Gợi ý hiện ngay khi note đang focus, ẩn mượt khi rời focus — wrap nhiều dòng giống ảnh mẫu
        AnimatedVisibility(
            visible = isFocused,
            enter = fadeIn(tween(150)) + expandVertically(tween(150)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(120))
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NoteSuggestions.forEach { suggestion ->
                    NoteSuggestionChip(
                        text = suggestion,
                        onClick = {
                            onNoteChange(suggestion)
                            focusManager.clearFocus()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Đường kẻ đứt mỏng vẽ bằng Canvas — đúng kiểu "- - - - -" trong ảnh mẫu, tone xám trung tính.
 */
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
    // ✅ Loại bỏ leading zeros nhưng giữ lại ít nhất 1 chữ số
    val normalized = rawAmount.trimStart('0').ifEmpty { "0" }
    val parts = normalized.split(".")
    val intPart = parts[0]
    val decPart = if (parts.size > 1) ".${parts[1]}" else ""
    val formattedInt = intPart.reversed().chunked(3).joinToString(",").reversed()
    return formattedInt + decPart
}

/**
 * Bàn phím số.
 * Nút xóa: bấm thường = xóa 1 ký tự, giữ (long-press) = xóa toàn bộ số đã gõ.
 */
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
        listOf("000", "0", "delete")   // ✅ đổi "." thành "000"
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
                        "000"    -> TripleZeroKey(onClick = { onNumber(key) })  // ✅ nút riêng cho đẹp
                        else     -> KeypadKey(key = key, onClick = { onNumber(key) })
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
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
        // ✅ Dùng fontSize nhỏ hơn để "000" vừa vặn trong nút
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

/**
 * Nút xóa: tap = xóa 1 số, giữ (long-press) = xóa toàn bộ ngay lập tức,
 * sau đó tiếp tục giữ sẽ không gây lỗi gì thêm vì amount đã về "0".
 */
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